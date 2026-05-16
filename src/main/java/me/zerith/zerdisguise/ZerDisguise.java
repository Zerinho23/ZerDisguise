package me.zerith.zerdisguise;

import org.bukkit.plugin.java.JavaPlugin;

public class ZerDisguise extends JavaPlugin {

    public static final String AUTHOR = "zerinho23";

    private static final String R   = "\u001B[0m";           // Reset
    private static final String BD  = "\u001B[1m";           // Bold
    private static final String P1  = "\u001B[38;2;255;100;255m"; // Rosa-violeta
    private static final String P2  = "\u001B[38;2;210;80;255m";  // Violeta
    private static final String P3  = "\u001B[38;2;170;70;255m";  // Morado
    private static final String P4  = "\u001B[38;2;130;60;255m";  // Morado oscuro
    private static final String P5  = "\u001B[38;2;100;80;255m";  // Azul-violeta
    private static final String YW  = "\u001B[38;2;255;220;50m";  // Dorado
    private static final String WH  = "\u001B[38;2;240;240;255m"; // Blanco suave
    private static final String GR  = "\u001B[38;2;160;160;180m"; // Gris azulado
    private static final String DG  = "\u001B[38;2;80;80;100m";   // Gris oscuro
    private static final String GN  = "\u001B[38;2;80;255;140m";  // Verde
    private static final String CY  = "\u001B[38;2;80;220;255m";  // Cian

    // Legacy aliases (used in onDisable / reload)
    private static final String ANSI_RESET  = R;
    private static final String ANSI_PURPLE = P2;
    private static final String ANSI_YELLOW = YW;
    private static final String ANSI_WHITE  = WH;
    private static final String ANSI_GRAY   = GR;
    private static final String ANSI_DGRAY  = DG;
    private static final String ANSI_GREEN  = GN;
    private static final String ANSI_CYAN   = CY;

    private static ZerDisguise instance;
    private ConfigManager   configManager;
    private DisguiseManager disguiseManager;
    private SkinFetcher     skinFetcher;
    private SkinApplier     skinApplier;
    private ChatListener    chatListener;

    @Override
    public void onEnable() {
        instance = this;

        configManager   = new ConfigManager(this);
        configManager.loadConfig();

        skinFetcher     = new SkinFetcher(this);
        skinApplier     = new SkinApplier(this);
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

        // ── ASCII art con gradiente ──────────────────────────────────────────
        log("");
        log(P1 + "  ╔══════════════════════════════════════════════════════════╗");
        log(P1 + "  ║                                                          ║");
        log(P1 + "  ║   ____          ____  _                   _             ║");
        log(P2 + "  ║  |_  /___ _ _  |  _ \\(_)______ _ _  _(_)__ ___        ║");
        log(P3 + "  ║   / // -_) '_| | |/ / (_-< _` | || | |(_-</ -_)       ║");
        log(P4 + "  ║  /___\\___|_|   |___/|_|___\\__, |\\_,_|_|___/\\___|    ║");
        log(P5 + "  ║                             |___/                        ║");
        log(P1 + "  ║                                                          ║");
        log(P1 + "  ╠══════════════════════════════════════════════════════════╣");
        // ── Info box ────────────────────────────────────────────────────────
        log(DG  + "  ║  " + GR  + " Versión  " + WH + BD + "ZerDisguise " + YW + "v" + v + R + DG + "                              ║");
        log(DG  + "  ║  " + GR  + " Autor    " + CY + BD + "✦ " + AUTHOR + R + DG + "                                    ║");
        log(DG  + "  ║  " + GR  + " Estado   " + GN + "✔ Plugin cargado correctamente" + DG + "          ║");
        log(DG  + "  ║  " + GR  + " MC       " + WH + "Paper / Spigot  1.20 → 1.21+" + DG + "          ║");
        log(DG  + "  ║  " + GR  + " Comando  " + P2 + "/disguise  /disfraz  /zd" + DG + "               ║");
        log(P1  + "  ╚══════════════════════════════════════════════════════════╝");
        log("");
    }

    private void log(String msg) {
        getLogger().info(msg + ANSI_RESET);
    }

    public static ZerDisguise getInstance()       { return instance; }
    public ConfigManager    getConfigManager()    { return configManager; }
    public DisguiseManager  getDisguiseManager()  { return disguiseManager; }
    public SkinFetcher      getSkinFetcher()      { return skinFetcher; }
    public SkinApplier      getSkinApplier()      { return skinApplier; }
    public ChatListener     getChatListener()     { return chatListener; }
}
