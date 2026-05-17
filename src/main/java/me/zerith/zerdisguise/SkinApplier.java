package me.zerith.zerdisguise;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Aplica y restaura skins y nameplate a jugadores.
 *
 * Skin: usa la API de Paper (com.destroystokyo.paper.profile.PlayerProfile +
 *   ProfileProperty). Esto es más fiable que reflexión NMS y funciona en
 *   Paper 1.20-1.21+.
 *   - El value y signature de Mojang se usan directamente.
 *   - Paper envía los paquetes PlayerInfo + respawn a todos los jugadores en línea
 *     automáticamente al llamar player.setPlayerProfile().
 *   - El perfil original se cachea para restaurarlo al quitar el disfraz.
 *
 * Nameplate: scoreboard Team de Bukkit para el prefijo visible sobre la cabeza.
 */
public class SkinApplier {

    private static final String TEXTURES = "textures";

    /** Perfiles originales antes de aplicar el disfraz (por UUID del jugador). */
    private final Map<UUID, PlayerProfile> originals = new HashMap<>();

    private final ZerDisguise plugin;

    public SkinApplier(ZerDisguise plugin) {
        this.plugin = plugin;
    }

    // ──────────────────────────────────────────────────────────────
    //  Skin
    // ──────────────────────────────────────────────────────────────

    /**
     * Aplica la skin del {@link SkinFetcher.SkinData} al jugador.
     * Guarda el perfil original si aún no estaba guardado (primera vez).
     *
     * @return true si la skin se aplicó correctamente.
     */
    public boolean applySkin(Player player, SkinFetcher.SkinData skin) {
        try {
            // Guardar perfil original solo la primera vez (antes de cualquier disfraz)
            originals.computeIfAbsent(player.getUniqueId(), k -> player.getPlayerProfile());

            // Modificar la propiedad "textures" del perfil actual del jugador
            PlayerProfile profile = player.getPlayerProfile();
            profile.removeProperty(TEXTURES);
            profile.setProperty(new ProfileProperty(TEXTURES, skin.value(), skin.signature()));

            // Paper envía los paquetes necesarios a todos los jugadores automáticamente
            player.setPlayerProfile(profile);
            return true;

        } catch (Exception e) {
            plugin.getLogger().warning("[SkinApplier] Error al aplicar skin a "
                    + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Restaura la skin original del jugador.
     * Si no había perfil guardado, crea uno limpio (el servidor lo recarga desde Mojang).
     */
    public void removeSkin(Player player) {
        try {
            PlayerProfile original = originals.remove(player.getUniqueId());
            if (original == null) {
                original = Bukkit.createProfile(player.getUniqueId(), player.getName());
            }
            player.setPlayerProfile(original);
        } catch (Exception e) {
            plugin.getLogger().warning("[SkinApplier] Error al restaurar skin de "
                    + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Limpia la entrada del jugador en el caché de perfiles originales sin restaurar
     * la skin (útil cuando el jugador se desconecta y ya no está online).
     */
    public void cleanupPlayer(UUID uuid) {
        originals.remove(uuid);
    }

    // ──────────────────────────────────────────────────────────────
    //  Nameplate (prefijo visible sobre la cabeza)
    // ──────────────────────────────────────────────────────────────

    public void applyNameplate(Player player, String rankPrefix) {
        Scoreboard board    = Bukkit.getScoreboardManager().getMainScoreboard();
        String     teamName = safeTeamName(player);

        Team team = board.getTeam(teamName);
        if (team == null) team = board.registerNewTeam(teamName);

        ConfigManager cfg    = plugin.getConfigManager();
        String        prefix = (rankPrefix == null || rankPrefix.isBlank()) ? "" : rankPrefix + " ";

        team.prefix(cfg.component(prefix));
        team.suffix(cfg.component(""));
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        team.setOption(Team.Option.COLLISION_RULE,      Team.OptionStatus.ALWAYS);

        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }
    }

    public void removeNameplate(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team       team  = board.getTeam(safeTeamName(player));
        if (team != null) team.unregister();
    }

    // ──────────────────────────────────────────────────────────────
    //  Helpers
    // ──────────────────────────────────────────────────────────────

    private static String safeTeamName(Player player) {
        String name = "zd_" + player.getName();
        return name.length() > 16 ? name.substring(0, 16) : name;
    }
}
