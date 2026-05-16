package me.zerith.zerdisguise;

  import net.kyori.adventure.text.Component;
  import net.kyori.adventure.text.format.TextDecoration;
  import org.bukkit.Bukkit;
  import org.bukkit.Material;
  import org.bukkit.NamespacedKey;
  import org.bukkit.enchantments.Enchantment;
  import org.bukkit.entity.Player;
  import org.bukkit.inventory.Inventory;
  import org.bukkit.inventory.ItemFlag;
  import org.bukkit.inventory.ItemStack;
  import org.bukkit.inventory.meta.ItemMeta;
  import org.bukkit.inventory.meta.SkullMeta;
  import org.bukkit.persistence.PersistentDataType;

  import java.util.ArrayList;
  import java.util.List;

  /**
   * Construye los menus de ZerDisguise (54 slots).
   *
   * Menu principal (54 slots):
   *   Fila 0  -> borde morado
   *   Fila 1  -> [Cabeza][F][F][F][Escribir][F][F][F][Quitar]
   *   Fila 2  -> divisor magenta + etiqueta central
   *   Filas 3-4 -> cabezas de jugadores online (18 por pagina)
   *   Fila 5  -> [Anterior][F][F][F][Pagina][F][F][F][Siguiente]
   *
   * Menu de confirmacion (54 slots):
   *   Fila 0  -> borde + cabeza preview en slot 4
   *   Filas 1-2 -> selector de rangos (slots 10-16, 19-25)
   *   Filas 3-4 -> relleno negro
   *   Fila 5  -> borde + [Renombrar][F][F][Confirmar][F][F][Volver]
   */
  public class MenuBuilder {

      public static final int PLAYERS_PER_PAGE = 18;

      // PDC Keys -- inicializadas desde ZerDisguise.onEnable() via initKeys()
      public static NamespacedKey KEY_ACTION;
      public static NamespacedKey KEY_PLAYER;
      public static NamespacedKey KEY_RANK;
      public static NamespacedKey KEY_DISGUISE;
      public static NamespacedKey KEY_PAGE;

      private static final Material BORDER  = Material.PURPLE_STAINED_GLASS_PANE;
      private static final Material FILLER  = Material.BLACK_STAINED_GLASS_PANE;
      private static final Material DIVIDER = Material.MAGENTA_STAINED_GLASS_PANE;

      private static final Material[] RANK_GLASS = {
          Material.PURPLE_STAINED_GLASS_PANE,
          Material.BLUE_STAINED_GLASS_PANE,
          Material.CYAN_STAINED_GLASS_PANE,
          Material.GREEN_STAINED_GLASS_PANE,
          Material.YELLOW_STAINED_GLASS_PANE,
          Material.ORANGE_STAINED_GLASS_PANE,
          Material.RED_STAINED_GLASS_PANE,
          Material.MAGENTA_STAINED_GLASS_PANE,
          Material.PINK_STAINED_GLASS_PANE,
          Material.LIME_STAINED_GLASS_PANE,
          Material.LIGHT_BLUE_STAINED_GLASS_PANE,
          Material.WHITE_STAINED_GLASS_PANE,
          Material.BROWN_STAINED_GLASS_PANE,
          Material.BLACK_STAINED_GLASS_PANE,
      };

      private final ZerDisguise plugin;

      /** Llamar desde ZerDisguise.onEnable() antes de abrir cualquier menu. */
      public static void initKeys(ZerDisguise plugin) {
          KEY_ACTION   = new NamespacedKey(plugin, "zd_action");
          KEY_PLAYER   = new NamespacedKey(plugin, "zd_player");
          KEY_RANK     = new NamespacedKey(plugin, "zd_rank");
          KEY_DISGUISE = new NamespacedKey(plugin, "zd_disguise");
          KEY_PAGE     = new NamespacedKey(plugin, "zd_page");
      }

      public MenuBuilder(ZerDisguise plugin) {
          this.plugin = plugin;
      }

      // ============================================================
      //  MENU PRINCIPAL
      // ============================================================

      public Inventory buildMainMenu(Player player) {
          return buildMainMenu(player, 0);
      }

      public Inventory buildMainMenu(Player player, int page) {
          ConfigManager cfg = plugin.getConfigManager();
          Inventory inv = Bukkit.createInventory(null, 54,
                  cfg.component("&8>> &#CC88FF&lZerDisguise &8<<"));

          ItemStack border  = glass(BORDER);
          ItemStack filler  = glass(FILLER);
          ItemStack divider = glass(DIVIDER);

          // Fila 0: borde morado
          for (int i = 0; i < 9; i++) inv.setItem(i, border);

          // Fila 1: info del jugador + escribir + quitar
          inv.setItem(9,  buildPlayerInfoHead(player));
          for (int i = 10; i <= 12; i++) inv.setItem(i, filler);
          inv.setItem(13, buildWriteButton());
          for (int i = 14; i <= 16; i++) inv.setItem(i, filler);
          inv.setItem(17, plugin.getDisguiseManager().isDisguised(player)
                  ? buildRemoveButton(player) : border);

          // Fila 2: divisor con etiqueta central
          for (int i = 18; i < 27; i++) inv.setItem(i, divider);
          inv.setItem(22, buildLabel(Material.COMPASS,
                  "&#FFAAFF&l* Jugadores en linea",
                  List.of("", "&7Haz clic en una cabeza para",
                          "&7ponerte su skin y nombre al instante.", "")));

          // Filas 3-4: cabezas de jugadores online
          List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
          int total      = online.size();
          int totalPages = Math.max(1, (int) Math.ceil(total / (double) PLAYERS_PER_PAGE));
          int safePage   = Math.max(0, Math.min(page, totalPages - 1));
          int start      = safePage * PLAYERS_PER_PAGE;

          int slot = 27;
          for (int i = start; i < Math.min(start + PLAYERS_PER_PAGE, total); i++) {
              inv.setItem(slot++, buildOnlinePlayerHead(online.get(i)));
          }
          while (slot <= 44) inv.setItem(slot++, filler);

          // Fila 5: navegacion de paginas
          for (int i = 45; i < 54; i++) inv.setItem(i, border);
          if (safePage > 0) {
              inv.setItem(45, navButton("prev_page", safePage - 1,
                      "&e&l<-- Pagina anterior",
                      List.of("", "&7Pagina &f" + safePage + " &7de &f" + totalPages, "")));
          }
          inv.setItem(49, buildLabel(Material.PAPER,
                  "&fPagina &e" + (safePage + 1) + " &8/ &e" + totalPages,
                  List.of("", "&8Jugadores online&7: &f" + total, "")));
          if (safePage < totalPages - 1) {
              inv.setItem(53, navButton("next_page", safePage + 1,
                      "&e&lPagina siguiente -->",
                      List.of("", "&7Pagina &f" + (safePage + 2) + " &7de &f" + totalPages, "")));
          }

          return inv;
      }

      // ============================================================
      //  MENU DE CONFIRMACION
      // ============================================================

      public Inventory buildConfirmMenu(Player player, String disguiseName, String selectedRankId) {
          ConfigManager cfg = plugin.getConfigManager();
          RankProvider  rp  = plugin.getRankProvider();

          Inventory inv = Bukkit.createInventory(null, 54,
                  cfg.component("&8>> &#CC88FF&lConfirmar disfraz &8<<"));

          ItemStack border = glass(BORDER);
          ItemStack filler = glass(FILLER);

          // Fila 0: borde + cabeza de preview en slot 4
          for (int i = 0; i < 9; i++) inv.setItem(i, border);
          inv.setItem(4, buildPreviewHead(disguiseName, selectedRankId));

          // Filas 1-2: selector de rangos (slots 10-16 y 19-25)
          for (int s : new int[]{9, 17, 18, 26}) inv.setItem(s, border);

          List<RankProvider.GroupEntry> groups = rp.getAllGroups();
          int[] rankSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
          for (int i = 0; i < Math.min(groups.size(), rankSlots.length); i++) {
              RankProvider.GroupEntry g = groups.get(i);
              boolean selected = g.id().equalsIgnoreCase(selectedRankId);
              inv.setItem(rankSlots[i], buildRankButton(g, disguiseName, selected, i));
          }
          for (int i = 9; i < 27; i++) {
              if (inv.getItem(i) == null) inv.setItem(i, filler);
          }

          // Filas 3-4: relleno negro
          for (int i = 27; i < 45; i++) inv.setItem(i, filler);

          // Fila 5: borde + botones de accion
          for (int i = 45; i < 54; i++) inv.setItem(i, border);
          inv.setItem(46, buildActionButton(Material.NAME_TAG, "rename",
                  disguiseName, selectedRankId,
                  "&#FFCC00&l+ Cambiar nombre",
                  List.of("", "&7Escribe un nombre diferente.", "")));
          inv.setItem(49, buildConfirmButton(disguiseName, selectedRankId));
          inv.setItem(52, buildActionButton(Material.ARROW, "back",
                  disguiseName, selectedRankId,
                  "&c&l<-- Volver al menu",
                  List.of("", "&7Regresa al menu principal.", "")));

          return inv;
      }

      // ============================================================
      //  CONSTRUCTORES DE ITEMS
      // ============================================================

      private ItemStack buildPlayerInfoHead(Player player) {
          ConfigManager   cfg = plugin.getConfigManager();
          DisguiseManager dm  = plugin.getDisguiseManager();
          RankProvider    rp  = plugin.getRankProvider();

          DisguiseManager.DisguiseData cur  = dm.getCurrent(player.getUniqueId());
          DisguiseManager.DisguiseData prev = dm.getPrevious(player.getUniqueId());

          String realPrefix = rp.getPlayerPrefix(player);
          if (realPrefix == null || realPrefix.isBlank()) realPrefix = "&8[&7Player&8]";

          String curName  = cur  != null ? "&#CC88FF" + cur.disguiseName()  : "&8Ninguno";
          String prevName = prev != null ? "&7"        + prev.disguiseName() : "&8Ninguno";

          ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
          SkullMeta meta  = (SkullMeta) skull.getItemMeta();
          meta.setOwningPlayer(player);
          meta.displayName(noItalic(cfg.component("&#FFFFFF&l" + player.getName())));

          List<Component> lore = new ArrayList<>();
          lore.add(empty());
          lore.add(noItalic(cfg.component("&8| &7Tu perfil")));
          lore.add(noItalic(cfg.componentAny("&8|  &8Rango&7:    " + realPrefix)));
          lore.add(noItalic(cfg.component("&8|  &7Disfraz&8:  " + curName)));
          lore.add(noItalic(cfg.component("&8|  &7Anterior&8: " + prevName)));
          lore.add(noItalic(cfg.component("&8|-------------------")));
          lore.add(empty());
          meta.lore(lore);
          skull.setItemMeta(meta);
          return skull;
      }

      private ItemStack buildWriteButton() {
          ConfigManager cfg  = plugin.getConfigManager();
          ItemStack     item = new ItemStack(Material.NAME_TAG);
          ItemMeta      meta = item.getItemMeta();

          meta.displayName(noItalic(cfg.component("&#CC88FF&l* Escribir nombre")));
          List<Component> lore = new ArrayList<>();
          lore.add(empty());
          lore.add(noItalic(cfg.component("&7Escribe el nombre de cualquier")));
          lore.add(noItalic(cfg.component("&7jugador de Minecraft en el chat.")));
          lore.add(empty());
          lore.add(noItalic(cfg.component("&#FFDD00>> &eHaz clic para comenzar")));
          lore.add(empty());
          meta.lore(lore);
          meta.getPersistentDataContainer().set(KEY_ACTION, PersistentDataType.STRING, "write");
          addGlow(meta);
          item.setItemMeta(meta);
          return item;
      }

      private ItemStack buildRemoveButton(Player player) {
          ConfigManager cfg = plugin.getConfigManager();
          DisguiseManager.DisguiseData cur = plugin.getDisguiseManager()
                  .getCurrent(player.getUniqueId());
          String curName = cur != null ? "&#CC88FF" + cur.disguiseName() : "ninguno";

          ItemStack item = new ItemStack(Material.BARRIER);
          ItemMeta  meta = item.getItemMeta();
          meta.displayName(noItalic(cfg.component("&c&l[X] Quitar disfraz")));
          List<Component> lore = new ArrayList<>();
          lore.add(empty());
          lore.add(noItalic(cfg.component("&7Elimina tu disfraz actual.")));
          lore.add(noItalic(cfg.component("&8Activo&7: " + curName)));
          lore.add(empty());
          lore.add(noItalic(cfg.component("&c>> Clic para remover")));
          lore.add(empty());
          meta.lore(lore);
          meta.getPersistentDataContainer().set(KEY_ACTION, PersistentDataType.STRING, "remove");
          item.setItemMeta(meta);
          return item;
      }

      private ItemStack buildOnlinePlayerHead(Player online) {
          ConfigManager cfg = plugin.getConfigManager();
          RankProvider  rp  = plugin.getRankProvider();

          String rankId = rp.getPlayerPrimaryGroup(online);
          String prefix = rp.getGroupPrefix(rankId);
          if (prefix == null || prefix.isBlank()) prefix = "&8[&7Player&8]";

          ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
          SkullMeta meta  = (SkullMeta) skull.getItemMeta();
          meta.setOwningPlayer(online);
          meta.displayName(noItalic(cfg.component("&#CC88FF&l" + online.getName())));

          List<Component> lore = new ArrayList<>();
          lore.add(empty());
          lore.add(noItalic(cfg.componentAny("&8|  &7Rango&8: " + prefix)));
          lore.add(noItalic(cfg.component("&8|  &7Ping&8:  &f" + online.getPing() + "ms")));
          lore.add(empty());
          lore.add(noItalic(cfg.component("&#FFDD00>> &eClic para disfrazarte")));
          lore.add(noItalic(cfg.component("&8   como &#CC88FF" + online.getName())));
          lore.add(empty());
          meta.lore(lore);

          var pdc = meta.getPersistentDataContainer();
          pdc.set(KEY_ACTION, PersistentDataType.STRING, "instant_disguise");
          pdc.set(KEY_PLAYER, PersistentDataType.STRING, online.getName());
          pdc.set(KEY_RANK,   PersistentDataType.STRING, rankId.isBlank() ? "default" : rankId);
          skull.setItemMeta(meta);
          return skull;
      }

      private ItemStack buildPreviewHead(String disguiseName, String selectedRankId) {
          ConfigManager cfg = plugin.getConfigManager();
          Player target = Bukkit.getPlayerExact(disguiseName);
          ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
          SkullMeta meta  = (SkullMeta) skull.getItemMeta();
          if (target != null) meta.setOwningPlayer(target);

          String rankDisplay = selectedRankId;
          for (ConfigManager.RankEntry r : cfg.getRanks()) {
              if (r.id().equalsIgnoreCase(selectedRankId)) {
                  rankDisplay = r.color() + r.name();
                  break;
              }
          }

          meta.displayName(noItalic(cfg.component("&#CC88FF&l" + disguiseName)));
          List<Component> lore = new ArrayList<>();
          lore.add(empty());
          lore.add(noItalic(cfg.component("&8| &7Vista previa")));
          lore.add(noItalic(cfg.component("&8|  &7Nombre&8: &#CC88FF" + disguiseName)));
          lore.add(noItalic(cfg.component("&8|  &7Rango&8:  &f" + rankDisplay)));
          lore.add(noItalic(cfg.component("&8|-------------------")));
          lore.add(empty());
          lore.add(noItalic(cfg.component("&8Selecciona el rango abajo.")));
          lore.add(empty());
          meta.lore(lore);
          skull.setItemMeta(meta);
          return skull;
      }

      private ItemStack buildRankButton(RankProvider.GroupEntry group,
                                        String disguiseName, boolean selected, int idx) {
          ConfigManager cfg = plugin.getConfigManager();
          Material      mat = RANK_GLASS[idx % RANK_GLASS.length];
          for (ConfigManager.RankEntry r : cfg.getRanks()) {
              if (r.id().equalsIgnoreCase(group.id())) { mat = r.glass(); break; }
          }

          ItemStack item = new ItemStack(mat);
          ItemMeta  meta = item.getItemMeta();
          meta.displayName(noItalic(cfg.componentAny(
                  group.displayPrefix() + " &7" + capitalize(group.id()))));

          List<Component> lore = new ArrayList<>();
          lore.add(empty());
          lore.add(noItalic(cfg.component(selected
                  ? "&#44FF44&l[*] Seleccionado"
                  : "&8[ ] Clic para elegir")));
          lore.add(empty());
          meta.lore(lore);

          var pdc = meta.getPersistentDataContainer();
          pdc.set(KEY_ACTION,   PersistentDataType.STRING, "select_rank");
          pdc.set(KEY_RANK,     PersistentDataType.STRING, group.id());
          pdc.set(KEY_DISGUISE, PersistentDataType.STRING, disguiseName);
          if (selected) addGlow(meta);
          item.setItemMeta(meta);
          return item;
      }

      private ItemStack buildConfirmButton(String disguiseName, String rankId) {
          ConfigManager cfg = plugin.getConfigManager();
          ItemStack item = new ItemStack(Material.LIME_CONCRETE);
          ItemMeta  meta = item.getItemMeta();

          String rankDisplay = rankId;
          for (ConfigManager.RankEntry r : cfg.getRanks()) {
              if (r.id().equalsIgnoreCase(rankId)) { rankDisplay = r.color() + r.name(); break; }
          }

          meta.displayName(noItalic(cfg.component("&#44FF44&l[OK] Confirmar disfraz")));
          List<Component> lore = new ArrayList<>();
          lore.add(empty());
          lore.add(noItalic(cfg.component("&8|  &7Nombre&8: &#CC88FF" + disguiseName)));
          lore.add(noItalic(cfg.component("&8|  &7Rango&8:  &f" + rankDisplay)));
          lore.add(empty());
          lore.add(noItalic(cfg.component("&#44FF44>> &aHaz clic para aplicar")));
          lore.add(empty());
          meta.lore(lore);

          var pdc = meta.getPersistentDataContainer();
          pdc.set(KEY_ACTION,   PersistentDataType.STRING, "confirm");
          pdc.set(KEY_DISGUISE, PersistentDataType.STRING, disguiseName);
          pdc.set(KEY_RANK,     PersistentDataType.STRING, rankId);
          addGlow(meta);
          item.setItemMeta(meta);
          return item;
      }

      private ItemStack buildActionButton(Material mat, String action,
                                          String disguiseName, String rankId,
                                          String name, List<String> loreTxt) {
          ConfigManager cfg  = plugin.getConfigManager();
          ItemStack     item = new ItemStack(mat);
          ItemMeta      meta = item.getItemMeta();
          meta.displayName(noItalic(cfg.component(name)));
          List<Component> lore = new ArrayList<>();
          for (String l : loreTxt) lore.add(noItalic(l.isEmpty() ? empty() : cfg.component(l)));
          meta.lore(lore);
          var pdc = meta.getPersistentDataContainer();
          pdc.set(KEY_ACTION,   PersistentDataType.STRING, action);
          pdc.set(KEY_DISGUISE, PersistentDataType.STRING, disguiseName != null ? disguiseName : "");
          pdc.set(KEY_RANK,     PersistentDataType.STRING, rankId != null ? rankId : "");
          item.setItemMeta(meta);
          return item;
      }

      private ItemStack buildLabel(Material mat, String name, List<String> loreTxt) {
          ConfigManager cfg  = plugin.getConfigManager();
          ItemStack     item = new ItemStack(mat);
          ItemMeta      meta = item.getItemMeta();
          meta.displayName(noItalic(cfg.component(name)));
          List<Component> lore = new ArrayList<>();
          for (String l : loreTxt) lore.add(noItalic(l.isEmpty() ? empty() : cfg.component(l)));
          meta.lore(lore);
          item.setItemMeta(meta);
          return item;
      }

      private ItemStack navButton(String action, int targetPage, String name, List<String> loreTxt) {
          ConfigManager cfg  = plugin.getConfigManager();
          ItemStack     item = new ItemStack(Material.ARROW);
          ItemMeta      meta = item.getItemMeta();
          meta.displayName(noItalic(cfg.component(name)));
          List<Component> lore = new ArrayList<>();
          for (String l : loreTxt) lore.add(noItalic(l.isEmpty() ? empty() : cfg.component(l)));
          meta.lore(lore);
          var pdc = meta.getPersistentDataContainer();
          pdc.set(KEY_ACTION, PersistentDataType.STRING, action);
          pdc.set(KEY_PAGE,   PersistentDataType.INTEGER, targetPage);
          item.setItemMeta(meta);
          return item;
      }

      // ============================================================
      //  HELPERS
      // ============================================================

      private static ItemStack glass(Material mat) {
          ItemStack item = new ItemStack(mat);
          ItemMeta  meta = item.getItemMeta();
          meta.displayName(Component.empty());
          item.setItemMeta(meta);
          return item;
      }

      private static Component noItalic(Component c) {
          return c.decoration(TextDecoration.ITALIC, false);
      }

      private static Component empty() {
          return Component.empty().decoration(TextDecoration.ITALIC, false);
      }

      private static void addGlow(ItemMeta meta) {
          meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
          meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
      }

      private static String capitalize(String s) {
          if (s == null || s.isEmpty()) return s;
          return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
      }
  }
  