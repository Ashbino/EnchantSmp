package com.enchantsmp;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class EnchantSmp extends JavaPlugin {
    private final Random random = new Random();
    private BukkitTask rollingTask;
    private boolean rollingEnabled;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (getCommand("enchant") != null) {
            getCommand("enchant").setExecutor(new EnchantCommand(this));
        }
        getLogger().info("EnchantSmp enabled.");
    }

    @Override
    public void onDisable() {
        stopRolling();
    }

    public boolean isRollingEnabled() {
        return rollingEnabled;
    }

    public void startRolling() {
        if (rollingEnabled) return;

        rollingEnabled = true;
        long intervalMinutes = Math.max(1L, getConfig().getLong("roll-interval-minutes", 10L));
        long intervalTicks = intervalMinutes * 60L * 20L;

        rollingTask = Bukkit.getScheduler().runTaskTimer(
                this,
                () -> Bukkit.getOnlinePlayers().forEach(this::rollForPlayer),
                intervalTicks,
                intervalTicks
        );
    }

    public void stopRolling() {
        rollingEnabled = false;
        if (rollingTask != null) {
            rollingTask.cancel();
            rollingTask = null;
        }
    }

    public boolean rollForPlayer(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            return false;
        }

        int maxLevel = Math.max(1, getConfig().getInt("maximum-enchantment-level", 5));
        Map<Enchantment, Integer> current = item.getEnchantments();
        List<Enchantment> eligible = new ArrayList<>();

        for (Enchantment enchantment : Enchantment.values()) {
            int currentLevel = current.getOrDefault(enchantment, 0);
            if (currentLevel < maxLevel) {
                eligible.add(enchantment);
            }
        }

        if (eligible.isEmpty()) {
            player.sendMessage("§cThis item has no eligible enchantment upgrades left.");
            return false;
        }

        Enchantment chosen = eligible.get(random.nextInt(eligible.size()));
        int oldLevel = current.getOrDefault(chosen, 0);
        int newLevel = oldLevel == 0
                ? 1 + random.nextInt(maxLevel)
                : oldLevel + 1 + random.nextInt(maxLevel - oldLevel);

        // Unsafe application allows enchantments on normally incompatible items.
        // Existing enchantments are preserved; the selected enchantment only
        // changes when the newly rolled level is strictly higher.
        item.addUnsafeEnchantment(chosen, newLevel);
        player.getInventory().setItemInMainHand(item);

        String key = chosen.getKey().getKey();
        player.sendMessage("§d§lENCHANT! §fYour held item received §b"
                + key + " " + roman(newLevel) + "§f!");
        return true;
    }

    public void rollAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            rollForPlayer(player);
        }
    }

    public void reloadPluginConfig() {
        reloadConfig();
    }

    private String roman(int number) {
        String[] numerals = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        if (number >= 1 && number < numerals.length) return numerals[number];
        return Integer.toString(number);
    }
}
