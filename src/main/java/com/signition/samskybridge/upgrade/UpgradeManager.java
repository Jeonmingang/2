package com.signition.samskybridge.upgrade;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

import com.signition.samskybridge.Main;
import com.signition.samskybridge.cfg.Configs;
import com.signition.samskybridge.visual.BorderService;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.island.Island;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.RanksManager;

public class UpgradeManager implements org.bukkit.event.Listener {

    private final Main plugin;
    private final Configs cfg;
    private final BorderService border;

    public UpgradeManager(Main plugin, Configs cfg, BorderService border) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.border = border;
    }

    // ======= ISLAND SIZE =======

    public void applyDefaultUpgrades(Island island) {
        int radius = cfg.upgrade.getInt("island-size.default", 100);
        setIslandRadius(island, radius);
    }

    public void onIslandLevel(Island island, long newLevel) {
        int best = currentRadiusFor(island); // start from current
        int def = cfg.upgrade.getInt("island-size.default", 100);
        if (best <= 0) best = def;

        ConfigurationSection tiers = cfg.upgrade.getConfigurationSection("island-size.tiers");
        if (tiers != null) {
            for (String key : tiers.getKeys(false)) {
                int req = tiers.getInt(key + ".required-level", 0);
                int r = tiers.getInt(key + ".radius", def);
                if (newLevel >= req && r > best) best = r;
            }
        }
        if (best > 0) setIslandRadius(island, best);
    }

    public int currentRadiusFor(Island island) {
        Integer val = invokeInt(island, "getProtectionRange");
        if (val == null) val = invokeInt(island, "getRange");
        if (val == null) val = invokeInt(island, "getIslandSize");
        if (val != null) return val.intValue();
        return cfg.upgrade.getInt("island-size.default", 100);
    }

    public int currentRadiusFor(Player p) {
        Island is = islandOf(p);
        return is != null ? currentRadiusFor(is) : cfg.upgrade.getInt("island-size.default", 100);
    }

    public int nextRadiusFor(Player p) {
        Island is = islandOf(p);
        if (is == null) return currentRadiusFor(p);
        long level = 999999; // TODO: replace with actual island level lookup if available
        int best = currentRadiusFor(is);
        int def = cfg.upgrade.getInt("island-size.default", 100);
        ConfigurationSection tiers = cfg.upgrade.getConfigurationSection("island-size.tiers");
        if (tiers != null) {
            for (String key : tiers.getKeys(false)) {
                int req = tiers.getInt(key + ".required-level", 0);
                int r = tiers.getInt(key + ".radius", def);
                if (level >= req && r > best) best = r;
            }
        }
        return best;
    }

    
    private int requiredLevelForNextRadius(Player p) {
        Island is = islandOf(p);
        if (is == null) return Integer.MAX_VALUE;
        int def = cfg.upgrade.getInt("island-size.default", 100);
        int current = currentRadiusFor(is);
        int best = current;
        int reqLevel = Integer.MAX_VALUE;
        ConfigurationSection tiers = cfg.upgrade.getConfigurationSection("island-size.tiers");
        if (tiers != null) {
            for (String key : tiers.getKeys(false)) {
                int req = tiers.getInt(key + ".required-level", 0);
                int r = tiers.getInt(key + ".radius", def);
                if (r > best) { best = r; reqLevel = req; }
            }
        }
        return reqLevel == Integer.MAX_VALUE ? 0 : reqLevel;
    }
    private double costForNextRadius(Player p) {
        Island is = islandOf(p);
        if (is == null) return 0.0;
        int def = cfg.upgrade.getInt("island-size.default", 100);
        int current = currentRadiusFor(is);
        int best = current;
        double cost = 0.0;
        ConfigurationSection tiers = cfg.upgrade.getConfigurationSection("island-size.tiers");
        if (tiers != null) {
            for (String key : tiers.getKeys(false)) {
                int r = tiers.getInt(key + ".radius", def);
                if (r > best) {
                    best = r;
                    cost = tiers.getDouble(key + ".cost", 0.0);
                }
            }
        }
        return cost;
    }

    public void tryUpgradeIslandSize(Player p) {
        Island is = islandOf(p);
        if (is == null) { p.sendMessage(color(Main.get().configs().messages.getString("messages.island-required"))); return; }
        int before = currentRadiusFor(is);
        int after = nextRadiusFor(p);
        if (after <= before) { p.sendMessage(color(Main.get().configs().messages.getString("messages.upgrade-none"))); return; }
        int needLv = requiredLevelForNextRadius(p);
        int curLv = Main.get().levels().levelOf(is);
        if (curLv < needLv) { p.sendMessage(color(Main.get().configs().messages.getString("messages.upgrade-need-level").replace("{level}", String.valueOf(needLv)))); return; }
        double cost = costForNextRadius(p);
        if (!payIfNeeded(p, cost)) return;
        setIslandRadius(is, after);
        p.sendMessage("§a섬 크기 업그레이드! 반지름: §e" + after + " §7(비용: " + cost + ")");
    }

    private void setIslandRadius(Island island, int radius) {
        // direct API
        try { island.setProtectionRange(radius); }
        catch (Throwable t) {
            try { island.setRange(radius); }
            catch (Throwable t2) { /* ignore */ }
        }
        // visuals for online members
        Location center = island.getCenter();
        for (User u : island.getMembers()) {
            Player p = Bukkit.getPlayer(u.getUniqueId());
            if (p != null) border.show(p, center, radius);
        }
    }

    private Integer invokeInt(Island island, String method) {
        try {
            Method m = island.getClass().getMethod(method);
            Object v = m.invoke(island);
            if (v instanceof Integer) return (Integer) v;
        } catch (Exception e) { }
        return null;
    }

    // ======= COST / REQUIREMENTS =======
    private boolean payIfNeeded(Player p, double cost) {
        if (cost <= 0) return true;
        if (!Main.get().econ().isReady()) { p.sendMessage("§cVault가 없어 결제를 건너뜁니다."); return true; }
        if (!Main.get().econ().has(p, cost)) { p.sendMessage(color(Main.get().configs().messages.getString("messages.upgrade-no-money").replace("{cost}", String.valueOf(cost)))); return false; }
        boolean ok = Main.get().econ().withdraw(p, cost);
        if (!ok) { p.sendMessage(color(Main.get().configs().messages.getString("messages.upgrade-pay-failed"))); return false; }
        return true;
    }

// ======= TEAM SIZE =======

    public int currentTeamSizeFor(Player p) {
        Island is = islandOf(p);
        if (is == null) return cfg.upgrade.getInt("team-size.default", 4);
        Integer v = is.getMaxMembers(RanksManager.MEMBER_RANK);
        if (v == null) {
            // world default; try to resolve via IslandsManager
            return cfg.upgrade.getInt("team-size.default", 4);
        }
        return v >= 0 ? v : 999; // negative = unlimited
    }

    public int nextTeamSizeFor(Player p) {
        Island is = islandOf(p);
        int current = currentTeamSizeFor(p);
        int def = cfg.upgrade.getInt("team-size.default", 4);
        int best = current <= 0 ? def : current;

        ConfigurationSection tiers = cfg.upgrade.getConfigurationSection("team-size.tiers");
        if (tiers != null) {
            for (String key : tiers.getKeys(false)) {
                int req = tiers.getInt(key + ".required-level", 0);
                int members = tiers.getInt(key + ".members", def);
                // TODO: replace level with real level check
                long level = 999999;
                if (level >= req && members > best) best = members;
            }
        }
        return best;
    }

    public void tryUpgradeTeamSize(Player p) {
        Island is = islandOf(p);
        if (is == null) { p.sendMessage(color(Main.get().configs().messages.getString("messages.island-required"))); return; }
        int before = currentTeamSizeFor(p);
        int after = nextTeamSizeFor(p);
        if (after <= before) { p.sendMessage(color(Main.get().configs().messages.getString("messages.upgrade-none"))); return; }
        IslandsManager im = BentoBox.getInstance().getIslands();
        im.setMaxMembers(is, RanksManager.MEMBER_RANK, after);
        p.sendMessage("§a팀원 수 제한 업그레이드! §e" + before + " §7→ §a" + after);
    }

    // ======= Helpers =======

    private Island islandOf(Player p) {
        Optional<Island> opt = BentoBox.getInstance().getIslands().getIslandAt(p.getLocation());
        return opt.orElse(null);
    }
}


    private int requiredLevelForNextTeam(Player p) {
        Island is = islandOf(p);
        if (is == null) return Integer.MAX_VALUE;
        int def = cfg.upgrade.getInt("team-size.default", 4);
        int current = currentTeamSizeFor(p);
        int best = current;
        int reqLevel = Integer.MAX_VALUE;
        ConfigurationSection tiers = cfg.upgrade.getConfigurationSection("team-size.tiers");
        if (tiers != null) {
            for (String key : tiers.getKeys(false)) {
                int members = tiers.getInt(key + ".members", def);
                int req = tiers.getInt(key + ".required-level", 0);
                if (members > best) { best = members; reqLevel = req; }
            }
        }
        return reqLevel == Integer.MAX_VALUE ? 0 : reqLevel;
    }
    private double costForNextTeam(Player p) {
        Island is = islandOf(p);
        if (is == null) return 0.0;
        int def = cfg.upgrade.getInt("team-size.default", 4);
        int current = currentTeamSizeFor(p);
        int best = current;
        double cost = 0.0;
        ConfigurationSection tiers = cfg.upgrade.getConfigurationSection("team-size.tiers");
        if (tiers != null) {
            for (String key : tiers.getKeys(false)) {
                int members = tiers.getInt(key + ".members", def);
                if (members > best) {
                    best = members;
                    cost = tiers.getDouble(key + ".cost", 0.0);
                }
            }
        }
        return cost;
    }

    public void tryUpgradeTeamSize(Player p) {
        Island is = islandOf(p);
        if (is == null) { p.sendMessage(color(Main.get().configs().messages.getString("messages.island-required"))); return; }
        int before = currentTeamSizeFor(p);
        int after = nextTeamSizeFor(p);
        if (after <= before) { p.sendMessage(color(Main.get().configs().messages.getString("messages.upgrade-none"))); return; }
        int needLv = requiredLevelForNextTeam(p);
        int curLv = Main.get().levels().levelOf(is);
        if (curLv < needLv) { p.sendMessage(color(Main.get().configs().messages.getString("messages.upgrade-need-level").replace("{level}", String.valueOf(needLv)))); return; }
        double cost = costForNextTeam(p);
        if (!payIfNeeded(p, cost)) return;
        world.bentobox.bentobox.managers.IslandsManager im = BentoBox.getInstance().getIslands();
        im.setMaxMembers(is, world.bentobox.bentobox.managers.RanksManager.MEMBER_RANK, after);
        p.sendMessage(color(Main.get().configs().messages.getString("messages.upgrade-team-success").replace("{before}", String.valueOf(before)).replace("{after}", String.valueOf(after)).replace("{cost}", String.valueOf(cost))));
    }

    private String color(String s) { return org.bukkit.ChatColor.translateAlternateColorCodes('&', s); }
