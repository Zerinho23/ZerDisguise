package me.zerith.zerdisguise;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class DisguiseCommand implements CommandExecutor, TabCompleter {

    private final ZerDisguise plugin;

    public DisguiseCommand(ZerDisguise plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ConfigManager cfg    = plugin.getConfigManager();
        Component     prefix = cfg.getPrefix();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix.append(cfg.component(cfg.getMsgPlayerOnly())));
            return true;
        }

        // /undisguise o /disguise remove — quitar disfraz activo
        boolean isUndisguise = command.getName().equalsIgnoreCase("undisguise");
        boolean isRemove     = args.length == 1 && args[0].equalsIgnoreCase("remove");

        if (isUndisguise || isRemove) {
            if (!player.hasPermission("zerdisguise.use")) {
                player.sendMessage(prefix.append(cfg.component(cfg.getMsgNoPermission())));
                return true;
            }
            plugin.getDisguiseManager().removeDisguise(player);
            return true;
        }

        // /disguise reload
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("zerdisguise.reload")) {
                player.sendMessage(prefix.append(cfg.component(cfg.getMsgNoPermission())));
                return true;
            }
            plugin.reload();
            player.sendMessage(prefix.append(cfg.component(cfg.getMsgReload())));
            return true;
        }

        // /disguise debug — diagnóstico del TAB hook (requiere zerdisguise.reload)
        if (args.length == 1 && args[0].equalsIgnoreCase("debug")) {
            if (!player.hasPermission("zerdisguise.reload")) {
                player.sendMessage(prefix.append(cfg.component(cfg.getMsgNoPermission())));
                return true;
            }
            TabHook.diagnose(player);
            DisguiseManager dm  = plugin.getDisguiseManager();
            DisguiseManager.DisguiseData cur = dm.getCurrent(player.getUniqueId());
            player.sendMessage("§8[§bZD§8] §7Disfraz activo: "
                    + (cur != null ? "§d" + cur.disguiseName() + " §7(" + cur.rankId() + ")" : "§cNinguno"));
            if (cur != null && TabHook.isAvailable()) {
                player.sendMessage("§8[§bZD§8] §7Forzando nombre en TAB ahora...");
                TabHook.setTabName(player.getUniqueId(), cur.disguiseName());
                player.sendMessage("§8[§bZD§8] §aHecho. Revisa el tab list.");
            }
            return true;
        }

        // Permiso base para el resto de acciones
        if (!player.hasPermission("zerdisguise.use")) {
            player.sendMessage(prefix.append(cfg.component(cfg.getMsgNoPermission())));
            return true;
        }

        // /disguise <jugador> — abrir menú del objetivo (requiere zerdisguise.others)
        if (args.length == 1) {
            if (!player.hasPermission("zerdisguise.others")) {
                player.sendMessage(prefix.append(cfg.component(cfg.getMsgNoPermission())));
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                player.sendMessage(prefix.append(cfg.component(
                        cfg.getMsgNotFound().replace("{player}", args[0]))));
                return true;
            }
            // Respetar el permiso bypass — el target no puede ser forzado
            if (target.hasPermission("zerdisguise.bypass") && !player.isOp()) {
                player.sendMessage(prefix.append(cfg.component(cfg.getMsgBypass())));
                return true;
            }
            target.openInventory(new MenuBuilder(plugin).buildMainMenu(target));
            player.sendMessage(prefix.append(cfg.component(
                    "&7Abriste el menú de disfraz para &d" + target.getName() + "&7.")));
            return true;
        }

        // /disguise — abre el menú del jugador
        player.openInventory(new MenuBuilder(plugin).buildMainMenu(player));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (!(sender instanceof Player player)) return result;
        if (args.length != 1) return result;

        // /undisguise no tiene subcomandos
        if (command.getName().equalsIgnoreCase("undisguise")) return result;

        List<String> options = new ArrayList<>();
        if (player.hasPermission("zerdisguise.use"))    options.add("remove");
        if (player.hasPermission("zerdisguise.reload")) { options.add("reload"); options.add("debug"); }
        if (player.hasPermission("zerdisguise.others")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                // No mostrar jugadores con bypass a quienes no son OP
                if (!p.hasPermission("zerdisguise.bypass") || player.isOp()) {
                    options.add(p.getName());
                }
            }
        }

        String typed = args[0].toLowerCase();
        for (String o : options) {
            if (o.toLowerCase().startsWith(typed)) result.add(o);
        }
        return result;
    }
}
