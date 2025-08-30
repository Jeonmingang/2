package com.signition.samskybridge.cfg;

import java.io.File;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import com.signition.samskybridge.Main;

public class Configs {
    private final Main plugin;
    public FileConfiguration customization, rank, reloadable, serverdata, settings, upgrade, blocks, levels, gui, messages, storage;

    public Configs(Main plugin) { this.plugin = plugin; }

    public void loadAll() {
        customization = load("customization.yml");
        rank = load("rank.yml");
        reloadable = load("reloadable.yml");
        blocks = load("blocks.yml");
        levels = load("levels.yml");
        serverdata = load("serverdata.yml");
        settings = load("settings.yml");
        upgrade = load("upgrade.yml");
        gui = load("gui.yml");
        messages = load("messages.yml");
        storage = load("storage.yml");
    }

    private FileConfiguration load(String name) {
        File f = new File(plugin.getDataFolder(), name);
        if (!f.exists()) plugin.saveResource(name, false);
        return YamlConfiguration.loadConfiguration(f);
    }
}
