package com.enchantsmp.effects;

import com.enchantsmp.EnchantManager;
import com.enchantsmp.EnchantSmp;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityAirChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Armor-style enchantments that work from the MAIN HAND:
 * Protection, Fire/Blast/Projectile Protection, Feather Falling, Thorns, Respiration.
 *
 * Protection is damage REDUCTION (like armor). It never adds hearts / max health.
 */
public final class DamageEffects implements Listener {

    private final EnchantSmp plugin;
    private boolean reflecting = false;

    public DamageEffects(EnchantSmp plugin) {
        this.plugin = plugin;
    }

    private boolean enabled(String key) {
        return plugin.getConfig().getBoolean("effects." + key + ".enabled", true);
    }

    private int level(ItemStack item, String key) {
        return enabled(key) ? plugin.enchants().levelOn(item, key) : 0;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir() || hand.getEnchantments().isEmpty()) {
            return;
        }

        DamageCause cause = event.getCause();
        switch (cause) {
            case VOID, SUICIDE, KILL, STARVATION, WORLD_BORDER -> {
                return; // protection never reduces these (same as vanilla)
            }
            default -> { }
        }

        // "Enchantment Protection Factor" - same values as vanilla armor.
        double epf = level(hand, "protection");

        if (cause == DamageCause.FIRE || cause == DamageCause.FIRE_TICK
                || cause == DamageCause.LAVA || cause == DamageCause.HOT_FLOOR) {
            epf += 2.0 * level(hand, "fire_protection");
        }
        if (cause == DamageCause.ENTITY_EXPLOSION || cause == DamageCause.BLOCK_EXPLOSION) {
            epf += 2.0 * level(hand, "blast_protection");
        }
        if (cause == DamageCause.PROJECTILE) {
            epf += 2.0 * level(hand, "projectile_protection");
        }
        if (cause == DamageCause.FALL) {
            epf += 3.0 * level(hand, "feather_falling");
        }

        if (epf <= 0) {
            return;
        }
        epf = Math.min(20.0, epf);
        event.setDamage(event.getDamage() * (1.0 - epf * 0.04));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHitByMob(EntityDamageByEntityEvent event) {
        if (reflecting || !(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!(event.getDamager() instanceof LivingEntity attacker)) {
            return;
        }
        int level = level(player.getInventory().getItemInMainHand(), "thorns");
        if (level <= 0) {
            return;
        }
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        if (rng.nextDouble() < 0.15 * level) {
            reflecting = true;
            try {
                attacker.damage(1 + rng.nextInt(4), player);
            } finally {
                reflecting = false;
            }
        }
    }

    /** Respiration: sometimes the air bubble doesn't drain, like vanilla. */
    @EventHandler(ignoreCancelled = true)
    public void onAirChange(EntityAirChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getAmount() >= player.getRemainingAir()) {
            return; // not draining
        }
        int level = level(player.getInventory().getItemInMainHand(), "respiration");
        if (level <= 0) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() < level / (level + 1.0)) {
            event.setCancelled(true);
        }
    }
}
