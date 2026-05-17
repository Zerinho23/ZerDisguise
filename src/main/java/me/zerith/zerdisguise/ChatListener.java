package me.zerith.zerdisguise;

  import io.papermc.paper.event.player.AsyncChatEvent;
  import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
  import org.bukkit.entity.Player;
  import org.bukkit.event.EventHandler;
  import org.bukkit.event.EventPriority;
  import org.bukkit.event.Listener;
  import org.bukkit.event.player.PlayerQuitEvent;
  import org.bukkit.event.player.PlayerRespawnEvent;

  import java.util.HashMap;
  import java.util.Map;
  import java.util.UUID;

  /**
   * Intercepta mensajes de chat para jugadores en modo "esperando nombre de disfraz".
   *
   * Comportamiento al morir:
   *   El disfraz NO se elimina al morir. Al respawnear se re-aplica
   *   automaticamente (skin, nombre y nameplate).
   *   Solo /undisguise o el boton "Quitar disfraz" eliminan el disfraz.
   *
   * Comportamiento al salir:
   *   Se limpia el estado del jugador para liberar memoria.
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

          plugin.getServer().getScheduler().runTask(plugin, () -> {
              if (!player.isOnline()) return;

              player.sendMessage(cfg.getPrefix().append(
                      cfg.component("&7Cargando skin de &d" + input + "&7...")));

              plugin.getSkinFetcher().fetchSkin(
                      input,
                      skinData -> {
                          if (player.isOnline()) {
                              player.openInventory(
                                      new MenuBuilder(plugin).buildConfirmMenu(player, input));
                          }
                      },
                      error -> {
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

      /**
       * Re-aplica el disfraz despues de respawnear.
       * El disfraz NO se pierde al morir — solo con /undisguise.
       */
      @EventHandler
      public void onRespawn(PlayerRespawnEvent e) {
          Player player = e.getPlayer();
          DisguiseManager dm = plugin.getDisguiseManager();

          if (!dm.isDisguised(player) && !dm.hasVisualRank(player)) return;

          // Esperar 2 ticks para que el cliente este completamente listo
          plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
              if (player.isOnline()) {
                  dm.reapplyAfterRespawn(player);
              }
          }, 2L);
      }

      @EventHandler
      public void onQuit(PlayerQuitEvent e) {
          Player player = e.getPlayer();
          awaitingInput.remove(player.getUniqueId());
          plugin.getDisguiseManager().cleanupOnQuit(player);
      }

      public void awaitInput(Player player)    { awaitingInput.put(player.getUniqueId(), true); }
      public boolean isAwaiting(Player player) { return awaitingInput.containsKey(player.getUniqueId()); }
      public void cancelAwait(Player player)   { awaitingInput.remove(player.getUniqueId()); }
  }
  