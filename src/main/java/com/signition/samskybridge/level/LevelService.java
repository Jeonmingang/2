
package com.signition.samskybridge.level;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import com.signition.samskybridge.Main;
import com.signition.samskybridge.data.DataService;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.island.Island;
import world.bentobox.bentobox.api.user.User;

public class LevelService implements Listener {

    private final Main plugin;
    private final DataService data;

    private final Map<String, Long> blockXP = new ConcurrentHashMap<>();
    private final Map<String, Long> prefixXP = new ConcurrentHashMap<>();
    private final Map<String, Long> containsXP = new ConcurrentHashMap<>();
    private final List<AbstractMap.SimpleEntry<java.util.regex.Pattern, Long>> regexXP = new ArrayList<>();
    private final TreeMap<Integer, Long> xpTable = new TreeMap<>(); // level -> cumulative XP

    private final Map<UUID, Long> tickXP = new ConcurrentHashMap<>();

    public LevelService(Main plugin, DataService data) {
        this.plugin = plugin;
        this.data = data;
        reload();
        Bukkit.getScheduler().runTaskTimer(plugin, () -> tickXP.clear(), 1L, 1L);
    }

    public void reload() {
        blockXP.clear();
        prefixXP.clear();
        containsXP.clear();
        regexXP.clear();
        xpTable.clear();

        ConfigurationSection bs = plugin.configs().blocks.getConfigurationSection("blocks");
        if (bs != null) {
            for (String k : bs.getKeys(false)) {
                blockXP.put(k.toLowerCase(java.util.Locale.ENGLISH), bs.getLong(k));
            }
        }

        loadPatternList("patterns.prefix", (pat, xp) -> prefixXP.put(pat.toLowerCase(java.util.Locale.ENGLISH), xp));
        loadPatternList("patterns.contains", (pat, xp) -> containsXP.put(pat.toLowerCase(java.util.Locale.ENGLISH), xp));
        loadRegexList("patterns.regex");

        ConfigurationSection tbl = plugin.configs().levels.getConfigurationSection("leveling.xp-table");
        if (tbl != null && !tbl.getKeys(false).isEmpty()) {
            xpTable.put(0, 0L);
            java.util.List<Integer> lvls = new ArrayList<>();
            for (String k : tbl.getKeys(false)) { try { lvls.add(Integer.parseInt(k)); } catch (NumberFormatException ignored) {} }
            java.util.Collections.sort(lvls);
            for (Integer lv : lvls) {
                long req = tbl.getLong(String.valueOf(lv));
                xpTable.put(lv, req);
            }
        } else {
            double base = plugin.configs().levels.getDouble("leveling.base-xp",
                            plugin.configs().levels.getDouble("leveling.xp-per-level", 1000.0));
            double growth = plugin.configs().levels.getDouble("leveling.growth-percent", 1.5) / 100.0;
            int maxLevel = plugin.configs().levels.getInt("leveling.max-level", 200);
            if (maxLevel < 1) maxLevel = 100;
            xpTable.put(0, 0L);
            long cumulative = 0L;
            double req = base;
            for (int lvl = 1; lvl <= maxLevel; lvl++) {
                cumulative += Math.max(1L, Math.round(req));
                xpTable.put(lvl, cumulative);
                req = req * (1.0 + growth);
            }
        }
    }

    private void loadPatternList(String path, java.util.function.BiConsumer<String, Long> sink) {
        java.util.List<?> list = plugin.configs().blocks.getList(path, java.util.Collections.emptyList());
        for (Object o : list) {
            if (o instanceof java.util.List) {
                java.util.List<?> a = (java.util.List<?>) o;
                if (a.size() >= 2) {
                    String pat = String.valueOf(a.get(0));
                    long val; try { val = Long.parseLong(String.valueOf(a.get(1))); } catch (Exception e) { continue; }
                    sink.accept(pat, val);
                }
            } else if (o instanceof java.util.Map) {
                java.util.Map<?,?> m = (java.util.Map<?,?>) o;
                for (java.util.Map.Entry<?,?> e : m.entrySet()) {
                    String pat = String.valueOf(e.getKey());
                    long val; try { val = Long.parseLong(String.valueOf(e.getValue())); } catch (Exception ex) { continue; }
                    sink.accept(pat, val);
                }
            }
        }
    }

    private void loadRegexList(String path) {
        java.util.List<?> list = plugin.configs().blocks.getList(path, java.util.Collections.emptyList());
        for (Object o : list) {
            if (o instanceof java.util.List) {
                java.util.List<?> a = (java.util.List<?>) o;
                if (a.size() >= 2) {
                    String pat = String.valueOf(a.get(0));
                    long val; try { val = Long.parseLong(String.valueOf(a.get(1))); } catch (Exception e) { continue; }
                    try { regexXP.add(new AbstractMap.SimpleEntry<>(java.util.regex.Pattern.compile(pat), val)); } catch (Exception ignored) {}
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (plugin.configs().storage.getBoolean("settings.ignore-creative", true) && p.getGameMode() == GameMode.CREATIVE) return;
        if (!isWorldEnabled(e.getBlockPlaced().getWorld().getName())) return;

        Block b = e.getBlockPlaced();
        Island is = BentoBox.getInstance().getIslands().getIslandAt(b.getLocation()).orElse(null);
        if (is == null) return;

        int cap = plugin.configs().storage.getInt("settings.xp-per-tick-cap", 200);
        if (cap > 0) {
            long sum = tickXP.getOrDefault(is.getUniqueId(), 0L);
            if (sum >= cap) return;
        }

        String key = blockKey(b);
        long xp = xpFor(key);
        if (xp <= 0) return;

        UUID id = is.getUniqueId();
        long cur = data.getXP(id) + xp;
        data.setXP(id, cur);
        if (cap > 0) tickXP.put(id, tickXP.getOrDefault(id, 0L) + xp);

        int oldL = data.getLevel(id);
        int newL = levelForXP(cur);
        if (newL != oldL) {
            data.setLevel(id, newL);
            String msg = plugin.configs().messages.getString("messages.level-up", "&b[섬 레벨] &aLv.{newLevel} &7(+{xp}xp)");
            msg = msg.replace("{newLevel}", String.valueOf(newL)).replace("{xp}", String.valueOf(xp));
            p.sendMessage(color(msg));
            plugin.ranking().recompute();
            for (world.bentobox.bentobox.api.user.User u : is.getMembers()) {
                Player op = Bukkit.getPlayer(u.getUniqueId());
                if (op != null) plugin.tag().apply(op);
            }
        }
    }

    public int levelForXP(long xp) {
        int lvl = 0;
        for (Map.Entry<Integer, Long> e : xpTable.entrySet()) {
            if (xp >= e.getValue()) lvl = e.getKey();
            else break;
        }
        return lvl;
    }

    public long thresholdFor(int level) {
        Long v = xpTable.get(level);
        if (v == null) {
            long last = 0L;
            for (Map.Entry<Integer, Long> e : xpTable.entrySet()) {
                if (e.getKey() <= level) last = e.getValue(); else break;
            }
            return last;
        }
        return v;
    }

    public long nextThreshold(int level) {
        Long v = xpTable.get(level + 1);
        if (v == null) return Long.MAX_VALUE / 4;
        return v;
    }

    public long neededFor(int nextLevel) { return thresholdFor(nextLevel); }

    public long xpFor(String key) {
        if (key == null) return 0L;
        String k = key.toLowerCase(java.util.Locale.ENGLISH);
        Long v = blockXP.get(k);
        if (v != null) return v;
        for (Map.Entry<String, Long> e : prefixXP.entrySet()) { if (k.startsWith(e.getKey())) return e.getValue(); }
        for (Map.Entry<String, Long> e : containsXP.entrySet()) { if (k.contains(e.getKey())) return e.getValue(); }
        for (AbstractMap.SimpleEntry<java.util.regex.Pattern, Long> e : regexXP) { if (e.getKey().matcher(k).find()) return e.getValue(); }
        return 0L;
    }

    private String blockKey(Block b) {
        try {
            Class<?> adapter = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            java.lang.reflect.Method adapt = adapter.getMethod("adapt", org.bukkit.block.Block.class);
            Object weBlock = adapt.invoke(null, b);
            java.lang.reflect.Method getType = weBlock.getClass().getMethod("getBlockType");
            Object type = getType.invoke(weBlock);
            java.lang.reflect.Method getId = type.getClass().getMethod("getId");
            Object id = getId.invoke(type);
            if (id != null) return id.toString();
        } catch (Throwable ignore) { }
        try {
            BlockData bd = b.getBlockData();
            String s = bd.getAsString();
            if (s != null && s.contains(":")) return s;
        } catch (Throwable t) {}
        return b.getType().name().toLowerCase(java.util.Locale.ENGLISH);
    }

    private boolean isWorldEnabled(String world) {
        java.util.List<String> list = plugin.configs().storage.getStringList("settings.enable-worlds");
        if (list == null || list.isEmpty()) return true;
        for (String w : list) if (w.equalsIgnoreCase(world)) return true;
        return false;
    }

    private String color(String s) { return org.bukkit.ChatColor.translateAlternateColorCodes('&', s); }

    public long xpOf(Island is) { return data.getXP(is.getUniqueId()); }
    public int levelOf(Island is) { return data.getLevel(is.getUniqueId()); }
}
