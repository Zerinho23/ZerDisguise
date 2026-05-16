package me.zerith.zerdisguise;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.entity.Player;

/**
 * Soft-integration with Vault and LuckPerms.
 *
 * Priority order for prefix lookup:
 *  1. Vault (Chat service) — works with LuckPerms, EssentialsX, etc.
 *  2. LuckPerms API directly — used if Vault is not installed.
 *  3. Fallback to config.yml "prefix" field.
 *
 * The plugin compiles against both APIs (provided scope), but uses
 * isPluginEnabled() guards + try/catch so it works without them at runtime.
 */
public class RankProvider {

    private Chat     vaultChat  = null;
    private LuckPerms luckPerms = null;

    private final ZerDisguise plugin;

    public RankProvider(ZerDisguise plugin) {
        this.plugin = plugin;
    }

    /** Call this after all plugins have loaded (in onEnable). */
    public void initialize() {
        // ── Vault ────────────────────────────────────────────────────────────
        try {
            if (plugin.getServer().getPluginManager().isPluginEnabled("Vault")) {
                var rsp = plugin.getServer().getServicesManager()
                        .getRegistration(Chat.class);
                if (rsp != null) {
                    vaultChat = rsp.getProvider();
                    plugin.getLogger().info("Vault Chat integrado correctamente.");
                } else {
                    plugin.getLogger().warning(
                            "Vault detectado pero sin servicio Chat registrado " +
                            "(¿falta un plugin de permisos con soporte Vault?).");
                }
            }
        } catch (NoClassDefFoundError | Exception e) {
            plugin.getLogger().warning("Vault no disponible: " + e.getMessage());
        }

        // ── LuckPerms (solo si Vault no está disponible) ──────────────────
        if (vaultChat == null) {
            try {
                if (plugin.getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
                    luckPerms = LuckPermsProvider.get();
                    plugin.getLogger().info("LuckPerms integrado correctamente.");
                }
            } catch (NoClassDefFoundError | Exception e) {
                plugin.getLogger().warning("LuckPerms no disponible: " + e.getMessage());
            }
        }

        if (vaultChat == null && luckPerms == null) {
            plugin.getLogger().info(
                    "Sin Vault ni LuckPerms — usando prefijos del config.yml.");
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns the real prefix for a permission group (e.g. "admin", "vip").
     * Returns {@code null} if the group has no prefix or integration is unavailable.
     */
    public String getGroupPrefix(String groupName) {
        if (vaultChat != null) {
            try {
                String world  = plugin.getServer().getWorlds().get(0).getName();
                String prefix = vaultChat.getGroupPrefix(world, groupName);
                if (prefix != null && !prefix.isEmpty()) return prefix;
            } catch (Exception ignored) {}
        }
        if (luckPerms != null) {
            try {
                var group = luckPerms.getGroupManager().getGroup(groupName);
                if (group != null) {
                    String prefix = group.getCachedData().getMetaData().getPrefix();
                    if (prefix != null && !prefix.isEmpty()) return prefix;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * Returns the real prefix for an online player (their active group prefix).
     * Returns {@code null} if unavailable.
     */
    public String getPlayerPrefix(Player player) {
        if (vaultChat != null) {
            try {
                String prefix = vaultChat.getPlayerPrefix(player);
                if (prefix != null && !prefix.isEmpty()) return prefix;
            } catch (Exception ignored) {}
        }
        if (luckPerms != null) {
            try {
                var user = luckPerms.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    String prefix = user.getCachedData().getMetaData().getPrefix();
                    if (prefix != null && !prefix.isEmpty()) return prefix;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    public boolean hasVault()      { return vaultChat  != null; }
    public boolean hasLuckPerms()  { return luckPerms  != null; }
    public boolean hasIntegration(){ return vaultChat != null || luckPerms != null; }
}
