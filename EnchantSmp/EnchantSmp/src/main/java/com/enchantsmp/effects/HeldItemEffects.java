package com.enchantsmp.effects;

import com.enchantsmp.EnchantSmp;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Effects that apply "while holding" the item, done with temporary attribute modifiers:
 *  - Efficiency (non-mining items): faster attack speed
 *  - Depth Strider, Swift Sneak, Aqua Affinity
 *  - Frost Walker (freezes water under you)
 *
 * The modifiers are removed when the item is put away, on quit and on shutdown.
 */
public final class HeldItemEffects implements Listener {

    private record Mod(double amount, AttributeModifier.Operation operation) { }

    private static final List<String> MANAGED = List.of(
            "attack_speed", "water_movement_efficiency", "sneaking_speed", "submerged_mining_speed");

    private final EnchantSmp plugin;
    private final Map<String, NamespacedKey> modifierKeys = new HashMap<>();
    private final Map<String, Attribute> attributes = new HashMap<>();
    private BukkitTask task;

    public HeldItemEffects(EnchantSmp plugin) {
        this.plugin = plugin;
        for (String name : MANAGED) {
            modifierKeys.put(name, new NamespacedKey(plugin, "held_" + name));
        }
    }

    public void start() {
        long period = Math.max(1L, plugin.getConfig().getLong("effects.update-ticks", 4));
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                update(player);
            }
        }, 20L, period);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            clear(player);
        }
    }

    private Attribute attribute(String key) {
        Attribute cached = attributes.get(key);
        if (cached != null) {
            return cached;
        }
        Attribute found = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.ATTRIBUTE).get(NamespacedKey.minecraft(key));
        if (found != null) {
            attributes.put(key, found);
        }
        return found;
    }

    private static boolean isMiningTool(Material type) {
        String n = type.name();
        return n.endsWith("_PICKAXE") || n.endsWith("_AXE") || n.endsWith("_SHOVEL")
                || n.endsWith("_HOE") || type == Material.SHEARS;
    }

    private boolean enabled(String path) {
        return plugin.getConfig().getBoolean("effects." + path + ".enabled", true);
    }

    private void update(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        Map<String, Mod> wanted = new HashMap<>();

        if (!hand.getType().isAir() && !hand.getEnchantments().isEmpty()) {
            FileConfiguration cfg = plugin.getConfig();
            var enchants = plugin.enchants();

            int efficiency = enchants.levelOn(hand, "efficiency");
            if (efficiency > 0 && enabled("efficiency.attack-speed") && !isMiningTool(hand.getType())) {
                double perLevel = cfg.getDouble("effects.efficiency.attack-speed.bonus-per-level", 0.10);
                wanted.put("attack_speed", new Mod(efficiency * perLevel, AttributeModifier.Operation.ADD_SCALAR));
            }
            int depth = enchants.levelOn(hand, "depth_strider");
            if (depth > 0 && enabled("depth_strider")) {
                // vanilla: each level adds 1/3 of full water speed (max 3 levels useful)
                wanted.put("water_movement_efficiency",
                        new Mod(Math.min(depth, 3) / 3.0, AttributeModifier.Operation.ADD_VALUE));
            }
            int sneak = enchants.levelOn(hand, "swift_sneak");
            if (sneak > 0 && enabled("swift_sneak")) {
                wanted.put("sneaking_speed", new Mod(0.15 * sneak, AttributeModifier.Operation.ADD_VALUE));
            }
            int aqua = enchants.levelOn(hand, "aqua_affinity");
            if (aqua > 0 && enabled("aqua_affinity")) {
                // underwater mining penalty normally multiplies speed by 0.2; +0.8 removes it
                wanted.put("submerged_mining_speed", new Mod(0.8, AttributeModifier.Operation.ADD_VALUE));
            }

            int frost = enchants.levelOn(hand, "frost_walker");
            if (frost > 0 && enabled("frost_walker")) {
                freezeWater(player, frost);
            }
        }

        for (String name : MANAGED) {
            apply(player, name, wanted.get(name));
        }
    }

    private void apply(Player player, String name, Mod mod) {
        Attribute attribute = attribute(name);
        if (attribute == null) {
            return;
        }
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        NamespacedKey key = modifierKeys.get(name);
        AttributeModifier existing = null;
        for (AttributeModifier m : instance.getModifiers()) {
            if (key.equals(m.getKey())) {
                existing = m;
                break;
            }
        }
        if (mod == null) {
            if (existing != null) {
                instance.removeModifier(existing);
            }
            return;
        }
        if (existing != null) {
            if (Math.abs(existing.getAmount() - mod.amount()) < 1.0E-9
                    && existing.getOperation() == mod.operation()) {
                return; // already correct
            }
            instance.removeModifier(existing);
        }
        instance.addModifier(new AttributeModifier(key, mod.amount(), mod.operation()));
    }

    private void clear(Player player) {
        for (String name : MANAGED) {
            apply(player, name, null);
        }
    }

    private void freezeWater(Player player, int level) {
        if (player.isFlying() || player.isSwimming()) {
            return;
        }
        int max = plugin.getConfig().getInt("effects.frost_walker.max-radius", 16);
        int radius = Math.min(max, 2 + level);
        Block feet = player.getLocation().getBlock().getRelative(0, -1, 0);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                Block b = feet.getRelative(dx, 0, dz);
                if (b.getType() == Material.WATER
                        && b.getBlockData() instanceof Levelled water && water.getLevel() == 0
                        && b.getRelative(0, 1, 0).getType().isAir()) {
                    b.setType(Material.FROSTED_ICE);
                }
            }
        }
    }

    // Update right away when the player changes what they're holding.
    @EventHandler
    public void onHeldChange(PlayerItemHeldEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> update(event.getPlayer()));
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> update(event.getPlayer()));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        clear(event.getPlayer()); // removes leftovers from a crash
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clear(event.getPlayer());
    }
}
