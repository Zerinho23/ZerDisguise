package me.zerith.zerdisguise;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class MenuListener implements Listener {

    private final ZerDisguise plugin;

    public MenuListener(ZerDisguise plugin) {
        this.plugin = plugin;
    }

    /** Devuelve el ZerInventoryHolder si el inventario es del plugin, o null. */
    private ZerInventoryHolder getHolder(org.bukkit.inventory.InventoryView view) {
        InventoryHolder holder = view.getTopInventory().getHolder();
        if (holder instanceof ZerInventoryHolder zh) return zh;
        return null;
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (getHolder(e.getView()) != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        ZerInventoryHolder holder = getHolder(e.getView());
        if (holder == null) return;

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || MenuBuilder.KEY_ACTION == null) return;

        var    pdc      = meta.getPersistentDataContainer();
        String action   = pdc.getOrDefault(MenuBuilder.KEY_ACTION,   PersistentDataType.STRING,  "");
        String disguise = pdc.getOrDefault(MenuBuilder.KEY_DISGUISE, PersistentDataType.STRING,  "");
        String rankId   = pdc.getOrDefault(MenuBuilder.KEY_RANK,     PersistentDataType.STRING,  "");
        String target   = pdc.getOrDefault(MenuBuilder.KEY_PLAYER,   PersistentDataType.STRING,  "");
        int    page     = pdc.getOrDefault(MenuBuilder.KEY_PAGE,     PersistentDataType.INTEGER,  0);

        ConfigManager cfg = plugin.getConfigManager();

        switch (action) {

            // ── Menú principal: abrir input de nombre por chat ────────────────
            case "write" -> {
                player.closeInventory();
                plugin.getChatListener().awaitInput(player);
                player.sendTitle(
                        cfg.colorize(cfg.getPromptTitle()),
                        cfg.colorize(cfg.getPromptSubtitle()),
                        cfg.getPromptFadeIn(), cfg.getPromptStay(), cfg.getPromptFadeOut()
                );
                player.sendMessage(cfg.getPrefix().append(
                        cfg.component(cfg.getMsgWriteDisguise())));
            }

            // ── Menú principal: quitar disfraz activo ─────────────────────────
            case "remove" -> {
                player.closeInventory();
                plugin.getDisguiseManager().removeDisguise(player);
            }

            // ── Menú principal: clic en cabeza de jugador online ──────────────
            case "instant_disguise" -> {
                if (target.isBlank() || player.getName().equalsIgnoreCase(target)) return;
                player.closeInventory();
                plugin.getDisguiseManager().applyDisguise(player, target,
                        rankId.isBlank() ? "default" : rankId);
            }

            // ── Menú de confirmación: confirmar disfraz ───────────────────────
            case "confirm" -> {
                if (disguise.isBlank()) return;
                player.closeInventory();
                plugin.getDisguiseManager().applyDisguise(player, disguise, "");
            }

            // ── Menú de confirmación: cambiar nombre (re-abrir chat) ──────────
            case "rename" -> {
                player.closeInventory();
                plugin.getChatListener().awaitInput(player);
                player.sendTitle(
                        cfg.colorize(cfg.getPromptTitle()),
                        cfg.colorize(cfg.getPromptSubtitle()),
                        cfg.getPromptFadeIn(), cfg.getPromptStay(), cfg.getPromptFadeOut()
                );
                player.sendMessage(cfg.getPrefix().append(
                        cfg.component(cfg.getMsgWriteDisguise())));
            }

            // ── Menú de confirmación: volver al menú principal ────────────────
            case "back" -> {
                player.openInventory(new MenuBuilder(plugin).buildMainMenu(player, 0));
            }

            // ── Navegación por páginas ────────────────────────────────────────
            case "prev_page", "next_page" -> {
                player.openInventory(new MenuBuilder(plugin).buildMainMenu(player, page));
            }

            default -> {}
        }
    }
}
