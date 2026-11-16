package com.enchantsmp;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /enchant start | stop | roll [player] | rollall | reload */
public final class EnchantCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = List.of("start", "stop", "roll", "rollall", "reload");

    private final EnchantSmp plugin;

    public EnchantCommand(EnchantSmp plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages msg = plugin.messages();
        RollService rolls = plugin.rolls();

        if (args.length == 0) {
            msg.send(sender, "usage");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "start" -> {
                if (!allowed(sender, "enchantsmp.start")) return true;
                if (rolls.start()) {
                    double minutes = plugin.getConfig().getDouble("rolling.interval-minutes", 10);
                    msg.send(sender, "started", Messages.text("minutes", String.valueOf(minutes)));
                } else {
                    msg.send(sender, "already-started");
                }
            }
            case "stop" -> {
                if (!allowed(sender, "enchantsmp.stop")) return true;
                msg.send(sender, rolls.stop() ? "stopped" : "already-stopped");
            }
            case "roll" -> {
                Player target;
                if (args.length >= 2) {
                    if (!allowed(sender, "enchantsmp.roll.others")) return true;
                    target = Bukkit.getPlayerExact(args[1]);
                    if (target == null) {
                        msg.send(sender, "player-not-found", Messages.text("player", args[1]));
                        return true;
                    }
                } else {
                    if (!(sender instanceof Player self)) {
                        msg.send(sender, "console-needs-player");
                        return true;
                    }
                    if (!allowed(sender, "enchantsmp.roll")) return true;
                    target = self;
                }

                RollService.RollResult result = rolls.roll(target);
                rolls.announce(target, result, false);
                if (!target.equals(sender)) {
                    if (result.status() == RollService.Status.SUCCESS) {
                        msg.send(sender, "roll-sent",
                                Messages.text("player", target.getName()),
                                Messages.component("enchant", result.name()));
                    } else {
                        msg.send(sender, "roll-sent-failed", Messages.text("player", target.getName()));
                    }
                }
            }
            case "rollall" -> {
                if (!allowed(sender, "enchantsmp.rollall")) return true;
                int count = rolls.rollAll(false);
                msg.send(sender, "rollall-done", Messages.text("count", String.valueOf(count)));
            }
            case "reload" -> {
                if (!allowed(sender, "enchantsmp.reload")) return true;
                plugin.reloadEverything();
                msg.send(sender, "reloaded");
            }
            default -> msg.send(sender, "usage");
        }
        return true;
    }

    private boolean allowed(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        plugin.messages().send(sender, "no-permission");
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String typed = args[0].toLowerCase(Locale.ROOT);
            for (String sub : SUBS) {
                if (sub.startsWith(typed) && sender.hasPermission("enchantsmp." + sub)) {
                    out.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("roll")
                && sender.hasPermission("enchantsmp.roll.others")) {
            String typed = args[1].toLowerCase(Locale.ROOT);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(typed)) {
                    out.add(p.getName());
                }
            }
        }
        return out;
    }
}
