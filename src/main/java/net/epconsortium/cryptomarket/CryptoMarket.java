package net.epconsortium.cryptomarket;

import net.epconsortium.cryptomarket.commands.CryptoMarketCommand;
import net.epconsortium.cryptomarket.database.dao.InvestorDao;
import net.epconsortium.cryptomarket.finances.Economy;
import net.epconsortium.cryptomarket.finances.ExchangeRates;
import net.epconsortium.cryptomarket.listeners.PlayerListeners;
import net.epconsortium.cryptomarket.task.SaveInvestorsTask;
import net.epconsortium.cryptomarket.task.UpdateExchangeRatesTask;
import net.epconsortium.cryptomarket.task.UpdateRichersListTask;
import net.epconsortium.cryptomarket.ui.InventoryController;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;

/**
 * Main class of the plugin
 * 
 * @author roinujnosde
 */
public class CryptoMarket extends JavaPlugin {

    private static CryptoMarket cm;
    private static boolean debug;
    private net.milkbowl.vault.economy.Economy econ = null;

    @Override
    public void onEnable() {
        cm = this;
        // Salvando configuração
        saveDefaultConfig();

        // Eventos
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(InventoryController.getInstance(), this);
        pluginManager.registerEvents(new PlayerListeners(this), this);

        //Comandos
		PluginCommand command = getCommand("cryptomarket");
        CryptoMarketCommand cmd = new CryptoMarketCommand(this);
        command.setExecutor(cmd);
        command.setTabCompleter(cmd);

        // Configurando Vault e economia
        if (!setupEconomy()) {
            warn("§4Vault and/or economy plugin not found. Disabling plugin...");
            pluginManager.disablePlugin(this);
            return;
        }

        debug = getConfig().getBoolean("debug", false);

        getInvestorDao().configureDatabase(this, (success) -> {
            if (!success) {
                getServer().getPluginManager().disablePlugin(this);
            } else {
                getLogger().info("Database configured successfuly!");

                getExchangeRates().updateAll();
                startTasks();
                if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                    new CMExpansion(this).register();
                }
                Collection<? extends Player> onlinePlayers = getServer().getOnlinePlayers();
                if (!onlinePlayers.isEmpty()) {
                    getLogger().info("Found players online (did you reload?), loading their data...");
                    onlinePlayers.forEach(getInvestorDao()::loadInvestor);
                }
            }
        });
    }

    private void startTasks() {
        new UpdateExchangeRatesTask(this).start();
        new SaveInvestorsTask(this).start();
        new UpdateRichersListTask(this).start();
    }

    @Override
    public void onDisable() {
    	getServer().getScheduler().cancelTasks(this);
        getInvestorDao().saveAll();
    }

    /**
     * Sends a debug message to the console if debug is enabled
     *
     * @param message message
     */
    public static void debug(String message) {
        if (debug) {
            getInstance().getLogger().info(message);
        }
    }

    /**
     * Sends an warn to the console
     *
     * @param message message
     */
    public static void warn(String message) {
        getInstance().getLogger().warning(message);
    }

    /**
     * Configures Vault economy
     *
     * @return true if success
     */
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp = getServer()
                .getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public InvestorDao getInvestorDao() {
        return InvestorDao.getInstance(this);
    }

    public ExchangeRates getExchangeRates() {
        return ExchangeRates.getInstance(this);
    }

    /**
     * Returns Vault's Economy
     *
     * @return economy
     */
    public net.milkbowl.vault.economy.Economy getVaultEconomy() {
        return econ;
    }

    public Economy getEconomy() {
        return Economy.getInstance(this);
    }

    /**
     * Returns the instance of CryptoMarket
     *
     * @return the instance
     */
    public static CryptoMarket getInstance() {
        return cm;
    }
}
