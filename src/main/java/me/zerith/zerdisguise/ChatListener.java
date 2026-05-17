package me.zerith.zerdisguise;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Intercepta mensajes de chat para jugadores en modo "esperando nombre de disfraz".
 * También maneja:
 *   - Muerte del jugador → quita disfraz automáticamente con mensaje.
 *   - Quit del jugador  → limpia todo el estado (disfraz, caché de skin, awaiting).
 *
 * Usa el evento moderno de Paper (AsyncChatEvent) en lugar del deprecated
 * AsyncPlayerChatEvent de Bukkit.
 */
public class ChatListener implements Listener {

    private final Map<UUID, Boolean> awaitingInput = new HashMap<>();

    private final ZerDisguise plugin;

    public ChatListener(ZerDisguise plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent e) {
        Player player = e.getPlayer();
        if (!awaitingInput.containsKey(player.getUniqueId())) return;

        if (!player.hasPermission("zerdisguise.use")) {
            awaitingInput.remove(player.getUniqueId());
            return;
        }

        e.setCancelled(true);
        awaitingInput.remove(player.getUniqueId());

        // Obtener el texto plano del mensaje (Adventure Component → String)
        String input = PlainTextComponentSerializer.plainText()
                .serialize(e.message()).trim();

        ConfigManager cfg = plugin.getConfigManager();

        if (input.equalsIgnoreCase("cancel") || input.isEmpty()) {
            player.sendMessage(cfg.getPrefix().append(cfg.component(cfg.getMsgCancelled())));
            return;
        }

        if (input.length() > 16 || !input.matches("[a-zA-Z0-9_]+")) {
            player.sendMessage(cfg.getPrefix().append(
                    cfg.component(cfg.getMsgInvalidName())));
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
        Player player = e.getPlayer();
        awaitingInput.remove(player.getUniqueId());

        // Limpiar el estado completo del jugador al desconectarse:
        //  - Quita el registro del disfraz activo
        //  - Libera el caché de skin original (ya no necesitamos restaurarla)
        //  - Quita el nameplate del scoreboard
        if (plugin.getDisguiseManager().isDisguised(player)) {
            plugin.getDisguiseManager().cleanupOnQuit(player);
        }
    }

    public void awaitInput(Player player) {
        awaitingInput.put(player.getUniqueId(), true);
    }

    public boolean isAwaiting(Player player) {
        return awaitingInput.containsKey(player.getUniqueId());
    }

    public void cancelAwait(Player player) {
        awaitingInput.remove(player.getUniqueId());
    }
}
