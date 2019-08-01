package net.epconsortium.cryptomarket.ui;

import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import net.epconsortium.cryptomarket.CryptoMarket;
import net.epconsortium.cryptomarket.util.Configuration;

/**
 * Class used to listen to clicks on the Ranking menu and process them
 * 
 * @author roinujnosde
 */
public class RankingListener implements Listener {

    private final CryptoMarket plugin;
    private final Configuration config;

    public RankingListener(CryptoMarket plugin) {
        this.plugin = Objects.requireNonNull(plugin);
        config = new Configuration(plugin);
    }

    @EventHandler
    public void onInventoryClickEvent(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        if (!Helper.isCustomInventory(event)) {
            return;
        }
        event.setCancelled(true);

        if (event.getInventory().getName().equals(config.getRankingMenuName())) {
            processBackButton(event, player);
        }
    }

    private boolean processBackButton(InventoryClickEvent event, Player player) {
        if (event.getCurrentItem().getItemMeta().getDisplayName().equals(
                config.getRankingMenuBackButton())) {
            player.closeInventory();
            Menu menu = new Menu(plugin, player);
            menu.open();
            return true;
        }

        return false;
    }
}
