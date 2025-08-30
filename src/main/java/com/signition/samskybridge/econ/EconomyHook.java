package com.signition.samskybridge.econ;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import net.milkbowl.vault.economy.Economy;

public class EconomyHook {
    private Economy econ;

    public void hook() {
        try {
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                econ = rsp.getProvider();
                Bukkit.getLogger().info("[SSB] Vault economy hooked: " + econ.getName());
            } else {
                Bukkit.getLogger().warning("[SSB] Vault economy provider not found.");
            }
        } catch (Throwable t) {
            Bukkit.getLogger().warning("[SSB] Vault not installed; money requirements disabled.");
        }
    }

    public boolean isReady() { return econ != null; }

    public boolean has(Player p, double amount) {
        if (econ == null) return true; // treat as free if no econ
        return econ.has(p, amount);
        // Note: for offline/owner, use OfflinePlayer variant
    }

    public boolean withdraw(Player p, double amount) {
        if (econ == null) return true;
        return econ.withdrawPlayer((OfflinePlayer)p, amount).transactionSuccess();
    }
}
