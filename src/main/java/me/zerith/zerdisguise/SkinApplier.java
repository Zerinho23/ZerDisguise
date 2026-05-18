package me.zerith.zerdisguise;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Aplica y restaura skins y nameplates a jugadores.
 *
 * SKIN
 * ────
 * player.setPlayerProfile() actualiza el perfil en la tab-list PERO no envía
 * el paquete de spawn de la entidad a los clientes de otros jugadores —
 * por eso la skin no cambia visualmente para los demás.
 * Solución: ciclo hidePlayer → showPlayer para forzar el re-spawn con la
 * nueva skin en todos los observadores.
 * Delay aumentado a 10 ticks para que el cliente procese el "Remove Entity"
 * antes de recibir el nuevo "Spawn Entity".
 *
 * NAMEPLATE (TAB, NameTagEdit, CMI, etc.)
 * ────────────────────────────────────────
 * Los plugins de tab/scoreboard gestionan scoreboards POR JUGADOR (no el
 * mainScoreboard), por lo que aplicar el team solo en el mainScoreboard es
 * insuficiente. Solución:
 *   1. applyNameplate() aplica el team en el mainScoreboard Y en el
 *      scoreboard personal de cada observador en línea.
 *   2. Una tarea de 2 ticks (antes 5) re-aplica si otro plugin remueve
 *      al jugador de nuestro team o sobreescribe el scoreboard.
 */
public class SkinApplier {

    private static final String TEXTURES = "textures";

    /** Ticks que esperamos antes de re-mostrar al jugador tras hidePlayer. */
    private static final long SHOW_DELAY_TICKS = 10L;

    /** Intervalo de la tarea de vigilancia del nameplate (ticks). */
    private static final long NAMEPLATE_TASK_PERIOD = 2L;

    private final Map<UUID, PlayerProfile> originals      = new HashMap<>();
    private final Map<UUID, BukkitTask>    nameplateTasks = new HashMap<>();

    private final ZerDisguise plugin;

    public SkinApplier(ZerDisguise plugin) {
        this.plugin = plugin;
    }

    // ── Skin ──────────────────────────────────────────────────────────────

    public boolean applySkin(Player player, SkinFetcher.SkinData skin) {
        try {
            originals.computeIfAbsent(player.getUniqueId(), k -> player.getPlayerProfile());
            PlayerProfile profile = player.getPlayerProfile();
            profile.removeProperty(TEXTURES);
            profile.setProperty(new ProfileProperty(TEXTURES, skin.value(), skin.signature()));
            player.setPlayerProfile(profile);
            refreshForObservers(player);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("[SkinApplier] Error al aplicar skin a "
                    + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    public void removeSkin(Player player) {
        try {
            PlayerProfile original = originals.remove(player.getUniqueId());
            if (original == null)
                original = Bukkit.createProfile(player.getUniqueId(), player.getName());
            player.setPlayerProfile(original);
            refreshForObservers(player);
        } catch (Exception e) {
            plugin.getLogger().warning("[SkinApplier] Error al restaurar skin de "
                    + player.getName() + ": " + e.getMessage());
        }
    }

    public void cleanupPlayer(UUID uuid) {
        originals.remove(uuid);
    }

    /**
     * Fuerza a todos los observadores a recibir los paquetes de spawn
     * del jugador con el perfil/skin actualizado.
     *
     * hidePlayer → "Remove Entity" en el cliente del observador.
     * Tras SHOW_DELAY_TICKS → "Add Player" + "Spawn Entity" con el nuevo perfil.
     *
     * Se procesan en lotes de 5 para evitar picos de paquetes en servidores
     * con muchos jugadores.
     */
    private void refreshForObservers(Player player) {
        List<Player> observers = Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.equals(player))
                .collect(Collectors.toList());
        if (observers.isEmpty()) return;

        for (Player obs : observers) obs.hidePlayer(plugin, player);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player obs : observers) {
                if (obs.isOnline()) obs.showPlayer(plugin, player);
            }
        }, SHOW_DELAY_TICKS);
    }

    // ── Nameplate ─────────────────────────────────────────────────────────

    /**
     * Aplica el prefijo de rango en nametag y scoreboard.
     *
     * Aplica en:
     *  - El mainScoreboard (base).
     *  - El scoreboard personal de cada jugador en línea (para TAB, NameTagEdit, CMI, etc.).
     *
     * Luego inicia la tarea de persistencia que re-aplica si otro plugin lo sobreescribe.
     */
    public void applyNameplate(Player player, String rankPrefix) {
        applyNameplateNow(player, rankPrefix);
        startNameplateTask(player, rankPrefix);
    }

    private void applyNameplateNow(Player player, String rankPrefix) {
        Collection<? extends Player> online = Bukkit.getOnlinePlayers();

        applyOnScoreboard(player, rankPrefix, Bukkit.getScoreboardManager().getMainScoreboard());

        for (Player obs : online) {
            Scoreboard obsBoard = obs.getScoreboard();
            if (obsBoard != null && !obsBoard.equals(Bukkit.getScoreboardManager().getMainScoreboard())) {
                applyOnScoreboard(player, rankPrefix, obsBoard);
            }
        }
    }

    /**
     * Registra (o actualiza) el team de ZerDisguise en el scoreboard indicado
     * y asegura que el jugador esté solo en ese team (removiéndolo de los demás).
     */
    private void applyOnScoreboard(Player player, String rankPrefix, Scoreboard board) {
        if (board == null) return;

        String teamName = safeTeamName(player);

        for (Team t : board.getTeams()) {
            if (!t.getName().equals(teamName) && t.hasEntry(player.getName())) {
                t.removeEntry(player.getName());
            }
        }

        Team team = board.getTeam(teamName);
        if (team == null) team = board.registerNewTeam(teamName);

        ConfigManager cfg    = plugin.getConfigManager();
        String        prefix = (rankPrefix == null || rankPrefix.isBlank()) ? "" : rankPrefix + " ";

        team.prefix(cfg.component(prefix));
        team.suffix(cfg.component(""));
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        team.setOption(Team.Option.COLLISION_RULE,      Team.OptionStatus.ALWAYS);
        if (!team.hasEntry(player.getName())) team.addEntry(player.getName());
    }

    /**
     * Tarea de NAMEPLATE_TASK_PERIOD ticks.
     * Re-aplica en todos los scoreboards si detecta que el jugador
     * fue removido de nuestro team (por TAB, NameTagEdit u otro plugin).
     */
    private void startNameplateTask(Player player, String rankPrefix) {
        stopNameplateTask(player.getUniqueId());
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) { stopNameplateTask(player.getUniqueId()); return; }

            boolean needsReapply = false;

            Scoreboard main    = Bukkit.getScoreboardManager().getMainScoreboard();
            Team       ourTeam = main.getTeam(safeTeamName(player));
            if (ourTeam == null || !ourTeam.hasEntry(player.getName())) {
                needsReapply = true;
            }

            if (!needsReapply) {
                for (Player obs : Bukkit.getOnlinePlayers()) {
                    Scoreboard obsBoard = obs.getScoreboard();
                    if (obsBoard == null || obsBoard.equals(main)) continue;
                    Team t = obsBoard.getTeam(safeTeamName(player));
                    if (t == null || !t.hasEntry(player.getName())) {
                        needsReapply = true;
                        break;
                    }
                }
            }

            if (needsReapply) applyNameplateNow(player, rankPrefix);

        }, 10L, NAMEPLATE_TASK_PERIOD);
        nameplateTasks.put(player.getUniqueId(), task);
    }

    private void stopNameplateTask(UUID uuid) {
        BukkitTask task = nameplateTasks.remove(uuid);
        if (task != null) task.cancel();
    }

    public void removeNameplate(Player player) {
        stopNameplateTask(player.getUniqueId());

        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        Team mainTeam = main.getTeam(safeTeamName(player));
        if (mainTeam != null) mainTeam.unregister();

        for (Player obs : Bukkit.getOnlinePlayers()) {
            Scoreboard obsBoard = obs.getScoreboard();
            if (obsBoard == null || obsBoard.equals(main)) continue;
            Team t = obsBoard.getTeam(safeTeamName(player));
            if (t != null) {
                try { t.unregister(); } catch (Exception ignored) {}
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Genera un nombre de team único por jugador.
     * Usa los primeros 8 chars del UUID (sin guiones) para garantizar unicidad
     * incluso con nombres largos. Máximo 16 chars (límite del scoreboard API).
     */
    private static String safeTeamName(Player player) {
        String uuidPart = player.getUniqueId().toString().replace("-", "").substring(0, 8);
        return "zd_" + uuidPart;
    }
}
