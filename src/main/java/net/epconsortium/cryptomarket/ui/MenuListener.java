package net.epconsortium.cryptomarket.ui;

import net.epconsortium.cryptomarket.CryptoMarket;
import net.epconsortium.cryptomarket.conversation.NegotiationConversation;
import net.epconsortium.cryptomarket.finances.ExchangeRates;
import net.epconsortium.cryptomarket.finances.Negotiation;
import net.epconsortium.cryptomarket.util.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.text.MessageFormat;
import java.util.Objects;

/**
 * Class used to listen to clicks on the Menu menu and process them
 *
 * @author roinujnosde
 */
public class MenuListener implements Listener {

    private final CryptoMarket plugin;
    private final Configuration config;

    public MenuListener(CryptoMarket plugin) {
        this.plugin = Objects.requireNonNull(plugin);
        config = new Configuration(plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        if (!Helper.isCustomInventory(event)) {
            return;
        }
        event.setCancelled(true);

        if (event.getView().getTitle().equals(config.getMenuName())) {
            if (processCoinsButton(event, player)) {
                return;
            }
            if (processUpdateButton(event, player)) {
                return;
            }
            if (processRankingButton(event, player)) {
                return;
            }
            processCalendarButton(event, player);
        }
    }

    private boolean processUpdateButton(InventoryClickEvent event, Player player) {
        if (event.getCurrentItem().getItemMeta().getDisplayName().equals(config.getButtonUpdateName())) {
            if (!player.hasPermission("cryptomarket.update")) {
                player.sendMessage(config.getMessageErrorNoPermission());
                player.closeInventory();
                return true;
            }
            if (ExchangeRates.errorOcurred()) {
                ExchangeRates er = new ExchangeRates(plugin);
                er.updateAll();
                player.closeInventory();

                String msg = config.getMessageUpdatingContent();
                msg = MessageFormat.format(msg, er.getMinutesToUpdate());
                player.sendMessage(msg);
                return true;
            } else {
                player.sendMessage(config.getMessageContentAlreadyUptodate());
                player.closeInventory();
                return true;
            }
        }
        return false;
    }

    private boolean processCoinsButton(InventoryClickEvent event, Player player) {
        if (event.getCurrentItem().getItemMeta().getDisplayName()
                .equals(config.getButtonCoinsName())) {
            if (!player.hasPermission("cryptomarket.negotiate")) {
                player.sendMessage(config.getMessageErrorNoPermission());
                player.closeInventory();
                return true;
            }
            if (event.getClick().isLeftClick()) {
                NegotiationConversation conv = new NegotiationConversation(
                        plugin, Negotiation.SELL, player);
                conv.start();
                player.closeInventory();
                return true;
            }

            if (event.getClick().isRightClick()) {
                NegotiationConversation conv = new NegotiationConversation(
                        plugin, Negotiation.PURCHASE, player);
                conv.start();
                player.closeInventory();
                return true;
            }
        }
        return false;
    }

    private boolean processRankingButton(InventoryClickEvent event, Player player) {
        if (event.getCurrentItem().getItemMeta().getDisplayName()
                .equals(config.getButtonRankingName())) {
            if (!player.hasPermission("cryptomarket.ranking")) {
                player.sendMessage(config.getMessageErrorNoPermission());
                player.closeInventory();
                return true;
            }
            Ranking ranking = new Ranking(plugin, player);
            ranking.open();
            return true;
        }
        return false;
    }

    private boolean processCalendarButton(InventoryClickEvent event, Player player) {
        if (event.getCurrentItem().getItemMeta().getDisplayName()
                .equals(config.getButtonCalendarName())) {
            if (!player.hasPermission("cryptomarket.calendar")) {
                player.sendMessage(config.getMessageErrorNoPermission());
                player.closeInventory();
                return true;
            }
            Calendar calendar = new Calendar(plugin, player);
            calendar.open();
            return true;
        }
        return false;
    }
}
