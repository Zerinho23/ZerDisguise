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

        // Permiso base
        if (!player.hasPermission("zerdisguise.use")) {
            player.sendMessage(prefix.append(cfg.component(cfg.getMsgNoPermission())));
            return true;
        }

        // /disguise remove
        if (args.length == 1 && args[0].equalsIgnoreCase("remove")) {
            plugin.getDisguiseManager().removeDisguise(player);
            return true;
        }

        // /disguise <jugador>
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
            target.openInventory(new MenuBuilder(plugin).buildMainMenu(target));
            player.sendMessage(prefix.append(cfg.component(
                    "&7Abriste el menu de disfraz para &d" + target.getName())));
            return true;
        }

        // /disguise — abre el menu
        player.openInventory(new MenuBuilder(plugin).buildMainMenu(player));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (!(sender instanceof Player player)) return result;
        if (args.length != 1) return result;

        List<String> options = new ArrayList<>();
        if (player.hasPermission("zerdisguise.use"))   options.add("remove");
        if (player.hasPermission("zerdisguise.reload")) options.add("reload");
        if (player.hasPermission("zerdisguise.others")) {
            for (Player p : Bukkit.getOnlinePlayers()) options.add(p.getName());
        }

        String typed = args[0].toLowerCase();
        for (String o : options) {
            if (o.toLowerCase().startsWith(typed)) result.add(o);
        }
        return result;
    }
}
