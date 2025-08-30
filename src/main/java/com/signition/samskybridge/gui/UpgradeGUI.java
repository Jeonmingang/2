package com.signition.samskybridge.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import com.signition.samskybridge.Main;
import com.signition.samskybridge.upgrade.UpgradeManager;

public class UpgradeGUI implements Listener {

    public static final String TITLE = ChatColor.AQUA + Main.get().configs().gui.getString("gui.upgrades.title", "섬 업그레이드");

    private final Main plugin;
    private final UpgradeManager upgrades;

    public UpgradeGUI(Main plugin, UpgradeManager upgrades) {
        this.plugin = plugin;
        this.upgrades = upgrades;
    }

    public void open(Player p) {
        int slotSize = Main.get().configs().gui.getInt("gui.upgrades.size.slot", 11);
        int slotTeam = Main.get().configs().gui.getInt("gui.upgrades.team.slot", 15);
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        inv.setItem(slotSize, islandSizeItem(p));
        inv.setItem(slotTeam, teamSizeItem(p));
        p.openInventory(inv);
    }

    private ItemStack islandSizeItem(Player p) {
        int current = upgrades.currentRadiusFor(p);
        int next = upgrades.nextRadiusFor(p);
        Material mat = Material.getMaterial(Main.get().configs().gui.getString("gui.upgrades.size.material", "MAP"));
        if (mat == null) mat = Material.MAP;
        ItemStack it = new ItemStack(mat);
        ItemMeta im = it.getItemMeta();
        im.setDisplayName(color(Main.get().configs().gui.getString("gui.upgrades.size.name", "&6섬 크기")));
        List<String> lore = new ArrayList<>();
        lore.add(color("&7현재 반지름: &e" + current));
        lore.add(color("&7필요 레벨: &e" + upgrades.requiredLevelForNextRadius(p)));
        lore.add(color("&7비용: &e" + upgrades.costForNextRadius(p)));
        if (next > current) {
            lore.add(color("&7다음 반지름: &a" + next));
            lore.add(color("&f클릭: 업그레이드"));
        } else {
            lore.add(color("&c더 이상 업그레이드 없음"));
        }
        im.setLore(lore);
        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        it.setItemMeta(im);
        return it;
    }

    private ItemStack teamSizeItem(Player p) {
        int current = upgrades.currentTeamSizeFor(p);
        int next = upgrades.nextTeamSizeFor(p);
        Material mat = Material.getMaterial(Main.get().configs().gui.getString("gui.upgrades.team.material", "PLAYER_HEAD"));
        if (mat == null) mat = Material.PLAYER_HEAD;
        ItemStack it = new ItemStack(mat);
        ItemMeta im = it.getItemMeta();
        im.setDisplayName(color(Main.get().configs().gui.getString("gui.upgrades.team.name", "&6팀원 수")));
        List<String> lore = new ArrayList<>();
        lore.add(color("&7현재 팀원 수 제한: &e" + current));
        lore.add(color("&7필요 레벨: &e" + upgrades.requiredLevelForNextTeam(p)));
        lore.add(color("&7비용: &e" + upgrades.costForNextTeam(p)));
        if (next > current) {
            lore.add(color("&7다음 제한: &a" + next));
            lore.add(color("&f클릭: 업그레이드"));
        } else {
            lore.add(color("&c더 이상 업그레이드 없음"));
        }
        im.setLore(lore);
        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        it.setItemMeta(im);
        return it;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (e.getClickedInventory() == null) return;
        if (!e.getView().getTitle().equals(TITLE)) return;
        e.setCancelled(true);

        Player p = (Player)e.getWhoClicked();
        int slot = e.getRawSlot();
        int slotSize = Main.get().configs().gui.getInt("gui.upgrades.size.slot", 11);
        int slotTeam = Main.get().configs().gui.getInt("gui.upgrades.team.slot", 15);
        if (slot == slotSize) {
            upgrades.tryUpgradeIslandSize(p);
            // refresh
            Bukkit.getScheduler().runTask(plugin, () -> open(p));
        } else if (slot == slotTeam) {
            upgrades.tryUpgradeTeamSize(p);
            Bukkit.getScheduler().runTask(plugin, () -> open(p));
        }
    }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }
}
