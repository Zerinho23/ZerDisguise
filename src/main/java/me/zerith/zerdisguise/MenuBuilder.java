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
 * Construye los menus de ZerDisguise con diseño oscuro tipo panel informativo.
 *
 * Menu principal (54 slots):
 *   Fila 0  -> borde gris
 *   Fila 1  -> [Cabeza info][relleno][Escribir][relleno][Quitar]
 *   Fila 2  -> divisor oscuro + etiqueta central
 *   Filas 3-4 -> cabezas de jugadores online (18 por pagina)
 *   Fila 5  -> borde + [Anterior][relleno][Info pagina][relleno][Siguiente]
 *
 * Menu de confirmacion (54 slots):
 *   Fila 0  -> borde + cabeza preview (slot 4)
 *   Filas 1-2 -> selector de rangos
 *   Filas 3-4 -> relleno
 *   Fila 5  -> borde + [Cambiar nombre][relleno][Confirmar][relleno][Volver]
 */
public class MenuBuilder {

    public static final int PLAYERS_PER_PAGE = 18;

    public static NamespacedKey KEY_ACTION;
    public static NamespacedKey KEY_PLAYER;
    public static NamespacedKey KEY_RANK;
    public static NamespacedKey KEY_DISGUISE;
    public static NamespacedKey KEY_PAGE;

    private static final Material BORDER  = Material.GRAY_STAINED_GLASS_PANE;
    private static final Material FILLER  = Material.BLACK_STAINED_GLASS_PANE;
    private static final Material DIVIDER = Material.PURPLE_STAINED_GLASS_PANE;
    private static final Material CORNER  = Material.CYAN_STAINED_GLASS_PANE;

    private static final Material[] RANK_GLASS = {
        Material.RED_STAINED_GLASS_PANE,
        Material.ORANGE_STAINED_GLASS_PANE,
        Material.YELLOW_STAINED_GLASS_PANE,
        Material.LIME_STAINED_GLASS_PANE,
        Material.GREEN_STAINED_GLASS_PANE,
        Material.CYAN_STAINED_GLASS_PANE,
        Material.LIGHT_BLUE_STAINED_GLASS_PANE,
        Material.BLUE_STAINED_GLASS_PANE,
        Material.PURPLE_STAINED_GLASS_PANE,
        Material.MAGENTA_STAINED_GLASS_PANE,
        Material.PINK_STAINED_GLASS_PANE,
        Material.WHITE_STAINED_GLASS_PANE,
        Material.BROWN_STAINED_GLASS_PANE,
        Material.GRAY_STAINED_GLASS_PANE,
    };

    private final ZerDisguise plugin;

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

    // ─────────────────────────────────────────────────────────
    //  MENU PRINCIPAL
    // ─────────────────────────────────────────────────────────

    public Inventory buildMainMenu(Player player) {
        return buildMainMenu(player, 0);
    }

    public Inventory buildMainMenu(Player player, int page) {
        ConfigManager cfg = plugin.getConfigManager();
        Inventory inv = Bukkit.createInventory(null, 54,
                cfg.component("&8» &#CC88FFZerDisguise &8«"));

        ItemStack border  = glass(BORDER, "");
        ItemStack filler  = glass(FILLER, "");
        ItemStack divider = glass(DIVIDER, "");
        ItemStack corner  = glass(CORNER, "");

        // Fila 0: esquinas cyan + borde gris
        inv.setItem(0, corner);
        for (int i = 1; i <= 7; i++) inv.setItem(i, border);
        inv.setItem(8, corner);

        // Fila 1: cabeza del jugador | relleno | escribir | relleno | quitar
        inv.setItem(9,  buildPlayerInfoHead(player));
        for (int i = 10; i <= 12; i++) inv.setItem(i, filler);
        inv.setItem(13, buildWriteButton());
        for (int i = 14; i <= 16; i++) inv.setItem(i, filler);
        inv.setItem(17, plugin.getDisguiseManager().isDisguised(player)
                ? buildRemoveButton(player) : glass(BORDER, ""));

        // Fila 2: divisor morado + etiqueta central
        for (int i = 18; i < 27; i++) inv.setItem(i, divider);
        inv.setItem(22, buildLabel(
                Material.NETHER_STAR,
                "&#CC88FF&l✦ Jugadores en linea",
                List.of(
                    "&8▸ &7Haz clic en una cabeza para",
                    "&8  &7ponerte su nombre y skin.",
                    "&8▸ &7Se aplica instantaneamente."
                )
        ));

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

        // Fila 5: navegacion
        inv.setItem(45, corner);
        for (int i = 46; i <= 53; i++) inv.setItem(i, border);
        inv.setItem(53, corner);

        if (safePage > 0) {
            inv.setItem(46, navButton("prev_page", safePage - 1,
                    "&e&l← Pagina anterior",
                    List.of(
                        "&8▸ &7Regresa a la pagina &f" + safePage,
                        "&8▸ &7de &f" + totalPages + " &7en total."
                    )
            ));
        }
        inv.setItem(49, buildLabel(Material.PAPER,
                "&f&lPagina &e" + (safePage + 1) + " &8/ &e" + totalPages,
                List.of(
                    "&8▸ &7Jugadores online&8: &f" + total,
                    "&8▸ &7Navega con las flechas."
                )
        ));
        if (safePage < totalPages - 1) {
            inv.setItem(52, navButton("next_page", safePage + 1,
                    "&e&lPagina siguiente →",
                    List.of(
                        "&8▸ &7Avanza a la pagina &f" + (safePage + 2),
                        "&8▸ &7de &f" + totalPages + " &7en total."
                    )
            ));
        }

        return inv;
    }

    // ─────────────────────────────────────────────────────────
    //  MENU DE CONFIRMACION
    // ─────────────────────────────────────────────────────────

    public Inventory buildConfirmMenu(Player player, String disguiseName, String selectedRankId) {
        ConfigManager cfg = plugin.getConfigManager();
        RankProvider  rp  = plugin.getRankProvider();

        Inventory inv = Bukkit.createInventory(null, 54,
                cfg.component("&8» &#CC88FF&lConfirmar disfraz &8«"));

        ItemStack border = glass(BORDER, "");
        ItemStack corner = glass(CORNER, "");
        ItemStack filler = glass(FILLER, "");

        // Fila 0: borde + cabeza de preview centrada en slot 4
        inv.setItem(0, corner);
        for (int i = 1; i <= 7; i++) inv.setItem(i, border);
        inv.setItem(8, corner);
        inv.setItem(4, buildPreviewHead(disguiseName, selectedRankId));

        // Fila 1-2: selector de rangos (slots 10-16, 19-25)
        inv.setItem(9, glass(DIVIDER, ""));
        inv.setItem(17, glass(DIVIDER, ""));
        inv.setItem(18, glass(DIVIDER, ""));
        inv.setItem(26, glass(DIVIDER, ""));

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

        // Filas 3-4: relleno
        for (int i = 27; i < 45; i++) inv.setItem(i, filler);

        // Fila 5: botones de accion
        inv.setItem(45, corner);
        for (int i = 46; i <= 53; i++) inv.setItem(i, border);
        inv.setItem(53, corner);

        inv.setItem(46, buildActionButton(Material.NAME_TAG, "rename",
                disguiseName, selectedRankId,
                "&e&l✎ Cambiar nombre",
                List.of(
                    "&8▸ &7Escribe un nombre diferente",
                    "&8  &7en el chat.",
                    "",
                    "&e&l» &eClic para cambiar"
                )
        ));
        inv.setItem(49, buildConfirmButton(disguiseName, selectedRankId));
        inv.setItem(52, buildActionButton(Material.ARROW, "back",
                disguiseName, selectedRankId,
                "&c&l← Volver",
                List.of(
                    "&8▸ &7Regresa al menu principal.",
                    "",
                    "&c&l» &cClic para volver"
                )
        ));

        return inv;
    }

    // ─────────────────────────────────────────────────────────
    //  CONSTRUCTORES DE ITEMS
    // ─────────────────────────────────────────────────────────

    private ItemStack buildPlayerInfoHead(Player player) {
        ConfigManager   cfg = plugin.getConfigManager();
        DisguiseManager dm  = plugin.getDisguiseManager();
        RankProvider    rp  = plugin.getRankProvider();

        DisguiseManager.DisguiseData cur  = dm.getCurrent(player.getUniqueId());
        DisguiseManager.DisguiseData prev = dm.getPrevious(player.getUniqueId());

        String realPrefix = rp.getPlayerPrefix(player);
        if (realPrefix == null || realPrefix.isBlank()) realPrefix = "&8[&7Default&8]";

        String curName  = cur  != null ? "&#CC88FF" + cur.disguiseName()  : "&8Ninguno";
        String prevName = prev != null ? "&7"        + prev.disguiseName() : "&8Ninguno";
        String estado   = cur  != null ? "&a● Disfrazado" : "&c● Sin disfraz";

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta  = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(player);
        meta.displayName(ni(cfg.component("&#FFFFFF&l" + player.getName())));

        List<Component> lore = new ArrayList<>();
        lore.add(ni(cfg.component("&8┌─────────────────")));
        lore.add(ni(cfg.component("&8│ &7Tu perfil")));
        lore.add(ni(cfg.component("&8├─────────────────")));
        lore.add(ni(cfg.componentAny("&8│ &8Rango&8:    " + realPrefix)));
        lore.add(ni(cfg.component("&8│ &7Estado&8:   " + estado)));
        lore.add(ni(cfg.component("&8│ &7Disfraz&8:  " + curName)));
        lore.add(ni(cfg.component("&8│ &7Anterior&8: " + prevName)));
        lore.add(ni(cfg.component("&8└─────────────────")));
        meta.lore(lore);
        skull.setItemMeta(meta);
        return skull;
    }

    private ItemStack buildWriteButton() {
        ConfigManager cfg  = plugin.getConfigManager();
        ItemStack     item = new ItemStack(Material.NAME_TAG);
        ItemMeta      meta = item.getItemMeta();

        meta.displayName(ni(cfg.component("&#CC88FF&l✦ Escribir nombre")));
        List<Component> lore = new ArrayList<>();
        lore.add(ni(cfg.component("&8┌─────────────────")));
        lore.add(ni(cfg.component("&8│ &7Escribe el nombre de cualquier")));
        lore.add(ni(cfg.component("&8│ &7jugador de Minecraft.")));
        lore.add(ni(cfg.component("&8│")));
        lore.add(ni(cfg.component("&8│ &8Nota&8: &7el nombre debe existir")));
        lore.add(ni(cfg.component("&8│ &7en los servidores de Mojang.")));
        lore.add(ni(cfg.component("&8├─────────────────")));
        lore.add(ni(cfg.component("&8│ &#FFDD00&l» &eHaz clic para comenzar")));
        lore.add(ni(cfg.component("&8└─────────────────")));
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
        String curName = cur != null ? "&#CC88FF" + cur.disguiseName() : "&8ninguno";

        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta  meta = item.getItemMeta();
        meta.displayName(ni(cfg.component("&c&l✖ Quitar disfraz")));
        List<Component> lore = new ArrayList<>();
        lore.add(ni(cfg.component("&8┌─────────────────")));
        lore.add(ni(cfg.component("&8│ &7Elimina tu disfraz actual")));
        lore.add(ni(cfg.component("&8│ &7y restaura tu apariencia.")));
        lore.add(ni(cfg.component("&8├─────────────────")));
        lore.add(ni(cfg.component("&8│ &8Activo&8: " + curName)));
        lore.add(ni(cfg.component("&8├─────────────────")));
        lore.add(ni(cfg.component("&8│ &c&l» &cClic para remover")));
        lore.add(ni(cfg.component("&8└─────────────────")));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(KEY_ACTION, PersistentDataType.STRING, "remove");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildOnlinePlayerHead(Player online) {
        ConfigManager cfg = plugin.getConfigManager();
        RankProvider  rp  = plugin.getRankProvider();
        DisguiseManager dm = plugin.getDisguiseManager();

        String rankId  = rp.getPlayerPrimaryGroup(online);
        String prefix  = rp.getGroupPrefix(rankId);
        if (prefix == null || prefix.isBlank()) prefix = "&8[&7Default&8]";

        boolean isDsg  = dm.isDisguised(online);
        String estado  = isDsg ? "&6● Disfrazado" : "&a● Online";

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta  = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(online);
        meta.displayName(ni(cfg.component("&#CC88FF&l" + online.getName())));

        List<Component> lore = new ArrayList<>();
        lore.add(ni(cfg.component("&8┌─────────────────")));
        lore.add(ni(cfg.componentAny("&8│ &8Rango&8:  " + prefix)));
        lore.add(ni(cfg.component("&8│ &7Ping&8:   &f" + online.getPing() + "ms")));
        lore.add(ni(cfg.component("&8│ &7Estado&8: " + estado)));
        lore.add(ni(cfg.component("&8├─────────────────")));
        lore.add(ni(cfg.component("&8│ &#FFDD00&l» &eClic para disfrazarte")));
        lore.add(ni(cfg.component("&8│ &7como &d" + online.getName())));
        lore.add(ni(cfg.component("&8└─────────────────")));
        meta.lore(lore);

        var pdc = meta.getPersistentDataContainer();
        pdc.set(KEY_ACTION, PersistentDataType.STRING, "instant_disguise");
        pdc.set(KEY_PLAYER, PersistentDataType.STRING, online.getName());
        pdc.set(KEY_RANK,   PersistentDataType.STRING, rankId.isBlank() ? "default" : rankId);
        skull.setItemMeta(meta);
        return skull;
    }

    private ItemStack buildPreviewHead(String disguiseName, String selectedRankId) {
        ConfigManager cfg    = plugin.getConfigManager();
        Player        target = Bukkit.getPlayerExact(disguiseName);

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

        meta.displayName(ni(cfg.component("&#CC88FF&l" + disguiseName)));
        List<Component> lore = new ArrayList<>();
        lore.add(ni(cfg.component("&8┌─────────────────")));
        lore.add(ni(cfg.component("&8│ &7Vista previa del disfraz")));
        lore.add(ni(cfg.component("&8├─────────────────")));
        lore.add(ni(cfg.component("&8│ &8Nombre&8: &d" + disguiseName)));
        lore.add(ni(cfg.component("&8│ &8Rango&8:  &f" + rankDisplay)));
        lore.add(ni(cfg.component("&8├─────────────────")));
        lore.add(ni(cfg.component("&8│ &7Selecciona el rango abajo.")));
        lore.add(ni(cfg.component("&8└─────────────────")));
        meta.lore(lore);
        addGlow(meta);
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
        meta.displayName(ni(cfg.componentAny(group.displayPrefix() + " &7" + capitalize(group.id()))));

        List<Component> lore = new ArrayList<>();
        lore.add(ni(cfg.component("&8┌─────────────────")));
        if (selected) {
            lore.add(ni(cfg.component("&8│ &a&l✔ Seleccionado")));
        } else {
            lore.add(ni(cfg.component("&8│ &7Haz clic para elegir")));
            lore.add(ni(cfg.component("&8│ &7este rango.")));
        }
        lore.add(ni(cfg.component("&8└─────────────────")));
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

        meta.displayName(ni(cfg.component("&a&l✔ Confirmar disfraz")));
        List<Component> lore = new ArrayList<>();
        lore.add(ni(cfg.component("&8┌─────────────────")));
        lore.add(ni(cfg.component("&8│ &7Aplicara lo siguiente&8:")));
        lore.add(ni(cfg.component("&8├─────────────────")));
        lore.add(ni(cfg.component("&8│ &8Nombre&8: &d" + disguiseName)));
        lore.add(ni(cfg.component("&8│ &8Rango&8:  &f" + rankDisplay)));
        lore.add(ni(cfg.component("&8│ &8Skin&8:   &7La de " + disguiseName)));
        lore.add(ni(cfg.component("&8├─────────────────")));
        lore.add(ni(cfg.component("&8│ &a&l» &aHaz clic para aplicar")));
        lore.add(ni(cfg.component("&8└─────────────────")));
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
        meta.displayName(ni(cfg.component(name)));

        List<Component> lore = new ArrayList<>();
        lore.add(ni(cfg.component("&8┌─────────────────")));
        for (String l : loreTxt) {
            lore.add(ni(l.isEmpty() ? Component.empty() : cfg.component("&8│ " + l)));
        }
        lore.add(ni(cfg.component("&8└─────────────────")));
        meta.lore(lore);

        var pdc = meta.getPersistentDataContainer();
        pdc.set(KEY_ACTION,   PersistentDataType.STRING, action);
        pdc.set(KEY_DISGUISE, PersistentDataType.STRING, disguiseName != null ? disguiseName : "");
        pdc.set(KEY_RANK,     PersistentDataType.STRING, rankId      != null ? rankId       : "");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildLabel(Material mat, String name, List<String> loreTxt) {
        ConfigManager cfg  = plugin.getConfigManager();
        ItemStack     item = new ItemStack(mat);
        ItemMeta      meta = item.getItemMeta();
        meta.displayName(ni(cfg.component(name)));
        List<Component> lore = new ArrayList<>();
        lore.add(ni(cfg.component("&8┌─────────────────")));
        for (String l : loreTxt) {
            lore.add(ni(l.isEmpty() ? Component.empty() : cfg.component("&8│ " + l)));
        }
        lore.add(ni(cfg.component("&8└─────────────────")));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack navButton(String action, int targetPage, String name, List<String> loreTxt) {
        ConfigManager cfg  = plugin.getConfigManager();
        ItemStack     item = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta      meta = item.getItemMeta();
        meta.displayName(ni(cfg.component(name)));
        List<Component> lore = new ArrayList<>();
        lore.add(ni(cfg.component("&8┌─────────────────")));
        for (String l : loreTxt) {
            lore.add(ni(l.isEmpty() ? Component.empty() : cfg.component("&8│ " + l)));
        }
        lore.add(ni(cfg.component("&8└─────────────────")));
        meta.lore(lore);
        var pdc = meta.getPersistentDataContainer();
        pdc.set(KEY_ACTION, PersistentDataType.STRING, action);
        pdc.set(KEY_PAGE,   PersistentDataType.INTEGER, targetPage);
        item.setItemMeta(meta);
        return item;
    }

    // ─────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────

    private ItemStack glass(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        meta.displayName(name.isEmpty()
                ? Component.empty()
                : plugin.getConfigManager().component(name).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private static Component ni(Component c) {
        return c.decoration(TextDecoration.ITALIC, false);
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
