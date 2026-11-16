package com.enchantsmp.effects;

import com.enchantsmp.EnchantSmp;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

/** Blast Protection on a thrown trident explodes where the trident lands. */
public final class ProjectileEffects implements Listener {

    private static final String EXPLODED_TAG = "enchantsmp_exploded";

    private final EnchantSmp plugin;

    public ProjectileEffects(EnchantSmp plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Trident trident)) {
            return;
        }
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("effects.blast_protection.enabled", true)
                || !cfg.getBoolean("effects.blast_protection.trident-explosion.enabled", true)) {
            return;
        }
        if (trident.getScoreboardTags().contains(EXPLODED_TAG)) {
            return; // one explosion per throw
        }

        int level = plugin.enchants().levelOn(trident.getItemStack(), "blast_protection");
        if (level <= 0) {
            return;
        }
        trident.addScoreboardTag(EXPLODED_TAG);

        String base = "effects.blast_protection.trident-explosion.";
        float power = (float) (cfg.getDouble(base + "base-power", 1.0)
                + cfg.getDouble(base + "power-per-level", 1.0) * level);
        boolean breakBlocks = cfg.getBoolean(base + "break-blocks", false);
        boolean fire = cfg.getBoolean(base + "set-fire", false);

        Location where = trident.getLocation();
        Entity source = trident.getShooter() instanceof Entity shooter ? shooter : trident;
        where.getWorld().createExplosion(where, power, fire, breakBlocks, source);
    }
}
