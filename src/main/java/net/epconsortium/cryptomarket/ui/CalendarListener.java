package net.epconsortium.cryptomarket.ui;

import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import net.epconsortium.cryptomarket.CryptoMarket;
import net.epconsortium.cryptomarket.util.Configuration;
import org.bukkit.event.inventory.InventoryCloseEvent;
/**
 * Class used to listen to clicks on the Calendar menu and process them
 * 
 * @author roinujnosde
 */
public class CalendarListener implements Listener {

    private final CryptoMarket plugin;
    private final Configuration config;

    public CalendarListener(CryptoMarket plugin) {
        this.plugin = Objects.requireNonNull(plugin);
        config = new Configuration(plugin);
    }

    @EventHandler
    public void onInventoryClickEvent(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        if (!Helper.isCustomInventory(event)) {
            return;
        }

        if (event.getView().getTitle().equals(
                config.getCalendarMenuName())) {
            event.setCancelled(true);

            if (processBackButton(event, player)) {
                return;
            }
            if (processPreviousMonthButton(event, player)) {
                return;
            }
            processNextMonthButton(event, player);
        }

    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String name = event.getView().getTitle();
        if (name == null || !name.equals(config.getCalendarMenuName())) {
            return;
        }

        Player player = (Player) event.getPlayer();
        Calendar.remove(player);
    }

    private boolean processBackButton(InventoryClickEvent event, Player player) {
        if (event.getCurrentItem().getItemMeta().getDisplayName().equals(
                config.getCalendarMenuBackButtonName())) {
            player.closeInventory();
            Menu menu = new Menu(plugin, player);
            menu.open();
            return true;
        }

        return false;
    }

    private boolean processNextMonthButton(InventoryClickEvent event, Player player) {
        if (event.getCurrentItem().getItemMeta().getDisplayName().equals(
                config.getCalendarMenuNextMonthButtonName())) {
            Calendar c = new Calendar(plugin, player);
            c.nextMonth();
            return true;
        }
        return false;
    }

    private boolean processPreviousMonthButton(InventoryClickEvent event, Player player) {
        if (event.getCurrentItem().getItemMeta().getDisplayName().equals(
                config.getCalendarMenuPreviousMonthButtonName())) {
            Calendar c = new Calendar(plugin, player);
            c.previousMonth();
            return true;
        }
        return false;
    }
}
