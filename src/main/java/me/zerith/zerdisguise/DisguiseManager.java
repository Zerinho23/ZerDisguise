package me.zerith.zerdisguise;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gestiona los disfraces activos y anteriores de cada jugador.
 *
 * Modos:
 *  - Disfraz completo: cambia skin, nombre, rango visual Y prefijo en LP/Vault.
 *  - Rango visual:     solo cambia el prefijo visible (nameplate + displayName + LP/Vault prefix),
 *                      sin cambiar la skin ni otorgar permisos reales.
 *
 * El prefijo en LuckPerms/Vault se sobreescribe con un nodo de prioridad 9999
 * para que los plugins de chat (EssentialsChat, LuckPerms chat format, TAB, etc.)
 * lean el prefijo del disfraz en vez del real — evitando que aparezcan dos rangos.
 *
 * El disfraz persiste tras la muerte — solo se elimina con /undisguise.
 */
public class DisguiseManager {

    public record DisguiseData(String disguiseName, String rankId) {}

    private final Map<UUID, DisguiseData> current         = new HashMap<>();
    private final Map<UUID, DisguiseData> previous        = new HashMap<>();
    private final Map<UUID, String>       visualRankOnly  = new HashMap<>();
    /** Momento (ms) en que se aplicó el disfraz — para calcular el tiempo en la actionbar. */
    private final Map<UUID, Long>         disguiseStart   = new HashMap<>();

    private final ZerDisguise plugin;

    public DisguiseManager(ZerDisguise plugin) {
        this.plugin = plugin;
    }

    // ──────────────────────────────────────────────────────────────
    //  Disfraz completo
    // ──────────────────────────────────────────────────────────────

    public void applyDisguise(Player player, String disguiseName, String rankId) {
        ConfigManager cfg = plugin.getConfigManager();
        RankProvider  rp  = plugin.getRankProvider();
        SkinApplier   sa  = plugin.getSkinApplier();

        DisguiseData cur = current.get(player.getUniqueId());
        if (cur != null) previous.put(player.getUniqueId(), cur);


        // Capturar rango previo ANTES de que visualRankOnly sea eliminado más abajo.
        // Fallback cuando no se especifica rankId y el jugador destino no está en línea
        // (evita que se pierda el rango Zeus al aplicar disfraz The_Titan19).
        String priorRankId = visualRankOnly.get(player.getUniqueId());
        if (priorRankId == null && cur != null && !"default".equals(cur.rankId()))
            priorRankId = cur.rankId();
        String resolvedRankId = rankId != null && !rankId.isBlank() ? rankId : "default";
        String rankPrefix     = null;
        String rankDisplay    = null;

        Player onlineTarget = Bukkit.getPlayerExact(disguiseName);
        if (onlineTarget != null) {
            resolvedRankId = rp.getPlayerPrimaryGroup(onlineTarget);
            rankPrefix     = rp.getPlayerPrefix(onlineTarget);
        }


        // Si el rango resuelto es "default" pero el jugador tenía un rango activo,
        // conservarlo. Ej: Zeus (visual) + The_Titan19 (disfraz) = Zeus + The_Titan19.
        if ("default".equals(resolvedRankId) && priorRankId != null) {
            resolvedRankId = priorRankId;
        }
        if (rankPrefix == null || rankPrefix.isBlank())
            rankPrefix = rp.getGroupPrefix(resolvedRankId);

        if (rankPrefix == null || rankPrefix.isBlank()) {
            for (ConfigManager.RankEntry r : cfg.getRanks()) {
                if (r.id().equalsIgnoreCase(resolvedRankId)) {
                    rankPrefix  = r.prefix();
                    rankDisplay = r.color() + r.name();
                    break;
                }
            }
        }

        if (rankPrefix  == null) rankPrefix  = "";
        if (rankDisplay == null) rankDisplay = "&f" + capitalize(resolvedRankId);

        current.put(player.getUniqueId(), new DisguiseData(disguiseName, resolvedRankId));
        visualRankOnly.remove(player.getUniqueId());
        disguiseStart.put(player.getUniqueId(), System.currentTimeMillis());

        String display = cfg.colorize(rankPrefix.isBlank()
                ? "&d" + disguiseName : rankPrefix + " &d" + disguiseName);
        player.setDisplayName(display);
        player.setPlayerListName(display);
        sa.applyNameplate(player, rankPrefix, display);

        // Sobreescribir el prefijo en LuckPerms/Vault para que el chat muestre
        // solo el prefijo del disfraz, sin duplicar el rango real del jugador.
        rp.setDisguisePrefix(player, rankPrefix);

        // TAB (neznamy) ignora setPlayerListName() — usar su API directamente.
        TabHook.setTabName(player.getUniqueId(), disguiseName);

        final String finalRankDisplay = rankDisplay;
        final String finalDisguiseName = disguiseName;
        player.sendMessage(cfg.getPrefix().append(
                cfg.component("&7Cargando skin de &d" + disguiseName + "&7...")));

        plugin.getSkinFetcher().fetchSkin(disguiseName,
                skinData -> {
                    if (!player.isOnline()) return;
                    boolean ok = sa.applySkin(player, skinData);
                    // Re-aplicar nombre en TAB tras el ciclo hidePlayer/showPlayer
                    // (TAB puede resetear el nombre al recibir el paquete de respawn).
                    TabHook.setTabName(player.getUniqueId(), finalDisguiseName);
                    String msg = cfg.getMsgApplied()
                            .replace("{disguise}", finalDisguiseName)
                            .replace("{rank}", finalRankDisplay);
                    player.sendMessage(cfg.getPrefix().append(cfg.component(msg)));
                    if (!ok) player.sendMessage(cfg.getPrefix().append(
                            cfg.component("&e⚠ &7La skin no pudo aplicarse (nombre si cambiado).")));
                },
                err -> {
                    if (!player.isOnline()) return;
                    String msg = cfg.getMsgApplied()
                            .replace("{disguise}", finalDisguiseName)
                            .replace("{rank}", finalRankDisplay);
                    player.sendMessage(cfg.getPrefix().append(cfg.component(msg)));
                    player.sendMessage(cfg.getPrefix().append(
                            cfg.component("&e⚠ &7No se pudo cargar la skin: &c" + err)));
                });
    }

    // ──────────────────────────────────────────────────────────────
    //  Solo rango visual (sin skin ni permisos)
    // ──────────────────────────────────────────────────────────────

    public void applyRankOnly(Player player, String rankId) {
        ConfigManager cfg = plugin.getConfigManager();
        RankProvider  rp  = plugin.getRankProvider();
        SkinApplier   sa  = plugin.getSkinApplier();

        String rankPrefix  = rp.getGroupPrefix(rankId);
        String rankDisplay = null;

        if (rankPrefix == null || rankPrefix.isBlank()) {
            for (ConfigManager.RankEntry r : cfg.getRanks()) {
                if (r.id().equalsIgnoreCase(rankId)) {
                    rankPrefix  = r.prefix();
                    rankDisplay = r.color() + r.name();
                    break;
                }
            }
        }

        if (rankPrefix  == null) rankPrefix  = "";
        if (rankDisplay == null) rankDisplay = "&f" + capitalize(rankId);

        DisguiseData cur = current.get(player.getUniqueId());
        String nameToUse = (cur != null) ? cur.disguiseName() : player.getName();

        if (cur != null) {
            previous.put(player.getUniqueId(), cur);
            current.put(player.getUniqueId(), new DisguiseData(cur.disguiseName(), rankId));
        }
        visualRankOnly.put(player.getUniqueId(), rankId);
        disguiseStart.put(player.getUniqueId(), System.currentTimeMillis());

        String display = cfg.colorize(rankPrefix.isBlank()
                ? "&d" + nameToUse : rankPrefix + " &d" + nameToUse);
        player.setDisplayName(display);
        player.setPlayerListName(display);
        sa.applyNameplate(player, rankPrefix, display);

        // Sobreescribir el prefijo en LP/Vault igual que en el disfraz completo
        rp.setDisguisePrefix(player, rankPrefix);

        String msg = cfg.getMsgRankApplied().replace("{rank}", rankDisplay);
        player.sendMessage(cfg.getPrefix().append(cfg.component(msg)));
    }

    // ──────────────────────────────────────────────────────────────
    //  Re-aplicar tras respawn
    // ──────────────────────────────────────────────────────────────

    public void reapplyAfterRespawn(Player player) {
        ConfigManager cfg = plugin.getConfigManager();
        RankProvider  rp  = plugin.getRankProvider();
        SkinApplier   sa  = plugin.getSkinApplier();

        DisguiseData cur      = current.get(player.getUniqueId());
        String       rankOnly = visualRankOnly.get(player.getUniqueId());
        if (cur == null && rankOnly == null) return;

        String nameToUse = (cur != null) ? cur.disguiseName() : player.getName();
        String rankId    = (cur != null) ? cur.rankId() : rankOnly;

        String rankPrefix = rp.getGroupPrefix(rankId);
        if (rankPrefix == null || rankPrefix.isBlank()) {
            for (ConfigManager.RankEntry r : cfg.getRanks()) {
                if (r.id().equalsIgnoreCase(rankId)) { rankPrefix = r.prefix(); break; }
            }
        }
        if (rankPrefix == null) rankPrefix = "";

        String display = cfg.colorize(rankPrefix.isBlank()
                ? "&d" + nameToUse : rankPrefix + " &d" + nameToUse);
        player.setDisplayName(display);
        player.setPlayerListName(display);
        sa.applyNameplate(player, rankPrefix, display);

        // Re-aplicar también el prefijo en LP/Vault (puede haberse perdido al morir
        // si algún plugin resetea el prefijo del jugador en el respawn)
        rp.setDisguisePrefix(player, rankPrefix);

        // Restaurar nombre en TAB tras respawn
        final String tabName = nameToUse;
        TabHook.setTabName(player.getUniqueId(), tabName);

        if (cur != null) {
            final String fn = nameToUse;
            plugin.getSkinFetcher().fetchSkin(fn,
                    skinData -> {
                        if (player.isOnline()) {
                            sa.applySkin(player, skinData);
                            // Re-aplicar tras el ciclo hide/show del skin
                            TabHook.setTabName(player.getUniqueId(), tabName);
                        }
                    },
                    err -> plugin.getLogger().warning(
                            "[DisguiseManager] No se pudo restaurar skin tras respawn: " + err));
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Quitar disfraz (solo manual)
    // ──────────────────────────────────────────────────────────────

    public void removeDisguise(Player player) {
        ConfigManager cfg = plugin.getConfigManager();
        SkinApplier   sa  = plugin.getSkinApplier();
        RankProvider  rp  = plugin.getRankProvider();

        boolean hasDisguise   = isDisguised(player);
        boolean hasVisualRank = visualRankOnly.containsKey(player.getUniqueId());

        if (!hasDisguise && !hasVisualRank) {
            player.sendMessage(cfg.getPrefix().append(
                    cfg.component("&7No tienes ningun disfraz activo.")));
            return;
        }

        DisguiseData cur = current.remove(player.getUniqueId());
        if (cur != null) previous.put(player.getUniqueId(), cur);
        visualRankOnly.remove(player.getUniqueId());
        disguiseStart.remove(player.getUniqueId());

        player.setDisplayName(player.getName());
        player.setPlayerListName(player.getName());
        sa.removeNameplate(player);
        if (hasDisguise) sa.removeSkin(player);

        // Restaurar el prefijo real en LuckPerms/Vault
        rp.clearDisguisePrefix(player);

        // Restaurar nombre real en TAB
        TabHook.clearTabName(player.getUniqueId());

        player.sendMessage(cfg.getPrefix().append(cfg.component(cfg.getMsgRemoved())));
    }

    public void cleanupOnQuit(Player player) {
        boolean wasDisguised  = isDisguised(player);
        boolean hadVisualRank = visualRankOnly.containsKey(player.getUniqueId());

        visualRankOnly.remove(player.getUniqueId());
        disguiseStart.remove(player.getUniqueId());
        DisguiseData cur = current.remove(player.getUniqueId());
        if (cur != null) previous.put(player.getUniqueId(), cur);
        plugin.getSkinApplier().removeNameplate(player);
        plugin.getSkinApplier().cleanupPlayer(player.getUniqueId());

        // Limpiar el prefijo de disfraz en LP/Vault al desconectarse
        if (wasDisguised || hadVisualRank) {
            plugin.getRankProvider().clearDisguisePrefix(player);
        }

        // Limpiar nombre en TAB al desconectarse
        TabHook.clearTabName(player.getUniqueId());
    }

    // ──────────────────────────────────────────────────────────────
    //  Action Bar (tick global desde ZerDisguise)
    // ──────────────────────────────────────────────────────────────

    /**
     * Llamado cada N ticks desde ZerDisguise. Envía la barra de acción a
     * todos los jugadores que tengan un disfraz activo.
     */
    public void tickActionbar() {
        ConfigManager cfg = plugin.getConfigManager();
        if (!cfg.isActionbarEnabled()) return;

        String format = cfg.getActionbarFormat();

        for (Player player : Bukkit.getOnlinePlayers()) {
            DisguiseData cur      = current.get(player.getUniqueId());
            String       rankOnly = visualRankOnly.get(player.getUniqueId());
            if (cur == null && rankOnly == null) continue;

            Long startMillis = disguiseStart.get(player.getUniqueId());
            if (startMillis == null) continue;

            long elapsed = (System.currentTimeMillis() - startMillis) / 1000L;
            long min     = elapsed / 60;
            long sec     = elapsed % 60;

            String nombre = cur != null ? cur.disguiseName() : player.getName();
            String rankId = cur != null ? cur.rankId() : rankOnly;
            String rango  = resolveRankPrefix(rankId);

            String msg = format
                    .replace("{nombre}",     nombre)
                    .replace("{rango}",      rango.isBlank() ? "Ninguno" : rango)
                    .replace("{tiempo}",     String.format("%02d:%02d", min, sec))
                    .replace("{tiempo_min}", String.valueOf(min))
                    .replace("{tiempo_seg}", String.format("%02d", sec));

            player.sendActionBar(cfg.component(msg));
        }
    }

    private String resolveRankPrefix(String rankId) {
        if (rankId == null || rankId.isBlank()) return "";
        ConfigManager cfg = plugin.getConfigManager();
        RankProvider  rp  = plugin.getRankProvider();

        String prefix = rp.getGroupPrefix(rankId);
        if (prefix == null || prefix.isBlank()) {
            for (ConfigManager.RankEntry r : cfg.getRanks()) {
                if (r.id().equalsIgnoreCase(rankId)) { prefix = r.prefix(); break; }
            }
        }
        return prefix != null ? prefix : capitalize(rankId);
    }

    // ──────────────────────────────────────────────────────────────
    //  Consultas
    // ──────────────────────────────────────────────────────────────

    public boolean isDisguised(Player player)   { return current.containsKey(player.getUniqueId()); }
    public boolean hasVisualRank(Player player) { return visualRankOnly.containsKey(player.getUniqueId()); }
    public String  getVisualRank(UUID uuid)     { return visualRankOnly.get(uuid); }
    public DisguiseData getCurrent(UUID uuid)   { return current.get(uuid); }
    public DisguiseData getPrevious(UUID uuid)  { return previous.get(uuid); }
    public Long getDisguiseStart(UUID uuid)     { return disguiseStart.get(uuid); }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
