package me.zerith.zerdisguise;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Method;

/**
 * Applies/removes a skin and nametag prefix to a player via:
 *  - Reflection to modify GameProfile textures (Paper 1.20-1.21+ compatible).
 *  - hide/showPlayer trick for all online players to reload the entity.
 *  - Scoreboard Team for nameplate prefix above the player's head.
 */
public class SkinApplier {

    private static final String TEXTURES = "textures";
    private final ZerDisguise plugin;

    public SkinApplier(ZerDisguise plugin) {
        this.plugin = plugin;
    }

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

    public void removeSkin(Player player) {
        try {
            clearTexturesProperty(player);
            refreshForAll(player);
        } catch (Exception e) {
            plugin.getLogger().warning("[SkinApplier] No se pudo restaurar skin: " + e.getMessage());
        }
    }

    public void applyNameplate(Player player, String rankPrefix) {
        Scoreboard board   = Bukkit.getScoreboardManager().getMainScoreboard();
        String     teamName = safeTeamName(player);

        Team team = board.getTeam(teamName);
        if (team == null) team = board.registerNewTeam(teamName);

        ConfigManager cfg = plugin.getConfigManager();
        String prefix = rankPrefix == null || rankPrefix.isBlank() ? "" : rankPrefix + " ";
        team.prefix(cfg.component(prefix));
        team.suffix(cfg.component(""));
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.ALWAYS);

        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }
    }

    public void removeNameplate(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = board.getTeam(safeTeamName(player));
        if (team != null) team.unregister();
    }

    private void patchGameProfile(Player player, String value, String signature) throws Exception {
        Object gameProfile = getGameProfile(player);
        Object propMap     = invoke(gameProfile, "getProperties");
        callByName(propMap, "removeAll", TEXTURES);
        Object newProp = buildProperty(value, signature);
        callByName(propMap, "put", TEXTURES, newProp);
    }

    private void clearTexturesProperty(Player player) throws Exception {
        Object gameProfile = getGameProfile(player);
        Object propMap     = invoke(gameProfile, "getProperties");
        callByName(propMap, "removeAll", TEXTURES);
    }

    private Object getGameProfile(Player player) throws Exception {
        Object handle = invoke(player, "getHandle");
        for (Method m : handle.getClass().getMethods()) {
            if (m.getParameterCount() == 0
                    && m.getReturnType().getSimpleName().equals("GameProfile")) {
                return m.invoke(handle);
            }
        }
        throw new Exception("getGameProfile() no encontrado en ServerPlayer");
    }

    private static Object buildProperty(String value, String signature) throws Exception {
        Class<?> cls = Class.forName("com.mojang.authlib.properties.Property");
        try {
            return cls.getConstructor(String.class, String.class, String.class)
                    .newInstance(TEXTURES, value, signature);
        } catch (NoSuchMethodException e) {
            return cls.getConstructor(String.class, String.class)
                    .newInstance(TEXTURES, value);
        }
    }

    private static Object invoke(Object target, String methodName) throws Exception {
        for (Method m : target.getClass().getMethods()) {
            if (m.getName().equals(methodName) && m.getParameterCount() == 0) {
                return m.invoke(target);
            }
        }
        throw new Exception("Metodo '" + methodName + "' no encontrado en "
                + target.getClass().getSimpleName());
    }

    private static void callByName(Object target, String methodName, Object... args) throws Exception {
        for (Method m : target.getClass().getMethods()) {
            if (m.getName().equals(methodName) && m.getParameterCount() == args.length) {
                m.invoke(target, args);
                return;
            }
        }
    }

    private void refreshForAll(Player player) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;
            online.hidePlayer(plugin, player);
            online.showPlayer(plugin, player);
        }
    }

    private static String safeTeamName(Player player) {
        String name = "zd_" + player.getName();
        return name.length() > 16 ? name.substring(0, 16) : name;
    }
}
