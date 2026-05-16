package me.zerith.zerdisguise;

  import net.luckperms.api.LuckPerms;
  import net.luckperms.api.LuckPermsProvider;
  import net.luckperms.api.model.group.Group;
  import net.milkbowl.vault.chat.Chat;
  import org.bukkit.entity.Player;

  import java.lang.reflect.Method;
  import java.util.ArrayList;
  import java.util.Comparator;
  import java.util.List;

  public class RankProvider {

      public record GroupEntry(String id, String displayPrefix, int weight) {}

      private Chat      vaultChat = null;
      private LuckPerms luckPerms = null;

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
              plugin.getLogger().info("Sin Vault ni LuckPerms — usando prefijos del config.yml.");
          }
      }

      // ── Obtener todos los grupos ──────────────────────────────────────────────

      /**
       * Devuelve todos los grupos de LuckPerms ordenados por peso descendente.
       * Si LuckPerms no está disponible, usa los rangos del config.yml.
       */
      public List<GroupEntry> getAllGroups() {
          List<GroupEntry> result = new ArrayList<>();

          if (luckPerms != null) {
              try {
                  for (Group group : luckPerms.getGroupManager().getLoadedGroups()) {
                      String prefix = group.getCachedData().getMetaData().getPrefix();
                      int weight = group.getWeight().orElse(0);
                      if (prefix == null || prefix.isBlank()) {
                          prefix = "&7[&f" + capitalize(group.getName()) + "&7]";
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
                              prefix = "&7[&f" + capitalize(g) + "&7]";
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

      // ── Grupo primario del jugador ────────────────────────────────────────────

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

      // ── Prefijo de un grupo ───────────────────────────────────────────────────

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
                  Group group = luckPerms.getGroupManager().getGroup(groupName);
                  if (group != null) {
                      String prefix = group.getCachedData().getMetaData().getPrefix();
                      if (prefix != null && !prefix.isEmpty()) return prefix;
                  }
              } catch (Exception ignored) {}
          }
          return null;
      }

      // ── Prefijo del jugador (con soporte de PlaceholderAPI) ───────────────────

      public String getPlayerPrefix(Player player) {
          // 1) PlaceholderAPI — %luckperms_prefix% o %vault_prefix%
          if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
              try {
                  Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                  Method set = papi.getMethod("setPlaceholders", Player.class, String.class);

                  String lpPrefix = (String) set.invoke(null, player, "%luckperms_prefix%");
                  if (valid(lpPrefix, "%luckperms_prefix%")) return lpPrefix;

                  String vPrefix = (String) set.invoke(null, player, "%vault_prefix%");
                  if (valid(vPrefix, "%vault_prefix%")) return vPrefix;
              } catch (Exception ignored) {}
          }

          // 2) Vault Chat directo
          if (vaultChat != null) {
              try {
                  String prefix = vaultChat.getPlayerPrefix(player);
                  if (prefix != null && !prefix.isEmpty()) return prefix;
              } catch (Exception ignored) {}
          }

          // 3) LuckPerms directo
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

      // ── Helpers ───────────────────────────────────────────────────────────────

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
  