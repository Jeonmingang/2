package com.signition.samskybridge.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.signition.samskybridge.upgrade.UpgradeManager;
import com.signition.samskybridge.visual.BorderService;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.events.island.IslandCreatedEvent;
import world.bentobox.bentobox.api.events.island.IslandEnterEvent;
import world.bentobox.bentobox.api.events.island.IslandLeaveEvent;
import world.bentobox.bentobox.api.events.island.IslandLevelEvent;
import world.bentobox.bentobox.api.island.Island;
import world.bentobox.bentobox.api.user.User;

public class BentoHook implements Listener {

    private final BentoBox api;
    private final UpgradeManager upgrades;
    private final BorderService border;

    public BentoHook(com.signition.samskybridge.Main plugin, UpgradeManager upgrades, BorderService border) {
        this.api = BentoBox.getInstance();
        this.upgrades = upgrades;
        this.border = border;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreate(IslandCreatedEvent e) {
        upgrades.applyDefaultUpgrades(e.getIsland());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnter(IslandEnterEvent e) {
        Island island = e.getIsland();
        int radius = upgrades.currentRadiusFor(island);
        User u = e.getUser();
        Player p = Bukkit.getPlayer(u.getUniqueId());
        if (p != null) border.show(p, island.getCenter(), radius);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLeave(IslandLeaveEvent e) {
        User u = e.getUser();
        Player p = Bukkit.getPlayer(u.getUniqueId());
        if (p != null) border.clear(p);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLevel(IslandLevelEvent e) {
        upgrades.onIslandLevel(e.getIsland(), e.getNewLevel());
    }
}
