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
   * Delay de 10 ticks para que el cliente procese el "Remove Entity" primero.
   *
   * NAMEPLATE (TAB, NameTagEdit, CMI, etc.)
   * ────────────────────────────────────────
   * Estrategia anti-conflicto con TAB/NameTagEdit:
   *
   * Solo gestionamos el mainScoreboard de Minecraft (nametag vanilla).
   * NO tocamos los scoreboards personales de cada jugador — esos los gestiona
   * TAB/NameTagEdit, que ya leen el nodo LP que inyectamos en priority 9999
   * (via RankProvider.setDisguisePrefix) y mostrarán el rango correcto sin conflictos.
   *
   * Para mantener el nombre del disfraz ("The_Titan19") en el tab list y el chat,
   * la tarea de vigilancia re-aplica setDisplayName y setPlayerListName cada
   * NAMEPLATE_TASK_PERIOD ticks, revirtiendo cualquier sobreescritura de TAB.
   */
  public class SkinApplier {

      private static final String TEXTURES = "textures";

      /** Ticks de espera antes de re-mostrar al jugador tras hidePlayer. */
      private static final long SHOW_DELAY_TICKS = 10L;

      /** Intervalo de la tarea de vigilancia del nameplate (ticks). */
      private static final long NAMEPLATE_TASK_PERIOD = 2L;

      private final Map<UUID, PlayerProfile> originals        = new HashMap<>();
      private final Map<UUID, BukkitTask>    nameplateTasks   = new HashMap<>();
      /** Nombre esperado (con colores §) que debe mostrarse en tab list y chat. */
      private final Map<UUID, String>        expectedDisplays = new HashMap<>();

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
       */
      private void refreshForObservers(Player player) {
          List<Player> observers = Bukkit.getOnlinePlayers().stream()
                  .filter(p -> !p.equals(player))
                  .collect(Collectors.toList());
          if (observers.isEmpty()) return;

          for (Player obs : observers) obs.hidePlayer(plugin, player);

          Bukkit.getScheduler().runTaskLater(plugin, () -> {
              for (Player obs : observers) {
                  if (obs.isOnline()) obs.showPlayer(plugin, player);
              }
          }, SHOW_DELAY_TICKS);
      }

      // ── Nameplate ─────────────────────────────────────────────────────────

      /**
       * Aplica el prefijo de rango en el mainScoreboard y lanza la tarea de vigilancia.
       *
       * @param rankPrefix  prefijo del rango con códigos &  (ej: "&4[&cOwner&4]")
       * @param displayName nombre ya formateado con § para tab list / chat
       *                    (ej: "§4[§cOwner§4] §dThe_Titan19")
       */
      public void applyNameplate(Player player, String rankPrefix, String displayName) {
          expectedDisplays.put(player.getUniqueId(), displayName);
          applyNameplateNow(player, rankPrefix);
          startNameplateTask(player, rankPrefix, displayName);
      }

      private void applyNameplateNow(Player player, String rankPrefix) {
          // Solo mainScoreboard — dejamos los scoreboards de TAB sin tocar.
          // TAB lee el nodo LP inyectado (priority 9999) y muestra el rango correcto
          // sin que tengamos que interferir con sus equipos.
          applyOnScoreboard(player, rankPrefix, Bukkit.getScoreboardManager().getMainScoreboard());
      }

      /**
       * Registra (o actualiza) el team de ZerDisguise en el scoreboard indicado.
       */
      private void applyOnScoreboard(Player player, String rankPrefix, Scoreboard board) {
          if (board == null) return;

          String teamName = safeTeamName(player);

          // Remover de cualquier otro team en ESTE scoreboard para evitar duplicados
          for (Team t : board.getTeams()) {
              if (!t.getName().equals(teamName) && t.hasEntry(player.getName())) {
                  t.removeEntry(player.getName());
              }
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
       * Tarea de NAMEPLATE_TASK_PERIOD ticks que mantiene el disfraz activo ante plugins
       * que intentan revertir los cambios (TAB, CMI, NameTagEdit, etc.).
       *
       * Hace dos cosas cada ciclo:
       *  1. Re-aplica setDisplayName y setPlayerListName si fueron sobreescritos,
       *     manteniendo el nombre del disfraz ("The_Titan19") en tab list y chat.
       *  2. Re-aplica el team en el mainScoreboard si fue removido.
       */
      private void startNameplateTask(Player player, String rankPrefix, String displayName) {
          stopNameplateTask(player.getUniqueId());

          BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
              if (!player.isOnline()) { stopNameplateTask(player.getUniqueId()); return; }

              String expected = expectedDisplays.getOrDefault(player.getUniqueId(), displayName);

              // ── Mantener nombre en tab list y chat ───────────────────
              if (!expected.equals(player.getDisplayName())) {
                  player.setDisplayName(expected);
              }
              if (!expected.equals(player.getPlayerListName())) {
                  player.setPlayerListName(expected);
              }

              // ── Mantener team en mainScoreboard ──────────────────────
              Scoreboard main   = Bukkit.getScoreboardManager().getMainScoreboard();
              Team       myTeam = main.getTeam(safeTeamName(player));
              if (myTeam == null || !myTeam.hasEntry(player.getName())) {
                  applyNameplateNow(player, rankPrefix);
              }

          }, 10L, NAMEPLATE_TASK_PERIOD);
          nameplateTasks.put(player.getUniqueId(), task);
      }

      private void stopNameplateTask(UUID uuid) {
          BukkitTask task = nameplateTasks.remove(uuid);
          if (task != null) task.cancel();
      }

      public void removeNameplate(Player player) {
          stopNameplateTask(player.getUniqueId());
          expectedDisplays.remove(player.getUniqueId());

          // Solo limpiamos el mainScoreboard (único donde creamos teams)
          Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
          Team mainTeam = main.getTeam(safeTeamName(player));
          if (mainTeam != null) mainTeam.unregister();
      }

      // ── Helpers ───────────────────────────────────────────────────────────

      /**
       * Nombre de team único por jugador basado en UUID.
       * "zd_" + 8 chars del UUID sin guiones = 11 chars (límite 16).
       */
      private static String safeTeamName(Player player) {
          String uuidPart = player.getUniqueId().toString().replace("-", "").substring(0, 8);
          return "zd_" + uuidPart;
      }
  }
  