package com.signition.samskybridge.tag;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Scoreboard;
import org.bukkit.ScoreboardManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scoreboard.Team;

import com.signition.samskybridge.Main;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.island.Island;

public class TagService implements Listener {

    private final Main plugin;
    private final Map<UUID, Integer> cachedRank = new ConcurrentHashMap<>(); // islandId -> rank
    private Scoreboard board;

    public TagService(Main plugin) {
        this.plugin = plugin;
        ScoreboardManager sm = Bukkit.getScoreboardManager();
        this.board = sm != null ? sm.getMainScoreboard() : Bukkit.getScoreboardManager().getNewScoreboard();
        // periodic refresh
        Bukkit.getScheduler().runTaskTimer(plugin, this::recomputeAndApplyAll, 200L, 200L); // every 10s
    }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }

    public void recomputeAndApplyAll() {
        // Build ranking list (by level desc, xp desc)
        List<Island> list = new ArrayList<>(BentoBox.getInstance().getIslands().getIslands());
        list.sort((a,b) -> {
            int la = plugin.levels().levelOf(a);
            int lb = plugin.levels().levelOf(b);
            if (lb != la) return Integer.compare(lb, la);
            long xa = plugin.data().getXP(a.getUniqueId());
            long xb = plugin.data().getXP(b.getUniqueId());
            return Long.compare(xb, xa);
        });
        int rank = 1;
        Map<UUID,Integer> map = new HashMap<>();
        for (Island is : list) {
            map.put(is.getUniqueId(), rank++);
        }
        cachedRank.clear();
        cachedRank.putAll(map);
        // Apply to online players
        for (Player p : Bukkit.getOnlinePlayers()) {
            apply(p);
        }
    }

    public void apply(Player p) {
        if (p == null) return;
        Island is = BentoBox.getInstance().getIslands().getIslandAt(p.getLocation()).orElse(null);
        if (is == null) return;
        Integer rk = cachedRank.get(is.getUniqueId());
        if (rk == null) return;

        boolean nameOn = plugin.configs().messages.getBoolean("rank-display.nametag.enabled", true);
        boolean chatOn = plugin.configs().messages.getBoolean("rank-display.chat.enabled", true);
        String fmtName = plugin.configs().messages.getString("rank-display.nametag.format", "&7[ &b섬 랭킹 &e<rank> &7] ");
        String fmtChat = plugin.configs().messages.getString("rank-display.chat.format", "&7[ &b섬 랭킹 &e<rank> &7] ");
        int max = plugin.configs().messages.getInt("rank-display.nametag.max", 100);

        String prefix = color(fmtName.replace("<rank>", String.valueOf(rk)));
        String chatPrefix = color(fmtChat.replace("<rank>", String.valueOf(rk)));

        if (nameOn && rk <= max) {
            String teamName = "SSB_R_" + rk;
            Team t = board.getTeam(teamName);
            if (t == null) t = board.registerNewTeam(teamName);
            t.setPrefix(prefix);
            if (!t.hasEntry(p.getName())) t.addEntry(p.getName());
            p.setScoreboard(board);
        }

        if (chatOn) {
            // Update display name so default chat format shows the prefix; other chat plugins may override
            p.setDisplayName(chatPrefix + p.getName());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> apply(e.getPlayer()), 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        // Optional: remove from teams to reduce clutter
        for (Team t : board.getTeams()) {
            t.removeEntry(e.getPlayer().getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent e) {
        if (!plugin.configs().messages.getBoolean("rank-display.chat.enabled", true)) return;
        // If some plugin resets display name, still ensure prefix
        // No heavy logic here; apply() already set displayName but we can ensure on each chat
        Player p = e.getPlayer();
        Island is = BentoBox.getInstance().getIslands().getIslandAt(p.getLocation()).orElse(null);
        if (is == null) return;
        Integer rk = cachedRank.get(is.getUniqueId());
        if (rk == null) return;
        String fmtChat = plugin.configs().messages.getString("rank-display.chat.format", "&7[ &b섬 랭킹 &e<rank> &7] ");
        String chatPrefix = color(fmtChat.replace("<rank>", String.valueOf(rk)));
        e.setFormat(chatPrefix + "%1$s: %2$s");
    }
}
