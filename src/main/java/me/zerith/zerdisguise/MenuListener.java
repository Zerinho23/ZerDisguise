package me.zerith.zerdisguise;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

public class MenuListener implements Listener {

    private final ZerDisguise plugin;

    public MenuListener(ZerDisguise plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        String title = plugin.getConfigManager().colorize(
                plugin.getConfigManager().getMenuTitle());

        String invTitle = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection().serialize(e.getView().title());

        boolean isMainMenu    = invTitle.contains("ZerDisguise") && !invTitle.contains("Confirmar");
        boolean isConfirmMenu = invTitle.contains("Confirmar disfraz");

        if (!isMainMenu && !isConfirmMenu) return;

        // Guarda de permiso: si el jugador perdió el permiso con el inventario abierto
        if (!player.hasPermission("zerdisguise.use")) {
            player.closeInventory();
            return;
        }

        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        ConfigManager   cfg = plugin.getConfigManager();
        DisguiseManager dm  = plugin.getDisguiseManager();
        MenuBuilder     mb  = new MenuBuilder(plugin);
        ChatListener    cl  = plugin.getChatListener();
        int slot = e.getSlot();

        if (isMainMenu) {
            switch (slot) {
                case 13 -> {
                    // Custom head — open chat input mode
                    player.closeInventory();
                    player.sendTitle(
                            cfg.colorize(cfg.getPromptTitle()),
                            cfg.colorize(cfg.getPromptSubtitle()),
                            cfg.getPromptFadeIn(),
                            cfg.getPromptStay(),
                            cfg.getPromptFadeOut());
                    player.sendMessage(cfg.getPrefix().append(
                            cfg.component(cfg.getMsgWriteDisguise())));
                    cl.awaitInput(player);
                }
                case 17 -> {
                    // Remove disguise
                    if (dm.isDisguised(player)) {
                        dm.removeDisguise(player);
                    }
                    player.openInventory(mb.buildMainMenu(player));
                }
            }
            return;
        }

        // ── Confirm menu ──────────────────────────────────────────────────────
        if (isConfirmMenu) {
            String[] state = cl.getPendingConfirm(player);
            if (state == null) { player.closeInventory(); return; }
            String disguiseName = state[0];
            String rankId       = state[1];

            switch (slot) {
                case 20 -> {
                    // Change name — go back to write mode
                    player.closeInventory();
                    player.sendTitle(
                            cfg.colorize(cfg.getPromptTitle()),
                            cfg.colorize(cfg.getPromptSubtitle()),
                            cfg.getPromptFadeIn(), cfg.getPromptStay(), cfg.getPromptFadeOut());
                    player.sendMessage(cfg.getPrefix().append(
                            cfg.component(cfg.getMsgWriteDisguise())));
                    cl.awaitInput(player);
                }
                case 22 -> {
                    // Confirm — close inventory first, then start async skin fetch
                    cl.clearPendingConfirm(player);
                    player.closeInventory();
                    dm.applyDisguise(player, disguiseName, rankId);
                }
                case 24 -> {
                    // Back to main menu
                    cl.clearPendingConfirm(player);
                    player.openInventory(mb.buildMainMenu(player));
                }
                default -> {
                    // Rank selection slots 10–16
                    if (slot >= 10 && slot <= 16) {
                        int rankIdx = slot - 10;
                        var ranks   = cfg.getRanks();
                        if (rankIdx < ranks.size()) {
                            ConfigManager.RankEntry rank = ranks.get(rankIdx);
                            if (!player.hasPermission(rank.permission())) {
                                player.sendMessage(cfg.getPrefix().append(
                                        cfg.component(cfg.getMsgNoPermission())));
                                return;
                            }
                            cl.setPendingConfirm(player, disguiseName, rank.id());
                            player.openInventory(mb.buildConfirmMenu(player, disguiseName, rank.id()));
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        plugin.getDisguiseManager().clearOnDeath(e.getEntity());
    }
}
