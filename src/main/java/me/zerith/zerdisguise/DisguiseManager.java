package me.zerith.zerdisguise;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks each player's current/previous disguise + rank.
 * Coordinates display-name, tab-list, nameplate and skin changes.
 */
public class DisguiseManager {

    public record DisguiseData(String disguiseName, String rankId) {}

    private final Map<UUID, DisguiseData> current  = new HashMap<>();
    private final Map<UUID, DisguiseData> previous = new HashMap<>();

    private final ZerDisguise plugin;

    public DisguiseManager(ZerDisguise plugin) {
        this.plugin = plugin;
    }

    // ── Apply ─────────────────────────────────────────────────────────────────

    /**
     * Applies name + rank immediately, then asynchronously fetches and applies
     * the skin of {@code disguiseName}. Must be called on the main thread.
     */
    public void applyDisguise(Player player, String disguiseName, String rankId) {
        ConfigManager cfg = plugin.getConfigManager();
        SkinApplier   sa  = plugin.getSkinApplier();

        // Persist previous
        DisguiseData cur = current.get(player.getUniqueId());
        if (cur != null) previous.put(player.getUniqueId(), cur);
        current.put(player.getUniqueId(), new DisguiseData(disguiseName, rankId));

        // Resolve rank
        String rankPrefix      = "";
        String rankDisplayName = rankId;
        for (ConfigManager.RankEntry r : cfg.getRanks()) {
            if (r.id().equalsIgnoreCase(rankId)) {
                rankPrefix      = r.prefix();
                rankDisplayName = r.color() + r.name();
                break;
            }
        }

        // ── 1. Name & tab list (instant) ─────────────────────────────────────
        String display = cfg.colorize(rankPrefix + " &d" + disguiseName);
        player.setDisplayName(display);
        player.setPlayerListName(display);

        // ── 2. Nametag prefix via Scoreboard ─────────────────────────────────
        sa.applyNameplate(player, rankPrefix);

        // ── 3. Skin (async fetch → sync apply) ───────────────────────────────
        final String finalRankDisplayName = rankDisplayName;

        player.sendMessage(cfg.getPrefix().append(
                cfg.component("&7Cargando skin de &d" + disguiseName + "&7...")));

        plugin.getSkinFetcher().fetchSkin(
                disguiseName,
                // onSuccess
                skinData -> {
                    if (!player.isOnline()) return;
                    boolean applied = sa.applySkin(player, skinData);

                    String msg = cfg.getMsgApplied()
                            .replace("{disguise}", disguiseName)
                            .replace("{rank}",     finalRankDisplayName);
                    player.sendMessage(cfg.getPrefix().append(cfg.component(
                            msg + (applied ? "" : " &8(&7skin no disponible&8)"))));
                },
                // onError
                errorMsg -> {
                    if (!player.isOnline()) return;
                    // Name is already applied — just warn that skin failed
                    String msg = cfg.getMsgApplied()
                            .replace("{disguise}", disguiseName)
                            .replace("{rank}",     finalRankDisplayName);
                    player.sendMessage(cfg.getPrefix().append(cfg.component(msg)));
                    player.sendMessage(cfg.getPrefix().append(
                            cfg.component("&e⚠ Skin no encontrada: &7" + errorMsg)));
                }
        );
    }

    // ── Remove ────────────────────────────────────────────────────────────────

    /** Removes the disguise, restores real name and skin. */
    public void removeDisguise(Player player) {
        ConfigManager cfg = plugin.getConfigManager();
        SkinApplier   sa  = plugin.getSkinApplier();

        DisguiseData cur = current.remove(player.getUniqueId());
        if (cur != null) previous.put(player.getUniqueId(), cur);

        player.setDisplayName(player.getName());
        player.setPlayerListName(player.getName());
        sa.removeNameplate(player);
        sa.removeSkin(player);

        player.sendMessage(cfg.getPrefix().append(cfg.component(cfg.getMsgRemoved())));
    }

    /** Called on player death — clears disguise silently. */
    public void clearOnDeath(Player player) {
        DisguiseData cur = current.remove(player.getUniqueId());
        if (cur == null) return;
        previous.put(player.getUniqueId(), cur);

        player.setDisplayName(player.getName());
        player.setPlayerListName(player.getName());
        plugin.getSkinApplier().removeNameplate(player);
        plugin.getSkinApplier().removeSkin(player);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public boolean isDisguised(Player player) {
        return current.containsKey(player.getUniqueId());
    }

    public DisguiseData getCurrent(UUID uuid)  { return current.get(uuid); }
    public DisguiseData getPrevious(UUID uuid) { return previous.get(uuid); }
}
