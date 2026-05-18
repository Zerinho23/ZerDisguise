package me.zerith.zerdisguise;

import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Hook para el plugin TAB (neznamy/TAB) mediante reflexión.
 *
 * El plugin TAB intercepta los paquetes del tab list a nivel de red e ignora
 * por completo setPlayerListName(). Para mostrar el nombre del disfraz en el
 * tab list cuando TAB está instalado hay que usar su propia API interna.
 *
 * Esta clase usa reflexión para evitar dependencia de compilación directa sobre
 * TAB, de modo que el plugin funciona con o sin TAB instalado.
 *
 * Compatible con TAB v4 / v5 / v6 (neznamy).
 *
 * Durante init() se emiten WARNING para que el resultado sea siempre visible
 * en la consola del servidor, independientemente del nivel de logging configurado.
 */
public class TabHook {

    private enum Strategy { NONE, MANAGER_STRING, MANAGER_COMPONENT, PLAYER_STRING, PLAYER_COMPONENT }

    private static Boolean  available = null;
    private static Strategy strategy  = Strategy.NONE;
    private static Logger   log;

    private static Object   apiInstance;
    private static Method   mGetPlayer;
    private static Method   mGetTabListManager;
    private static Method   mSetCustomTabName;
    private static Method   mFromColoredText;
    private static Class<?> clsTabPlayer;

    // ── Inicialización ────────────────────────────────────────────────────────

    /**
     * Intenta inicializar la integración con TAB.
     * Debe llamarse en onEnable() una vez que TAB ya está habilitado.
     * Emite WARNING en consola para cada paso — el resultado siempre es visible.
     *
     * @return true si la integración quedó operativa.
     */
    public static synchronized boolean init(Logger logger) {
        if (available != null) return available;
        log = logger;

        if (!Bukkit.getPluginManager().isPluginEnabled("TAB")) {
            log.info("[TabHook] TAB no detectado — hook desactivado.");
            return available = false;
        }

        String tabVer = Bukkit.getPluginManager().getPlugin("TAB").getPluginMeta().getVersion();
        log.warning("[TabHook] ══════════════════════════════════════════════════════");
        log.warning("[TabHook] TAB detectado v" + tabVer + " — iniciando hook...");

        try {
            // ── 1. TabAPI class ───────────────────────────────────────────────
            Class<?> clsTabAPI;
            try {
                clsTabAPI = Class.forName("me.neznamy.tab.api.TabAPI");
            } catch (ClassNotFoundException e) {
                log.warning("[TabHook] FALLO: me.neznamy.tab.api.TabAPI no encontrado en classpath.");
                log.warning("[TabHook] ══════════════════════════════════════════════════════");
                return available = false;
            }
            log.warning("[TabHook] TabAPI class OK.");

            // ── 2. TabAPI.getInstance() ───────────────────────────────────────
            apiInstance = clsTabAPI.getMethod("getInstance").invoke(null);
            if (apiInstance == null) {
                log.warning("[TabHook] FALLO: TabAPI.getInstance() devolvio null. TAB no esta listo?");
                log.warning("[TabHook] ══════════════════════════════════════════════════════");
                return available = false;
            }
            log.warning("[TabHook] TabAPI instance: " + apiInstance.getClass().getName());
            log.warning("[TabHook] Metodos TabAPI: " + dumpMethods(clsTabAPI));

            // ── 3. TabPlayer class ────────────────────────────────────────────
            try {
                clsTabPlayer = Class.forName("me.neznamy.tab.api.TabPlayer");
            } catch (ClassNotFoundException e) {
                log.warning("[TabHook] FALLO: me.neznamy.tab.api.TabPlayer no encontrado.");
                log.warning("[TabHook] ══════════════════════════════════════════════════════");
                return available = false;
            }
            log.warning("[TabHook] TabPlayer class OK. Metodos: " + dumpMethods(clsTabPlayer));

            // ── 4. getPlayer(UUID) ────────────────────────────────────────────
            mGetPlayer = clsTabAPI.getMethod("getPlayer", UUID.class);
            log.warning("[TabHook] getPlayer(UUID) OK.");

            // ── 5. Estrategia A: TabAPI.getTabListManager() ───────────────────
            try {
                mGetTabListManager = clsTabAPI.getMethod("getTabListManager");
                Object mgr = mGetTabListManager.invoke(apiInstance);

                if (mgr == null) {
                    log.warning("[TabHook] getTabListManager() devolvio null. La feature tablist-name puede estar");
                    log.warning("[TabHook] desactivada en la config de TAB (tablist-name-formatting.enabled: false).");
                } else {
                    log.warning("[TabHook] Manager clase: " + mgr.getClass().getName());
                    log.warning("[TabHook] Metodos manager: " + dumpMethods(mgr.getClass()));

                    // 5a. String directo
                    Method m = findMethod(mgr.getClass(), "setCustomTabName", clsTabPlayer, String.class);
                    if (m != null) {
                        mSetCustomTabName = m;
                        strategy = Strategy.MANAGER_STRING;
                        log.warning("[TabHook] Estrategia: MANAGER_STRING OK.");
                    } else {
                        // 5b. Componente
                        Class<?> clsComp = resolveClass(
                                "me.neznamy.tab.api.chat.TabComponent",
                                "me.neznamy.tab.api.chat.IChatBaseComponent",
                                "me.neznamy.tab.api.TabComponent");

                        if (clsComp != null) {
                            log.warning("[TabHook] TabComponent class: " + clsComp.getName());
                            m = findMethod(mgr.getClass(), "setCustomTabName", clsTabPlayer, clsComp);
                            if (m != null) {
                                mSetCustomTabName = m;
                                mFromColoredText  = resolveStaticMethod(clsComp,
                                        "fromColoredText", "fromLegacyText", "of", "fromString");
                                strategy = Strategy.MANAGER_COMPONENT;
                                log.warning("[TabHook] Estrategia: MANAGER_COMPONENT"
                                        + " (factory=" + (mFromColoredText != null ? mFromColoredText.getName() : "NINGUNO") + ") OK.");
                            } else {
                                log.warning("[TabHook] No se encontro setCustomTabName(TabPlayer, "
                                        + clsComp.getSimpleName() + ") en el manager.");
                            }
                        } else {
                            log.warning("[TabHook] No se encontro ninguna clase TabComponent en el classpath.");
                        }
                    }
                }
            } catch (NoSuchMethodException e) {
                log.warning("[TabHook] getTabListManager() no existe en esta version de TAB.");
            }

            // ── 6. Estrategia B: directamente en TabPlayer ────────────────────
            if (strategy == Strategy.NONE) {
                log.warning("[TabHook] Intentando estrategia directa sobre TabPlayer...");

                Method m = findMethod(clsTabPlayer, "setCustomTabName", String.class);
                if (m != null) {
                    mSetCustomTabName = m;
                    strategy = Strategy.PLAYER_STRING;
                    log.warning("[TabHook] Estrategia: PLAYER_STRING OK.");
                } else {
                    Class<?> clsComp = resolveClass(
                            "me.neznamy.tab.api.chat.TabComponent",
                            "me.neznamy.tab.api.chat.IChatBaseComponent",
                            "me.neznamy.tab.api.TabComponent");
                    if (clsComp != null) {
                        m = findMethod(clsTabPlayer, "setCustomTabName", clsComp);
                        if (m != null) {
                            mSetCustomTabName = m;
                            mFromColoredText  = resolveStaticMethod(clsComp,
                                    "fromColoredText", "fromLegacyText", "of", "fromString");
                            strategy = Strategy.PLAYER_COMPONENT;
                            log.warning("[TabHook] Estrategia: PLAYER_COMPONENT OK.");
                        }
                    }
                    if (strategy == Strategy.NONE)
                        log.warning("[TabHook] setCustomTabName no encontrado directamente en TabPlayer.");
                }
            }

            // ── 7. Resultado ──────────────────────────────────────────────────
            if (strategy == Strategy.NONE) {
                log.warning("[TabHook] *** FALLO TOTAL: ninguna estrategia encontrada. ***");
                log.warning("[TabHook] El nombre del disfraz NO se podra mostrar en el tab list.");
                log.warning("[TabHook] Por favor reporta los logs anteriores en GitHub Issues de ZerDisguise.");
                log.warning("[TabHook] ══════════════════════════════════════════════════════");
                return available = false;
            }

            log.warning("[TabHook] Hook ACTIVO — estrategia: " + strategy);
            log.warning("[TabHook] ══════════════════════════════════════════════════════");
            return available = true;

        } catch (Exception e) {
            log.warning("[TabHook] Excepcion inesperada en init(): " + e.getClass().getName() + ": " + e.getMessage());
            log.warning("[TabHook] ══════════════════════════════════════════════════════");
            return available = false;
        }
    }

    // ── API pública ───────────────────────────────────────────────────────────

    /** Aplica el nombre del disfraz en la tab list via TAB API. Sin efectos si TAB no está disponible. */
    public static void setTabName(UUID uuid, String disguiseName) {
        if (!Boolean.TRUE.equals(available)) return;
        try {
            Object tabPlayer = mGetPlayer.invoke(apiInstance, uuid);
            if (tabPlayer == null) return; // TAB aún no procesó al jugador

            Object nameArg = buildName(disguiseName);

            switch (strategy) {
                case MANAGER_STRING, MANAGER_COMPONENT -> {
                    Object mgr = mGetTabListManager.invoke(apiInstance);
                    if (mgr != null) mSetCustomTabName.invoke(mgr, tabPlayer, nameArg);
                }
                case PLAYER_STRING, PLAYER_COMPONENT ->
                        mSetCustomTabName.invoke(tabPlayer, nameArg);
                default -> {}
            }
        } catch (Exception e) {
            if (log != null)
                log.warning("[TabHook] setTabName(" + uuid + ") error: "
                        + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /** Elimina el nombre personalizado en la tab list (TAB restaurará su propio formato). */
    public static void clearTabName(UUID uuid) {
        if (!Boolean.TRUE.equals(available)) return;
        try {
            Object tabPlayer = mGetPlayer.invoke(apiInstance, uuid);
            if (tabPlayer == null) return;

            switch (strategy) {
                case MANAGER_STRING, MANAGER_COMPONENT -> {
                    Object mgr = mGetTabListManager.invoke(apiInstance);
                    if (mgr != null) mSetCustomTabName.invoke(mgr, tabPlayer, (Object) null);
                }
                case PLAYER_STRING, PLAYER_COMPONENT ->
                        mSetCustomTabName.invoke(tabPlayer, (Object) null);
                default -> {}
            }
        } catch (Exception e) {
            if (log != null)
                log.warning("[TabHook] clearTabName(" + uuid + ") error: " + e.getMessage());
        }
    }

    public static boolean isAvailable() { return Boolean.TRUE.equals(available); }
    public static String  getStrategyName() { return strategy != null ? strategy.name() : "NONE"; }

    /**
     * Muestra el estado del hook en la consola y al sender (para /zd debug).
     * Puede llamarse DESPUÉS de init().
     */
    public static void diagnose(org.bukkit.command.CommandSender sender) {
        String line = "§8[§bTabHook§8] ";
        sender.sendMessage(line + "§7Disponible: " + (Boolean.TRUE.equals(available) ? "§aYES" : "§cNO"));
        sender.sendMessage(line + "§7Estrategia: §e" + (strategy != null ? strategy.name() : "NONE"));
        sender.sendMessage(line + "§7Ver consola para detalle completo de metodos.");
        if (log != null && Boolean.FALSE.equals(available)) {
            log.warning("[TabHook] diagnose() invocado — reintentando init...");
            available = null;
        }
    }

    // ── Helpers de reflexión ──────────────────────────────────────────────────

    private static Object buildName(String disguiseName) throws Exception {
        String colored = "\u00A7d" + disguiseName;
        if (mFromColoredText != null) return mFromColoredText.invoke(null, colored);
        return colored;
    }

    /**
     * Busca un método por nombre y parámetros en la clase concreta, sus interfaces y superclase.
     * Usa getMethod() (métodos públicos heredados incluidos) para cada tipo de la jerarquía.
     */
    private static Method findMethod(Class<?> cls, String name, Class<?>... params) {
        for (Class<?> c : allTypes(cls)) {
            try { return c.getMethod(name, params); }
            catch (NoSuchMethodException ignored) {}
        }
        // Fallback: buscar por nombre y cantidad de params si el tipo exacto no coincide
        for (Class<?> c : allTypes(cls)) {
            for (Method m : c.getMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == params.length) {
                    return m;
                }
            }
        }
        return null;
    }

    private static Class<?> resolveClass(String... candidates) {
        for (String c : candidates) {
            try { return Class.forName(c); } catch (ClassNotFoundException ignored) {}
        }
        return null;
    }

    private static Method resolveStaticMethod(Class<?> cls, String... names) {
        for (String name : names) {
            try { return cls.getMethod(name, String.class); }
            catch (NoSuchMethodException ignored) {}
        }
        return null;
    }

    private static List<Class<?>> allTypes(Class<?> cls) {
        List<Class<?>> types = new ArrayList<>();
        types.add(cls);
        for (Class<?> iface : cls.getInterfaces()) types.add(iface);
        if (cls.getSuperclass() != null) types.add(cls.getSuperclass());
        return types;
    }

    private static String dumpMethods(Class<?> cls) {
        return Arrays.stream(cls.getMethods())
                .map(Method::getName)
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));
    }
}
