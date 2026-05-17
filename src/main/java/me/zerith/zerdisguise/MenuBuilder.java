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
 * Construye los menús de ZerDisguise.
 * Todos los materiales, títulos y textos de botones se leen desde menu.yml via MenuConfig.
 *
 * Menú principal (54 slots):
 *   Fila 0  → borde superior (esquinas del color corner)
 *   Fila 1  → [Info jugador][relleno][Escribir][relleno][Quitar]
 *   Fila 2  → divisor + etiqueta central
 *   Filas 3-4 → cabezas de jugadores online (18 por página)
 *   Fila 5  → borde + [Anterior][relleno][Página info][relleno][Siguiente]
 *
 * Menú de confirmación (54 slots):
 *   Fila 0  → borde + cabeza preview (slot 4)
 *   Filas 1-2 → selector de rangos
 *   Filas 3-4 → relleno
 *   Fila 5  → borde + [Cambiar nombre][relleno][Confirmar][relleno][Volver]
 */
public class MenuBuilder {

    public static final int PLAYERS_PER_PAGE = 18;

    public static NamespacedKey KEY_ACTION;
    public static NamespacedKey KEY_PLAYER;
    public static NamespacedKey KEY_RANK;
    public static NamespacedKey KEY_DISGUISE;
    public static NamespacedKey KEY_PAGE;

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

    // ─────────────────────────────────────────────────────────────
    //  MENÚ PRINCIPAL
    // ─────────────────────────────────────────────────────────────

    public Inventory buildMainMenu(Player player) {
        return buildMainMenu(player, 0);
    }

    public Inventory buildMainMenu(Player player, int page) {
        ConfigManager cfg = plugin.getConfigManager();
        MenuConfig    mc  = plugin.getMenuConfig();

        Inventory inv = Bukkit.createInventory(null, 54,
                cfg.component(mc.getMainTitle()));

        ItemStack border  = glass(mc.getBorderMaterial(),  "");
        ItemStack filler  = glass(mc.getFillerMaterial(),  "");
        ItemStack divider = glass(mc.getDividerMaterial(), "");
        ItemStack corner  = glass(mc.getCornerMaterial(),  "");

        // ── Fila 0: borde superior ────────────────────────────────
        inv.setItem(0, corner);
        for (int i = 1; i <= 7; i++) inv.setItem(i, border);
        inv.setItem(8, corner);

        // ── Fila 1: cabeza del jugador, botones ──────────────────
        inv.setItem(9, buildPlayerInfoHead(player));
        for (int i = 10; i <= 12; i++) inv.setItem(i, filler);
        inv.setItem(mc.getWriteSlot(), buildWriteButton(mc));
        for (int i = 14; i <= 16; i++) inv.setItem(i, filler);
        inv.setItem(mc.getRemoveSlot(),
                plugin.getDisguiseManager().isDisguised(player)
                        ? buildRemoveButton(player, mc)
                        : glass(mc.getBorderMaterial(), ""));

        // ── Fila 2: divisor + etiqueta central ───────────────────
        for (int i = 18; i < 27; i++) inv.setItem(i, divider);
        inv.setItem(mc.getLabelSlot(), buildLabel(
                mc.getLabelMaterial(),
                mc.getLabelName(),
                mc.getLabelLore().isEmpty()
                        ? List.of(
                            "&8▸ &7Haz clic en una cabeza para",
                            "&8  &7ponerte su nombre y skin.",
                            "&8▸ &7Se aplica instantáneamente.")
                        : mc.getLabelLore()
        ));

        // ── Filas 3-4: cabezas de jugadores online ───────────────
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

        // ── Fila 5: navegación ────────────────────────────────────
        inv.setItem(45, corner);
        for (int i = 46; i <= 53; i++) inv.setItem(i, border);
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
    //  MENÚ DE CONFIRMACIÓN
    // ─────────────────────────────────────────────────────────────

    /**
     * Menú de confirmación simplificado: solo muestra la cabeza del objetivo,
     * info del disfraz y botones Confirmar / Cambiar nombre / Volver.
     * El rango se resuelve automáticamente en DisguiseManager al aplicar.
     */
    public Inventory buildConfirmMenu(Player player, String disguiseName) {
        ConfigManager cfg = plugin.getConfigManager();
        MenuConfig    mc  = plugin.getMenuConfig();
        RankProvider  rp  = plugin.getRankProvider();

        Inventory inv = Bukkit.createInventory(null, 54,
                cfg.component(mc.getConfirmTitle()));

        ItemStack border  = glass(mc.getBorderMaterial(),  "");
        ItemStack corner  = glass(mc.getCornerMaterial(),  "");
        ItemStack filler  = glass(mc.getFillerMaterial(),  "");
        ItemStack divider = glass(mc.getDividerMaterial(), "");

        // ── Fila 0: borde + cabeza preview centrada ───────────────
        inv.setItem(0, corner);
        for (int i = 1; i <= 7; i++) inv.setItem(i, border);
        inv.setItem(8, corner);
        inv.setItem(4, buildPreviewHead(disguiseName, rp, cfg));

        // ── Filas 1-4: relleno limpio con divisor central ─────────
        for (int i = 9;  i < 18; i++) inv.setItem(i, filler);
        for (int i = 18; i < 27; i++) inv.setItem(i, divider);
        for (int i = 27; i < 45; i++) inv.setItem(i, filler);

        // Etiqueta informativa en el centro del divisor (slot 22)
        inv.setItem(22, buildDisguiseInfoLabel(disguiseName, rp, cfg, mc));

        // ── Fila 5: botones de acción ─────────────────────────────
        inv.setItem(45, corner);
        for (int i = 46; i <= 53; i++) inv.setItem(i, border);
        inv.setItem(53, corner);

        List<String> renameLore = mc.getRenameLore().isEmpty()
                ? List.of("&7Escribe un nombre diferente en el chat.", "",
                          "&e&l» &eClic para cambiar")
                : mc.getRenameLore();

        List<String> backLore = mc.getBackLore().isEmpty()
                ? List.of("&7Regresa al menú principal.", "",
                          "&c&l» &cClic para volver")
                : mc.getBackLore();

        inv.setItem(mc.getRenameSlot(),    buildActionButton(mc.getRenameMaterial(),
                "rename", disguiseName, "", mc.getRenameName(), renameLore));
        inv.setItem(mc.getConfirmBtnSlot(), buildConfirmButton(disguiseName, mc));
        inv.setItem(mc.getBackSlot(),      buildActionButton(mc.getBackMaterial(),
                "back", disguiseName, "", mc.getBackName(), backLore));

        return inv;
    }

    // ─────────────────────────────────────────────────────────────
    //  CONSTRUCTORES DE ÍTEMS
    // ─────────────────────────────────────────────────────────────

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
        addLoreLine(lore, cfg, "&8┌─────────────────");
        addLoreLine(lore, cfg, "&8│ &8▸ &7Tu perfil");
        addLoreLine(lore, cfg, "&8├─────────────────");
        lore.add(ni(cfg.componentAny("&8│ &8Rango&8:    " + realPrefix)));
        addLoreLine(lore, cfg, "&8│ &7Estado&8:   " + estado);
        addLoreLine(lore, cfg, "&8│ &7Disfraz&8:  " + curName);
        addLoreLine(lore, cfg, "&8│ &7Anterior&8: " + prevName);
        addLoreLine(lore, cfg, "&8└─────────────────");
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
                ? List.of("&7Escribe el nombre de cualquier jugador de Minecraft.",
                          "",
                          "&8Nota&8: &7el nombre debe existir en Mojang.",
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
                ? List.of("&7Elimina tu disfraz actual",
                          "&7y restaura tu apariencia original.",
                          "",
                          "&8Activo&8: " + curName,
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
        String prefix = rp.getGroupPrefix(rankId);
        if (prefix == null || prefix.isBlank()) prefix = "&8[&7Default&8]";

        boolean isDsg  = dm.isDisguised(online);
        String  estado = isDsg ? "&6● Disfrazado" : "&a● Online";

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta  = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(online);
        meta.displayName(ni(cfg.component("&#CC88FF&l" + online.getName())));

        List<Component> lore = new ArrayList<>();
        addLoreLine(lore, cfg, "&8┌─────────────────");
        lore.add(ni(cfg.componentAny("&8│ &8Rango&8:  " + prefix)));
        addLoreLine(lore, cfg, "&8│ &7Ping&8:   &f" + online.getPing() + "ms");
        addLoreLine(lore, cfg, "&8│ &7Estado&8: " + estado);
        addLoreLine(lore, cfg, "&8├─────────────────");
        addLoreLine(lore, cfg, "&8│ &#FFDD00&l» &eClic para disfrazarte");
        addLoreLine(lore, cfg, "&8│ &7como &d" + online.getName());
        addLoreLine(lore, cfg, "&8└─────────────────");
        meta.lore(lore);

        var pdc = meta.getPersistentDataContainer();
        pdc.set(KEY_ACTION, PersistentDataType.STRING, "instant_disguise");
        pdc.set(KEY_PLAYER, PersistentDataType.STRING, online.getName());
        pdc.set(KEY_RANK,   PersistentDataType.STRING, rankId.isBlank() ? "default" : rankId);
        skull.setItemMeta(meta);
        return skull;
    }

    private ItemStack buildPreviewHead(String disguiseName, RankProvider rp, ConfigManager cfg) {
        Player    target  = Bukkit.getPlayerExact(disguiseName);
        boolean   online  = target != null;
        String    rankPrefix = online ? rp.getPlayerPrefix(target) : null;
        String    rankId     = online ? rp.getPlayerPrimaryGroup(target) : "?";
        String    estado     = online ? "&a● Online" : "&8● Offline";

        // Limpiar colores del prefijo para mostrarlo limpio
        String rankClean = rankPrefix != null && !rankPrefix.isBlank()
                ? rankPrefix
                : "&8[&7" + capitalize(rankId) + "&8]";

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta  = (SkullMeta) skull.getItemMeta();
        if (target != null) meta.setOwningPlayer(target);

        meta.displayName(ni(cfg.component("&#CC88FF&l" + disguiseName)));
        List<Component> lore = new ArrayList<>();
        addLoreLine(lore, cfg, "&8┌─────────────────");
        addLoreLine(lore, cfg, "&8│ &7Vista previa del disfraz");
        addLoreLine(lore, cfg, "&8├─────────────────");
        addLoreLine(lore, cfg, "&8│ &8Nombre&8: &d" + disguiseName);
        lore.add(ni(cfg.componentAny("&8│ &8Rango&8:  " + rankClean)));
        addLoreLine(lore, cfg, "&8│ &7Estado&8: " + estado);
        addLoreLine(lore, cfg, "&8├─────────────────");
        addLoreLine(lore, cfg, "&8│ &7Confirma abajo para aplicar.");
        addLoreLine(lore, cfg, "&8└─────────────────");
        meta.lore(lore);
        addGlow(meta);
        skull.setItemMeta(meta);
        return skull;
    }

    private ItemStack buildDisguiseInfoLabel(String disguiseName, RankProvider rp,
                                             ConfigManager cfg, MenuConfig mc) {
        Player  target   = Bukkit.getPlayerExact(disguiseName);
        boolean online   = target != null;
        String  rankPrefix = online ? rp.getPlayerPrefix(target) : null;
        String  rankId     = online ? rp.getPlayerPrimaryGroup(target) : null;
        String  rankClean  = rankPrefix != null && !rankPrefix.isBlank()
                ? rankPrefix
                : (rankId != null ? "&8[&f" + capitalize(rankId) + "&8]" : "&8[&7Default&8]");
        String  estado   = online ? "&a● Online" : "&8● Offline &7(skin desde Mojang)";

        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta  meta = item.getItemMeta();
        meta.displayName(ni(cfg.component("&#CC88FF&l✦ Información del disfraz")));

        List<Component> lore = new ArrayList<>();
        addLoreLine(lore, cfg, "&8┌─────────────────");
        addLoreLine(lore, cfg, "&8│ &8Nombre&8: &d" + disguiseName);
        lore.add(ni(cfg.componentAny("&8│ &8Rango&8:  " + rankClean)));
        addLoreLine(lore, cfg, "&8│ &7Estado&8: " + estado);
        addLoreLine(lore, cfg, "&8├─────────────────");
        addLoreLine(lore, cfg, "&8│ &7El rango se detecta automáticamente");
        addLoreLine(lore, cfg, "&8│ &7de LuckPerms al confirmar.");
        addLoreLine(lore, cfg, "&8└─────────────────");
        meta.lore(lore);
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
                : "&7automático al aplicar";

        ItemStack item = new ItemStack(mc.getConfirmBtnMaterial());
        ItemMeta  meta = item.getItemMeta();

        meta.displayName(ni(cfg.component(mc.getConfirmBtnName())));
        List<Component> lore = new ArrayList<>();
        addLoreLine(lore, cfg, "&8┌─────────────────");
        addLoreLine(lore, cfg, "&8│ &7Aplicará lo siguiente&8:");
        addLoreLine(lore, cfg, "&8├─────────────────");
        addLoreLine(lore, cfg, "&8│ &8Nombre&8: &d" + disguiseName);
        lore.add(ni(cfg.componentAny("&8│ &8Rango&8:  " + rankTxt)));
        addLoreLine(lore, cfg, "&8│ &8Skin&8:   &7La de &d" + disguiseName);
        addLoreLine(lore, cfg, "&8├─────────────────");
        addLoreLine(lore, cfg, "&8│ &a&l» &aHaz clic para aplicar");
        addLoreLine(lore, cfg, "&8└─────────────────");
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

    /** Construye un lore encuadrado con bordes ┌───┐ automáticamente. */
    private List<Component> buildBoxedLore(List<String> lines, ConfigManager cfg) {
        List<Component> result = new ArrayList<>();
        result.add(ni(cfg.component("&8┌─────────────────")));
        for (String line : lines) {
            if (line == null || line.isEmpty()) {
                result.add(ni(cfg.component("&8│")));
            } else {
                result.add(ni(cfg.component("&8│ " + line)));
            }
        }
        result.add(ni(cfg.component("&8└─────────────────")));
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
        // UNBREAKING accesible por clave en Paper 1.20-1.21+
        Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));
        if (ench != null) meta.addEnchant(ench, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
