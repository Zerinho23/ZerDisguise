package me.zerith.zerdisguise;

  import org.bukkit.entity.Player;
  import org.bukkit.event.EventHandler;
  import org.bukkit.event.Listener;
  import org.bukkit.event.inventory.InventoryClickEvent;
  import org.bukkit.inventory.ItemStack;
  import org.bukkit.inventory.meta.ItemMeta;
  import org.bukkit.persistence.PersistentDataType;

  public class MenuListener implements Listener {

      private final ZerDisguise plugin;

      public MenuListener(ZerDisguise plugin) {
          this.plugin = plugin;
      }

      @EventHandler
      public void onInventoryClick(InventoryClickEvent e) {
          if (!(e.getWhoClicked() instanceof Player player)) return;

          // Serializar el título del inventario a texto plano para comparar
          String title = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                  .legacySection().serialize(e.getView().title());

          boolean isMain    = title.contains("ZerDisguise");
          boolean isConfirm = title.contains("Confirmar disfraz");
          if (!isMain && !isConfirm) return;

          e.setCancelled(true);

          ItemStack clicked = e.getCurrentItem();
          if (clicked == null || !clicked.hasItemMeta()) return;
          ItemMeta meta = clicked.getItemMeta();
          if (meta == null) return;

          // Protección: las claves deben estar inicializadas
          if (MenuBuilder.KEY_ACTION == null) return;

          var    pdc      = meta.getPersistentDataContainer();
          String action   = pdc.getOrDefault(MenuBuilder.KEY_ACTION,   PersistentDataType.STRING,  "");
          String rankId   = pdc.getOrDefault(MenuBuilder.KEY_RANK,     PersistentDataType.STRING,  "");
          String disguise = pdc.getOrDefault(MenuBuilder.KEY_DISGUISE,  PersistentDataType.STRING,  "");
          String target   = pdc.getOrDefault(MenuBuilder.KEY_PLAYER,   PersistentDataType.STRING,  "");
          int    page     = pdc.getOrDefault(MenuBuilder.KEY_PAGE,     PersistentDataType.INTEGER,  0);

          switch (action) {

              // ── Abrir chat para escribir nombre ───────────────────────────────
              case "write" -> {
                  player.closeInventory();
                  plugin.getChatListener().awaitInput(player);
                  player.sendMessage(plugin.getConfigManager().getPrefix().append(
                          plugin.getConfigManager().component(
                                  plugin.getConfigManager().getMsgWriteDisguise())));
              }

              // ── Remover disfraz activo ────────────────────────────────────────
              case "remove" -> {
                  player.closeInventory();
                  plugin.getDisguiseManager().removeDisguise(player);
              }

              // ── Disfraz instantáneo desde cabeza de jugador online ────────────
              case "instant_disguise" -> {
                  if (target.isBlank() || player.getName().equalsIgnoreCase(target)) return;
                  player.closeInventory();
                  plugin.getDisguiseManager().applyDisguise(player, target,
                          rankId.isBlank() ? "default" : rankId);
              }

              // ── Cambiar rango en menú de confirmación ─────────────────────────
              case "select_rank" -> {
                  if (disguise.isBlank() || rankId.isBlank()) return;
                  player.openInventory(new MenuBuilder(plugin)
                          .buildConfirmMenu(player, disguise, rankId));
              }

              // ── Confirmar disfraz ─────────────────────────────────────────────
              case "confirm" -> {
                  if (disguise.isBlank()) return;
                  player.closeInventory();
                  plugin.getDisguiseManager().applyDisguise(player,
                          disguise, rankId.isBlank() ? "default" : rankId);
              }

              // ── Renombrar (volver al chat prompt) ─────────────────────────────
              case "rename" -> {
                  player.closeInventory();
                  plugin.getChatListener().awaitInput(player);
                  player.sendMessage(plugin.getConfigManager().getPrefix().append(
                          plugin.getConfigManager().component(
                                  plugin.getConfigManager().getMsgWriteDisguise())));
              }

              // ── Volver al menú principal ──────────────────────────────────────
              case "back" -> {
                  player.openInventory(new MenuBuilder(plugin).buildMainMenu(player, 0));
              }

              // ── Paginación ────────────────────────────────────────────────────
              case "prev_page", "next_page" -> {
                  player.openInventory(new MenuBuilder(plugin).buildMainMenu(player, page));
              }

              default -> {}
          }
      }
  }
  