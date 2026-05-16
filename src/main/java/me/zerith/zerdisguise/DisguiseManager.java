package me.zerith.zerdisguise;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks each player's current and previous disguise + rank.
 * The actual display-name change is done here via Adventure API.
 */
public class DisguiseManager {

    public record DisguiseData(String disguiseName, String rankId) {}

    private final Map<UUID, DisguiseData> current  = new HashMap<>();
    private final Map<UUID, DisguiseData> previous = new HashMap<>();

    private final ZerDisguise plugin;

    public DisguiseManager(ZerDisguise plugin) {
        this.plugin = plugin;
    }

    /** Apply a disguise (name + rank) to a player. */
    public void applyDisguise(Player player, String disguiseName, String rankId) {
        ConfigManager cfg = plugin.getConfigManager();

        // Store previous before overwriting
        DisguiseData cur = current.get(player.getUniqueId());
        if (cur != null) previous.put(player.getUniqueId(), cur);

        current.put(player.getUniqueId(), new DisguiseData(disguiseName, rankId));

        // Find rank prefix
        String prefix = "";
        String rankDisplayName = rankId;
        for (ConfigManager.RankEntry r : cfg.getRanks()) {
            if (r.id().equalsIgnoreCase(rankId)) {
                prefix = r.prefix();
                rankDisplayName = r.color() + r.name();
                break;
            }
        }

        // Change the display name seen by other players
        String display = cfg.colorize(prefix + " &d" + disguiseName);
        player.setDisplayName(display);
        player.setPlayerListName(display);

        // Announce
        String msg = cfg.getMsgApplied()
                .replace("{disguise}", disguiseName)
                .replace("{rank}", rankDisplayName);
        player.sendMessage(cfg.getPrefix().append(cfg.component(msg)));
    }

    /** Remove the disguise from a player (restore real name). */
    public void removeDisguise(Player player) {
        ConfigManager cfg = plugin.getConfigManager();
        DisguiseData cur = current.remove(player.getUniqueId());
        if (cur != null) previous.put(player.getUniqueId(), cur);

        player.setDisplayName(player.getName());
        player.setPlayerListName(player.getName());
        player.sendMessage(cfg.getPrefix().append(cfg.component(cfg.getMsgRemoved())));
    }

    public boolean isDisguised(Player player) {
        return current.containsKey(player.getUniqueId());
    }

    public DisguiseData getCurrent(UUID uuid)  { return current.get(uuid); }
    public DisguiseData getPrevious(UUID uuid) { return previous.get(uuid); }

    public void clearOnDeath(Player player) {
        DisguiseData cur = current.remove(player.getUniqueId());
        if (cur != null) {
            previous.put(player.getUniqueId(), cur);
            player.setDisplayName(player.getName());
            player.setPlayerListName(player.getName());
        }
    }
}
