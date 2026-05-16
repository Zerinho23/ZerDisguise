package me.zerith.zerdisguise;

import org.bukkit.plugin.java.JavaPlugin;

public class ZerDisguise extends JavaPlugin {

    public static final String AUTHOR = "zerinho23";

    private static final String ANSI_RESET  = "\u001B[0m";
    private static final String ANSI_PURPLE = "\u001B[38;2;255;85;255m";
    private static final String ANSI_YELLOW = "\u001B[38;2;255;255;85m";
    private static final String ANSI_WHITE  = "\u001B[38;2;255;255;255m";
    private static final String ANSI_GRAY   = "\u001B[38;2;170;170;170m";
    private static final String ANSI_DGRAY  = "\u001B[38;2;85;85;85m";
    private static final String ANSI_GREEN  = "\u001B[38;2;85;255;85m";
    private static final String ANSI_CYAN   = "\u001B[38;2;85;255;255m";

    private static ZerDisguise instance;
    private ConfigManager   configManager;
    private DisguiseManager disguiseManager;
    private ChatListener    chatListener;

    @Override
    public void onEnable() {
        instance = this;

        configManager   = new ConfigManager(this);
        configManager.loadConfig();

        disguiseManager = new DisguiseManager(this);
        chatListener    = new ChatListener(this);

        DisguiseCommand cmd = new DisguiseCommand(this);
        getCommand("disguise").setExecutor(cmd);
        getCommand("disguise").setTabCompleter(cmd);

        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(chatListener, this);

        printBanner();
    }

    @Override
    public void onDisable() {
        getLogger().info(ANSI_PURPLE + "ZerDisguise" + ANSI_GRAY + " deshabilitado." + ANSI_RESET);
    }

    public void reload() {
        configManager.loadConfig();
        getLogger().info(ANSI_GREEN + "Configuración recargada correctamente." + ANSI_RESET);
    }

    private void printBanner() {
        String v = getPluginMeta().getVersion();
        String P = ANSI_PURPLE;
        String Y = ANSI_YELLOW;
        String W = ANSI_WHITE;
        String C = ANSI_CYAN;
        String A = ANSI_GREEN;
        String D = ANSI_DGRAY;
        String S = ANSI_GRAY;
        String R = ANSI_RESET;

        getLogger().info(P + "    ______         ____  _                   _         " + R);
        getLogger().info(P + "   /_  / /__ _____/ __ \\(_)__ ___ __ __(_)__ ___ " + R);
        getLogger().info(P + "    / / / -_) __/ /_/ / (_-</ _ `/ // / /(_-</ -_)" + R);
        getLogger().info(P + "   /___/\\__/_/  \\____/_/___/\\_, /\\_,_/_/___/\\__/ " + R);
        getLogger().info(P + "                              /_/                    " + R);
        getLogger().info("");
        getLogger().info(D + "  ┌────────────────────────────────────────────┐" + R);
        getLogger().info(D + "  │  " + Y + "Plugin" + D + "  " + W + "ZerDisguise " + Y + "v" + v + D + "                     │" + R);
        getLogger().info(D + "  │  " + S + "Autor " + D + "  " + C + AUTHOR + D + "                             │" + R);
        getLogger().info(D + "  │  " + S + "Estado" + D + "  " + A + "✔ Cargado correctamente" + D + "         │" + R);
        getLogger().info(D + "  │  " + S + "MC    " + D + "  " + W + "1.20 → 1.21+" + D + "                      │" + R);
        getLogger().info(D + "  └────────────────────────────────────────────┘" + R);
        getLogger().info("");
    }

    public static ZerDisguise getInstance()       { return instance; }
    public ConfigManager    getConfigManager()    { return configManager; }
    public DisguiseManager  getDisguiseManager()  { return disguiseManager; }
    public ChatListener     getChatListener()     { return chatListener; }
}
