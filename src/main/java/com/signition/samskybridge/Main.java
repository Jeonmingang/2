package com.signition.samskybridge;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;

import com.signition.samskybridge.cfg.Configs;
import com.signition.samskybridge.data.DataService;
import com.signition.samskybridge.level.LevelService;
import com.signition.samskybridge.econ.EconomyHook;
import com.signition.samskybridge.hook.BentoHook;
import com.signition.samskybridge.upgrade.UpgradeManager;
import com.signition.samskybridge.gui.UpgradeGUI;
import com.signition.samskybridge.visual.BorderService;
import com.signition.samskybridge.rank.RankingService;
import com.signition.samskybridge.tag.TagService;

public class Main extends JavaPlugin {

    private static Main inst;
    private Configs configs;
    private DataService dataService;
    private LevelService levelService;
    private EconomyHook economy;
    private BentoHook bentoHook;
    private UpgradeManager upgradeManager;
    private BorderService borderService;
    private UpgradeGUI upgradeGUI;
    private RankingService rankingService;
    private TagService tagService;

    @Override
    public void onEnable() {
        inst = this;
        saveDefaultConfigFiles();

        this.configs = new Configs(this);
        this.dataService = new DataService(this);
        this.levelService = new LevelService(this, dataService);
        this.economy = new EconomyHook(); this.economy.hook();
        this.configs.loadAll();

        this.borderService = new BorderService(this, configs);

        this.upgradeManager = new UpgradeManager(this, configs, borderService);
        this.bentoHook = new BentoHook(this, upgradeManager, borderService);
        this.upgradeGUI = new UpgradeGUI(this, upgradeManager);
        Bukkit.getPluginManager().registerEvents(upgradeGUI, this);
        this.rankingService = new RankingService(this);
        this.tagService = new TagService(this);
        Bukkit.getPluginManager().registerEvents(tagService, this);

        Bukkit.getPluginManager().registerEvents(bentoHook, this);
        Bukkit.getPluginManager().registerEvents(upgradeManager, this);
        Bukkit.getPluginManager().registerEvents(levelService, this);

        getLogger().info("SamSkyBridge enabled.");
    }

    @Override
    public void onDisable() {
        if (dataService != null) dataService.shutdown();
        if (borderService != null) borderService.clearAll();
        getLogger().info("SamSkyBridge disabled.");
    }

    private void saveDefaultConfigFiles() {
        String[] files = {"customization.yml","rank.yml","reloadable.yml","serverdata.yml","settings.yml","upgrade.yml"};
        for (String n : files) {
            saveResource(n, false);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    // Only root command: /섬 (alias of 'island')
    String name = cmd.getName().toLowerCase(java.util.Locale.KOREAN);
    if (!name.equals("island") && !name.equals("섬")) return false;

    if (args.length == 0 || args[0].equalsIgnoreCase("도움말")) {
        sender.sendMessage("§b/섬 업그레이드 §7- 업그레이드 GUI 열기");
        sender.sendMessage("§b/섬 랭킹 [상위|나|재집계] §7- 섬 랭킹");
        sender.sendMessage("§b/섬 레벨 §7- 내 섬 레벨/경험치");
        if (sender.hasPermission("samskybridge.admin")) sender.sendMessage("§b/섬 리로드 §7- 설정 리로드");
        return true;
    }

    String sub = args[0];

    if (sub.equalsIgnoreCase("업그레이드")) {
        if (!(sender instanceof Player)) { sender.sendMessage(color(configs.messages.getString("messages.players-only"))); return true; }
        if (upgradeGUI != null) upgradeGUI.open((Player)sender);
        return true;
    }

    if (sub.equalsIgnoreCase("랭킹")) {
        String opt = args.length >= 2 ? args[1] : "상위";
        if (opt.equalsIgnoreCase("상위") || opt.equalsIgnoreCase("top")) {
            rankingService.showTop(sender, 10);
            return true;
        }
        if (opt.equalsIgnoreCase("나") || opt.equalsIgnoreCase("me")) {
            if (!(sender instanceof Player)) { sender.sendMessage(color(configs.messages.getString("messages.players-only"))); return true; }
            rankingService.showMe((Player)sender);
            return true;
        }
        if (opt.equalsIgnoreCase("재집계") || opt.equalsIgnoreCase("recalc")) {
            if (!sender.hasPermission("samskybridge.admin")) { sender.sendMessage("§c권한이 없습니다."); return true; }
            rankingService.recalcNotice(sender);
            return true;
        }
        sender.sendMessage("§7사용법: /섬 랭킹 [상위|나|재집계]");
        return true;
    }

    if (sub.equalsIgnoreCase("레벨")) {
        if (!(sender instanceof Player)) { sender.sendMessage(color(configs.messages.getString("messages.players-only"))); return true; }
        rankingService.showMe((Player)sender);
        return true;
    }

    if (sub.equalsIgnoreCase("리로드")) {
        if (!sender.hasPermission("samskybridge.admin")) { sender.sendMessage("§c권한이 없습니다."); return true; }
        configs.loadAll();
        borderService.reloadSettings();
        sender.sendMessage("§a리로드 완료.");
        return true;
    }

    sender.sendMessage("§7/섬 도움말");
    return true;
}
}

    public static Main get() { return inst; }
    public Configs configs() { return configs; }
    private String color(String s) { return org.bukkit.ChatColor.translateAlternateColorCodes('&', s); }
    public DataService data() { return dataService; }
    public LevelService levels() { return levelService; }
    public EconomyHook econ() { return economy; }
    public com.signition.samskybridge.rank.RankingService ranking() { return rankingService; }
    public TagService tag() { return tagService; }
}
