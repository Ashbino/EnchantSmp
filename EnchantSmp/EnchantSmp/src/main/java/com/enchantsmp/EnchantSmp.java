package com.enchantsmp;

import com.enchantsmp.effects.DamageEffects;
import com.enchantsmp.effects.HeldItemEffects;
import com.enchantsmp.effects.MovementEffects;
import com.enchantsmp.effects.ProjectileEffects;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * EnchantSmp - any enchantment on any item, plus random enchantment rolling.
 */
public final class EnchantSmp extends JavaPlugin {

    private Messages messages;
    private EnchantManager enchantManager;
    private RollService rollService;
    private HeldItemEffects heldItemEffects;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.messages = new Messages(this);
        this.enchantManager = new EnchantManager(this);
        this.rollService = new RollService(this);
        this.heldItemEffects = new HeldItemEffects(this);

        PluginCommand command = getCommand("enchant");
        if (command == null) {
            getLogger().severe("Command 'enchant' is missing from plugin.yml - disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        EnchantCommand handler = new EnchantCommand(this);
        command.setExecutor(handler);
        command.setTabCompleter(handler);

        getServer().getPluginManager().registerEvents(heldItemEffects, this);
        getServer().getPluginManager().registerEvents(new DamageEffects(this), this);
        getServer().getPluginManager().registerEvents(new ProjectileEffects(this), this);
        getServer().getPluginManager().registerEvents(new MovementEffects(this), this);

        heldItemEffects.start();

        if (getConfig().getBoolean("rolling.start-on-enable", false)) {
            rollService.start();
        }
        getLogger().info("EnchantSmp enabled. " + enchantManager.all().size() + " enchantments known.");
    }

    @Override
    public void onDisable() {
        if (rollService != null) {
            rollService.stop();
        }
        if (heldItemEffects != null) {
            heldItemEffects.stop();
        }
    }

    /** Re-reads config.yml and restarts the timers. */
    public void reloadEverything() {
        reloadConfig();
        enchantManager.reload();
        heldItemEffects.stop();
        heldItemEffects.start();
        if (rollService.isRunning()) {
            rollService.stop();
            rollService.start();
        }
    }

    public Messages messages() {
        return messages;
    }

    public EnchantManager enchants() {
        return enchantManager;
    }

    public RollService rolls() {
        return rollService;
    }
}
