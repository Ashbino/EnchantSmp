package com.enchantsmp;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Random enchantment rolling, manual and automatic. */
public final class RollService {

    public enum Status { SUCCESS, NO_ITEM, NOTHING_ELIGIBLE }

    public record RollResult(Status status, Enchantment enchantment, int previousLevel, int newLevel) {
        static RollResult of(Status status) {
            return new RollResult(status, null, 0, 0);
        }

        public Component name() {
            return enchantment.displayName(newLevel);
        }
    }

    private final EnchantSmp plugin;
    private BukkitTask task;

    public RollService(EnchantSmp plugin) {
        this.plugin = plugin;
    }

    /** Rolls one enchantment for the item in the player's main hand. Each call is independent. */
    public RollResult roll(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            return RollResult.of(Status.NO_ITEM);
        }
        EnchantManager manager = plugin.enchants();
        List<Enchantment> pool = manager.eligible(hand);
        if (pool.isEmpty()) {
            return RollResult.of(Status.NOTHING_ELIGIBLE);
        }

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        Enchantment chosen = pool.get(rng.nextInt(pool.size()));
        int previous = hand.getEnchantmentLevel(chosen);
        int max = manager.maxLevel(chosen);
        // New level is anywhere above the current one, up to the maximum.
        int next = previous + 1 + rng.nextInt(max - previous);

        ItemStack updated = hand.clone();
        if (!manager.applyUpgrade(updated, chosen, next)) {
            return RollResult.of(Status.NOTHING_ELIGIBLE);
        }
        player.getInventory().setItemInMainHand(updated);
        return new RollResult(Status.SUCCESS, chosen, previous, next);
    }

    /** Tells the player what happened with their roll. */
    public void announce(Player player, RollResult result, boolean automatic) {
        Messages msg = plugin.messages();
        switch (result.status()) {
            case SUCCESS -> {
                String key = result.previousLevel() > 0 ? "rolled-upgrade" : "rolled";
                msg.send(player, key,
                        Messages.component("enchant", result.name()),
                        Messages.text("old", String.valueOf(result.previousLevel())));
                if (plugin.getConfig().getBoolean("rolling.broadcast", false)) {
                    Bukkit.getServer().sendMessage(msg.get("broadcast",
                            Messages.text("player", player.getName()),
                            Messages.component("enchant", result.name())));
                }
            }
            case NO_ITEM -> {
                if (!automatic || plugin.getConfig().getBoolean("rolling.notify-failures", true)) {
                    msg.send(player, "roll-no-item");
                }
            }
            case NOTHING_ELIGIBLE -> {
                if (!automatic || plugin.getConfig().getBoolean("rolling.notify-failures", true)) {
                    msg.send(player, "roll-nothing-eligible");
                }
            }
        }
    }

    /** Independently rolls for every online player. Returns how many players were rolled for. */
    public int rollAll(boolean automatic) {
        int count = 0;
        for (Player player : List.copyOf(Bukkit.getOnlinePlayers())) {
            RollResult result = roll(player);
            announce(player, result, automatic);
            count++;
        }
        return count;
    }

    public boolean isRunning() {
        return task != null;
    }

    public boolean start() {
        if (task != null) {
            return false;
        }
        double minutes = Math.max(0.05, plugin.getConfig().getDouble("rolling.interval-minutes", 10));
        long ticks = Math.max(20L, (long) (minutes * 60.0 * 20.0));
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> rollAll(true), ticks, ticks);
        return true;
    }

    public boolean stop() {
        if (task == null) {
            return false;
        }
        task.cancel();
        task = null;
        return true;
    }
}
