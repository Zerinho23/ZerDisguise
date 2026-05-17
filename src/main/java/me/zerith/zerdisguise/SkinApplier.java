package me.zerith.zerdisguise;

  import com.destroystokyo.paper.profile.PlayerProfile;
  import com.destroystokyo.paper.profile.ProfileProperty;
  import org.bukkit.Bukkit;
  import org.bukkit.entity.Player;
  import org.bukkit.scheduler.BukkitTask;
  import org.bukkit.scoreboard.Scoreboard;
  import org.bukkit.scoreboard.Team;

  import java.util.HashMap;
  import java.util.List;
  import java.util.Map;
  import java.util.UUID;
  import java.util.stream.Collectors;

  /**
   * Aplica y restaura skins y nameplates a jugadores.
   *
   * SKIN
   * ────
   * player.setPlayerProfile() actualiza el perfil en la tab-list PERO no envía
   * el paquete de spawn de la entidad a los clientes de otros jugadores —
   * por eso la skin no cambia visualmente para los demás.
   * Solución: ciclo hidePlayer → showPlayer para forzar el re-spawn con la
   * nueva skin en todos los observadores.
   *
   * NAMEPLATE (UltimateNameTag)
   * ───────────────────────────
   * Un jugador solo puede pertenecer a UN team de scoreboard a la vez.
   * UltimateNameTag y otros plugins crean sus propios teams y sobreescriben
   * el nuestro periódicamente.
   * Solución:
   *   1. applyNameplate() saca al jugador de TODOS los teams existentes antes
   *      de añadirlo al nuestro.
   *   2. Una tarea de 5 ticks comprueba si sigue en nuestro team; si no, lo
   *      re-aplica inmediatamente.
   */
  public class SkinApplier {

      private static final String TEXTURES = "textures";

      private final Map<UUID, PlayerProfile> originals      = new HashMap<>();
      private final Map<UUID, BukkitTask>    nameplateTasks = new HashMap<>();

      private final ZerDisguise plugin;

      public SkinApplier(ZerDisguise plugin) {
          this.plugin = plugin;
      }

      // ── Skin ──────────────────────────────────────────────────────────────

      public boolean applySkin(Player player, SkinFetcher.SkinData skin) {
          try {
              originals.computeIfAbsent(player.getUniqueId(), k -> player.getPlayerProfile());
              PlayerProfile profile = player.getPlayerProfile();
              profile.removeProperty(TEXTURES);
              profile.setProperty(new ProfileProperty(TEXTURES, skin.value(), skin.signature()));
              player.setPlayerProfile(profile);
              // Forzar re-spawn en todos los clientes con la nueva skin/perfil
              refreshForObservers(player);
              return true;
          } catch (Exception e) {
              plugin.getLogger().warning("[SkinApplier] Error al aplicar skin a "
                      + player.getName() + ": " + e.getMessage());
              return false;
          }
      }

      public void removeSkin(Player player) {
          try {
              PlayerProfile original = originals.remove(player.getUniqueId());
              if (original == null)
                  original = Bukkit.createProfile(player.getUniqueId(), player.getName());
              player.setPlayerProfile(original);
              refreshForObservers(player);
          } catch (Exception e) {
              plugin.getLogger().warning("[SkinApplier] Error al restaurar skin de "
                      + player.getName() + ": " + e.getMessage());
          }
      }

      public void cleanupPlayer(UUID uuid) {
          originals.remove(uuid);
      }

      /**
       * Fuerza a todos los observadores a recibir los paquetes de spawn
       * del jugador con el perfil/skin actualizado.
       *
       * hidePlayer → "Remove Entity" en el cliente del observador.
       * showPlayer → "Add Player" + "Spawn Entity" con el nuevo perfil.
       */
      private void refreshForObservers(Player player) {
          List<Player> observers = Bukkit.getOnlinePlayers().stream()
                  .filter(p -> !p.equals(player))
                  .collect(Collectors.toList());
          if (observers.isEmpty()) return;

          for (Player obs : observers) obs.hidePlayer(plugin, player);

          Bukkit.getScheduler().runTaskLater(plugin, () -> {
              for (Player obs : observers)
                  if (obs.isOnline()) obs.showPlayer(plugin, player);
          }, 3L);
      }

      // ── Nameplate ─────────────────────────────────────────────────────────

      /**
       * Aplica el prefijo de rango en nametag y scoreboard.
       * Quita al jugador de todos los teams existentes para sobreescribir
       * UltimateNameTag y cualquier otro plugin de nametag, luego inicia la
       * tarea de persistencia que lo re-aplica si otro plugin lo mueve.
       */
      public void applyNameplate(Player player, String rankPrefix) {
          applyNameplateNow(player, rankPrefix);
          startNameplateTask(player, rankPrefix);
      }

      private void applyNameplateNow(Player player, String rankPrefix) {
          Scoreboard board    = Bukkit.getScoreboardManager().getMainScoreboard();
          String     teamName = safeTeamName(player);

          // Sacar de TODOS los teams → anula UltimateNameTag, TabPlugin, etc.
          for (Team t : board.getTeams()) {
              if (!t.getName().equals(teamName) && t.hasEntry(player.getName()))
                  t.removeEntry(player.getName());
          }

          Team team = board.getTeam(teamName);
          if (team == null) team = board.registerNewTeam(teamName);

          ConfigManager cfg    = plugin.getConfigManager();
          String        prefix = (rankPrefix == null || rankPrefix.isBlank()) ? "" : rankPrefix + " ";

          team.prefix(cfg.component(prefix));
          team.suffix(cfg.component(""));
          team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
          team.setOption(Team.Option.COLLISION_RULE,      Team.OptionStatus.ALWAYS);
          if (!team.hasEntry(player.getName())) team.addEntry(player.getName());
      }

      /**
       * Tarea de 5 ticks (250 ms). Overhead mínimo: solo actúa cuando
       * otro plugin movió al jugador fuera de nuestro team.
       */
      private void startNameplateTask(Player player, String rankPrefix) {
          stopNameplateTask(player.getUniqueId());
          BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
              if (!player.isOnline()) { stopNameplateTask(player.getUniqueId()); return; }
              Scoreboard board   = Bukkit.getScoreboardManager().getMainScoreboard();
              Team       ourTeam = board.getTeam(safeTeamName(player));
              if (ourTeam == null || !ourTeam.hasEntry(player.getName()))
                  applyNameplateNow(player, rankPrefix);
          }, 10L, 5L);
          nameplateTasks.put(player.getUniqueId(), task);
      }

      private void stopNameplateTask(UUID uuid) {
          BukkitTask task = nameplateTasks.remove(uuid);
          if (task != null) task.cancel();
      }

      public void removeNameplate(Player player) {
          stopNameplateTask(player.getUniqueId());
          Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
          Team       team  = board.getTeam(safeTeamName(player));
          if (team != null) team.unregister();
      }

      // ── Helpers ───────────────────────────────────────────────────────────

      private static String safeTeamName(Player player) {
          String name = "zd_" + player.getName();
          return name.length() > 16 ? name.substring(0, 16) : name;
      }
  }
  