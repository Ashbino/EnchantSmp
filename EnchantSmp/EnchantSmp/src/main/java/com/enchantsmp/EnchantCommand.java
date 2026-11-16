package com.enchantsmp;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class EnchantCommand implements CommandExecutor {
    private final EnchantSmp plugin;

    public EnchantCommand(EnchantSmp plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("enchantsmp.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§d§lEnchantSmp §7Commands:");
            sender.sendMessage("§f/enchant start §7- Start automatic rolls");
            sender.sendMessage("§f/enchant stop §7- Stop automatic rolls");
            sender.sendMessage("§f/enchant roll [player] §7- Roll for a player");
            sender.sendMessage("§f/enchant rollall §7- Roll for everyone");
            sender.sendMessage("§f/enchant reload §7- Reload config");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> {
                if (plugin.isRollingEnabled()) {
                    sender.sendMessage("§eAutomatic enchantment rolling is already running.");
                } else {
                    plugin.startRolling();
                    sender.sendMessage("§aAutomatic enchantment rolling started. Interval: "
                            + plugin.getConfig().getLong("roll-interval-minutes", 10L) + " minutes.");
                }
            }
            case "stop" -> {
                plugin.stopRolling();
                sender.sendMessage("§cAutomatic enchantment rolling stopped.");
            }
            case "roll" -> {
                Player target;
                if (args.length >= 2) {
                    target = Bukkit.getPlayerExact(args[1]);
                    if (target == null) {
                        sender.sendMessage("§cThat player is not online.");
                        return true;
                    }
                } else if (sender instanceof Player player) {
                    target = player;
                } else {
                    sender.sendMessage("§cConsole usage: /enchant roll <player>");
                    return true;
                }

                if (!plugin.rollForPlayer(target)) {
                    sender.sendMessage("§cCould not roll an enchantment for " + target.getName() + ".");
                } else {
                    sender.sendMessage("§aRolled an enchantment for " + target.getName() + ".");
                }
            }
            case "rollall" -> {
                plugin.rollAllPlayers();
                sender.sendMessage("§aRolled an enchantment independently for every online player.");
            }
            case "reload" -> {
                plugin.reloadPluginConfig();
                sender.sendMessage("§aEnchantSmp configuration reloaded.");
            }
            default -> sender.sendMessage("§cUnknown subcommand. Use /enchant for help.");
        }
        return true;
    }
}
