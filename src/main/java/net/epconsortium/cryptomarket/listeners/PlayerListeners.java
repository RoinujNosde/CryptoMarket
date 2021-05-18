package net.epconsortium.cryptomarket.listeners;

import net.epconsortium.cryptomarket.CryptoMarket;
import net.epconsortium.cryptomarket.database.dao.InvestorDao;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerListeners implements Listener {

    private final CryptoMarket plugin;
    private final InvestorDao investorDao;

    public PlayerListeners(@NotNull CryptoMarket plugin) {
        this.plugin = plugin;
        investorDao = plugin.getInvestorDao();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> investorDao.loadInvestor(event.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        investorDao.unloadInvestor(event.getPlayer());
    }

}
