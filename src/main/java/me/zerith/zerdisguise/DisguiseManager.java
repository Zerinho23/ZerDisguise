package me.zerith.zerdisguise;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gestiona los disfraces activos y anteriores de cada jugador.
 *
 * Al aplicar un disfraz:
 *  1. Si el jugador objetivo está online → su rango se obtiene directamente de LuckPerms/Vault.
 *  2. Si está offline y se proporcionó un rankId → se busca el prefijo en LuckPerms/Vault,
 *     luego en config.yml como fallback.
 *  3. Si no hay ningún rango disponible → se muestra el nombre solo, sin prefijo.
 *
 * El perfil de skin original siempre se guarda antes del primer disfraz y se restaura
 * automáticamente al llamar removeDisguise() o clearOnDeath().
 */
public class DisguiseManager {

    public record DisguiseData(String disguiseName, String rankId) {}

    private final Map<UUID, DisguiseData> current  = new HashMap<>();
    private final Map<UUID, DisguiseData> previous = new HashMap<>();

    private final ZerDisguise plugin;

    public DisguiseManager(ZerDisguise plugin) {
        this.plugin = plugin;
    }

    /**
     * Aplica un disfraz detectando automáticamente el rango del jugador objetivo.
     * Si el objetivo está online, usa su rango real de LuckPerms/Vault.
     * Si está offline, usa el rankId proporcionado (o "default" si está vacío).
     */
    public void applyDisguise(Player player, String disguiseName, String rankId) {
        ConfigManager cfg = plugin.getConfigManager();
        RankProvider  rp  = plugin.getRankProvider();
        SkinApplier   sa  = plugin.getSkinApplier();

        // Guardar disfraz anterior
        DisguiseData cur = current.get(player.getUniqueId());
        if (cur != null) previous.put(player.getUniqueId(), cur);

        // ── Resolver el rango real ────────────────────────────────────────────
        String resolvedRankId = rankId != null && !rankId.isBlank() ? rankId : "default";
        String rankPrefix     = null;
        String rankDisplay    = null;

        // 1) Si el jugador objetivo está online → tomar su rango real de LuckPerms/Vault
        Player onlineTarget = Bukkit.getPlayerExact(disguiseName);
        if (onlineTarget != null) {
            resolvedRankId = rp.getPlayerPrimaryGroup(onlineTarget);
            rankPrefix     = rp.getPlayerPrefix(onlineTarget);
        }

        // 2) Si no se obtuvo prefijo (offline o sin LP) → buscar en LuckPerms por groupId
        if (rankPrefix == null || rankPrefix.isBlank()) {
            rankPrefix = rp.getGroupPrefix(resolvedRankId);
        }

        // 3) Fallback a config.yml
        if (rankPrefix == null || rankPrefix.isBlank()) {
            for (ConfigManager.RankEntry r : cfg.getRanks()) {
                if (r.id().equalsIgnoreCase(resolvedRankId)) {
                    rankPrefix  = r.prefix();
                    rankDisplay = r.color() + r.name();
                    break;
                }
            }
        }

        // 4) Último fallback: mostrar el ID capitalizado
        if (rankPrefix  == null) rankPrefix  = "";
        if (rankDisplay == null) {
            rankDisplay = "&f" + capitalize(resolvedRankId);
        }

        current.put(player.getUniqueId(), new DisguiseData(disguiseName, resolvedRankId));

        // ── Aplicar nombre visible y nameplate ───────────────────────────────
        String display = cfg.colorize(
                rankPrefix.isBlank() ? "&d" + disguiseName : rankPrefix + " &d" + disguiseName);
        player.setDisplayName(display);
        player.setPlayerListName(display);
        sa.applyNameplate(player, rankPrefix);

        // ── Fetch y aplicar skin ──────────────────────────────────────────────
        final String finalRankDisplay = rankDisplay;

        player.sendMessage(cfg.getPrefix().append(
                cfg.component("&7Cargando skin de &d" + disguiseName + "&7...")));

        plugin.getSkinFetcher().fetchSkin(
                disguiseName,
                skinData -> {
                    if (!player.isOnline()) return;
                    boolean skinOk = sa.applySkin(player, skinData);

                    if (skinOk) {
                        String msg = cfg.getMsgApplied()
                                .replace("{disguise}", disguiseName)
                                .replace("{rank}",     finalRankDisplay);
                        player.sendMessage(cfg.getPrefix().append(cfg.component(msg)));
                    } else {
                        // Nombre y rango cambiados, pero la skin falló
                        String msg = cfg.getMsgApplied()
                                .replace("{disguise}", disguiseName)
                                .replace("{rank}",     finalRankDisplay);
                        player.sendMessage(cfg.getPrefix().append(cfg.component(msg)));
                        player.sendMessage(cfg.getPrefix().append(
                                cfg.component("&e⚠ &7La skin no pudo aplicarse (nombre sí cambiado).")));
                    }
                },
                errorMsg -> {
                    if (!player.isOnline()) return;
                    // El disfraz de nombre y rango ya está activo — avisar que la skin no cargó
                    String msg = cfg.getMsgApplied()
                            .replace("{disguise}", disguiseName)
                            .replace("{rank}",     finalRankDisplay);
                    player.sendMessage(cfg.getPrefix().append(cfg.component(msg)));
                    player.sendMessage(cfg.getPrefix().append(
                            cfg.component("&e⚠ &7No se pudo cargar la skin: &c" + errorMsg)));
                }
        );
    }

    /** Quita el disfraz manualmente y restaura la apariencia original del jugador. */
    public void removeDisguise(Player player) {
        ConfigManager cfg = plugin.getConfigManager();
        SkinApplier   sa  = plugin.getSkinApplier();

        if (!isDisguised(player)) {
            player.sendMessage(cfg.getPrefix().append(
                    cfg.component("&7No tienes ningún disfraz activo.")));
            return;
        }

        DisguiseData cur = current.remove(player.getUniqueId());
        if (cur != null) previous.put(player.getUniqueId(), cur);

        player.setDisplayName(player.getName());
        player.setPlayerListName(player.getName());
        sa.removeNameplate(player);
        sa.removeSkin(player);

        player.sendMessage(cfg.getPrefix().append(cfg.component(cfg.getMsgRemoved())));
    }

    /**
     * Quita el disfraz al morir. No restaura la skin (el jugador ya está muriendo/
     * reiniciando) — solo limpia estado y nameplate. Envía mensaje informativo.
     */
    public void clearOnDeath(Player player) {
        DisguiseData cur = current.remove(player.getUniqueId());
        if (cur == null) return;
        previous.put(player.getUniqueId(), cur);

        player.setDisplayName(player.getName());
        player.setPlayerListName(player.getName());
        plugin.getSkinApplier().removeNameplate(player);
        plugin.getSkinApplier().removeSkin(player);

        ConfigManager cfg2 = plugin.getConfigManager();
        player.sendMessage(cfg2.getPrefix().append(
                cfg2.component(cfg2.getMsgDeathRemoved())));
    }

    /**
     * Limpia el estado del jugador al desconectarse.
     * No intenta restaurar la skin (ya no está online) — solo libera memoria.
     */
    public void cleanupOnQuit(Player player) {
        DisguiseData cur = current.remove(player.getUniqueId());
        if (cur != null) previous.put(player.getUniqueId(), cur);

        plugin.getSkinApplier().removeNameplate(player);
        plugin.getSkinApplier().cleanupPlayer(player.getUniqueId());
    }

    public boolean isDisguised(Player player) {
        return current.containsKey(player.getUniqueId());
    }

    public DisguiseData getCurrent(UUID uuid)  { return current.get(uuid); }
    public DisguiseData getPrevious(UUID uuid) { return previous.get(uuid); }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
