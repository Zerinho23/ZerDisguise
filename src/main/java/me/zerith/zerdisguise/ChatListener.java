package me.zerith.zerdisguise;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Intercepts chat messages for players who are in "awaiting disguise name" mode.
 * Also handles player death (disguise removal) and quit cleanup.
 */
public class ChatListener implements Listener {

    private final Map<UUID, Boolean>   awaitingInput = new HashMap<>();
    private final Map<UUID, String[]>  pendingConfirm = new HashMap<>();

    private final ZerDisguise plugin;

    public ChatListener(ZerDisguise plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        if (!awaitingInput.containsKey(player.getUniqueId())) return;

        if (!player.hasPermission("zerdisguise.use")) {
            awaitingInput.remove(player.getUniqueId());
            return;
        }

        e.setCancelled(true);
        awaitingInput.remove(player.getUniqueId());

        String input = e.getMessage().trim();
        if (input.equalsIgnoreCase("cancel") || input.isEmpty()) {
            player.sendMessage(plugin.getConfigManager().getPrefix().append(
                    plugin.getConfigManager().component(
                            plugin.getConfigManager().getMsgCancelled())));
            return;
        }

        if (input.length() > 16 || !input.matches("[a-zA-Z0-9_]+")) {
            player.sendMessage(plugin.getConfigManager().getPrefix().append(
                    plugin.getConfigManager().component(
                            "&cNombre invalido. Solo letras, numeros y _ (max 16 caracteres).")));
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            setPendingConfirm(player, input, "default");
            player.openInventory(new MenuBuilder(plugin).buildConfirmMenu(player, input, "default"));
        });
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        plugin.getDisguiseManager().clearOnDeath(e.getEntity());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        awaitingInput.remove(id);
        pendingConfirm.remove(id);
    }

    public void awaitInput(Player player) {
        awaitingInput.put(player.getUniqueId(), true);
    }

    public void setPendingConfirm(Player player, String disguiseName, String rankId) {
        pendingConfirm.put(player.getUniqueId(), new String[]{disguiseName, rankId});
    }

    public String[] getPendingConfirm(Player player) {
        return pendingConfirm.get(player.getUniqueId());
    }

    public void clearPendingConfirm(Player player) {
        pendingConfirm.remove(player.getUniqueId());
    }
}
