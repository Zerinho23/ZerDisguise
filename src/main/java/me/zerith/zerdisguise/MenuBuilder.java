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
  import org.bukkit.profile.PlayerProfile;
  import org.bukkit.profile.PlayerTextures;

  import java.net.MalformedURLException;
  import java.net.URI;
  import java.util.*;

  /**
   * Construye los menús de ZerDisguise (54 slots).
   *
   * Menú principal (54 slots):
   *  Fila 0  → borde morado
   *  Fila 1  → [HEAD][F][F][F][WRITE][F][F][F][REMOVE]
   *  Fila 2  → borde divisor + etiqueta "Jugadores conectados"
   *  Filas 3-4 → cabezas de jugadores online (hasta 18)
   *  Fila 5  → borde inferior
   *
   * Menú de confirmación (54 slots):
   *  Fila 0  → borde + HEAD en slot 4
   *  Filas 1-2 → selector de rangos (hasta 14, slots 10-16 y 19-25)
   *  Filas 3-4 → relleno
   *  Fila 5  → borde + [RENAME][CONFIRM][BACK]
   */
  public class MenuBuilder {

      // ── PDC Keys (identifican acciones en ítems) ──────────────────────────────
      public static NamespacedKey KEY_ACTION;
      public static NamespacedKey KEY_PLAYER;
      public static NamespacedKey KEY_RANK;
      public static NamespacedKey KEY_DISGUISE;

      // Textura cabeza decorativa (signo de interrogación morado)
      private static final String CUSTOM_HEAD_B64 =
          "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0" +
          "L3RleHR1cmUvYmFkYzA0OGE3Y2U3OGY3ZGE3MzI0YWYzYTM1ZmRmMThjZjQ4NzAzYWFmZDIyZWFh" +
          "YmM3OTRhZmM2YSJ9fX0=";

      private static final Material BORDER  = Material.PURPLE_STAINED_GLASS_PANE;
      private static final Material FILLER  = Material.GRAY_STAINED_GLASS_PANE;
      private static final Material DIVIDER = Material.MAGENTA_STAINED_GLASS_PANE;

      // Ciclo de colores para ítems de rango
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

      public MenuBuilder(ZerDisguise plugin) {
          this.plugin = plugin;
          if (KEY_ACTION == null) {
              KEY_ACTION   = new NamespacedKey(plugin, "zd_action");
              KEY_PLAYER   = new NamespacedKey(plugin, "zd_player");
              KEY_RANK     = new NamespacedKey(plugin, "zd_rank");
              KEY_DISGUISE = new NamespacedKey(plugin, "zd_disguise");
          }
      }

      // ═══════════════════════════════════════════════════════════════════════════
      //  MENÚ PRINCIPAL (54 slots)
      // ═══════════════════════════════════════════════════════════════════════════

      public Inventory buildMainMenu(Player player) {
          ConfigManager cfg = plugin.getConfigManager();
          Inventory inv = Bukkit.createInventory(null, 54, cfg.component(cfg.getMenuTitle()));

          ItemStack border  = glass(BORDER);
          ItemStack filler  = glass(FILLER);
          ItemStack divider = glass(DIVIDER);

          // Fila 0: borde superior
          for (int i = 0; i < 9; i++) inv.setItem(i, border);

          // Fila 1: info del jugador + botones
          inv.setItem(9,  buildPlayerInfoHead(player));
          for (int i = 10; i <= 12; i++) inv.setItem(i, filler);
          inv.setItem(13, buildWriteButton());
          for (int i = 14; i <= 16; i++) inv.setItem(i, filler);
          inv.setItem(17, plugin.getDisguiseManager().isDisguised(player)
                  ? buildRemoveButton(player) : filler);

          // Fila 2: divisor con etiqueta central
          for (int i = 18; i < 27; i++) inv.setItem(i, divider);
          inv.setItem(22, buildSectionLabel(
                  "&#CC88FF&lJugadores conectados",
                  List.of("&8", "&7Haz clic en una cabeza para", "&7ponerte su skin y nombre.", "&8"),
                  Material.COMPASS));

          // Filas 3-4: cabezas de jugadores online (slots 27-44, 18 slots)
          List<? extends Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
          int slot = 27;
          for (Player p : online) {
              if (slot > 44) break;
              inv.setItem(slot++, buildOnlinePlayerHead(p, player));
          }
          while (slot <= 44) inv.setItem(slot++, filler);

          // Fila 5: borde inferior
          for (int i = 45; i < 54; i++) inv.setItem(i, border);

          return inv;
      }

      // ═══════════════════════════════════════════════════════════════════════════
      //  MENÚ DE CONFIRMACIÓN (54 slots)
      // ═══════════════════════════════════════════════════════════════════════════

      public Inventory buildConfirmMenu(Player player, String disguiseName, String selectedRankId) {
          ConfigManager cfg = plugin.getConfigManager();
          RankProvider  rp  = plugin.getRankProvider();

          Inventory inv = Bukkit.createInventory(null, 54,
                  cfg.component("&8» &#CC88FF&lConfirmar disfraz &8«"));

          ItemStack border = glass(BORDER);
          ItemStack filler = glass(FILLER);

          // Fila 0: borde + cabeza preview en slot 4
          for (int i = 0; i < 9; i++) inv.setItem(i, border);
          inv.setItem(4, buildTargetHead(disguiseName, selectedRankId));

          // Filas 1-2: selector de rangos (slots 10-16 y 19-25)
          inv.setItem(9,  border); inv.setItem(17, border);
          inv.setItem(18, border); inv.setItem(26, border);

          List<RankProvider.GroupEntry> groups = rp.getAllGroups();
          int[] rankSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};

          for (int i = 0; i < rankSlots.length; i++) {
              if (i < groups.size()) {
                  RankProvider.GroupEntry g = groups.get(i);
                  boolean sel = g.id().equalsIgnoreCase(selectedRankId);
                  inv.setItem(rankSlots[i], buildRankItem(g, i, sel, disguiseName));
              } else {
                  inv.setItem(rankSlots[i], filler);
              }
          }

          // Filas 3-4: relleno
          for (int i = 27; i < 45; i++) inv.setItem(i, filler);

          // Fila 5: borde + acciones
          for (int i = 45; i < 54; i++) inv.setItem(i, border);
          inv.setItem(46, filler);
          inv.setItem(47, buildActionItem(Material.NAME_TAG,   "rename",  disguiseName, selectedRankId,
                  "&#FFCC00&l✎ Cambiar nombre",
                  List.of("&8", "&7Vuelve al chat para", "&7escribir otro nombre.", "&8")));
          inv.setItem(48, filler);
          inv.setItem(49, buildConfirmButton(disguiseName, selectedRankId));
          inv.setItem(50, filler);
          inv.setItem(51, buildActionItem(Material.ARROW,      "back",    disguiseName, selectedRankId,
                  "&c&l← Volver",
                  List.of("&8", "&7Regresa al menú principal.", "&8")));
          inv.setItem(52, filler);

          return inv;
      }

      // ═══════════════════════════════════════════════════════════════════════════
      //  CONSTRUCTORES DE ÍTEMS
      // ═══════════════════════════════════════════════════════════════════════════

      /** Cabeza del jugador con rango real, disfraz actual y anterior. */
      private ItemStack buildPlayerInfoHead(Player player) {
          ConfigManager   cfg = plugin.getConfigManager();
          DisguiseManager dm  = plugin.getDisguiseManager();
          RankProvider    rp  = plugin.getRankProvider();

          DisguiseManager.DisguiseData cur  = dm.getCurrent(player.getUniqueId());
          DisguiseManager.DisguiseData prev = dm.getPrevious(player.getUniqueId());

          String realPrefix = rp.getPlayerPrefix(player);
          if (realPrefix == null) realPrefix = "&8[&7Default&8]&r";

          String currentDisguise  = cur  != null ? "&#CC88FF" + cur.disguiseName()  : "&8Ninguno";
          String previousDisguise = prev != null ? "&7"       + prev.disguiseName() : "&8Ninguno";

          String rankDisplay = "&7Default";
          if (cur != null) {
              for (ConfigManager.RankEntry r : cfg.getRanks()) {
                  if (r.id().equalsIgnoreCase(cur.rankId())) {
                      String gp = rp.getGroupPrefix(r.id());
                      rankDisplay = (gp != null) ? gp : r.prefix();
                      break;
                  }
              }
          } else {
              rankDisplay = realPrefix;
          }

          ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
          SkullMeta meta  = (SkullMeta) skull.getItemMeta();
          meta.setOwningPlayer(player);
          meta.displayName(noItalic(cfg.component("&f&l" + player.getName())));

          List<Component> lore = new ArrayList<>();
          lore.add(empty());
          lore.add(noItalic(cfg.component("&#9966FF▌ &7Rango en juego")));
          lore.add(noItalic(cfg.componentAny("  " + rankDisplay)));
          lore.add(empty());
          lore.add(noItalic(cfg.component("&#9966FF▌ &7Disfraz actual&8:  " + currentDisguise)));
          lore.add(noItalic(cfg.component("&#9966FF▌ &7Disfraz anterior&8: " + previousDisguise)));
          lore.add(empty());
          meta.lore(lore);
          skull.setItemMeta(meta);
          return skull;
      }

      /** Botón "Escribir disfraz" con cabeza Base64 personalizada. */
      private ItemStack buildWriteButton() {
          ConfigManager cfg   = plugin.getConfigManager();
          ItemStack     skull = new ItemStack(Material.PLAYER_HEAD);
          SkullMeta     meta  = (SkullMeta) skull.getItemMeta();

          try {
              String decoded = new String(Base64.getDecoder().decode(CUSTOM_HEAD_B64));
              String urlStr  = decoded.replaceAll(".*\"url\":\"([^\"]+)\".*", "$1");
              PlayerProfile  profile  = Bukkit.createPlayerProfile(UUID.randomUUID(), "ZerDisguise");
              PlayerTextures textures = profile.getTextures();
              textures.setSkin(URI.create(urlStr).toURL());
              profile.setTextures(textures);
              meta.setOwnerProfile(profile);
          } catch (MalformedURLException | IllegalArgumentException ignored) {}

          meta.displayName(noItalic(cfg.component("&#CC88FF&l✎ Escribir disfraz")));
          List<Component> lore = new ArrayList<>();
          lore.add(empty());
          lore.add(noItalic(cfg.component("&7Escribe en el chat el nombre")));
          lore.add(noItalic(cfg.component("&7de cualquier jugador de Minecraft.")));
          lore.add(empty());
          lore.add(noItalic(cfg.component("&#FFCC00▶ &eHaz clic para comenzar")));
          lore.add(empty());
          meta.lore(lore);

          meta.getPersistentDataContainer().set(KEY_ACTION, PersistentDataType.STRING, "write");
          skull.setItemMeta(meta);
          return skull;
      }

      /** Botón para remover el disfraz activo. */
      private ItemStack buildRemoveButton(Player player) {
          ConfigManager cfg = plugin.getConfigManager();
          DisguiseManager.DisguiseData cur = plugin.getDisguiseManager()
                  .getCurrent(player.getUniqueId());
          String currentName = cur != null ? cur.disguiseName() : "?";

          ItemStack item = new ItemStack(Material.BARRIER);
          ItemMeta  meta = item.getItemMeta();
          meta.displayName(noItalic(cfg.component("&c&l✖ Remover disfraz")));
          List<Component> lore = new ArrayList<>();
          lore.add(empty());
          lore.add(noItalic(cfg.component("&7Disfraz activo&8: &c" + currentName)));
          lore.add(empty());
          lore.add(noItalic(cfg.component("&c▶ Haz clic para quitarlo")));
          lore.add(empty());
          meta.lore(lore);
          meta.getPersistentDataContainer().set(KEY_ACTION, PersistentDataType.STRING, "remove");
          item.setItemMeta(meta);
          return item;
      }

      /** Cabeza de un jugador online para disfraz instantáneo. */
      private ItemStack buildOnlinePlayerHead(Player target, Player viewer) {
          ConfigManager cfg = plugin.getConfigManager();
          RankProvider  rp  = plugin.getRankProvider();

          String prefix       = rp.getPlayerPrefix(target);
          String primaryGroup = rp.getPlayerPrimaryGroup(target);
          if (prefix == null) prefix = "&8[&7Default&8]";

          ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
          SkullMeta meta  = (SkullMeta) skull.getItemMeta();
          meta.setOwningPlayer(target);

          boolean isSelf = target.equals(viewer);
          meta.displayName(noItalic(cfg.component(
                  (isSelf ? "&7" : "&#CC88FF&l") + target.getName()
                          + (isSelf ? " &8(tú)" : ""))));

          List<Component> lore = new ArrayList<>();
          lore.add(empty());
          lore.add(noItalic(cfg.componentAny("  " + prefix)));
          lore.add(empty());
          if (!isSelf) {
              lore.add(noItalic(cfg.component("&#FFCC00▶ &eHaz clic para disfrazarte")));
              lore.add(empty());
          }
          meta.lore(lore);

          // PDC: identificar la acción
          meta.getPersistentDataContainer().set(KEY_ACTION,  PersistentDataType.STRING, "instant_disguise");
          meta.getPersistentDataContainer().set(KEY_PLAYER,  PersistentDataType.STRING, target.getName());
          meta.getPersistentDataContainer().set(KEY_RANK,    PersistentDataType.STRING, primaryGroup);

          skull.setItemMeta(meta);
          return skull;
      }

      /** Ítem selector de rango (cristal de color + PDC). */
      private ItemStack buildRankItem(RankProvider.GroupEntry group, int colorIndex,
                                      boolean selected, String disguiseName) {
          ConfigManager cfg = plugin.getConfigManager();

          Material mat = selected
                  ? Material.LIME_STAINED_GLASS_PANE
                  : RANK_GLASS[colorIndex % RANK_GLASS.length];

          ItemStack item = new ItemStack(mat);
          ItemMeta  meta = item.getItemMeta();

          meta.displayName(noItalic(cfg.componentAny(group.displayPrefix())));
          List<Component> lore = new ArrayList<>();
          lore.add(empty());
          lore.add(noItalic(cfg.component("&8Grupo&8: &7" + group.id())));
          lore.add(empty());
          if (selected) {
              lore.add(noItalic(cfg.component("&a✔ Seleccionado")));
          } else {
              lore.add(noItalic(cfg.component("&#FFCC00▶ &eHaz clic para seleccionar")));
          }
          lore.add(empty());
          meta.lore(lore);

          if (selected) {
              meta.addEnchant(Enchantment.LUCK, 1, true);
              meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
          }

          meta.getPersistentDataContainer().set(KEY_ACTION,  PersistentDataType.STRING, "select_rank");
          meta.getPersistentDataContainer().set(KEY_RANK,    PersistentDataType.STRING, group.id());
          meta.getPersistentDataContainer().set(KEY_DISGUISE,PersistentDataType.STRING, disguiseName);

          item.setItemMeta(meta);
          return item;
      }

      /** Cabeza del objetivo con vista previa del nombre + rango. */
      private ItemStack buildTargetHead(String disguiseName, String rankId) {
          ConfigManager cfg = plugin.getConfigManager();
          RankProvider  rp  = plugin.getRankProvider();

          String rankPrefix = rp.getGroupPrefix(rankId);
          if (rankPrefix == null) {
              // Buscar en config
              for (ConfigManager.RankEntry r : cfg.getRanks()) {
                  if (r.id().equalsIgnoreCase(rankId)) { rankPrefix = r.prefix(); break; }
              }
          }
          if (rankPrefix == null) rankPrefix = "&8[&7Default&8]";

          ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
          SkullMeta meta  = (SkullMeta) skull.getItemMeta();

          Player target = Bukkit.getPlayerExact(disguiseName);
          if (target != null) meta.setOwningPlayer(target);
          else meta.setOwningPlayer(Bukkit.getOfflinePlayer(disguiseName));

          meta.displayName(noItalic(cfg.component("&#CC88FF&l" + disguiseName)));
          List<Component> lore = new ArrayList<>();
          lore.add(empty());
          lore.add(noItalic(cfg.component("&7Vista previa en el chat&8:")));
          lore.add(empty());
          lore.add(noItalic(cfg.componentAny(rankPrefix + " &#CC88FF" + disguiseName)));
          lore.add(empty());
          meta.lore(lore);
          skull.setItemMeta(meta);
          return skull;
      }

      /** Botón de confirmar con PDC. */
      private ItemStack buildConfirmButton(String disguiseName, String rankId) {
          ConfigManager cfg  = plugin.getConfigManager();
          ItemStack     item = new ItemStack(Material.LIME_DYE);
          ItemMeta      meta = item.getItemMeta();
          meta.displayName(noItalic(cfg.component("&a&l✔ Confirmar disfraz")));
          List<Component> lore = new ArrayList<>();
          lore.add(empty());
          lore.add(noItalic(cfg.component("&7Nombre&8: &#CC88FF" + disguiseName)));
          RankProvider rp = plugin.getRankProvider();
          String rp2 = rp.getGroupPrefix(rankId);
          if (rp2 != null) lore.add(noItalic(cfg.componentAny("&7Rango&8:  " + rp2)));
          else lore.add(noItalic(cfg.component("&7Rango&8:  &f" + rankId)));
          lore.add(empty());
          lore.add(noItalic(cfg.component("&a▶ Haz clic para aplicar")));
          lore.add(empty());
          meta.lore(lore);
          meta.addEnchant(Enchantment.LUCK, 1, true);
          meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
          meta.getPersistentDataContainer().set(KEY_ACTION,  PersistentDataType.STRING, "confirm");
          meta.getPersistentDataContainer().set(KEY_DISGUISE,PersistentDataType.STRING, disguiseName);
          meta.getPersistentDataContainer().set(KEY_RANK,    PersistentDataType.STRING, rankId);
          item.setItemMeta(meta);
          return item;
      }

      /** Ítem genérico de acción (con PDC). */
      private ItemStack buildActionItem(Material mat, String action, String disguise,
                                        String rank, String name, List<String> loreLines) {
          ConfigManager cfg  = plugin.getConfigManager();
          ItemStack     item = new ItemStack(mat);
          ItemMeta      meta = item.getItemMeta();
          meta.displayName(noItalic(cfg.component(name)));
          List<Component> lore = new ArrayList<>();
          for (String l : loreLines) lore.add(noItalic(cfg.component(l)));
          meta.lore(lore);
          meta.getPersistentDataContainer().set(KEY_ACTION,  PersistentDataType.STRING, action);
          meta.getPersistentDataContainer().set(KEY_DISGUISE,PersistentDataType.STRING, disguise);
          meta.getPersistentDataContainer().set(KEY_RANK,    PersistentDataType.STRING, rank);
          item.setItemMeta(meta);
          return item;
      }

      /** Etiqueta de sección (sin acción). */
      private ItemStack buildSectionLabel(String title, List<String> loreLines, Material mat) {
          ConfigManager   cfg  = plugin.getConfigManager();
          ItemStack       item = new ItemStack(mat);
          ItemMeta        meta = item.getItemMeta();
          meta.displayName(noItalic(cfg.component(title)));
          List<Component> lore = new ArrayList<>();
          for (String l : loreLines) lore.add(noItalic(cfg.component(l)));
          meta.lore(lore);
          item.setItemMeta(meta);
          return item;
      }

      // ── Helpers ───────────────────────────────────────────────────────────────

      private static ItemStack glass(Material mat) {
          ItemStack g    = new ItemStack(mat);
          ItemMeta  meta = g.getItemMeta();
          meta.displayName(Component.empty());
          g.setItemMeta(meta);
          return g;
      }

      private static Component noItalic(Component c) {
          return c.decoration(TextDecoration.ITALIC, false);
      }

      private static Component empty() {
          return Component.empty().decoration(TextDecoration.ITALIC, false);
      }
  }
  