package me.zerith.zerdisguise;

  import org.bukkit.Bukkit;
  import org.bukkit.entity.Player;

  import java.util.HashMap;
  import java.util.Map;
  import java.util.UUID;

  /**
   * Gestiona los disfraces activos y anteriores de cada jugador.
   *
   * Modos:
   *  - Disfraz completo: cambia skin, nombre y rango visualmente.
   *  - Rango visual:     solo cambia el prefijo visible (nameplate + displayName),
   *                      sin cambiar la skin ni otorgar permisos reales.
   */
  public class DisguiseManager {

      public record DisguiseData(String disguiseName, String rankId) {}

      private final Map<UUID, DisguiseData> current        = new HashMap<>();
      private final Map<UUID, DisguiseData> previous       = new HashMap<>();
      /** Jugadores que tienen rango visual activo (con o sin disfraz completo). */
      private final Map<UUID, String>       visualRankOnly = new HashMap<>();

      private final ZerDisguise plugin;

      public DisguiseManager(ZerDisguise plugin) {
          this.plugin = plugin;
      }

      // ──────────────────────────────────────────────────────────────────────────
      //  Disfraz completo (skin + nombre + rango)
      // ──────────────────────────────────────────────────────────────────────────

      public void applyDisguise(Player player, String disguiseName, String rankId) {
          ConfigManager cfg = plugin.getConfigManager();
          RankProvider  rp  = plugin.getRankProvider();
          SkinApplier   sa  = plugin.getSkinApplier();

          DisguiseData cur = current.get(player.getUniqueId());
          if (cur != null) previous.put(player.getUniqueId(), cur);

          String resolvedRankId = rankId != null && !rankId.isBlank() ? rankId : "default";
          String rankPrefix     = null;
          String rankDisplay    = null;

          Player onlineTarget = Bukkit.getPlayerExact(disguiseName);
          if (onlineTarget != null) {
              resolvedRankId = rp.getPlayerPrimaryGroup(onlineTarget);
              rankPrefix     = rp.getPlayerPrefix(onlineTarget);
          }

          if (rankPrefix == null || rankPrefix.isBlank()) {
              rankPrefix = rp.getGroupPrefix(resolvedRankId);
          }

          if (rankPrefix == null || rankPrefix.isBlank()) {
              for (ConfigManager.RankEntry r : cfg.getRanks()) {
                  if (r.id().equalsIgnoreCase(resolvedRankId)) {
                      rankPrefix  = r.prefix();
                      rankDisplay = r.color() + r.name();
                      break;
                  }
              }
          }

          if (rankPrefix  == null) rankPrefix  = "";
          if (rankDisplay == null) rankDisplay = "&f" + capitalize(resolvedRankId);

          current.put(player.getUniqueId(), new DisguiseData(disguiseName, resolvedRankId));
          visualRankOnly.remove(player.getUniqueId());

          String display = cfg.colorize(
                  rankPrefix.isBlank() ? "&d" + disguiseName : rankPrefix + " &d" + disguiseName);
          player.setDisplayName(display);
          player.setPlayerListName(display);
          sa.applyNameplate(player, rankPrefix);

          final String finalRankDisplay = rankDisplay;

          player.sendMessage(cfg.getPrefix().append(
                  cfg.component("&7Cargando skin de &d" + disguiseName + "&7...")));

          plugin.getSkinFetcher().fetchSkin(
                  disguiseName,
                  skinData -> {
                      if (!player.isOnline()) return;
                      boolean skinOk = sa.applySkin(player, skinData);

                      String msg = cfg.getMsgApplied()
                              .replace("{disguise}", disguiseName)
                              .replace("{rank}",     finalRankDisplay);
                      player.sendMessage(cfg.getPrefix().append(cfg.component(msg)));

                      if (!skinOk) {
                          player.sendMessage(cfg.getPrefix().append(
                                  cfg.component("&e⚠ &7La skin no pudo aplicarse (nombre si cambiado).")));
                      }
                  },
                  errorMsg -> {
                      if (!player.isOnline()) return;
                      String msg = cfg.getMsgApplied()
                              .replace("{disguise}", disguiseName)
                              .replace("{rank}",     finalRankDisplay);
                      player.sendMessage(cfg.getPrefix().append(cfg.component(msg)));
                      player.sendMessage(cfg.getPrefix().append(
                              cfg.component("&e⚠ &7No se pudo cargar la skin: &c" + errorMsg)));
                  }
          );
      }

      // ──────────────────────────────────────────────────────────────────────────
      //  Rango visual solamente (sin skin ni permisos)
      // ──────────────────────────────────────────────────────────────────────────

      /**
       * Aplica solo el prefijo visual del rango indicado al jugador.
       * No cambia la skin ni otorga permisos reales.
       * Si tiene disfraz activo, actualiza el prefijo de ese disfraz.
       * Si no tiene disfraz, aplica el prefijo sobre su nombre real.
       */
      public void applyRankOnly(Player player, String rankId) {
          ConfigManager cfg = plugin.getConfigManager();
          RankProvider  rp  = plugin.getRankProvider();
          SkinApplier   sa  = plugin.getSkinApplier();

          String rankPrefix  = rp.getGroupPrefix(rankId);
          String rankDisplay = null;

          if (rankPrefix == null || rankPrefix.isBlank()) {
              for (ConfigManager.RankEntry r : cfg.getRanks()) {
                  if (r.id().equalsIgnoreCase(rankId)) {
                      rankPrefix  = r.prefix();
                      rankDisplay = r.color() + r.name();
                      break;
                  }
              }
          }

          if (rankPrefix  == null) rankPrefix  = "";
          if (rankDisplay == null) rankDisplay = "&f" + capitalize(rankId);

          DisguiseData cur = current.get(player.getUniqueId());
          String nameToUse = (cur != null) ? cur.disguiseName() : player.getName();

          if (cur != null) {
              previous.put(player.getUniqueId(), cur);
              current.put(player.getUniqueId(), new DisguiseData(cur.disguiseName(), rankId));
          }

          visualRankOnly.put(player.getUniqueId(), rankId);

          String display = cfg.colorize(
                  rankPrefix.isBlank() ? "&d" + nameToUse : rankPrefix + " &d" + nameToUse);
          player.setDisplayName(display);
          player.setPlayerListName(display);
          sa.applyNameplate(player, rankPrefix);

          String msg = cfg.getMsgRankApplied().replace("{rank}", rankDisplay);
          player.sendMessage(cfg.getPrefix().append(cfg.component(msg)));
      }

      // ──────────────────────────────────────────────────────────────────────────
      //  Quitar disfraz / limpiar estado
      // ──────────────────────────────────────────────────────────────────────────

      public void removeDisguise(Player player) {
          ConfigManager cfg = plugin.getConfigManager();
          SkinApplier   sa  = plugin.getSkinApplier();

          boolean hasDisguise   = isDisguised(player);
          boolean hasVisualRank = visualRankOnly.containsKey(player.getUniqueId());

          if (!hasDisguise && !hasVisualRank) {
              player.sendMessage(cfg.getPrefix().append(
                      cfg.component("&7No tienes ningun disfraz activo.")));
              return;
          }

          DisguiseData cur = current.remove(player.getUniqueId());
          if (cur != null) previous.put(player.getUniqueId(), cur);

          visualRankOnly.remove(player.getUniqueId());

          player.setDisplayName(player.getName());
          player.setPlayerListName(player.getName());
          sa.removeNameplate(player);

          if (hasDisguise) sa.removeSkin(player);

          player.sendMessage(cfg.getPrefix().append(cfg.component(cfg.getMsgRemoved())));
      }

      public void clearOnDeath(Player player) {
          visualRankOnly.remove(player.getUniqueId());
          DisguiseData cur = current.remove(player.getUniqueId());
          if (cur == null) return;
          previous.put(player.getUniqueId(), cur);

          player.setDisplayName(player.getName());
          player.setPlayerListName(player.getName());
          plugin.getSkinApplier().removeNameplate(player);
          plugin.getSkinApplier().removeSkin(player);

          ConfigManager cfg2 = plugin.getConfigManager();
          player.sendMessage(cfg2.getPrefix().append(
                  cfg2.component(cfg2.getMsgDeathRemoved())));
      }

      public void cleanupOnQuit(Player player) {
          visualRankOnly.remove(player.getUniqueId());
          DisguiseData cur = current.remove(player.getUniqueId());
          if (cur != null) previous.put(player.getUniqueId(), cur);

          plugin.getSkinApplier().removeNameplate(player);
          plugin.getSkinApplier().cleanupPlayer(player.getUniqueId());
      }

      // ──────────────────────────────────────────────────────────────────────────
      //  Consultas de estado
      // ──────────────────────────────────────────────────────────────────────────

      public boolean isDisguised(Player player) {
          return current.containsKey(player.getUniqueId());
      }

      public boolean hasVisualRank(Player player) {
          return visualRankOnly.containsKey(player.getUniqueId());
      }

      public String getVisualRank(UUID uuid) {
          return visualRankOnly.get(uuid);
      }

      public DisguiseData getCurrent(UUID uuid)  { return current.get(uuid); }
      public DisguiseData getPrevious(UUID uuid) { return previous.get(uuid); }

      private static String capitalize(String s) {
          if (s == null || s.isEmpty()) return s;
          return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
      }
  }
  