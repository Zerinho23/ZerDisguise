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
 * Intercepta mensajes de chat para jugadores en modo "esperando nombre de disfraz".
 * También maneja la muerte del jugador (quitar disfraz) y limpieza al salir.
 */
public class ChatListener implements Listener {

    private final Map<UUID, Boolean> awaitingInput = new HashMap<>();

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
                            "&cNombre inválido. Solo letras, números y _ (máx 16 caracteres).")));
            return;
        }

        // Abrir menú de confirmación en el hilo principal
        plugin.getServer().getScheduler().runTask(plugin, () ->
                player.openInventory(new MenuBuilder(plugin).buildConfirmMenu(player, input)));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        plugin.getDisguiseManager().clearOnDeath(e.getEntity());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        awaitingInput.remove(e.getPlayer().getUniqueId());
    }

    public void awaitInput(Player player) {
        awaitingInput.put(player.getUniqueId(), true);
    }
}
