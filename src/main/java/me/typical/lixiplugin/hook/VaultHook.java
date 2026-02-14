package me.typical.lixiplugin.hook;

import me.typical.lixiplugin.LXPlugin;
import me.typical.lixiplugin.service.IService;
import me.typical.lixiplugin.util.MessageUtil;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Integration with Vault Economy API.
 * Provides methods for depositing, withdrawing, and checking balances.
 */
public class VaultHook implements IService {

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
        // Nothing to clean up
    }

    /**
     * Check if Vault economy is available
     */
    public boolean isAvailable() {
        return economy != null;
    }

    /**
     * Withdraw money from a player's account
     *
     * @param player The player
     * @param amount The amount to withdraw
     * @return EconomyResponse with result
     */
    public EconomyResponse withdraw(OfflinePlayer player, double amount) {
        if (!isAvailable()) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Economy not available");
        }
        return economy.withdrawPlayer(player, amount);
    }

    /**
     * Deposit money into a player's account
     *
     * @param player The player
     * @param amount The amount to deposit
     * @return EconomyResponse with result
     */
    public EconomyResponse deposit(OfflinePlayer player, double amount) {
        if (!isAvailable()) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Economy not available");
        }
        return economy.depositPlayer(player, amount);
    }

    /**
     * Get a player's balance
     *
     * @param player The player
     * @return The player's balance
     */
    public double getBalance(OfflinePlayer player) {
        if (!isAvailable()) {
            return 0;
        }
        return economy.getBalance(player);
    }

    /**
     * Format an amount as a currency string
     *
     * @param amount The amount to format
     * @return Formatted currency string
     */
    public String format(double amount) {
        if (!isAvailable()) {
            return String.valueOf(amount);
        }
        return economy.format(amount);
    }

    /**
     * Get the economy provider instance
     */
    public Economy getEconomy() {
        return economy;
    }
}
