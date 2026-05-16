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

          String invTitle = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                  .legacySection().serialize(e.getView().title());

          boolean isMainMenu    = invTitle.contains("ZerDisguise") && !invTitle.contains("Confirmar");
          boolean isConfirmMenu = invTitle.contains("Confirmar disfraz");

          if (!isMainMenu && !isConfirmMenu) return;
          e.setCancelled(true);

          ItemStack clicked = e.getCurrentItem();
          if (clicked == null || !clicked.hasItemMeta()) return;

          ItemMeta meta = clicked.getItemMeta();
          if (meta == null) return;

          var    pdc     = meta.getPersistentDataContainer();
          String action  = pdc.getOrDefault(MenuBuilder.KEY_ACTION,  PersistentDataType.STRING,  "");
          String rankId  = pdc.getOrDefault(MenuBuilder.KEY_RANK,    PersistentDataType.STRING,  "");
          String disguise= pdc.getOrDefault(MenuBuilder.KEY_DISGUISE,PersistentDataType.STRING,  "");
          String targetPl= pdc.getOrDefault(MenuBuilder.KEY_PLAYER,  PersistentDataType.STRING,  "");
          int    page    = pdc.getOrDefault(MenuBuilder.KEY_PAGE,    PersistentDataType.INTEGER,  0);

          ConfigManager cfg = plugin.getConfigManager();

          switch (action) {

              // ── Abrir chat para escribir nombre ───────────────────────────────
              case "write" -> {
                  player.closeInventory();
                  plugin.getChatListener().awaitInput(player);
              }

              // ── Remover disfraz ───────────────────────────────────────────────
              case "remove" -> {
                  player.closeInventory();
                  plugin.getDisguiseManager().removeDisguise(player);
              }

              // ── Disfraz instantáneo desde cabeza de jugador online ────────────
              case "instant_disguise" -> {
                  if (targetPl.isBlank() || player.getName().equals(targetPl)) return;
                  player.closeInventory();
                  String rank = rankId.isBlank() ? "default" : rankId;
                  plugin.getDisguiseManager().applyDisguise(player, targetPl, rank);
              }

              // ── Seleccionar rango en menú de confirmación ─────────────────────
              case "select_rank" -> {
                  if (disguise.isBlank() || rankId.isBlank()) return;
                  MenuBuilder mb = new MenuBuilder(plugin);
                  player.openInventory(mb.buildConfirmMenu(player, disguise, rankId));
              }

              // ── Confirmar disfraz ─────────────────────────────────────────────
              case "confirm" -> {
                  if (disguise.isBlank()) return;
                  player.closeInventory();
                  plugin.getDisguiseManager().applyDisguise(player,
                          disguise, rankId.isBlank() ? "default" : rankId);
              }

              // ── Volver al menú principal (página 0) ───────────────────────────
              case "back" -> {
                  MenuBuilder mb = new MenuBuilder(plugin);
                  player.openInventory(mb.buildMainMenu(player, 0));
              }

              // ── Renombrar (volver al prompt de chat) ──────────────────────────
              case "rename" -> {
                  player.closeInventory();
                  plugin.getChatListener().awaitInput(player);
              }

              // ── Página anterior ───────────────────────────────────────────────
              case "prev_page" -> {
                  MenuBuilder mb = new MenuBuilder(plugin);
                  player.openInventory(mb.buildMainMenu(player, page));
              }

              // ── Página siguiente ──────────────────────────────────────────────
              case "next_page" -> {
                  MenuBuilder mb = new MenuBuilder(plugin);
                  player.openInventory(mb.buildMainMenu(player, page));
              }

              default -> {}
          }
      }
  }
  