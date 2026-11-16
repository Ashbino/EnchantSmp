package com.enchantsmp;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Knows every enchantment, the configured maximum levels, and applies enchantments
 * with the strict "add or upgrade only" rules.
 */
public final class EnchantManager {

    private static final int HARD_CAP = 255;

    private final EnchantSmp plugin;
    private final Registry<Enchantment> registry;
    private final Map<String, Enchantment> cache = new HashMap<>();
    private final Set<String> excluded = new HashSet<>();
    private final Map<String, Integer> maxOverrides = new HashMap<>();

    public EnchantManager(EnchantSmp plugin) {
        this.plugin = plugin;
        this.registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
        reload();
    }

    public void reload() {
        excluded.clear();
        for (String s : plugin.getConfig().getStringList("enchantments.excluded")) {
            excluded.add(clean(s));
        }
        maxOverrides.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("enchantments.max-levels");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                int value = section.getInt(key, 0);
                if (value > 0) {
                    maxOverrides.put(clean(key), Math.min(value, HARD_CAP));
                }
            }
        }
    }

    private static String clean(String s) {
        String k = s.toLowerCase(Locale.ROOT).trim();
        return k.startsWith("minecraft:") ? k.substring("minecraft:".length()) : k;
    }

    /** Looks up an enchantment by its short key, e.g. "efficiency". Null if it doesn't exist. */
    public Enchantment byKey(String key) {
        Enchantment cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            Enchantment found = registry.get(NamespacedKey.minecraft(key));
            if (found != null) {
                cache.put(key, found);
            }
            return found;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public List<Enchantment> all() {
        List<Enchantment> list = new ArrayList<>();
        for (Enchantment e : registry) {
            list.add(e);
        }
        return list;
    }

    public static String keyOf(Enchantment e) {
        return e.getKey().getKey();
    }

    public boolean isExcluded(Enchantment e) {
        return excluded.contains(keyOf(e));
    }

    /** Configured maximum level (or vanilla maximum). */
    public int maxLevel(Enchantment e) {
        Integer override = maxOverrides.get(keyOf(e));
        int max = override != null ? override : e.getMaxLevel();
        return Math.min(max, HARD_CAP);
    }

    /** Level of the enchantment with the given short key on the item (0 if none). */
    public int levelOn(ItemStack item, String key) {
        if (item == null || item.getType().isAir()) {
            return 0;
        }
        Enchantment e = byKey(key);
        return e == null ? 0 : item.getEnchantmentLevel(e);
    }

    /** All enchantments that could still roll on this item (not excluded, below max). */
    public List<Enchantment> eligible(ItemStack item) {
        List<Enchantment> pool = new ArrayList<>();
        for (Enchantment e : registry) {
            if (isExcluded(e)) {
                continue;
            }
            int max = maxLevel(e);
            if (max < 1) {
                continue;
            }
            if (item.getEnchantmentLevel(e) < max) {
                pool.add(e);
            }
        }
        return pool;
    }

    /**
     * Adds the enchantment, or raises it. Never lowers anything, never touches any other
     * enchantment on the item. Returns true only if something actually changed.
     */
    public boolean applyUpgrade(ItemStack item, Enchantment e, int level) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        int current = item.getEnchantmentLevel(e);
        if (level <= current || level > maxLevel(e)) {
            return false;
        }
        item.addUnsafeEnchantment(e, level); // "unsafe" = allowed on any item / any level
        return true;
    }
}
