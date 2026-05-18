package me.zerith.zerdisguise;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PrefixNode;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RankProvider {

    public record GroupEntry(String id, String displayPrefix, int weight) {}

    /**
     * Prioridad del nodo de prefijo que inyectamos en LuckPerms al disfrazarse.
     * Debe ser mayor que cualquier prefijo de grupo real del servidor.
     * 9999 es suficientemente alto para casi cualquier configuración.
     */
    private static final int DISGUISE_PREFIX_PRIORITY = 9999;

    private Chat      vaultChat = null;
    private LuckPerms luckPerms = null;

    /** Prefijos originales en Vault — para restaurarlos al quitar el disfraz. */
    private final Map<UUID, String> vaultOriginalPrefixes = new HashMap<>();

    private final ZerDisguise plugin;

    public RankProvider(ZerDisguise plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        try {
            if (plugin.getServer().getPluginManager().isPluginEnabled("Vault")) {
                var rsp = plugin.getServer().getServicesManager().getRegistration(Chat.class);
                if (rsp != null) {
                    vaultChat = rsp.getProvider();
                    plugin.getLogger().info("Vault Chat integrado correctamente.");
                } else {
                    plugin.getLogger().warning("Vault detectado pero sin servicio Chat registrado.");
                }
            }
        } catch (NoClassDefFoundError | Exception e) {
            plugin.getLogger().warning("Vault no disponible: " + e.getMessage());
        }

        try {
            if (plugin.getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
                luckPerms = LuckPermsProvider.get();
                plugin.getLogger().info("LuckPerms integrado correctamente.");
            }
        } catch (NoClassDefFoundError | Exception e) {
            plugin.getLogger().warning("LuckPerms no disponible: " + e.getMessage());
        }

        if (vaultChat == null && luckPerms == null) {
            plugin.getLogger().info("Sin Vault ni LuckPerms — usando rangos del config.yml.");
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Prefijo de disfraz — sobreescribe el prefijo real en LP/Vault
    // ──────────────────────────────────────────────────────────────

    /**
     * Inyecta un nodo de prefijo temporal en LuckPerms con prioridad DISGUISE_PREFIX_PRIORITY
     * (9999), que sobreescribe el prefijo real del grupo del jugador.
     * Los plugins de chat que lean LP/Vault verán solo este prefijo, evitando
     * que aparezcan dos rangos a la vez en el chat.
     *
     * Si prefix es null o vacío se elimina el nodo sin añadir uno nuevo (equivale a clearDisguisePrefix).
     */
    public void setDisguisePrefix(Player player, String prefix) {
        // ── LuckPerms ──────────────────────────────────────────────
        if (luckPerms != null) {
            try {
                User user = luckPerms.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    // Eliminar cualquier nodo de prefijo previo con nuestra prioridad
                    user.data().clear(NodeType.PREFIX.predicate(
                            n -> n.getPriority() == DISGUISE_PREFIX_PRIORITY));

                    if (prefix != null && !prefix.isBlank()) {
                        // Convertir códigos & a § para que LP los interprete correctamente
                        String lpPrefix = toSectionCodes(prefix);
                        user.data().add(PrefixNode.builder(lpPrefix, DISGUISE_PREFIX_PRIORITY).build());
                    }

                    // Guardar de forma asíncrona (no bloquea el hilo principal)
                    luckPerms.getUserManager().saveUser(user);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[RankProvider] Error al establecer prefijo de disfraz en LP: "
                        + e.getMessage());
            }
            // Si hay LP no tocamos Vault aunque esté presente (LP tiene prioridad)
            return;
        }

        // ── Vault (fallback) ───────────────────────────────────────
        if (vaultChat != null) {
            try {
                String world = plugin.getServer().getWorlds().get(0).getName();

                // Guardar el prefijo original solo la primera vez
                vaultOriginalPrefixes.computeIfAbsent(player.getUniqueId(),
                        k -> {
                            String orig = vaultChat.getPlayerPrefix(world, player.getName());
                            return orig != null ? orig : "";
                        });

                String newPrefix = (prefix != null && !prefix.isBlank())
                        ? toSectionCodes(prefix) : "";
                vaultChat.setPlayerPrefix(world, player.getName(), newPrefix);
            } catch (Exception e) {
                plugin.getLogger().warning("[RankProvider] Error al establecer prefijo de disfraz en Vault: "
                        + e.getMessage());
            }
        }
    }

    /**
     * Elimina el nodo de prefijo de disfraz y restaura el prefijo real del jugador.
     */
    public void clearDisguisePrefix(Player player) {
        // ── LuckPerms ──────────────────────────────────────────────
        if (luckPerms != null) {
            try {
                User user = luckPerms.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    user.data().clear(NodeType.PREFIX.predicate(
                            n -> n.getPriority() == DISGUISE_PREFIX_PRIORITY));
                    luckPerms.getUserManager().saveUser(user);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[RankProvider] Error al limpiar prefijo de disfraz en LP: "
                        + e.getMessage());
            }
            return;
        }

        // ── Vault (fallback) ───────────────────────────────────────
        if (vaultChat != null) {
            try {
                String world    = plugin.getServer().getWorlds().get(0).getName();
                String original = vaultOriginalPrefixes.remove(player.getUniqueId());
                vaultChat.setPlayerPrefix(world, player.getName(),
                        original != null ? original : "");
            } catch (Exception e) {
                plugin.getLogger().warning("[RankProvider] Error al restaurar prefijo en Vault: "
                        + e.getMessage());
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Grupos y prefijos
    // ──────────────────────────────────────────────────────────────

    public List<GroupEntry> getAllGroups() {
        List<GroupEntry> result = new ArrayList<>();

        if (luckPerms != null) {
            try {
                for (Group group : luckPerms.getGroupManager().getLoadedGroups()) {
                    String prefix = group.getCachedData().getMetaData().getPrefix();
                    int weight = group.getWeight().orElse(0);
                    if (prefix == null || prefix.isBlank()) {
                        prefix = "&8[&f" + capitalize(group.getName()) + "&8]";
                    }
                    result.add(new GroupEntry(group.getName(), prefix, weight));
                }
                result.sort(Comparator.comparingInt(GroupEntry::weight).reversed());
                return result;
            } catch (Exception e) {
                plugin.getLogger().warning("[RankProvider] Error al obtener grupos LP: " + e.getMessage());
            }
        }

        if (vaultChat != null) {
            try {
                String world = plugin.getServer().getWorlds().get(0).getName();
                String[] groups = vaultChat.getGroups();
                if (groups != null) {
                    for (String g : groups) {
                        String prefix = vaultChat.getGroupPrefix(world, g);
                        if (prefix == null || prefix.isBlank()) {
                            prefix = "&8[&f" + capitalize(g) + "&8]";
                        }
                        result.add(new GroupEntry(g, prefix, 0));
                    }
                    return result;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[RankProvider] Error al obtener grupos Vault: " + e.getMessage());
            }
        }

        // Fallback: config.yml
        for (ConfigManager.RankEntry r : plugin.getConfigManager().getRanks()) {
            result.add(new GroupEntry(r.id(), r.prefix(), 0));
        }
        return result;
    }

    public String getPlayerPrimaryGroup(Player player) {
        if (luckPerms != null) {
            try {
                var user = luckPerms.getUserManager().getUser(player.getUniqueId());
                if (user != null) return user.getPrimaryGroup();
            } catch (Exception ignored) {}
        }
        if (vaultChat != null) {
            try {
                String group = vaultChat.getPrimaryGroup(player);
                if (group != null && !group.isBlank()) return group;
            } catch (Exception ignored) {}
        }
        return "default";
    }

    public String getGroupPrefix(String groupName) {
        if (luckPerms != null) {
            try {
                Group group = luckPerms.getGroupManager().getGroup(groupName);
                if (group != null) {
                    String prefix = group.getCachedData().getMetaData().getPrefix();
                    if (prefix != null && !prefix.isEmpty()) return prefix;
                }
            } catch (Exception ignored) {}
        }
        if (vaultChat != null) {
            try {
                String world  = plugin.getServer().getWorlds().get(0).getName();
                String prefix = vaultChat.getGroupPrefix(world, groupName);
                if (prefix != null && !prefix.isEmpty()) return prefix;
            } catch (Exception ignored) {}
        }
        return null;
    }

    public String getPlayerPrefix(Player player) {
        // 1) PlaceholderAPI
        if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                Method   set  = papi.getMethod("setPlaceholders", Player.class, String.class);

                String lpPrefix = (String) set.invoke(null, player, "%luckperms_prefix%");
                if (valid(lpPrefix, "%luckperms_prefix%")) return lpPrefix;

                String vPrefix = (String) set.invoke(null, player, "%vault_prefix%");
                if (valid(vPrefix, "%vault_prefix%")) return vPrefix;
            } catch (Exception ignored) {}
        }

        // 2) LuckPerms directo
        if (luckPerms != null) {
            try {
                var user = luckPerms.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    String prefix = user.getCachedData().getMetaData().getPrefix();
                    if (prefix != null && !prefix.isEmpty()) return prefix;
                }
            } catch (Exception ignored) {}
        }

        // 3) Vault Chat
        if (vaultChat != null) {
            try {
                String prefix = vaultChat.getPlayerPrefix(player);
                if (prefix != null && !prefix.isEmpty()) return prefix;
            } catch (Exception ignored) {}
        }

        return null;
    }

    // ──────────────────────────────────────────────────────────────
    //  Helpers
    // ──────────────────────────────────────────────────────────────

    /**
     * Convierte códigos de color con & a § para que LuckPerms y Vault
     * los interpreten correctamente al mostrar el prefijo en el chat.
     */
    private static String toSectionCodes(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '&' && i + 1 < text.length()) {
                char next = text.charAt(i + 1);
                if ("0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(next) >= 0) {
                    sb.append('\u00A7').append(next);
                    i++;
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static boolean valid(String value, String placeholder) {
        return value != null && !value.isBlank() && !value.equals(placeholder);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    public boolean hasVault()       { return vaultChat  != null; }
    public boolean hasLuckPerms()   { return luckPerms  != null; }
    public boolean hasIntegration() { return vaultChat != null || luckPerms != null; }
}
