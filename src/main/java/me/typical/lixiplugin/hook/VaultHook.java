package me.typical.lixiplugin.hook;

import me.typical.lixiplugin.LXPlugin;
import me.typical.lixiplugin.economy.EconomyProvider;
import me.typical.lixiplugin.economy.LixiCurrency;
import me.typical.lixiplugin.service.IService;
import me.typical.lixiplugin.util.MessageUtil;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/** Vault economy integration. */
public class VaultHook implements IService, EconomyProvider {

    private Economy economy;
    private final LXPlugin plugin = LXPlugin.getInstance();

    @Override
    public void setup() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            MessageUtil.warn("Vault not found! Economy features will not work.");
            return;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer()
                .getServicesManager()
                .getRegistration(Economy.class);

        if (rsp == null) {
            MessageUtil.warn("No economy provider found! Please install an economy plugin.");
            return;
        }

        economy = rsp.getProvider();
        MessageUtil.info("Vault economy hooked: " + economy.getName());
    }

    @Override
    public void shutdown() {
    }

    @Override
    public LixiCurrency getCurrencyType() {
        return LixiCurrency.VAULT;
    }

    @Override
    public boolean isAvailable() {
        return economy != null;
    }

    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
        EconomyResponse r = withdrawResponse(player, amount);
        return r.transactionSuccess();
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        EconomyResponse r = depositResponse(player, amount);
        return r.transactionSuccess();
    }

    public EconomyResponse withdrawResponse(OfflinePlayer player, double amount) {
        if (!isAvailable()) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Economy not available");
        }
        return economy.withdrawPlayer(player, amount);
    }

    public EconomyResponse depositResponse(OfflinePlayer player, double amount) {
        if (!isAvailable()) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Economy not available");
        }
        return economy.depositPlayer(player, amount);
    }

    public double getBalance(OfflinePlayer player) {
        if (!isAvailable()) {
            return 0;
        }
        return economy.getBalance(player);
    }

    public String format(double amount) {
        if (!isAvailable()) {
            return String.valueOf(amount);
        }
        return economy.format(amount);
    }

    public Economy getEconomy() {
        return economy;
    }
}
