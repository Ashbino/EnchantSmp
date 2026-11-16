package com.enchantsmp.effects;

import com.enchantsmp.EnchantSmp;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Riptide (right-click: launch forward, no water needed, non-trident items only)
 * Lunge (left-click in the air: dash forward).
 */
public final class MovementEffects implements Listener {

    private final EnchantSmp plugin;
    private final Map<UUID, Long> riptideReady = new HashMap<>();
    private final Map<UUID, Long> lungeReady = new HashMap<>();

    public MovementEffects(EnchantSmp plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.useItemInHand() == Event.Result.DENY) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir() || hand.getEnchantments().isEmpty()) {
            return;
        }
        Action action = event.getAction();

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            // Don't hijack clicks on chests, doors, etc.
            if (action == Action.RIGHT_CLICK_BLOCK && !player.isSneaking()
                    && event.getClickedBlock() != null && event.getClickedBlock().getType().isInteractable()) {
                return;
            }
            tryRiptide(player, hand);
        } else if (action == Action.LEFT_CLICK_AIR) {
            tryLunge(player, hand);
        }
    }

    private boolean ready(Map<UUID, Long> map, Player player, long cooldownTicks) {
        long now = System.currentTimeMillis();
        Long next = map.get(player.getUniqueId());
        if (next != null && now < next) {
            return false;
        }
        map.put(player.getUniqueId(), now + cooldownTicks * 50L);
        return true;
    }

    private void tryRiptide(Player player, ItemStack hand) {
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("effects.riptide.enabled", true)) {
            return;
        }
        if (hand.getType() == Material.TRIDENT) {
            return; // real tridents keep their vanilla (water / rain) behaviour
        }
        int level = plugin.enchants().levelOn(hand, "riptide");
        if (level <= 0) {
            return;
        }
        if (!ready(riptideReady, player, cfg.getLong("effects.riptide.cooldown-ticks", 40))) {
            return;
        }

        double speed = 3.0 * (1 + level) / 4.0 * cfg.getDouble("effects.riptide.speed-multiplier", 1.0);
        Vector velocity = player.getLocation().getDirection().normalize().multiply(speed);
        if (player.isOnGround()) {
            velocity.setY(Math.max(velocity.getY(), 0.4)); // unstick from the ground like vanilla
        }
        player.setVelocity(velocity);
        player.setFallDistance(0f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 1.0f, 1.0f);
    }

    private void tryLunge(Player player, ItemStack hand) {
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("effects.lunge.enabled", true)) {
            return;
        }
        int level = plugin.enchants().levelOn(hand, "lunge");
        if (level <= 0) {
            return;
        }
        if (!ready(lungeReady, player, cfg.getLong("effects.lunge.cooldown-ticks", 15))) {
            return;
        }

        double speed = cfg.getDouble("effects.lunge.base-speed", 0.6)
                + cfg.getDouble("effects.lunge.speed-per-level", 0.45) * level;
        Vector dir = player.getLocation().getDirection();
        dir.setY(0);
        if (dir.lengthSquared() < 1.0E-6) {
            return;
        }
        Vector velocity = dir.normalize().multiply(speed);
        velocity.setY(0.1);
        player.setVelocity(velocity);

        int hunger = cfg.getInt("effects.lunge.hunger-per-level", 0) * level;
        if (hunger > 0) {
            player.setFoodLevel(Math.max(0, player.getFoodLevel() - hunger));
        }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        riptideReady.remove(event.getPlayer().getUniqueId());
        lungeReady.remove(event.getPlayer().getUniqueId());
    }
}
