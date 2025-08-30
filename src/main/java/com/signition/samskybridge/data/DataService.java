package com.signition.samskybridge.data;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import com.signition.samskybridge.Main;

public class DataService {

    private final Main plugin;
    private final File file;
    private FileConfiguration yml;

    private final Map<UUID, Long> xp = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> level = new ConcurrentHashMap<>();
    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private BukkitTask autosaveTask;

    public DataService(Main plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "serverdata.yml");
        this.yml = YamlConfiguration.loadConfiguration(file);
        // Load into memory
        if (yml.isConfigurationSection("islands")) {
            for (String k : yml.getConfigurationSection("islands").getKeys(false)) {
                try {
                    UUID id = UUID.fromString(k);
                    xp.put(id, yml.getLong("islands."+k+".xp", 0L));
                    level.put(id, yml.getInt("islands."+k+".level", 0));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        // Schedule autosave
        int periodSec = plugin.configs().storage.getInt("storage.autosave-seconds", 60);
        if (periodSec < 10) periodSec = 10;
        autosaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::saveIfDirty, periodSec*20L, periodSec*20L);
    }

    public void shutdown() {
        if (autosaveTask != null) autosaveTask.cancel();
        saveNow();
        if (plugin.configs().storage.getBoolean("storage.backup-on-shutdown", true)) backup();
    }

    private void backup() {
        try {
            if (!file.exists()) return;
            String ts = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
            File dest = new File(plugin.getDataFolder(), "serverdata-" + ts + ".bak.yml");
            org.bukkit.util.FileUtil.copy(file, dest);
        } catch (Throwable ignored) {}
    }

    private synchronized void saveIfDirty() {
        if (dirty.compareAndSet(true, false)) saveNow();
    }

    public synchronized void saveNow() {
        for (Map.Entry<UUID, Long> e : xp.entrySet()) {
            String k = e.getKey().toString();
            yml.set("islands."+k+".xp", e.getValue());
        }
        for (Map.Entry<UUID, Integer> e : level.entrySet()) {
            String k = e.getKey().toString();
            yml.set("islands."+k+".level", e.getValue());
        }
        try { yml.save(file); } catch (IOException e) {
            plugin.getLogger().warning("[SSB] Failed to save serverdata.yml: " + e.getMessage());
        }
    }

    public long getXP(UUID islandId) { return xp.getOrDefault(islandId, 0L); }
    public int getLevel(UUID islandId) { return level.getOrDefault(islandId, 0); }

    public void setXP(UUID islandId, long v) { xp.put(islandId, v); dirty.set(true); }
    public void setLevel(UUID islandId, int v) { level.put(islandId, v); dirty.set(true); }
}
