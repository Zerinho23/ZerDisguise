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
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Construye los dos menús de ZerDisguise con diseño completo a color.
 *
 * Integra con Vault / LuckPerms vía RankProvider para mostrar
 * el prefijo del rango EXACTAMENTE como se ve en el chat de juego.
 *
 * ── Menú principal (27 slots) ───────────────────────────────────────
 *  Row 0 → [G][G][G][G][G][G][G][G][G]   border
 *  Row 1 → [HEAD][F][F][F][WRITE][F][F][F][REMOVE]
 *  Row 2 → [G][G][G][G][G][G][G][G][G]   border
 *
 * ── Menú de confirmación (27 slots) ────────────────────────────────
 *  Row 0 → [G][G][G][G][HEAD ][G][G][G][G]
 *  Row 1 → [G][R0][R1][R2][R3][R4][R5][R6][G]  (hasta 7 rangos)
 *  Row 2 → [G][F][RENAME][F][CONFIRM][F][BACK][F][G]
 */
public class MenuBuilder {

    // ── Cabeza decorativa Base64 (textura de interrogación morada) ────────────
    private static final String CUSTOM_HEAD_B64 =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0" +
            "L3RleHR1cmUvYmFkYzA0OGE3Y2U3OGY3ZGE3MzI0YWYzYTM1ZmRmMThjZjQ4NzAzYWFmZDIyZWFh" +
            "YmM3OTRhZmM2YSJ9fX0=";

    // Colores del tema (para separadores y cristales por defecto)
    private static final Material BORDER_GLASS  = Material.PURPLE_STAINED_GLASS_PANE;
    private static final Material FILLER_GLASS  = Material.GRAY_STAINED_GLASS_PANE;

    private final ZerDisguise plugin;

    public MenuBuilder(ZerDisguise plugin) {
        this.plugin = plugin;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  MENÚ PRINCIPAL
    // ═══════════════════════════════════════════════════════════════════════════

    public Inventory buildMainMenu(Player player) {
        ConfigManager cfg  = plugin.getConfigManager();
        int           size = cfg.getMenuSize();

        Inventory inv = Bukkit.createInventory(null, size,
                cfg.component(cfg.getMenuTitle()));

        // Borde superior + inferior (violeta)
        ItemStack border = glass(BORDER_GLASS);
        for (int i = 0; i < 9; i++)           inv.setItem(i, border);
        for (int i = size - 9; i < size; i++) inv.setItem(i, border);

        // Relleno fila central (gris oscuro para contraste)
        ItemStack filler = glass(FILLER_GLASS);
        for (int i = 10; i <= 12; i++) inv.setItem(i, filler);
        for (int i = 14; i <= 16; i++) inv.setItem(i, filler);

        // Slot 9  — Cabeza del jugador con info completa
        inv.setItem(9, buildPlayerInfoHead(player));

        // Slot 13 — Botón "Escribir disfraz"
        inv.setItem(13, buildWriteButton());

        // Slot 17 — Remover disfraz (solo si está activo)
        if (plugin.getDisguiseManager().isDisguised(player)) {
            inv.setItem(17, buildRemoveButton(player));
        } else {
            inv.setItem(17, filler);
        }

        return inv;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  MENÚ DE CONFIRMACIÓN
    // ═══════════════════════════════════════════════════════════════════════════

    public Inventory buildConfirmMenu(Player player, String disguiseName, String rankId) {
        ConfigManager cfg  = plugin.getConfigManager();
        int           size = cfg.getMenuSize();

        Inventory inv = Bukkit.createInventory(null, size,
                cfg.component("&8» &#CC88FF&lConfirmar disfraz &8«"));

        ItemStack border = glass(BORDER_GLASS);
        ItemStack filler = glass(FILLER_GLASS);

        // Borde superior
        for (int i = 0; i < 9; i++) inv.setItem(i, border);

        // Lateral fila 1
        inv.setItem(9,  border);
        inv.setItem(17, border);

        // Borde inferior + laterales fila 2
        inv.setItem(18, border);
        inv.setItem(19, filler);
        inv.setItem(21, filler);
        inv.setItem(23, filler);
        inv.setItem(25, filler);
        inv.setItem(26, border);

        // Slot 4 — Cabeza del objetivo (skin del nombre del disfraz)
        inv.setItem(4, buildTargetHead(disguiseName, rankId));

        // Slots 10-16 — Selector de rangos
        buildRankSlots(inv, player, disguiseName, rankId);

        // Slot 20 — Cambiar nombre
        inv.setItem(20, makeItem(Material.NAME_TAG,
                "&e&l✎ Cambiar nombre",
                List.of(
                        "&8",
                        "&7Volver al chat para",
                        "&7escribir un nuevo nombre.")));

        // Slot 22 — Confirmar
        inv.setItem(22, buildConfirmButton(disguiseName, rankId));

        // Slot 24 — Volver
        inv.setItem(24, makeItem(Material.ARROW,
                "&c&l← Volver",
                List.of(
                        "&8",
                        "&7Regresa al menú principal.")));

        return inv;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CONSTRUCTORES DE ÍTEMS
    // ═══════════════════════════════════════════════════════════════════════════

    /** Cabeza del jugador con su información y rango real (Vault/LP). */
    private ItemStack buildPlayerInfoHead(Player player) {
        ConfigManager   cfg = plugin.getConfigManager();
        DisguiseManager dm  = plugin.getDisguiseManager();
        RankProvider    rp  = plugin.getRankProvider();

        DisguiseManager.DisguiseData cur  = dm.getCurrent(player.getUniqueId());
        DisguiseManager.DisguiseData prev = dm.getPrevious(player.getUniqueId());

        // ── Prefijo real del jugador ────────────────────────────────────────
        String realPrefix = rp.getPlayerPrefix(player);

        // Si no hay integración, usamos el prefijo del rango actual del disfraz
        if (realPrefix == null && cur != null) {
            for (ConfigManager.RankEntry r : cfg.getRanks()) {
                if (r.id().equalsIgnoreCase(cur.rankId())) {
                    realPrefix = r.prefix();
                    break;
                }
            }
        }
        if (realPrefix == null) realPrefix = "&8[&7Default&8]&r";

        // ── Disfraz actual / anterior ──────────────────────────────────────
        String currentDisguise  = cur  != null ? "&#CC88FF" + cur.disguiseName()  : "&8Ninguno";
        String previousDisguise = prev != null ? "&7"       + prev.disguiseName() : "&8Ninguno";

        // ── Nombre de rango para mostrar ──────────────────────────────────
        String rankDisplay = "&7Default";
        if (cur != null) {
            for (ConfigManager.RankEntry r : cfg.getRanks()) {
                if (r.id().equalsIgnoreCase(cur.rankId())) {
                    // Intenta prefijo real del grupo
                    String gp = rp.getGroupPrefix(r.id());
                    rankDisplay = (gp != null) ? gp : r.prefix();
                    break;
                }
            }
        }

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta  = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(player);

        // Nombre del ítem: nombre real del jugador
        meta.displayName(noItalic(cfg.component("&f&l" + player.getName())));

        List<Component> lore = new ArrayList<>();
        lore.add(empty());
        lore.add(noItalic(cfg.component("&#9966FF▌ &7Rango en juego")));
        // Prefijo exactamente como se ve en el chat
        lore.add(noItalic(cfg.componentAny("  " + rankDisplay)));
        lore.add(empty());
        lore.add(noItalic(cfg.component("&#9966FF▌ &7Disfraz actual&8: " + currentDisguise)));
        lore.add(noItalic(cfg.component("&#9966FF▌ &7Disfraz anterior&8: " + previousDisguise)));
        lore.add(empty());

        meta.lore(lore);
        skull.setItemMeta(meta);
        return skull;
    }

    /** Botón de escritura de disfraz con cabeza Base64 personalizada. */
    private ItemStack buildWriteButton() {
        ConfigManager cfg   = plugin.getConfigManager();
        ItemStack     skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta     meta  = (SkullMeta) skull.getItemMeta();

        try {
            String decoded = new String(Base64.getDecoder().decode(CUSTOM_HEAD_B64));
            String urlStr  = decoded.replaceAll(".*\"url\":\"([^\"]+)\".*", "$1");
            PlayerProfile   profile  = Bukkit.createPlayerProfile(UUID.randomUUID(), "ZerDisguise");
            PlayerTextures  textures = profile.getTextures();
            textures.setSkin(URI.create(urlStr).toURL());
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
        } catch (MalformedURLException | IllegalArgumentException ignored) {}

        meta.displayName(noItalic(cfg.component("&#CC88FF&l✎ Escribir disfraz")));

        List<Component> lore = new ArrayList<>();
        lore.add(empty());
        lore.add(noItalic(cfg.component("&7Haz clic para escribir el")));
        lore.add(noItalic(cfg.component("&7nombre de tu disfraz en el chat.")));
        lore.add(empty());
        lore.add(noItalic(cfg.component("&#FFCC00▶ &eHaz clic para comenzar")));
        lore.add(empty());
        meta.lore(lore);
        skull.setItemMeta(meta);
        return skull;
    }

    /** Botón para remover el disfraz (muestra nombre actual). */
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
        item.setItemMeta(meta);
        return item;
    }

    /** Cabeza del objetivo (skin del nombre escrito + vista previa del disfraz). */
    private ItemStack buildTargetHead(String disguiseName, String rankId) {
        ConfigManager cfg = plugin.getConfigManager();
        RankProvider  rp  = plugin.getRankProvider();

        // Buscar el rango seleccionado
        String rankPrefix = null;
        for (ConfigManager.RankEntry r : cfg.getRanks()) {
            if (r.id().equalsIgnoreCase(rankId)) {
                String gp = rp.getGroupPrefix(r.id());
                rankPrefix = (gp != null) ? gp : r.prefix();
                break;
            }
        }
        if (rankPrefix == null) rankPrefix = "&8[&7Default&8]&r";

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta  = (SkullMeta) skull.getItemMeta();

        // Skin del jugador con ese nombre (online primero, luego offline)
        Player target = Bukkit.getPlayerExact(disguiseName);
        if (target != null) {
            meta.setOwningPlayer(target);
        } else {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(disguiseName));
        }

        meta.displayName(noItalic(cfg.component("&#CC88FF&l" + disguiseName)));

        List<Component> lore = new ArrayList<>();
        lore.add(empty());
        lore.add(noItalic(cfg.component("&7Vista previa en el chat&8:")));
        lore.add(empty());
        // Muestra exactamente cómo se verá: [Prefix] NombreDisfraz
        lore.add(noItalic(cfg.componentAny(rankPrefix + " &#CC88FF" + disguiseName)));
        lore.add(empty());
        meta.lore(lore);
        skull.setItemMeta(meta);
        return skull;
    }

    /** Rellena los slots 10-16 con los ítems de selección de rango. */
    private void buildRankSlots(Inventory inv, Player player,
                                String disguiseName, String rankId) {
        ConfigManager cfg    = plugin.getConfigManager();
        RankProvider  rp     = plugin.getRankProvider();
        List<ConfigManager.RankEntry> ranks = cfg.getRanks();

        int[] slots = {10, 11, 12, 13, 14, 15, 16};

        for (int i = 0; i < slots.length; i++) {
            if (i >= ranks.size()) {
                inv.setItem(slots[i], glass(FILLER_GLASS));
                continue;
            }

            ConfigManager.RankEntry rank   = ranks.get(i);
            boolean                 sel    = rank.id().equalsIgnoreCase(rankId);
            boolean                 hasPerm= player.hasPermission(rank.permission());

            // ── Prefijo real del grupo (Vault/LP → config fallback) ────────
            String gp          = rp.getGroupPrefix(rank.id());
            String realPrefix  = (gp != null) ? gp : rank.prefix();

            // ── Nombre del ítem: prefijo real + nombre de rango ────────────
            // Esto es exactamente lo que el jugador ve en el chat
            Component itemName = noItalic(
                    cfg.componentAny(realPrefix)
                       .append(cfg.component(" " + rank.color() + rank.name()))
            );

            // ── Lore ────────────────────────────────────────────────────────
            List<Component> lore = new ArrayList<>();
            lore.add(empty());

            // Vista previa: cómo se verá el jugador en el chat
            lore.add(noItalic(cfg.component("&7Vista previa&8:")));
            lore.add(noItalic(
                    cfg.componentAny(realPrefix)
                       .append(cfg.component(" &#CC88FF" + disguiseName))
            ));
            lore.add(empty());

            if (!hasPerm) {
                lore.add(noItalic(cfg.component("&c✖ &cSin permiso")));
            } else if (sel) {
                lore.add(noItalic(cfg.component("&a✔ &aSeleccionado")));
            } else {
                lore.add(noItalic(cfg.component("&e▶ &eHaz clic para seleccionar")));
            }
            lore.add(empty());

            // ── Material e ítem ────────────────────────────────────────────
            Material mat = hasPerm ? rank.material() : Material.BARRIER;
            ItemStack item = new ItemStack(mat);
            ItemMeta  meta = item.getItemMeta();
            meta.displayName(itemName);
            meta.lore(lore);

            // Brillo para el rango seleccionado
            if (sel && hasPerm) {
                Enchantment unbreaking = Enchantment.getByKey(
                        NamespacedKey.minecraft("unbreaking"));
                if (unbreaking != null) meta.addEnchant(unbreaking, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            item.setItemMeta(meta);
            inv.setItem(slots[i], item);

            // Cristal de color del rango como borde lateral si está seleccionado
            if (sel && hasPerm) {
                ItemStack rankGlass = glass(rank.glass());
                if (slots[i] == 10) inv.setItem(9,  rankGlass);
                if (slots[i] == 16) inv.setItem(17, rankGlass);
            }
        }
    }

    /** Botón verde de confirmación con vista previa del disfraz. */
    private ItemStack buildConfirmButton(String disguiseName, String rankId) {
        ConfigManager cfg = plugin.getConfigManager();
        RankProvider  rp  = plugin.getRankProvider();

        String rankPrefix = null;
        String rankColor  = "&7";
        for (ConfigManager.RankEntry r : cfg.getRanks()) {
            if (r.id().equalsIgnoreCase(rankId)) {
                String gp = rp.getGroupPrefix(r.id());
                rankPrefix = (gp != null) ? gp : r.prefix();
                rankColor  = r.color();
                break;
            }
        }
        if (rankPrefix == null) rankPrefix = "&8[&7Default&8]&r";

        ItemStack item = new ItemStack(Material.LIME_DYE);
        ItemMeta  meta = item.getItemMeta();
        meta.displayName(noItalic(cfg.component("&a&l✔ Confirmar disfraz")));

        List<Component> lore = new ArrayList<>();
        lore.add(empty());
        lore.add(noItalic(cfg.component("&7Se aplicará este disfraz&8:")));
        lore.add(empty());
        // Vista previa exacta de cómo se verá
        lore.add(noItalic(
                cfg.componentAny(rankPrefix)
                   .append(cfg.component(" &#CC88FF" + disguiseName))
        ));
        lore.add(empty());
        lore.add(noItalic(cfg.component("&a▶ Haz clic para confirmar")));
        lore.add(empty());
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  UTILIDADES
    // ═══════════════════════════════════════════════════════════════════════════

    /** Ítem simple con nombre y lore (colores &). */
    private ItemStack makeItem(Material mat, String name, List<String> loreStrings) {
        ConfigManager cfg  = plugin.getConfigManager();
        ItemStack     item = new ItemStack(mat);
        ItemMeta      meta = item.getItemMeta();
        meta.displayName(noItalic(cfg.component(name)));
        List<Component> lore = new ArrayList<>();
        lore.add(empty());
        for (String s : loreStrings) lore.add(noItalic(cfg.component(s)));
        lore.add(empty());
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /** Cristal de color sin nombre (relleno/borde). */
    private static ItemStack glass(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        meta.displayName(Component.empty());
        item.setItemMeta(meta);
        return item;
    }

    /** Componente vacío para separar líneas en el lore. */
    private static Component empty() {
        return Component.empty();
    }

    /**
     * Elimina la cursiva que Minecraft aplica por defecto a todos los
     * nombres e lores de ítems personalizados.
     */
    private static Component noItalic(Component c) {
        return c.decoration(TextDecoration.ITALIC, false);
    }

    // Acceso paquete-privado para MenuListener (necesita buildCustomHead)
    ItemStack buildCustomHead(String displayName, List<String> loreStrings) {
        return buildWriteButton();
    }
}
