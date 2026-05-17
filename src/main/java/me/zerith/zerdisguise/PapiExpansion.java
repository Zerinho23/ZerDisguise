package me.zerith.zerdisguise;

  import me.clip.placeholderapi.expansion.PlaceholderExpansion;
  import org.bukkit.entity.Player;
  import org.bukkit.OfflinePlayer;
  import org.jetbrains.annotations.NotNull;

  /**
   * Expansion de PlaceholderAPI para ZerDisguise.
   *
   * Placeholders:
   *   %zerdisguise_name%            - nombre del disfraz (real si no hay)
   *   %zerdisguise_displayname%     - displayName con colores
   *   %zerdisguise_rank%            - prefijo del rango visual (con colores)
   *   %zerdisguise_rankid%          - ID del rango (ej: vip, admin)
   *   %zerdisguise_is_disguised%    - true si tiene disfraz completo
   *   %zerdisguise_has_visual_rank% - true si solo tiene rango visual
   *   %zerdisguise_active%          - true si hay cualquier disfraz
   *   %zerdisguise_time%            - tiempo disfrazado MM:SS
   *   %zerdisguise_time_min%        - minutos del tiempo
   *   %zerdisguise_time_seg%        - segundos del tiempo (2 digitos)
   *
   * Uso con UltimateNameTag: configurar nametag con
   *   %zerdisguise_rank% %zerdisguise_name%
   */
  public class PapiExpansion extends PlaceholderExpansion {

      private final ZerDisguise plugin;

      public PapiExpansion(ZerDisguise plugin) {
          this.plugin = plugin;
      }

      @Override public @NotNull String getIdentifier() { return "zerdisguise"; }
      @Override public @NotNull String getAuthor()     { return ZerDisguise.AUTHOR; }
      @Override public @NotNull String getVersion()    { return plugin.getPluginMeta().getVersion(); }
      @Override public boolean persist()               { return true; }
      @Override public boolean canRegister()           { return true; }

      @Override
      public String onRequest(OfflinePlayer offline, @NotNull String params) {
          if (offline == null) return null;

          Player player = offline.getPlayer();

          if (player == null || !player.isOnline()) {
              return switch (params.toLowerCase()) {
                  case "is_disguised", "has_visual_rank", "active" -> "false";
                  case "name", "displayname" -> offline.getName() != null ? offline.getName() : "";
                  case "time", "time_min", "time_seg" -> "0";
                  default -> "";
              };
          }

          DisguiseManager              dm       = plugin.getDisguiseManager();
          DisguiseManager.DisguiseData cur      = dm.getCurrent(player.getUniqueId());
          String                       rankOnly = dm.getVisualRank(player.getUniqueId());
          RankProvider                 rp       = plugin.getRankProvider();
          ConfigManager                cfg      = plugin.getConfigManager();

          return switch (params.toLowerCase()) {
              case "name" ->
                  cur != null ? cur.disguiseName() : player.getName();

              case "displayname" ->
                  (cur != null || rankOnly != null) ? player.getDisplayName() : player.getName();

              case "rank" -> {
                  String rankId = cur != null ? cur.rankId()
                                : rankOnly != null ? rankOnly : null;
                  if (rankId == null) yield "";
                  String prefix = rp.getGroupPrefix(rankId);
                  if (prefix == null || prefix.isBlank()) {
                      for (ConfigManager.RankEntry r : cfg.getRanks()) {
                          if (r.id().equalsIgnoreCase(rankId)) { prefix = r.prefix(); break; }
                      }
                  }
                  yield prefix != null ? cfg.colorize(prefix) : "";
              }

              case "rankid" -> {
                  String rankId = cur != null ? cur.rankId()
                                : rankOnly != null ? rankOnly : null;
                  yield rankId != null ? rankId : "";
              }

              case "is_disguised"    -> String.valueOf(dm.isDisguised(player));
              case "has_visual_rank" -> String.valueOf(dm.hasVisualRank(player));
              case "active"          -> String.valueOf(dm.isDisguised(player) || dm.hasVisualRank(player));

              case "time" -> {
                  Long start = dm.getDisguiseStart(player.getUniqueId());
                  if (start == null) yield "0";
                  long elapsed = (System.currentTimeMillis() - start) / 1000L;
                  yield String.format("%02d:%02d", elapsed / 60, elapsed % 60);
              }

              case "time_min" -> {
                  Long start = dm.getDisguiseStart(player.getUniqueId());
                  if (start == null) yield "0";
                  yield String.valueOf((System.currentTimeMillis() - start) / 1000L / 60);
              }

              case "time_seg" -> {
                  Long start = dm.getDisguiseStart(player.getUniqueId());
                  if (start == null) yield "0";
                  yield String.format("%02d", (System.currentTimeMillis() - start) / 1000L % 60);
              }

              default -> null;
          };
      }
  }
  