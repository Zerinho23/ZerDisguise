package me.zerith.zerdisguise;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Construye los menús de ZerDisguise.
 *
 * MENÚ PRINCIPAL (54 slots):
 *   Fila 0  → borde (corner en 0 y 8)
 *   Fila 1  → [Cabeza jugador:9] [relleno] [✦ Escribir:13] [relleno] [✖ Quitar:17]
 *   Fila 2  → divisor + etiqueta central
 *   Filas 3-4 → cabezas de jugadores online (18 por página)
 *   Fila 5  → borde + [◄ Prev:46] [Página:49] [Sig ►:52]
 *
 * MENÚ DE CONFIRMACIÓN (54 slots):
 *   Fila 0  → borde completo
 *   Fila 1  → filler + [SKULL CON SKIN:13] + filler
 *   Fila 2  → divisor con [★ Info:22] al centro
 *   Filas 3-4 → relleno
 *   Fila 5  → borde + [✎ Nombre:46] + [✔ Confirmar:49] + [◄ Volver:52]
 */
public class MenuBuilder {

    public static final int PLAYERS_PER_PAGE = 18;

    public static NamespacedKey KEY_ACTION;
    public static NamespacedKey KEY_PLAYER;
    public static NamespacedKey KEY_RANK;
    public static NamespacedKey KEY_DISGUISE;
    public static NamespacedKey KEY_PAGE;
    public static NamespacedKey KEY_RANK_PAGE;

    private final ZerDisguise plugin;

    public static void initKeys(ZerDisguise plugin) {
        KEY_ACTION   = new NamespacedKey(plugin, "zd_action");
        KEY_PLAYER   = new NamespacedKey(plugin, "zd_player");
        KEY_RANK     = new NamespacedKey(plugin, "zd_rank");
        KEY_DISGUISE = new NamespacedKey(plugin, "zd_disguise");
        KEY_PAGE     = new NamespacedKey(plugin, "zd_page");
        KEY_RANK_PAGE = new NamespacedKey(plugin, "zd_rank_page");
    }

    public MenuBuilder(ZerDisguise plugin) {
        this.plugin = plugin;
    }

    // ─────────────────────────────────────────────────────────────
    //  MENÚ PRINCIPAL
    // ─────────────────────────────────────────────────────────────

    public Inventory buildMainMenu(Player player) {
        return buildMainMenu(player, 0);
    }

    public Inventory buildMainMenu(Player player, int page) {
        ConfigManager cfg = plugin.getConfigManager();
        MenuConfig    mc  = plugin.getMenuConfig();

        ZerInventoryHolder holder = new ZerInventoryHolder(ZerInventoryHolder.MenuType.MAIN);
        Inventory inv = Bukkit.createInventory(holder, 54,
                cfg.component(mc.getMainTitle()));
        holder.setInventory(inv);

        ItemStack border  = glass(mc.getBorderMaterial(),  "");
        ItemStack filler  = glass(mc.getFillerMaterial(),  "");
        ItemStack divider = glass(mc.getDividerMaterial(), "");
        ItemStack corner  = glass(mc.getCornerMaterial(),  "");
        // Acento extra para el borde superior (ligeramente diferente)
        ItemStack accent  = glass(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "");

        // ── Fila 0: borde superior con degradado ──────────────────
        inv.setItem(0, corner);
        inv.setItem(1, accent);
        for (int i = 2; i <= 6; i++) inv.setItem(i, border);
        inv.setItem(7, accent);
        inv.setItem(8, corner);

        // ── Fila 1: cabeza del jugador + botones ──────────────────
        inv.setItem(9, buildPlayerInfoHead(player));
        inv.setItem(10, filler);
          inv.setItem(mc.getRankSelectorSlot(), buildRankSelectorButton(player, mc));
          inv.setItem(12, filler);
        inv.setItem(mc.getWriteSlot(), buildWriteButton(mc));
        for (int i = 14; i <= 16; i++) inv.setItem(i, filler);
        inv.setItem(mc.getRemoveSlot(),
                plugin.getDisguiseManager().isDisguised(player)
                        ? buildRemoveButton(player, mc)
                        : glass(mc.getFillerMaterial(), ""));

        // ── Fila 2: divisor completo + etiqueta ───────────────────
        for (int i = 18; i < 27; i++) inv.setItem(i, divider);
        inv.setItem(mc.getLabelSlot(), buildLabel(
                mc.getLabelMaterial(),
                mc.getLabelName(),
                mc.getLabelLore().isEmpty()
                        ? List.of(
                            "&#CC88FF▸ &7Clic en una cabeza para disfrazarte",
                            "&8  &7instantáneamente con su skin y rango real.",
                            "",
                            "&8▸ &7O usa &e✦ Escribir nombre &7para cualquier jugador.")
                        : mc.getLabelLore()
        ));

        // ── Filas 3-4: cabezas de jugadores online ────────────────
        List<Player> online    = new ArrayList<>(Bukkit.getOnlinePlayers());
        int total      = online.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) PLAYERS_PER_PAGE));
        int safePage   = Math.max(0, Math.min(page, totalPages - 1));
        int start      = safePage * PLAYERS_PER_PAGE;

        int slot = 27;
        for (int i = start; i < Math.min(start + PLAYERS_PER_PAGE, total); i++) {
            inv.setItem(slot++, buildOnlinePlayerHead(online.get(i)));
        }
        while (slot <= 44) inv.setItem(slot++, filler);

        // ── Fila 5: borde inferior + navegación ───────────────────
        inv.setItem(45, corner);
        inv.setItem(46, border);
        inv.setItem(47, border);
        inv.setItem(48, border);
        inv.setItem(49, border); // central
        inv.setItem(50, border);
        inv.setItem(51, border);
        inv.setItem(52, border);
        inv.setItem(53, corner);

        if (safePage > 0) {
            inv.setItem(mc.getPrevSlot(), navButton(
                    "prev_page", safePage - 1, mc.getPrevMaterial(),
                    mc.getPrevName(),
                    List.of("&8▸ &7Regresa a la página &f" + safePage,
                            "&8▸ &7de &f" + totalPages + " &7en total.")
            ));
        }
        inv.setItem(mc.getPageInfoSlot(), buildLabel(
                mc.getPageInfoMaterial(),
                "&f&lPágina &e" + (safePage + 1) + " &8/ &e" + totalPages,
                List.of("&8▸ &7Jugadores online&8: &f" + total,
                        "&8▸ &7Navega con las flechas.")
        ));
        if (safePage < totalPages - 1) {
            inv.setItem(mc.getNextSlot(), navButton(
                    "next_page", safePage + 1, mc.getNextMaterial(),
                    mc.getNextName(),
                    List.of("&8▸ &7Avanza a la página &f" + (safePage + 2),
                            "&8▸ &7de &f" + totalPages + " &7en total.")
            ));
        }

        return inv;
    }

    // ─────────────────────────────────────────────────────────────
    //  MENÚ DE CONFIRMACIÓN — rediseñado v2.2
    // ─────────────────────────────────────────────────────────────

    public Inventory buildConfirmMenu(Player player, String disguiseName) {
        ConfigManager cfg = plugin.getConfigManager();
        MenuConfig    mc  = plugin.getMenuConfig();
        RankProvider  rp  = plugin.getRankProvider();

        ZerInventoryHolder holder = new ZerInventoryHolder(ZerInventoryHolder.MenuType.CONFIRM);
        Inventory inv = Bukkit.createInventory(holder, 54,
                cfg.component(mc.getConfirmTitle()));
        holder.setInventory(inv);

        ItemStack border  = glass(mc.getBorderMaterial(),  "");
        ItemStack corner  = glass(mc.getCornerMaterial(),  "");
        ItemStack filler  = glass(mc.getFillerMaterial(),  "");
        ItemStack divider = glass(mc.getDividerMaterial(), "");
        ItemStack accent  = glass(Material.CYAN_STAINED_GLASS_PANE, "");
        ItemStack mglass  = glass(Material.MAGENTA_STAINED_GLASS_PANE, "");

        // ── Fila 0: borde superior con detalle ────────────────────
        inv.setItem(0, corner);
        inv.setItem(1, accent);
        inv.setItem(2, border);
        inv.setItem(3, mglass);
        inv.setItem(4, border); // centro superior
        inv.setItem(5, mglass);
        inv.setItem(6, border);
        inv.setItem(7, accent);
        inv.setItem(8, corner);

        // ── Fila 1: skull prominente en el centro (slot 13) ───────
        inv.setItem(9,  glass(Material.GRAY_STAINED_GLASS_PANE, ""));
        inv.setItem(10, glass(Material.GRAY_STAINED_GLASS_PANE, ""));
        inv.setItem(11, mglass);
        inv.setItem(12, filler);
        inv.setItem(13, buildPreviewHead(disguiseName, rp, cfg));  // SKULL CON SKIN
        inv.setItem(14, filler);
        inv.setItem(15, mglass);
        inv.setItem(16, glass(Material.GRAY_STAINED_GLASS_PANE, ""));
        inv.setItem(17, glass(Material.GRAY_STAINED_GLASS_PANE, ""));

        // ── Fila 2: divisor + estrella de info al centro ──────────
        inv.setItem(18, divider);
        inv.setItem(19, divider);
        inv.setItem(20, divider);
        inv.setItem(21, divider);
        inv.setItem(22, buildDisguiseInfoLabel(disguiseName, rp, cfg, mc));
        inv.setItem(23, divider);
        inv.setItem(24, divider);
        inv.setItem(25, divider);
        inv.setItem(26, divider);

        // ── Filas 3-4: relleno con pequeños detalles decorativos ──
        for (int i = 27; i < 36; i++) inv.setItem(i, filler);
        for (int i = 36; i < 45; i++) inv.setItem(i, filler);

        // Detalles decorativos en la fila 3 centrada
        inv.setItem(30, glass(Material.PURPLE_STAINED_GLASS_PANE, ""));
        inv.setItem(31, buildStatusItem(disguiseName, rp, cfg));
        inv.setItem(32, glass(Material.PURPLE_STAINED_GLASS_PANE, ""));

        // ── Fila 5: borde + botones de acción ─────────────────────
        inv.setItem(45, corner);
        inv.setItem(46, buildActionButton(mc.getRenameMaterial(),
                "rename", disguiseName, "", mc.getRenameName(),
                mc.getRenameLore().isEmpty()
                        ? List.of("&7Escribe un nombre diferente en el chat.", "",
                                  "&e&l» &eClic para cambiar")
                        : mc.getRenameLore()));
        inv.setItem(47, border);
        inv.setItem(48, border);
        inv.setItem(49, buildConfirmButton(disguiseName, mc));
        inv.setItem(50, border);
        inv.setItem(51, border);
        inv.setItem(52, buildActionButton(mc.getBackMaterial(),
                "back", disguiseName, "", mc.getBackName(),
                mc.getBackLore().isEmpty()
                        ? List.of("&7Regresa al menú principal.", "",
                                  "&c&l» &cClic para volver")
                        : mc.getBackLore()));
        inv.setItem(53, corner);

        return inv;
    }

    // ─────────────────────────────────────────────────────────────
      //  MENÚ DE SELECCIÓN DE RANGO VISUAL
      // ─────────────────────────────────────────────────────────────

      /**
       * Construye el menú de selección de rango visual.
       * Muestra todos los grupos de LuckPerms/Vault (o config.yml si no hay integración).
       * Al elegir un rango, solo se aplica el prefijo visual — sin permisos reales.
       *
       * Layout (54 slots):
       *   Fila 0  → borde
       *   Fila 1  → relleno + etiqueta central
       *   Fila 2  → divisor
       *   Filas 3-4 → ítems de rango (hasta 18 por página)
       *   Fila 5  → [◄ Volver:45] [◄ Prev:46] [Página:49] [Sig ►:52] [corner:53]
       */
      public Inventory buildRankMenu(Player player, int page) {
          ConfigManager cfg = plugin.getConfigManager();
          MenuConfig    mc  = plugin.getMenuConfig();
          RankProvider  rp  = plugin.getRankProvider();

          ZerInventoryHolder holder = new ZerInventoryHolder(ZerInventoryHolder.MenuType.RANK);
          Inventory inv = Bukkit.createInventory(holder, 54,
                  cfg.component(mc.getRankMenuTitle()));
          holder.setInventory(inv);

          ItemStack border  = glass(mc.getBorderMaterial(),  "");
          ItemStack filler  = glass(mc.getFillerMaterial(),  "");
          ItemStack divider = glass(mc.getDividerMaterial(), "");
          ItemStack corner  = glass(mc.getCornerMaterial(),  "");
          ItemStack accent  = glass(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "");

          // ── Fila 0: borde superior ────────────────────────────────
          inv.setItem(0, corner);
          inv.setItem(1, accent);
          for (int i = 2; i <= 6; i++) inv.setItem(i, border);
          inv.setItem(7, accent);
          inv.setItem(8, corner);

          // ── Fila 1: relleno + etiqueta central ───────────────────
          for (int i = 9; i <= 17; i++) inv.setItem(i, filler);
          inv.setItem(13, buildLabel(
                  Material.CHEST,
                  "&#CC88FF&l\u2726 Selector de Rango Visual",
                  List.of(
                      "&8\u25B8 &7Elige un rango para cambiar",
                      "&8  &7solo tu prefijo visible.",
                      "",
                      "&8\u25B8 &o&7Sin permisos reales.",
                      "&8\u25B8 &7Usa &c\u2716 Quitar disfraz &7para restaurar."
                  )
          ));

          // ── Fila 2: divisor ───────────────────────────────────────
          for (int i = 18; i < 27; i++) inv.setItem(i, divider);

          // ── Filas 3-4: ítems de rango ─────────────────────────────
          List<RankProvider.GroupEntry> groups = rp.getAllGroups();
          int total      = groups.size();
          int totalPages = Math.max(1, (int) Math.ceil(total / (double) PLAYERS_PER_PAGE));
          int safePage   = Math.max(0, Math.min(page, totalPages - 1));
          int start      = safePage * PLAYERS_PER_PAGE;
          Material[] glassFallback = mc.getRankGlassFallback();

          int slot = 27;
          for (int i = start; i < Math.min(start + PLAYERS_PER_PAGE, total); i++) {
              inv.setItem(slot++, buildRankItem(groups.get(i), i, glassFallback, cfg, mc));
          }
          while (slot <= 44) inv.setItem(slot++, filler);

          // ── Fila 5: navegación ────────────────────────────────────
          inv.setItem(mc.getRankMenuBackSlot(), buildBackToMainButton(mc));
          inv.setItem(46, border);
          inv.setItem(47, border);
          inv.setItem(48, border);
          inv.setItem(49, border);
          inv.setItem(50, border);
          inv.setItem(51, border);
          inv.setItem(52, border);
          inv.setItem(53, corner);

          if (safePage > 0) {
              inv.setItem(mc.getPrevSlot(), navRankButton(
                      "rank_prev_page", safePage - 1,
                      mc.getPrevMaterial(), mc.getPrevName(),
                      List.of("&8\u25B8 &7Regresa a la pagina &f" + safePage,
                              "&8\u25B8 &7de &f" + totalPages + " &7en total.")
              ));
          }
          inv.setItem(mc.getPageInfoSlot(), buildLabel(
                  mc.getPageInfoMaterial(),
                  "&f&lPagina &e" + (safePage + 1) + " &8/ &e" + totalPages,
                  List.of("&8\u25B8 &7Rangos disponibles&8: &f" + total,
                          "&8\u25B8 &7Navega con las flechas.")
          ));
          if (safePage < totalPages - 1) {
              inv.setItem(mc.getNextSlot(), navRankButton(
                      "rank_next_page", safePage + 1,
                      mc.getNextMaterial(), mc.getNextName(),
                      List.of("&8\u25B8 &7Avanza a la pagina &f" + (safePage + 2),
                              "&8\u25B8 &7de &f" + totalPages + " &7en total.")
              ));
          }

          return inv;
      }

      // ─────────────────────────────────────────────────────────────
    //  CONSTRUCTORES DE ÍTEMS — MENÚ PRINCIPAL
    // ─────────────────────────────────────────────────────────────

    private ItemStack buildPlayerInfoHead(Player player) {
        ConfigManager   cfg = plugin.getConfigManager();
        DisguiseManager dm  = plugin.getDisguiseManager();
        RankProvider    rp  = plugin.getRankProvider();

        DisguiseManager.DisguiseData cur  = dm.getCurrent(player.getUniqueId());
        DisguiseManager.DisguiseData prev = dm.getPrevious(player.getUniqueId());

        String realPrefix = rp.getPlayerPrefix(player);
        if (realPrefix == null || realPrefix.isBlank()) realPrefix = "&8[&7Default&8]";

        boolean disguised = cur != null;
        String curName  = disguised ? "&#CC88FF" + cur.disguiseName()  : "&8─ ninguno";
        String prevName = prev != null ? "&7" + prev.disguiseName()    : "&8─ ninguno";
        String estadoColor = disguised ? "&a" : "&7";
        String estadoIcon  = disguised ? "● Disfrazado" : "● Sin disfraz";

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta  = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(player);
        meta.displayName(ni(cfg.component("&#FFFFFF&l" + player.getName())));

        List<Component> lore = new ArrayList<>();
        addLoreLine(lore, cfg, "&8┌──────────────────────");
        addLoreLine(lore, cfg, "&8│ &8▸ &dTu perfil");
        addLoreLine(lore, cfg, "&8├──────────────────────");
        lore.add(ni(cfg.componentAny("&8│ &7Rango&8:    " + realPrefix)));
        addLoreLine(lore, cfg, "&8│ &7Estado&8:   " + estadoColor + estadoIcon);
        addLoreLine(lore, cfg, "&8│ &7Disfraz&8:  " + curName);
        addLoreLine(lore, cfg, "&8│ &7Anterior&8: " + prevName);
        addLoreLine(lore, cfg, "&8├──────────────────────");
        addLoreLine(lore, cfg, "&8│ &8Ping&8: &f" + player.getPing() + "ms");
        addLoreLine(lore, cfg, "&8└──────────────────────");
        meta.lore(lore);
        skull.setItemMeta(meta);
        return skull;
    }

    private ItemStack buildWriteButton(MenuConfig mc) {
        ConfigManager cfg  = plugin.getConfigManager();
        ItemStack     item = new ItemStack(mc.getWriteMaterial());
        ItemMeta      meta = item.getItemMeta();

        meta.displayName(ni(cfg.component(mc.getWriteName())));

        List<String> rawLore = mc.getWriteLore().isEmpty()
                ? List.of(
                    "&7Escribe el nombre de cualquier jugador",
                    "&7de Minecraft (online u offline).",
                    "",
                    "&8│ &7Online  &8→ &aSkin + rango detectados",
                    "&8│ &7Offline &8→ &7Skin desde Mojang API",
                    "",
                    "&#FFDD00&l» &eHaz clic para comenzar")
                : mc.getWriteLore();

        meta.lore(buildBoxedLore(rawLore, cfg));
        meta.getPersistentDataContainer().set(KEY_ACTION, PersistentDataType.STRING, "write");
        addGlow(meta);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildRemoveButton(Player player, MenuConfig mc) {
        ConfigManager cfg = plugin.getConfigManager();
        DisguiseManager.DisguiseData cur = plugin.getDisguiseManager()
                .getCurrent(player.getUniqueId());
        String curName = cur != null ? "&#CC88FF" + cur.disguiseName() : "&8ninguno";

        ItemStack item = new ItemStack(mc.getRemoveMaterial());
        ItemMeta  meta = item.getItemMeta();
        meta.displayName(ni(cfg.component(mc.getRemoveName())));

        List<String> rawLore = mc.getRemoveLore().isEmpty()
                ? List.of(
                    "&7Elimina tu disfraz actual y restaura",
                    "&7tu nombre y skin originales.",
                    "",
                    "&8│ &7Activo&8: " + curName,
                    "",
                    "&c&l» &cClic para remover")
                : mc.getRemoveLore();

        meta.lore(buildBoxedLore(rawLore, cfg));
        meta.getPersistentDataContainer().set(KEY_ACTION, PersistentDataType.STRING, "remove");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildOnlinePlayerHead(Player online) {
        ConfigManager   cfg = plugin.getConfigManager();
        RankProvider    rp  = plugin.getRankProvider();
        DisguiseManager dm  = plugin.getDisguiseManager();

        String rankId = rp.getPlayerPrimaryGroup(online);
        String prefix = rp.getPlayerPrefix(online);
        if (prefix == null || prefix.isBlank()) {
            prefix = rp.getGroupPrefix(rankId);
        }
        if (prefix == null || prefix.isBlank()) prefix = "&8[&7Default&8]";

        boolean isDsg  = dm.isDisguised(online);
        String  estado = isDsg ? "&6● Disfrazado" : "&a● Online";

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta  = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(online);
        meta.displayName(ni(cfg.component("&#CC88FF&l" + online.getName())));

        List<Component> lore = new ArrayList<>();
        addLoreLine(lore, cfg, "&8┌──────────────────────");
        lore.add(ni(cfg.componentAny("&8│ &8Rango&8:  " + prefix)));
        addLoreLine(lore, cfg, "&8│ &7Ping&8:   &f" + online.getPing() + "ms");
        addLoreLine(lore, cfg, "&8│ &7Estado&8: " + estado);
        if (isDsg) {
            DisguiseManager.DisguiseData d = dm.getCurrent(online.getUniqueId());
            if (d != null) addLoreLine(lore, cfg, "&8│ &7Como&8:   &d" + d.disguiseName());
        }
        addLoreLine(lore, cfg, "&8├──────────────────────");
        addLoreLine(lore, cfg, "&8│ &#FFDD00&l» &eClic para disfrazarte");
        addLoreLine(lore, cfg, "&8│ &7como &d" + online.getName());
        addLoreLine(lore, cfg, "&8└──────────────────────");
        meta.lore(lore);

        var pdc = meta.getPersistentDataContainer();
        pdc.set(KEY_ACTION, PersistentDataType.STRING, "instant_disguise");
        pdc.set(KEY_PLAYER, PersistentDataType.STRING, online.getName());
        pdc.set(KEY_RANK,   PersistentDataType.STRING, rankId.isBlank() ? "default" : rankId);
        skull.setItemMeta(meta);
        return skull;
    }

    // ─────────────────────────────────────────────────────────────
    //  CONSTRUCTORES DE ÍTEMS — MENÚ DE CONFIRMACIÓN
    // ─────────────────────────────────────────────────────────────

    /**
     * Construye la cabeza de vista previa en el menú de confirmación.
     *
     * Para jugadores ONLINE → usa su perfil directo (SkullMeta.setOwningPlayer).
     * Para jugadores OFFLINE → si la skin ya fue pre-cargada en caché por ChatListener,
     *   crea un PlayerProfile con la propiedad "textures" y lo aplica con setPlayerProfile().
     *   De lo contrario, usa setOwner(name) como fallback (el servidor resuelve async).
     */
    private ItemStack buildPreviewHead(String disguiseName, RankProvider rp, ConfigManager cfg) {
        Player    target = Bukkit.getPlayerExact(disguiseName);
        boolean   online = target != null;
        String    rankPrefix = online ? rp.getPlayerPrefix(target) : null;
        String    rankId     = online ? rp.getPlayerPrimaryGroup(target) : "?";
        String    estado     = online ? "&a● Online" : "&8● Offline";

        String rankClean = rankPrefix != null && !rankPrefix.isBlank()
                ? rankPrefix
                : "&8[&7" + capitalize(rankId) + "&8]";

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta  = (SkullMeta) skull.getItemMeta();

        // ── Aplicar skin correctamente ────────────────────────────
        applySkullSkin(meta, disguiseName, target);

        meta.displayName(ni(cfg.component("&#CC88FF&l" + disguiseName)));
        List<Component> lore = new ArrayList<>();
        addLoreLine(lore, cfg, "&8┌──────────────────────");
        addLoreLine(lore, cfg, "&8│ &7Vista previa del disfraz");
        addLoreLine(lore, cfg, "&8├──────────────────────");
        addLoreLine(lore, cfg, "&8│ &8Nombre&8: &d" + disguiseName);
        lore.add(ni(cfg.componentAny("&8│ &8Rango&8:  " + rankClean)));
        addLoreLine(lore, cfg, "&8│ &7Estado&8: " + estado);
        addLoreLine(lore, cfg, "&8├──────────────────────");
        addLoreLine(lore, cfg, "&8│ &7Confirma abajo para aplicar.");
        addLoreLine(lore, cfg, "&8└──────────────────────");
        meta.lore(lore);
        addGlow(meta);
        skull.setItemMeta(meta);
        return skull;
    }

    /**
     * Aplica la textura de skin al SkullMeta de forma óptima:
     *  1. Online  → setOwningPlayer (tiene el perfil completo con su skin real).
     *  2. Offline + skin en caché → setPlayerProfile con textura cacheada.
     *  3. Fallback → setOwner(name) (el servidor resuelve desde Mojang asíncronamente).
     */
    private void applySkullSkin(SkullMeta meta, String playerName, Player onlinePlayer) {
        if (onlinePlayer != null) {
            meta.setOwningPlayer(onlinePlayer);
            return;
        }

        SkinFetcher.SkinData cached = plugin.getSkinFetcher().getCached(playerName);
        if (cached != null) {
            try {
                // UUID offline estándar de Minecraft: nameUUIDFromBytes("OfflinePlayer:<name>")
                UUID offlineId = UUID.nameUUIDFromBytes(
                        ("OfflinePlayer:" + playerName).getBytes(StandardCharsets.UTF_8));
                PlayerProfile profile = Bukkit.createProfile(offlineId, playerName);
                profile.setProperty(new ProfileProperty("textures", cached.value(), cached.signature()));
                meta.setPlayerProfile(profile);
                return;
            } catch (Exception e) {
                // Si falla la asignación de perfil custom, caemos al fallback
            }
        }

        // Fallback: el servidor buscará el UUID y skin de forma asíncrona
        //noinspection deprecation
        meta.setOwner(playerName);
    }

    private ItemStack buildDisguiseInfoLabel(String disguiseName, RankProvider rp,
                                             ConfigManager cfg, MenuConfig mc) {
        Player  target     = Bukkit.getPlayerExact(disguiseName);
        boolean online     = target != null;
        String  rankPrefix = online ? rp.getPlayerPrefix(target) : null;
        String  rankId     = online ? rp.getPlayerPrimaryGroup(target) : null;
        String  rankClean  = rankPrefix != null && !rankPrefix.isBlank()
                ? rankPrefix
                : (rankId != null ? "&8[&f" + capitalize(rankId) + "&8]" : "&8[&7Default&8]");
        String  estado     = online ? "&a● Online" : "&8● Offline &7(skin desde Mojang)";

        boolean skinCached = plugin.getSkinFetcher().getCached(disguiseName) != null;
        String  skinStatus = online ? "&a✔ Skin directa del jugador"
                           : (skinCached ? "&a✔ Skin lista" : "&e⚠ Skin en espera");

        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta  meta = item.getItemMeta();
        meta.displayName(ni(cfg.component("&#CC88FF&l✦ Información del disfraz")));
        addGlow(meta);

        List<Component> lore = new ArrayList<>();
        addLoreLine(lore, cfg, "&8┌──────────────────────");
        addLoreLine(lore, cfg, "&8│ &8Nombre&8: &d" + disguiseName);
        lore.add(ni(cfg.componentAny("&8│ &8Rango&8:  " + rankClean)));
        addLoreLine(lore, cfg, "&8│ &7Estado&8: " + estado);
        addLoreLine(lore, cfg, "&8│ &7Skin&8:   " + skinStatus);
        addLoreLine(lore, cfg, "&8├──────────────────────");
        addLoreLine(lore, cfg, "&8│ &7Al confirmar se aplicarán&8:");
        addLoreLine(lore, cfg, "&8│  &#CC88FF✦ &7Nombre visible");
        addLoreLine(lore, cfg, "&8│  &#CC88FF✦ &7Skin del jugador");
        addLoreLine(lore, cfg, "&8│  &#CC88FF✦ &7Rango en nameplate");
        addLoreLine(lore, cfg, "&8└──────────────────────");
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Ítem de estado decorativo que muestra si el objetivo es online/offline
     * con su información básica.
     */
    private ItemStack buildStatusItem(String disguiseName, RankProvider rp, ConfigManager cfg) {
        Player  target = Bukkit.getPlayerExact(disguiseName);
        boolean online = target != null;

        Material mat = online ? Material.LIME_DYE : Material.GRAY_DYE;
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        meta.displayName(ni(cfg.component(online ? "&a&l● Online" : "&8&l● Offline")));

        List<Component> lore = new ArrayList<>();
        if (online) {
            addLoreLine(lore, cfg, "&7Ping&8: &f" + target.getPing() + "ms");
            addLoreLine(lore, cfg, "&7Mundo&8: &f" + target.getWorld().getName());
        } else {
            addLoreLine(lore, cfg, "&7El jugador no está conectado.");
            addLoreLine(lore, cfg, "&7La skin se carga desde Mojang API.");
        }
        meta.lore(lore.isEmpty() ? null : lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildConfirmButton(String disguiseName, MenuConfig mc) {
        ConfigManager cfg  = plugin.getConfigManager();
        RankProvider  rp   = plugin.getRankProvider();
        Player        tgt  = Bukkit.getPlayerExact(disguiseName);
        String        rankTxt = tgt != null
                ? (rp.getPlayerPrefix(tgt) != null && !rp.getPlayerPrefix(tgt).isBlank()
                        ? rp.getPlayerPrefix(tgt) : "&7" + rp.getPlayerPrimaryGroup(tgt))
                : "&7auto (de LuckPerms al confirmar)";

        ItemStack item = new ItemStack(mc.getConfirmBtnMaterial());
        ItemMeta  meta = item.getItemMeta();

        meta.displayName(ni(cfg.component(mc.getConfirmBtnName())));
        List<Component> lore = new ArrayList<>();
        addLoreLine(lore, cfg, "&8┌──────────────────────");
        addLoreLine(lore, cfg, "&8│ &7Se aplicará&8:");
        addLoreLine(lore, cfg, "&8│  &#CC88FF✦ &8Nombre&8: &d" + disguiseName);
        lore.add(ni(cfg.componentAny("&8│  &#CC88FF✦ &8Rango&8:  " + rankTxt)));
        addLoreLine(lore, cfg, "&8│  &#CC88FF✦ &8Skin&8:   &7La de &d" + disguiseName);
        addLoreLine(lore, cfg, "&8├──────────────────────");
        addLoreLine(lore, cfg, "&8│ &a&l» &aHaz clic para confirmar");
        addLoreLine(lore, cfg, "&8└──────────────────────");
        meta.lore(lore);

        var pdc = meta.getPersistentDataContainer();
        pdc.set(KEY_ACTION,   PersistentDataType.STRING, "confirm");
        pdc.set(KEY_DISGUISE, PersistentDataType.STRING, disguiseName);
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
        meta.displayName(ni(cfg.component(name)));
        meta.lore(buildBoxedLore(loreTxt, cfg));

        var pdc = meta.getPersistentDataContainer();
        pdc.set(KEY_ACTION,   PersistentDataType.STRING, action);
        pdc.set(KEY_DISGUISE, PersistentDataType.STRING, disguiseName != null ? disguiseName : "");
        pdc.set(KEY_RANK,     PersistentDataType.STRING, rankId       != null ? rankId       : "");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildLabel(Material mat, String name, List<String> loreTxt) {
        ConfigManager cfg  = plugin.getConfigManager();
        ItemStack     item = new ItemStack(mat);
        ItemMeta      meta = item.getItemMeta();
        meta.displayName(ni(cfg.component(name)));
        meta.lore(buildBoxedLore(loreTxt, cfg));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack navButton(String action, int targetPage,
                                Material mat, String name, List<String> loreTxt) {
        ConfigManager cfg  = plugin.getConfigManager();
        ItemStack     item = new ItemStack(mat);
        ItemMeta      meta = item.getItemMeta();
        meta.displayName(ni(cfg.component(name)));
        meta.lore(buildBoxedLore(loreTxt, cfg));
        var pdc = meta.getPersistentDataContainer();
        pdc.set(KEY_ACTION, PersistentDataType.STRING, action);
        pdc.set(KEY_PAGE,   PersistentDataType.INTEGER, targetPage);
        item.setItemMeta(meta);
        return item;
    }

    // ─────────────────────────────────────────────────────────────
    //  UTILIDADES
    // ─────────────────────────────────────────────────────────────

    private List<Component> buildBoxedLore(List<String> lines, ConfigManager cfg) {
        List<Component> result = new ArrayList<>();
        result.add(ni(cfg.component("&8┌──────────────────────")));
        for (String line : lines) {
            if (line == null || line.isEmpty()) {
                result.add(ni(cfg.component("&8│")));
            } else {
                result.add(ni(cfg.component("&8│ " + line)));
            }
        }
        result.add(ni(cfg.component("&8└──────────────────────")));
        return result;
    }

    private void addLoreLine(List<Component> lore, ConfigManager cfg, String text) {
        lore.add(ni(cfg.component(text)));
    }

    private static ItemStack glass(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        meta.displayName(Component.empty());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    private static Component ni(Component c) {
        return c.decoration(TextDecoration.ITALIC, false);
    }

    private static void addGlow(ItemMeta meta) {
        Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));
        if (ench != null) meta.addEnchant(ench, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
    // ─────────────────────────────────────────────────────────────
      //  HELPERS DEL MENÚ DE RANGO
      // ─────────────────────────────────────────────────────────────

      private ItemStack buildRankSelectorButton(Player player, MenuConfig mc) {
          ConfigManager   cfg = plugin.getConfigManager();
          DisguiseManager dm  = plugin.getDisguiseManager();

          String  activeRank = dm.getVisualRank(player.getUniqueId());
          boolean hasActive  = activeRank != null || dm.isDisguised(player);

          ItemStack item = new ItemStack(mc.getRankSelectorMaterial());
          ItemMeta  meta = item.getItemMeta();
          meta.displayName(ni(cfg.component(mc.getRankSelectorName())));

          List<String>    loreCfg = mc.getRankSelectorLore();
          List<Component> lore    = new ArrayList<>();
          if (loreCfg.isEmpty()) {
              addLoreLine(lore, cfg, "&7Elige un rango para cambiar");
              addLoreLine(lore, cfg, "&7solo tu prefijo visible.");
              addLoreLine(lore, cfg, "");
              addLoreLine(lore, cfg, "&8Nota&8: &o&7sin permisos reales.");
              if (hasActive) {
                  addLoreLine(lore, cfg, "");
                  addLoreLine(lore, cfg, "&8Rango actual&8: &#CC88FF"
                          + (activeRank != null ? activeRank : "del disfraz"));
              }
              addLoreLine(lore, cfg, "");
              addLoreLine(lore, cfg, "&#FFDD00&l\u00BB &eClic para abrir");
          } else {
              for (String line : loreCfg) lore.add(ni(cfg.component(line)));
          }
          meta.lore(lore);

          var pdc = meta.getPersistentDataContainer();
          pdc.set(KEY_ACTION, PersistentDataType.STRING, "rank_menu");
          item.setItemMeta(meta);
          return item;
      }

      private ItemStack buildRankItem(RankProvider.GroupEntry group, int index,
                                      Material[] glassFallback, ConfigManager cfg,
                                      MenuConfig mc) {
          Material mat = glassFallback[index % glassFallback.length];

          for (ConfigManager.RankEntry r : cfg.getRanks()) {
              if (r.id().equalsIgnoreCase(group.id())) {
                  mat = r.glass();
                  break;
              }
          }

          ItemStack item = new ItemStack(mat);
          ItemMeta  meta = item.getItemMeta();
          meta.displayName(ni(cfg.componentAny(group.displayPrefix())));

          List<String>    itemLoreCfg = mc.getRankMenuItemLore();
          List<Component> lore = new ArrayList<>();
          addLoreLine(lore, cfg, "&8ID&8: &7" + group.id());
          if (itemLoreCfg.isEmpty()) {
              addLoreLine(lore, cfg, "");
              addLoreLine(lore, cfg, "&8▸ &7Solo cambia el prefijo visible.");
              addLoreLine(lore, cfg, "&8▸ &o&7Sin permisos reales.");
              addLoreLine(lore, cfg, "");
              addLoreLine(lore, cfg, "&#FFDD00&l» &eClic para aplicar");
          } else {
              for (String line : itemLoreCfg) lore.add(ni(cfg.component(line)));
          }
          meta.lore(lore);

          var pdc = meta.getPersistentDataContainer();
          pdc.set(KEY_ACTION, PersistentDataType.STRING, "select_rank");
          pdc.set(KEY_RANK,   PersistentDataType.STRING, group.id());
          item.setItemMeta(meta);
          return item;
      }

      private ItemStack buildBackToMainButton(MenuConfig mc) {
          ConfigManager cfg  = plugin.getConfigManager();
          ItemStack     item = new ItemStack(mc.getRankMenuBackMaterial());
          ItemMeta      meta = item.getItemMeta();
          meta.displayName(ni(cfg.component(mc.getRankMenuBackName())));

          List<String>    loreCfg = mc.getRankMenuBackLore();
          List<Component> lore    = new ArrayList<>();
          if (loreCfg.isEmpty()) {
              addLoreLine(lore, cfg, "&7Regresa al menu principal.");
              addLoreLine(lore, cfg, "");
              addLoreLine(lore, cfg, "&c&l» &cClic para volver");
          } else {
              for (String line : loreCfg) lore.add(ni(cfg.component(line)));
          }
          meta.lore(lore);

          var pdc = meta.getPersistentDataContainer();
          pdc.set(KEY_ACTION, PersistentDataType.STRING, "rank_back");
          item.setItemMeta(meta);
          return item;
      }

      private ItemStack navRankButton(String action, int targetPage, Material mat,
                                      String name, List<String> loreLines) {
          ConfigManager   cfg  = plugin.getConfigManager();
          ItemStack       item = new ItemStack(mat);
          ItemMeta        meta = item.getItemMeta();
          meta.displayName(ni(cfg.component(name)));

          List<Component> lore = new ArrayList<>();
          for (String line : loreLines) addLoreLine(lore, cfg, line);
          meta.lore(lore);

          var pdc = meta.getPersistentDataContainer();
          pdc.set(KEY_ACTION,    PersistentDataType.STRING,  action);
          pdc.set(KEY_RANK_PAGE, PersistentDataType.INTEGER, targetPage);
          item.setItemMeta(meta);
          return item;
      }

  
}
