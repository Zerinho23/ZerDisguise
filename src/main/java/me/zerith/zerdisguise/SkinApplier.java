package me.zerith.zerdisguise;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Method;

/**
 * Applies a skin and nameplate to a player using:
 *
 *  - Reflection to modify the player's GameProfile textures property
 *    (works on Paper 1.20–1.21+ without version-specific imports).
 *  - hide/showPlayer trick to make all online players reload the entity
 *    with the new skin.
 *  - Scoreboard Team to set a prefix that appears above the player's head
 *    (nametag = [rankPrefix] realName).
 */
public class SkinApplier {

    private static final String TEXTURES = "textures";

    private final ZerDisguise plugin;

    public SkinApplier(ZerDisguise plugin) {
        this.plugin = plugin;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Apply {@code skin} textures to {@code player}. Must run on main thread.
     * Returns true if the skin was applied successfully.
     */
    public boolean applySkin(Player player, SkinFetcher.SkinData skin) {
        try {
            patchGameProfile(player, skin.value(), skin.signature());
            refreshForAll(player);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("[SkinApplier] No se pudo aplicar skin a "
                    + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Removes any custom skin and restores the player's original textures
     * from their online session. This is done by performing a refresh; the
     * client will request the real skin again from Mojang.
     */
    public void removeSkin(Player player) {
        try {
            // Clear the textures property so Paper re-fetches the real skin
            clearTexturesProperty(player);
            refreshForAll(player);
        } catch (Exception e) {
            plugin.getLogger().warning("[SkinApplier] No se pudo restaurar skin: " + e.getMessage());
        }
    }

    /**
     * Sets a Scoreboard team prefix that shows {@code rankPrefix} before the
     * player's nametag. Other players will see: [rankPrefix] realName
     */
    public void applyNameplate(Player player, String rankPrefix) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName  = teamName(player);

        Team team = board.getTeam(teamName);
        if (team == null) team = board.registerNewTeam(teamName);

        team.prefix(plugin.getConfigManager().component(rankPrefix + " "));
        team.suffix(plugin.getConfigManager().component(""));
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.ALWAYS);

        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }
    }

    /** Removes the Scoreboard team (restores vanilla nametag). */
    public void removeNameplate(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = board.getTeam(teamName(player));
        if (team != null) team.unregister();
    }

    // ── Skin patch via reflection ─────────────────────────────────────────────

    /**
     * Uses reflection to replace the "textures" property in the player's
     * GameProfile with the provided value and signature.
     *
     * Works on Paper 1.20 – 1.21+ without importing CraftBukkit or NMS classes.
     */
    private void patchGameProfile(Player player, String value, String signature)
            throws Exception {

        Object gameProfile = getGameProfile(player);
        Object propMap     = invoke(gameProfile, "getProperties");

        // Remove existing textures entry
        callByName(propMap, "removeAll", TEXTURES);

        // Build new Property — com.mojang.authlib.properties.Property
        Object newProp = buildProperty(value, signature);

        // Add it back
        callByName(propMap, "put", TEXTURES, newProp);
    }

    private void clearTexturesProperty(Player player) throws Exception {
        Object gameProfile = getGameProfile(player);
        Object propMap     = invoke(gameProfile, "getProperties");
        callByName(propMap, "removeAll", TEXTURES);
    }

    private Object getGameProfile(Player player) throws Exception {
        // CraftPlayer → getHandle() → ServerPlayer
        Object handle = invoke(player, "getHandle");

        // Find method whose return type is GameProfile
        for (Method m : handle.getClass().getMethods()) {
            if (m.getParameterCount() == 0
                    && m.getReturnType().getSimpleName().equals("GameProfile")) {
                return m.invoke(handle);
            }
        }
        throw new Exception("Método getGameProfile() no encontrado en ServerPlayer");
    }

    private static Object buildProperty(String value, String signature) throws Exception {
        Class<?> cls = Class.forName("com.mojang.authlib.properties.Property");
        try {
            // Paper 1.20+ — Property(String name, String value, String signature)
            return cls.getConstructor(String.class, String.class, String.class)
                    .newInstance(TEXTURES, value, signature);
        } catch (NoSuchMethodException e) {
            // Older authlib — Property(String name, String value)
            return cls.getConstructor(String.class, String.class)
                    .newInstance(TEXTURES, value);
        }
    }

    /** Invokes the first method matching {@code name} with no parameters. */
    private static Object invoke(Object target, String methodName) throws Exception {
        for (Method m : target.getClass().getMethods()) {
            if (m.getName().equals(methodName) && m.getParameterCount() == 0) {
                return m.invoke(target);
            }
        }
        throw new Exception("Método '" + methodName + "' no encontrado en "
                + target.getClass().getSimpleName());
    }

    /** Invokes the first method matching {@code name} with one or two arguments. */
    private static void callByName(Object target, String methodName, Object... args)
            throws Exception {
        for (Method m : target.getClass().getMethods()) {
            if (m.getName().equals(methodName) && m.getParameterCount() == args.length) {
                m.invoke(target, args);
                return;
            }
        }
        // Not fatal — log but don't throw
    }

    // ── Player refresh ────────────────────────────────────────────────────────

    /**
     * Makes every online player "forget" and re-load the target player's entity,
     * forcing a skin refresh.
     */
    private void refreshForAll(Player player) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;
            online.hidePlayer(plugin, player);
            online.showPlayer(plugin, player);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String teamName(Player player) {
        String name = "zd_" + player.getName();
        return name.length() > 16 ? name.substring(0, 16) : name;
    }
}
