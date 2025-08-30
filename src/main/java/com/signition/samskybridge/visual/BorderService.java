package com.signition.samskybridge.visual;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.signition.samskybridge.Main;
import com.signition.samskybridge.cfg.Configs;

public class BorderService {

    private final Main plugin;
    private final Configs cfg;
    private final Map<UUID, BukkitTask> tasks = new ConcurrentHashMap<>();

    private boolean enabled;
    private double step;
    private long periodTicks;

    public BorderService(Main plugin, Configs cfg) {
        this.plugin = plugin;
        this.cfg = cfg;
        reloadSettings();
    }

    public void reloadSettings() {
        this.enabled = cfg.settings.getBoolean("visual.border.enabled", true);
        this.step = cfg.settings.getDouble("visual.border.step", 1.5);
        this.periodTicks = cfg.settings.getLong("visual.border.period-ticks", 10);
    }

    public void show(Player p, Location center, int radius) {
        if (!enabled || p == null || center == null) return;
        clear(p);

        World w = center.getWorld();
        if (w == null || p.getWorld() != w) return;

        // Square perimeter corners
        final int minX = center.getBlockX() - radius;
        final int maxX = center.getBlockX() + radius;
        final int minZ = center.getBlockZ() - radius;
        final int maxZ = center.getBlockZ() + radius;
        final double y = p.getLocation().getY() + 0.1; // slightly above feet

        // White dust particle
        final Particle.DustOptions WHITE = new Particle.DustOptions(Color.fromRGB(255,255,255), 1.2f);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!p.isOnline() || p.getWorld() != w) {
                clear(p);
                return;
            }
            // four edges
            drawLine(w, new Location(w, minX, y, minZ), new Location(w, maxX, y, minZ), p, WHITE);
            drawLine(w, new Location(w, minX, y, maxZ), new Location(w, maxX, y, maxZ), p, WHITE);
            drawLine(w, new Location(w, minX, y, minZ), new Location(w, minX, y, maxZ), p, WHITE);
            drawLine(w, new Location(w, maxX, y, minZ), new Location(w, maxX, y, maxZ), p, WHITE);
        }, 1L, periodTicks);

        tasks.put(p.getUniqueId(), task);
    }

    public void clear(Player p) {
        if (p == null) return;
        BukkitTask t = tasks.remove(p.getUniqueId());
        if (t != null) t.cancel();
    }

    public void clearAll() {
        for (BukkitTask t : tasks.values()) {
            if (t != null) t.cancel();
        }
        tasks.clear();
    }

    private void drawLine(World w, Location a, Location b, Player viewer, Particle.DustOptions white) {
        Vector dir = b.toVector().subtract(a.toVector());
        double len = dir.length();
        if (len <= 0.01) return;
        dir.normalize(); dir.multiply(step);
        double traveled = 0;
        Location cur = a.clone();
        while (traveled < len) {
            viewer.spawnParticle(Particle.REDSTONE, cur, 1, 0,0,0, 0, white, true);
            cur.add(dir);
            traveled += step;
        }
    }
}
