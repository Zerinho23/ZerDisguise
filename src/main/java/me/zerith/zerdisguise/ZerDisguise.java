package me.zerith.zerdisguise;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class ZerDisguise extends JavaPlugin {

    public static final String AUTHOR = "zerinho23";

    private static final String R  = "\u001B[0m";
    private static final String BD = "\u001B[1m";
    private static final String P1 = "\u001B[38;2;255;100;255m";
    private static final String P2 = "\u001B[38;2;210;80;255m";
    private static final String P3 = "\u001B[38;2;170;70;255m";
    private static final String P4 = "\u001B[38;2;130;60;255m";
    private static final String P5 = "\u001B[38;2;100;80;255m";
    private static final String YW = "\u001B[38;2;255;220;50m";
    private static final String WH = "\u001B[38;2;240;240;255m";
    private static final String GR = "\u001B[38;2;160;160;180m";
    private static final String DG = "\u001B[38;2;80;80;100m";
    private static final String GN = "\u001B[38;2;80;255;140m";
    private static final String CY = "\u001B[38;2;80;220;255m";

    private static ZerDisguise instance;
    private ConfigManager   configManager;
    private MenuConfig      menuConfig;
    private RankProvider    rankProvider;
    private DisguiseManager disguiseManager;
    private SkinFetcher     skinFetcher;
    private SkinApplier     skinApplier;
    private ChatListener    chatListener;
    private ProtocolLibHook protocolLibHook;
    private BukkitTask      actionbarTask;

    @Override
    public void onEnable() {
        instance = this;

        configManager = new ConfigManager(this);
        configManager.loadConfig();

        menuConfig = new MenuConfig(this);
        menuConfig.load();

        MenuBuilder.initKeys(this);

        rankProvider    = new RankProvider(this);
        rankProvider.initialize();

        skinFetcher     = new SkinFetcher(this);
        skinApplier     = new SkinApplier(this);
        disguiseManager = new DisguiseManager(this);
        chatListener    = new ChatListener(this);
        protocolLibHook = new ProtocolLibHook(this);
        protocolLibHook.register();

        // Hook de TAB (neznamy) — debe inicializarse después de que TAB esté habilitado
        if (TabHook.init()) {
            getLogger().info(GN + "TAB hook activo — nombre del disfraz en tab list garantizado." + R);
        } else if (Bukkit.getPluginManager().isPluginEnabled("TAB")) {
            getLogger().warning("TAB detectado pero API no accesible — nombre del disfraz puede no verse en tab list.");
        }

        DisguiseCommand cmd = new DisguiseCommand(this);

        // Registro del comando principal /disguise y sus alias
        getCommand("disguise").setExecutor(cmd);
        getCommand("disguise").setTabCompleter(cmd);

        // Registro del comando /undisguise como atajo independiente
        getCommand("undisguise").setExecutor(cmd);
        getCommand("undisguise").setTabCompleter(cmd);

        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(chatListener, this);

        // Registrar expansión de PlaceholderAPI si está presente
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PapiExpansion(this).register();
            getLogger().info(GN + "PlaceholderAPI expansion registrada." + R);
        }

        // Tarea global de la barra de acción (guardamos referencia para poder reiniciarla en /reload)
        scheduleActionbarTask();

        printBanner();
    }

    @Override
    public void onDisable() {
        getLogger().info(P2 + "ZerDisguise" + GR + " deshabilitado." + R);
    }

    public void reload() {
        configManager.loadConfig();
        menuConfig.load();
        // Reiniciar la tarea para que el nuevo interval sea efectivo
        if (actionbarTask != null) actionbarTask.cancel();
        scheduleActionbarTask();
        getLogger().info(GN + "Configuracion recargada correctamente." + R);
    }

    private void scheduleActionbarTask() {
        int interval = configManager.getActionbarInterval();
        actionbarTask = Bukkit.getScheduler().runTaskTimer(
                this, () -> disguiseManager.tickActionbar(), interval, interval);
    }

    private void printBanner() {
        String v = getPluginMeta().getVersion();
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
        log(DG  + "  ║  " + GR + " Version  " + WH + BD + "ZerDisguise " + YW + "v" + v + R + DG + "                             ║");
        log(DG  + "  ║  " + GR + " Autor    " + CY + BD + "✦ " + AUTHOR + R + DG + "                                    ║");
        log(DG  + "  ║  " + GR + " Estado   " + GN + "✔ Plugin cargado correctamente" + DG + "          ║");
        log(DG  + "  ║  " + GR + " MC       " + WH + "Paper / Spigot  1.20 - 1.21+" + DG + "           ║");
        log(DG  + "  ║  " + GR + " Comandos " + P2 + "/disguise  /undisguise  /zd" + DG + "           ║");
        log(P1  + "  ╚══════════════════════════════════════════════════════════╝");
        log("");
    }

    private void log(String msg) { getLogger().info(msg + R); }

    public static ZerDisguise getInstance()       { return instance; }
    public ConfigManager    getConfigManager()    { return configManager; }
    public MenuConfig       getMenuConfig()       { return menuConfig; }
    public RankProvider     getRankProvider()     { return rankProvider; }
    public DisguiseManager  getDisguiseManager()  { return disguiseManager; }
    public SkinFetcher      getSkinFetcher()      { return skinFetcher; }
    public SkinApplier      getSkinApplier()      { return skinApplier; }
    public ChatListener     getChatListener()     { return chatListener; }
    public ProtocolLibHook  getProtocolLibHook()  { return protocolLibHook; }
}
