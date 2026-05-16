package me.zerith.zerdisguise;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
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
 * Builds the two GUI menus for ZerDisguise.
 *
 * Menu 1 — Main menu (slot layout for size 27):
 *   [0-8]  Glass pane border (top row)
 *   [9]    Player head (info: rank, current disguise, previous disguise)
 *   [13]   Custom Base64 head — click to enter disguise-write mode
 *   [17]   Remove disguise (if active)
 *   [18-26] Glass pane border (bottom row)
 *
 * Menu 2 — Confirm/configure menu (size 27):
 *   [4]    Skin head with disguise name
 *   [10-16] Rank selector items
 *   [20]   Change name (go back to write mode)
 *   [22]   Confirm / apply
 *   [24]   Back button
 */
public class MenuBuilder {

    // Base64-encoded custom head texture (purple question-mark style head)
    // This is a valid Minecraft player skin URL encoded as Base64 for use with SkullMeta.
    private static final String CUSTOM_HEAD_B64 =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0" +
            "L3RleHR1cmUvYmFkYzA0OGE3Y2U3OGY3ZGE3MzI0YWYzYTM1ZmRmMThjZjQ4NzAzYWFmZDIyZWFh" +
            "YmM3OTRhZmM2YSJ9fX0=";

    private final ZerDisguise plugin;

    public MenuBuilder(ZerDisguise plugin) {
        this.plugin = plugin;
    }

    // ── Menu 1: Main ─────────────────────────────────────────────────────────

    public Inventory buildMainMenu(Player player) {
        ConfigManager cfg  = plugin.getConfigManager();
        int           size = cfg.getMenuSize();

        Inventory inv = Bukkit.createInventory(null, size,
                cfg.component(cfg.getMenuTitle()));

        // Glass pane border (top + bottom rows)
        ItemStack glass = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++)           inv.setItem(i, glass);
        for (int i = size - 9; i < size; i++) inv.setItem(i, glass);

        // Player head with info
        inv.setItem(9, buildPlayerInfoHead(player));

        // Custom Base64 head — triggers write mode
        inv.setItem(13, buildCustomHead(
                "&d&lEscribir Disfraz",
                List.of("&7Haz clic para escribir",
                        "&7el nombre de tu disfraz",
                        "&7en el chat.")));

        // Remove disguise (only if active)
        if (plugin.getDisguiseManager().isDisguised(player)) {
            inv.setItem(17, makeItem(Material.BARRIER,
                    "&cRemover disfraz",
                    List.of("&7Haz clic para quitarte",
                            "&7el disfraz actual.")));
        }

        return inv;
    }

    // ── Menu 2: Confirm ───────────────────────────────────────────────────────

    public Inventory buildConfirmMenu(Player player, String disguiseName, String rankId) {
        ConfigManager cfg  = plugin.getConfigManager();
        int           size = cfg.getMenuSize();

        Inventory inv = Bukkit.createInventory(null, size,
                cfg.component("&8» &dConfirmar disfraz &8«"));

        // Glass pane border
        ItemStack glass = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++)           inv.setItem(i, glass);
        for (int i = size - 9; i < size; i++) inv.setItem(i, glass);

        // Skin head of the target player (or Steve if not found)
        inv.setItem(4, buildTargetHead(player, disguiseName));

        // Rank selector
        List<ConfigManager.RankEntry> ranks = cfg.getRanks();
        int[] rankSlots = {10, 11, 12, 13, 14, 15, 16};
        for (int i = 0; i < ranks.size() && i < rankSlots.length; i++) {
            ConfigManager.RankEntry rank = ranks.get(i);
            boolean selected = rank.id().equalsIgnoreCase(rankId);
            boolean hasPerms = player.hasPermission(rank.permission());

            List<String> lore = new ArrayList<>();
            lore.add(hasPerms ? "&7Clic para seleccionar" : "&cNo tienes permiso");
            if (selected) lore.add("&a✔ Seleccionado");

            ItemStack item = makeItem(
                    hasPerms ? rank.material() : Material.BARRIER,
                    rank.color() + rank.name() + (selected ? " &a✔" : ""),
                    lore);
            inv.setItem(rankSlots[i], item);
        }

        // Change name
        inv.setItem(20, makeItem(Material.NAME_TAG,
                "&ecambiar nombre",
                List.of("&7Haz clic para escribir",
                        "&7un nuevo nombre de disfraz.")));

        // Confirm / Apply
        inv.setItem(22, makeItem(Material.LIME_DYE,
                "&a&lConfirmar",
                List.of("&7Aplica el disfraz:",
                        "&d" + disguiseName)));

        // Back
        inv.setItem(24, makeItem(Material.ARROW,
                "&cVolver",
                List.of("&7Regresa al menú anterior.")));

        return inv;
    }

    // ── Item builders ─────────────────────────────────────────────────────────

    private ItemStack buildPlayerInfoHead(Player player) {
        ConfigManager cfg = plugin.getConfigManager();
        DisguiseManager dm = plugin.getDisguiseManager();

        DisguiseManager.DisguiseData cur  = dm.getCurrent(player.getUniqueId());
        DisguiseManager.DisguiseData prev = dm.getPrevious(player.getUniqueId());

        String currentDisguise  = cur  != null ? "&d" + cur.disguiseName()  : "&7Ninguno";
        String previousDisguise = prev != null ? "&d" + prev.disguiseName() : "&7Ninguno";

        // Rank display
        String rankDisplay = "&7Default";
        if (cur != null) {
            for (ConfigManager.RankEntry r : cfg.getRanks()) {
                if (r.id().equalsIgnoreCase(cur.rankId())) {
                    rankDisplay = r.color() + r.name();
                    break;
                }
            }
        }

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta  = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(player);
        meta.displayName(cfg.component("&f" + player.getName()));

        List<Component> lore = new ArrayList<>();
        lore.add(cfg.component("&8┌ &7Información"));
        lore.add(cfg.component("&8│ &7Rango: " + rankDisplay));
        lore.add(cfg.component("&8│ &7Disfraz: " + currentDisguise));
        lore.add(cfg.component("&8└ &7Anterior: " + previousDisguise));

        meta.lore(lore);
        skull.setItemMeta(meta);
        return skull;
    }

    private ItemStack buildTargetHead(Player requester, String targetName) {
        ConfigManager cfg = plugin.getConfigManager();

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta  = (SkullMeta) skull.getItemMeta();

        // Try to find online player with that name
        Player target = Bukkit.getPlayerExact(targetName);
        if (target != null) {
            meta.setOwningPlayer(target);
        } else {
            // Offline lookup
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(targetName));
        }

        meta.displayName(cfg.component("&d" + targetName));
        List<Component> lore = new ArrayList<>();
        lore.add(cfg.component("&7Disfraz seleccionado"));
        meta.lore(lore);
        skull.setItemMeta(meta);
        return skull;
    }

    ItemStack buildCustomHead(String displayName, List<String> loreStrings) {
        ConfigManager cfg   = plugin.getConfigManager();
        ItemStack     skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta     meta  = (SkullMeta) skull.getItemMeta();

        try {
            // Decode the Base64 value to extract the texture URL
            String decoded = new String(Base64.getDecoder().decode(CUSTOM_HEAD_B64));
            String urlStr  = decoded.replaceAll(".*\"url\":\"([^\"]+)\".*", "$1");

            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "ZerDisguise");
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(URI.create(urlStr).toURL());
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
        } catch (MalformedURLException | IllegalArgumentException ignored) {
            // Fallback — just show a plain skull
        }

        meta.displayName(cfg.component(displayName));
        List<Component> lore = new ArrayList<>();
        for (String s : loreStrings) lore.add(cfg.component(s));
        meta.lore(lore);
        skull.setItemMeta(meta);
        return skull;
    }

    private ItemStack makeItem(Material mat, String name, List<String> loreStrings) {
        ConfigManager cfg  = plugin.getConfigManager();
        ItemStack     item = new ItemStack(mat);
        ItemMeta      meta = item.getItemMeta();
        meta.displayName(cfg.component(name));
        List<Component> lore = new ArrayList<>();
        for (String s : loreStrings) lore.add(cfg.component(s));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeItem(Material mat, String name) {
        return makeItem(mat, name, List.of());
    }
}
