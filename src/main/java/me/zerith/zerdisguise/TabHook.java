package me.zerith.zerdisguise;

import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
 */
public class TabHook {

    private static Boolean   available        = null;
    private static Object    apiInstance;
    private static Method    getPlayer;
    private static Method    getTabListManager;
    private static Method    setCustomTabName;
    /** null si la API acepta String directamente; no null si usa un objeto componente. */
    private static Method    fromColoredText;
    private static Class<?>  tabPlayerIface;

    // ── Inicialización ────────────────────────────────────────────────────────

    /**
     * Intenta inicializar la integración con TAB.
     * Debe llamarse en onEnable() una vez que TAB ya está habilitado.
     *
     * @return true si la integración quedó operativa.
     */
    public static synchronized boolean init() {
        if (available != null) return available;

        if (!Bukkit.getPluginManager().isPluginEnabled("TAB")) {
            return available = false;
        }

        try {
            Class<?> tabApiClass = Class.forName("me.neznamy.tab.api.TabAPI");
            tabPlayerIface        = Class.forName("me.neznamy.tab.api.TabPlayer");

            Method getInstance = tabApiClass.getMethod("getInstance");
            apiInstance        = getInstance.invoke(null);
            if (apiInstance == null) return available = false;

            getPlayer         = tabApiClass.getMethod("getPlayer", UUID.class);
            getTabListManager = tabApiClass.getMethod("getTabListManager");

            Object mgr = getTabListManager.invoke(apiInstance);
            if (mgr == null) return available = false;

            // 1) Intentar API con String directamente (TAB v4 / v5 early)
            setCustomTabName = resolveMethod(mgr, "setCustomTabName", tabPlayerIface, String.class);

            if (setCustomTabName == null) {
                // 2) Intentar API con objeto componente (TAB v5+ / v6)
                Class<?> compClass = resolveClass(
                        "me.neznamy.tab.api.chat.TabComponent",
                        "me.neznamy.tab.api.chat.IChatBaseComponent",
                        "me.neznamy.tab.api.TabComponent");

                if (compClass != null) {
                    setCustomTabName = resolveMethod(mgr, "setCustomTabName", tabPlayerIface, compClass);
                    if (setCustomTabName != null) {
                        fromColoredText = resolveStaticMethod(compClass,
                                "fromColoredText", "fromLegacyText", "of", "fromString");
                    }
                }
            }

            available = (setCustomTabName != null);
        } catch (Exception e) {
            available = false;
        }

        return available;
    }

    // ── API pública ───────────────────────────────────────────────────────────

    /** Aplica el nombre del disfraz en la tab list via TAB API. Sin efectos si TAB no está. */
    public static void setTabName(UUID uuid, String disguiseName) {
        if (!Boolean.TRUE.equals(available)) return;
        try {
            Object tabPlayer = getPlayer.invoke(apiInstance, uuid);
            if (tabPlayer == null) return;
            Object mgr = getTabListManager.invoke(apiInstance);
            if (mgr == null) return;

            if (fromColoredText != null) {
                // API de componente: pasamos el nombre con color §d (light_purple)
                Object comp = fromColoredText.invoke(null, "\u00A7d" + disguiseName);
                setCustomTabName.invoke(mgr, tabPlayer, comp);
            } else {
                // API de String: pasamos directamente
                setCustomTabName.invoke(mgr, tabPlayer, "\u00A7d" + disguiseName);
            }
        } catch (Exception ignored) {
            // Nunca romper el juego por una función cosmética
        }
    }

    /** Elimina el nombre personalizado en la tab list (TAB restaurará el formato real). */
    public static void clearTabName(UUID uuid) {
        if (!Boolean.TRUE.equals(available)) return;
        try {
            Object tabPlayer = getPlayer.invoke(apiInstance, uuid);
            if (tabPlayer == null) return;
            Object mgr = getTabListManager.invoke(apiInstance);
            if (mgr == null) return;
            // null → TAB restaura su propio formato
            setCustomTabName.invoke(mgr, tabPlayer, (Object) null);
        } catch (Exception ignored) {}
    }

    public static boolean isAvailable() {
        return Boolean.TRUE.equals(available);
    }

    // ── Helpers de reflexión ──────────────────────────────────────────────────

    /** Busca un método en la clase concreta del objeto y en todas sus interfaces. */
    private static Method resolveMethod(Object obj, String name, Class<?>... params) {
        for (Class<?> c : typeHierarchy(obj.getClass())) {
            try { return c.getMethod(name, params); }
            catch (NoSuchMethodException ignored) {}
        }
        return null;
    }

    /** Devuelve la primera clase de los candidatos que pueda cargarse. */
    private static Class<?> resolveClass(String... candidates) {
        for (String c : candidates) {
            try { return Class.forName(c); }
            catch (ClassNotFoundException ignored) {}
        }
        return null;
    }

    /** Busca un método estático con un único parámetro String entre los nombres candidatos. */
    private static Method resolveStaticMethod(Class<?> cls, String... names) {
        for (String name : names) {
            try { return cls.getMethod(name, String.class); }
            catch (NoSuchMethodException ignored) {}
        }
        return null;
    }

    /** Clase concreta + todas sus interfaces + superclase directa. */
    private static List<Class<?>> typeHierarchy(Class<?> cls) {
        List<Class<?>> types = new ArrayList<>();
        types.add(cls);
        for (Class<?> iface : cls.getInterfaces()) types.add(iface);
        if (cls.getSuperclass() != null) types.add(cls.getSuperclass());
        return types;
    }
}
