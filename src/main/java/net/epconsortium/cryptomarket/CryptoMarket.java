package net.epconsortium.cryptomarket;

import net.epconsortium.cryptomarket.commands.CryptoMarketCommand;
import net.epconsortium.cryptomarket.finances.ExchangeRates;
import net.epconsortium.cryptomarket.task.UpdateExchangeRatesTask;
import net.epconsortium.cryptomarket.ui.MenuListener;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import net.epconsortium.cryptomarket.database.dao.InvestorDao;
import net.epconsortium.cryptomarket.task.SaveInvestorsTask;
import net.epconsortium.cryptomarket.ui.CalendarListener;
import net.epconsortium.cryptomarket.ui.RankingListener;

/**
 * Main class of the plugin
 * 
 * @author roinujnosde
 */
public class CryptoMarket extends JavaPlugin {

    private static CryptoMarket cm;

    private Economy econ = null;
    private SaveInvestorsTask saveInvestors;
    private UpdateExchangeRatesTask updateRates;

    private static boolean debug;

    @Override
    public void onEnable() {
        cm = this;
        // Salvando configuração
        saveDefaultConfig();

        // Eventos
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new MenuListener(this), this);
        pluginManager.registerEvents(new CalendarListener(this), this);
        pluginManager.registerEvents(new RankingListener(this), this);

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

        // Atualizando as cotações
        ExchangeRates rates = new ExchangeRates(this);
        rates.updateAll();
        // Iniciando tarefa repetitiva de atualizacão de cotações
        updateRates = new UpdateExchangeRatesTask(rates);
        updateRates.start(this);
        //Starting the Save Investors task
        saveInvestors = new SaveInvestorsTask(this);
        saveInvestors.start();

        debug = getConfig().getBoolean("debug", false);

        InvestorDao.configureDatabase(this, (success) -> {
            if (!success) {
                getServer().getPluginManager().disablePlugin(this);
            } else {
                getServer().getConsoleSender().sendMessage(
                        "[CryptoMarket] Database configured successfuly!");
            }
        });
    }

    @Override
    public void onDisable() {
        new InvestorDao(this).saveAll();
        saveInvestors.cancel();
        updateRates.cancel();
    }

    /**
     * Sends a debug message to the console if debug is enabled
     *
     * @param message message
     */
    public static void debug(String message) {
        if (debug) {
            Bukkit.getServer().getLogger().log(Level.INFO, "[CryptoMarket] {0}", message);
        }
    }

    /**
     * Sends an warn to the console
     *
     * @param message message
     */
    public static void warn(String message) {
        Bukkit.getServer().getLogger().log(Level.WARNING, "[CryptoMarket] {0}", message);
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
        RegisteredServiceProvider<Economy> rsp = getServer()
                .getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    /**
     * Returns Vault's Economy
     *
     * @return economy
     */
    public Economy getEconomy() {
        return econ;
    }

    /**
     * Returns the instance of CryptoMarket
     *
     * @return the instance
     */
    public static CryptoMarket getCryptoMarket() {
        return cm;
    }
}
