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
 *
 * Cuando el jugador escribe un nombre válido:
 *  1. Se pre-carga la skin en caché de forma asíncrona.
 *  2. Una vez lista (o fallida), se abre el menú de confirmación con la cabeza correcta.
 *
 * También maneja muerte (quita disfraz) y quit (limpia estado completo).
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

        String input = PlainTextComponentSerializer.plainText()
                .serialize(e.message()).trim();

        ConfigManager cfg = plugin.getConfigManager();

        if (input.equalsIgnoreCase("cancel") || input.isEmpty()) {
            player.sendMessage(cfg.getPrefix().append(cfg.component(cfg.getMsgCancelled())));
            return;
        }

        if (input.length() > 16 || !input.matches("[a-zA-Z0-9_]+")) {
            player.sendMessage(cfg.getPrefix().append(cfg.component(cfg.getMsgInvalidName())));
            return;
        }

        // Pre-cargar la skin de forma asíncrona para que la cabeza en el menú
        // de confirmación muestre la skin real del jugador objetivo.
        // El menú se abre DESPUÉS de que la skin esté lista (o si falla).
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;

            player.sendMessage(cfg.getPrefix().append(
                    cfg.component("&7Cargando skin de &d" + input + "&7...")));

            plugin.getSkinFetcher().fetchSkin(
                    input,
                    skinData -> {
                        // Skin lista y en caché → abrir menú con cabeza correcta
                        if (player.isOnline()) {
                            player.openInventory(
                                    new MenuBuilder(plugin).buildConfirmMenu(player, input));
                        }
                    },
                    error -> {
                        // Skin falló → abrir de todas formas (mostrará cabeza default o por nombre)
                        if (player.isOnline()) {
                            player.openInventory(
                                    new MenuBuilder(plugin).buildConfirmMenu(player, input));
                            player.sendMessage(cfg.getPrefix().append(
                                    cfg.component("&e⚠ &7No se pudo precargar la skin.")));
                        }
                    }
            );
        });
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        plugin.getDisguiseManager().clearOnDeath(e.getEntity());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        awaitingInput.remove(player.getUniqueId());
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
