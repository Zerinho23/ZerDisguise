package me.zerith.zerdisguise;

  import com.comphenix.protocol.PacketType;
  import com.comphenix.protocol.ProtocolLibrary;
  import com.comphenix.protocol.ProtocolManager;
  import com.comphenix.protocol.events.ListenerPriority;
  import com.comphenix.protocol.events.PacketAdapter;
  import com.comphenix.protocol.events.PacketContainer;
  import com.comphenix.protocol.events.PacketEvent;
  import com.comphenix.protocol.wrappers.EnumWrappers;
  import com.comphenix.protocol.wrappers.PlayerInfoData;
  import com.comphenix.protocol.wrappers.WrappedChatComponent;
  import com.comphenix.protocol.wrappers.WrappedGameProfile;
  import org.bukkit.Bukkit;

  import java.util.ArrayList;
  import java.util.List;
  import java.util.UUID;

  /**
   * Integración con ProtocolLib para garantizar que el nombre del disfraz
   * aparezca correctamente en el tab list (tecla TAB) incluso cuando el plugin TAB
   * o cualquier otro plugin intercepta y sobreescribe los paquetes de información
   * del jugador.
   *
   * Estrategia:
   *  - Registra un listener en PLAYER_INFO (= PlayerInfoUpdate en 1.19.3+) con prioridad HIGHEST.
   *  - Esto hace que se ejecute DESPUÉS del listener del plugin TAB (que usa NORMAL/HIGH).
   *  - Para cada jugador disfrazado en el paquete, reemplaza el displayName con el
   *    nombre del disfraz (en color §d), anulando lo que TAB haya puesto.
   *
   * Dependencia opcional: si ProtocolLib no está instalado, el hook no hace nada
   * y el plugin sigue funcionando con el mecanismo de re-aplicación periódica
   * (SkinApplier.startNameplateTask, forzado cada 10 ciclos = ~1s).
   */
  public class ProtocolLibHook {

      private final ZerDisguise plugin;
      private ProtocolManager   pm;

      public ProtocolLibHook(ZerDisguise plugin) {
          this.plugin = plugin;
      }

      /** Devuelve true si ProtocolLib está instalado y habilitado en este servidor. */
      public static boolean isAvailable() {
          return Bukkit.getPluginManager().isPluginEnabled("ProtocolLib");
      }

      /**
       * Inicializa el hook. Debe llamarse en onEnable() DESPUÉS de inicializar
       * el DisguiseManager, ya que los listeners lo necesitan.
       */
      public void register() {
          if (!isAvailable()) {
              plugin.getLogger().info("[ProtocolLibHook] ProtocolLib no encontrado — "
                      + "se usara el mecanismo de re-aplicacion periodica.");
              return;
          }
          try {
              pm = ProtocolLibrary.getProtocolManager();
              registerPlayerInfoUpdateListener();
              plugin.getLogger().info("[ProtocolLibHook] Integrado — "
                      + "nombre de disfraz en tab list garantizado.");
          } catch (Exception e) {
              plugin.getLogger().warning("[ProtocolLibHook] Error al registrar: " + e.getMessage());
          }
      }

      // ── Listener ─────────────────────────────────────────────────────────────

      /**
       * PLAYER_INFO_UPDATE se envía a los clientes cada vez que cambia información
       * de un jugador en el tab list (nombre, latencia, gamemode, etc.).
       *
       * El plugin TAB intercepta este paquete para inyectar su propio display name.
       * Nosotros escuchamos con HIGHEST priority para ejecutarnos después y revertir
       * cualquier nombre que TAB haya escrito por encima del nombre del disfraz.
       */
      private void registerPlayerInfoUpdateListener() {
          pm.addPacketListener(new PacketAdapter(plugin, ListenerPriority.HIGHEST,
                  PacketType.Play.Server.PLAYER_INFO) {

              @Override
              public void onPacketSending(PacketEvent event) {
                  try {
                      overrideDisguisedNames(event.getPacket());
                  } catch (Exception ignored) {
                      // Nunca romper el juego por una funcion cosmetica
                  }
              }
          });
      }

      /**
       * Recorre las entradas del paquete PLAYER_INFO_UPDATE y, para cada jugador
       * que esté disfrazado, sustituye el displayName por el nombre del disfraz.
       */
      private void overrideDisguisedNames(PacketContainer packet) {
          DisguiseManager dm = plugin.getDisguiseManager();

          List<PlayerInfoData> entries = packet.getPlayerInfoDataLists().read(0);
          if (entries == null || entries.isEmpty()) return;

          List<PlayerInfoData> modified = new ArrayList<>(entries.size());
          boolean changed = false;

          for (PlayerInfoData entry : entries) {
              WrappedGameProfile profile = entry.getProfile();
              if (profile == null) {
                  modified.add(entry);
                  continue;
              }

              UUID uuid = profile.getUUID();
              DisguiseManager.DisguiseData data = dm.getCurrent(uuid);

              if (data == null) {
                  // Sin disfraz — no modificar
                  modified.add(entry);
                  continue;
              }

              // Construir displayName como JSON de Adventure (color light_purple = §d)
              String disguiseName = data.disguiseName();
              WrappedChatComponent displayName = WrappedChatComponent.fromJson(
                      "{\"text\":\"" + safeJson(disguiseName) + "\",\"color\":\"light_purple\"}");

              // Crear nueva entrada preservando todos los campos originales
              // salvo el displayName que reemplazamos con el nombre del disfraz.
              modified.add(new PlayerInfoData(profile,
                      entry.getLatency(),
                      entry.getGameMode(),
                      displayName));
              changed = true;
          }

          if (changed) {
              packet.getPlayerInfoDataLists().write(0, modified);
          }
      }

      // ── Helpers ──────────────────────────────────────────────────────────────

      private static String safeJson(String s) {
          return s.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n");
      }
  }
  