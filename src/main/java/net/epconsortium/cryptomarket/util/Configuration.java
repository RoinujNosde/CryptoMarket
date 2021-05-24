package net.epconsortium.cryptomarket.util;

import net.epconsortium.cryptomarket.CryptoMarket;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class Configuration {

    private final CryptoMarket plugin;
    private static final Set<String> COINS = new HashSet<>();

    public Configuration(CryptoMarket plugin) {
        this.plugin = Objects.requireNonNull(plugin);
    }

    /**
     * Returns the Plugin config
     *
     * @return the config
     */
    private FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    /**
     * Colors the String and returns it
     *
     * @param path path
     * @param def def
     * @return the colored String
     */
    private String getColoredString(String path, String def) {
        return ChatColor.translateAlternateColorCodes('&',
                getConfig().getString(path, def));
    }

    /**
     * Returns the API KEY
     *
     * @return the key
     */
    public String getApiKey() {
        return getConfig().getString("api-key", "99X0JFXBLX2YRZA7");
    }

    /**
     * Checks if MySQL is enabled
     *
     * @return true if enabled
     */
    public boolean isMySQLEnabled() {
        return getConfig().getBoolean("mysql.enabled", false);
    }

    /**
     * Returns MySQL hostname
     *
     * @return hostname
     */
    public String getMySQLHostname() {
        return getConfig().getString("mysql.hostname", "localhost");
    }

    /**
     * Returns MySQL port
     *
     * @return port
     */
    public int getMySQLPort() {
        return getConfig().getInt("mysql.port", 3306);
    }

    /**
     * Returns the database name
     *
     * @return database name
     */
    public String getMySQLDatabaseName() {
        return getConfig().getString("mysql.database", "cryptomarket");
    }

    /**
     * Returns MySQL user
     *
     * @return user
     */
    public String getMySQLUser() {
        return getConfig().getString("mysql.user", "root");
    }

    /**
     * Returns MySQL password
     *
     * @return password
     */
    public String getMySQLPassword() {
        return getConfig().getString("mysql.password", "");
    }

    /**
     * Returns the interval to update the Exchange Rates in server ticks
     *
     * @return the interval
     */
    public long getIntervalExchangeRatesUpdateInTicks() {
        final int maxRequests = 500;
        final int dayInMinutes = 1440;
        double requests = getCoins().size() * 2;

        double interval = getConfig().getInt("update-interval", 60);
        double minInterval = dayInMinutes / (maxRequests / requests);

        if (minInterval > interval) {
            interval = Math.ceil(minInterval);
            CryptoMarket.warn("Update interval set to " + interval + ""
                    + " to obey the API limit!");
        }

        return (long) (interval * 60 * 20);
    }

    /**
     * Returns the interval to save the investors' data in server ticks
     *
     *
     * @return the interval
     */
    public long getIntervalSavingInvestorsInTicks() {
        return getConfig().getLong("saving-interval", 10) * 60 * 20;
    }

    /**
     * Returns the interval to update the Exchange Rates in milliseconds
     *
     * @return the interval
     */
    public long getIntervalExchangeRatesUpdateInMillis() {
        return getIntervalExchangeRatesUpdateInTicks() / 20 * 1000;
    }

    /**
     * Returns the richers update interval in ticks
     *
     * @return the interval
     */
    public long getIntervalRichersUpdateInTicks() {
        return getConfig().getLong("richers-update-interval", 15) * 60 * 20;
    }

    /**
     * Returns the physical currency to compare against the cryptocoins
     *
     * @return the physical currency
     */
    public String getPhysicalCurrency() {
        return getConfig().getString("physical-currency", "USD").toUpperCase();
    }

    /**
     * Returns the coins list. Only valid coins are returned, if none is valid,
     * BTC will be added to the list
     *
     * @return the coins
     */
    public List<String> getCoins() {
        List<String> coins = getConfig().getStringList("coins");
        if (coins != null) {
            coins.removeIf(m -> !isValid(m));
        } else {
            coins = new ArrayList<>();
        }
        if (coins.isEmpty()) {
            coins.add("BTC");
        }
        return coins;
    }

    /**
     * Checks if the argument is a valid cryptocoin
     *
     * @param coin coin
     * @return true if valid
     */
    public boolean isValid(String coin) {
        Objects.requireNonNull(coin);

        BufferedReader reader = new BufferedReader(new InputStreamReader(
                plugin.getResource("digital_currency_list.csv")));
        if (COINS.isEmpty()) {
            try {
                boolean first = true;
                while (reader.ready()) {
                    String line = reader.readLine();
                    String[] strings = line.split(",");
                    if (first) {
                        first = false;
                        continue;
                    }
                    COINS.add(strings[0]);
                }
            } catch (IOException ex) {
                CryptoMarket.warn("Error reading the currency list file");
                ex.printStackTrace();
            }
        }
        return COINS.contains(coin);
    }

    /**
     * Returns the word the user must type in order to leave the Negotiation
     * Chat
     *
     * @return the exit word
     */
    public String getConversationWordOfExit() {
        return getColoredString("negotiation-chat.exit-word", "quit");
    }

    /**
     * Returns the chat prefix of the negotiation chat
     *
     * @return prefix
     */
    public String getNegotiationChatPrefix() {
        return getColoredString("negotiation-chat.prefix", "[CriptoMarket] ");
    }

    /**
     * Returns the message of selling coins
     *
     * @return the message
     */
    public String getMessageSellNegotiation() {
        return getColoredString("negotiation-chat.how-much-sell",
                "How much do you want to sell?");
    }

    /**
     * Returns the message of buying coins
     *
     * @return the message
     */
    public String getMessageBuyNegotiation() {
        return getColoredString("negotiation-chat.how-much-buy",
                "How much do you want to buy?");
    }

    /**
     * Returns the connecting to database error message
     *
     * @return the message
     */
    public String getMessageErrorConnectingToDatabase() {
        return getColoredString("messages.error-database",
                "There has been an error connecting to the database, "
                + "contact an admin, please!");
    }

    /**
     * Returns the insufficient balance error message
     *
     * @return the message
     */
    public String getMessageErrorInsufficientBalance() {
        return getColoredString("messages.error-take-insufficient-balance",
                "&fThe player does not have enough balance!");
    }

    /**
     * Returns the invalid value error message
     *
     * @return the message
     */
    public String getMessageErrorInvalidValue() {
        return getColoredString("negotiation-chat.error-invalid-value",
                "Please, insert a valid value!");
    }

    /**
     * Returns the invalid coin error message
     *
     * @return the message
     */
    public String getMessageErrorInvalidCoin() {
        return getColoredString("messages.error-invalid-coin",
                "&fThe inserted coin is not valid!");
    }

    /**
     * Returns the player's balance updated message
     *
     * @return the message
     */
    public String getMessagePlayerBalanceUpdated() {
        return getColoredString("messages.player-balance-updated",
                "&f{0}'s balance updated!");
    }

    /**
     * Returns the insufficient balance error message
     *
     * @return the message
     */
    public String getMessageErrorNotEnoughBalance() {
        return getColoredString("negotiation-chat.error-insufficient-balance",
                "You don't have enough balance!");
    }

    /**
     * Returns the no permission error message
     *
     * @return the message
     */
    public String getMessageErrorNoPermission() {
        return getColoredString("messages.error-no-permission",
                "&fYou don't have permission to do this!");
    }

    /**
     * Returns the valid coins list message
     *
     * @return the message
     */
    public String getMessageValidCoins() {
        return getColoredString("negotiation-chat.valid-coins",
                "Valid coins: {0}");
    }

    /**
     * Returns the choose a coin to negotiate message
     *
     * @return the message
     */
    public String getMessageChooseCoin() {
        return getColoredString("negotiation-chat.choose-coin",
                "Which coin do you want to negotiate?");
    }

    /**
     * Returns the new balance message
     *
     * @return the message
     */
    public String getMessageNewBalance() {
        return getColoredString("messages.new-balance",
                "Your new {0} balance is: {1}");
    }

    /**
     * Returns the player not found error message
     *
     * @return the message
     */
    public String getMessageErrorPlayerNotFound() {
        return getColoredString("messages.error-player-not-found",
                "&fThe inserted player was not found!");
    }

    /**
     * Returns the invalid amount error message
     *
     * @return the message
     */
    public String getMessageErrorInvalidAmount() {
        return getColoredString("messages.error-invalid-amount",
                "&fThe inserted amount is not a valid number or is equal"
                + " or less than zero!");
    }

    /**
     * Returns the name of the main menu
     *
     * @return menu name
     */
    public String getMenuName() {
        return getColoredString("menu.name", "CryptoMarket");
    }

    /**
     * Returns name of the main menu's Coins button
     *
     * @return button name
     */
    public String getButtonCoinsName() {
        return getColoredString("menu.items.coins.name", "Coins / Values");
    }

    /**
     * Returns the lore of the main menu's Coins button
     *
     * @return button lore
     */
    public List<String> getButtonCoinsLore() {
        List<String> lore = getConfig().getStringList("menu.items.coins.lore");
        if (lore == null) {
            lore = new ArrayList<>();
        }
        if (lore.isEmpty()) {
            lore.add("{0}");
        }

        ArrayList<String> newLore = new ArrayList<>();
        for (String s : lore) {
            newLore.add(ChatColor.translateAlternateColorCodes('&', s));
        }
        return newLore;
    }

    /**
     * Returns the main menu's Coins button's coin line
     *
     * @return coin line
     */
    public String getButtonCoinsCoinLine() {
        return getColoredString("menu.items.coins.coin-line", "{0} {1}");
    }

    /**
     * Returns the main menu's Wallet button's coin line
     *
     * @return coin line
     */
    public String getButtonWalletCoinLine() {
        return getColoredString("menu.items.wallet.coin-line", "{0} {1}");
    }

    /**
     * Returns the main menu's Profit button's coin line
     *
     * @return coin line
     */
    public String getButtonProfitCoinLine() {
        return getColoredString("menu.items.profit.coin-line", "{0} {1}{2}%");
    }

    /**
     * Returns the main menu's Profit button's negative color
     *
     * @return the color
     */
    public String getButtonProfitNegativeColor() {
        return getColoredString("menu.items.profit.color.negative", "&c");
    }
    
     /**
     * Returns the main menu's Profit button's neuter color
     * 
     * @return the color
     */
    public String getButtonProfitNeuterColor() {
        return getColoredString("menu.items.profit.color.neuter", "&f");
    }

    /**
     * Returns the main menu's Profit button's positive color
     *
     * @return the color
     */
    public String getButtonProfitPositiveColor() {
        return getColoredString("menu.items.profit.color.positive", "&a");
    }

    /**
     * Returns the lore of the main menu's Profit button
     *
     * @return button lore
     */
    public List<String> getButtonProfitLore() {
        List<String> lore = getConfig().getStringList("menu.items.profit.lore");
        if (lore == null) {
            lore = new ArrayList<>();
        }
        if (lore.isEmpty()) {
            lore.add("{0}");
        }
        ArrayList<String> newLore = new ArrayList<>();
        for (String s : lore) {
            newLore.add(ChatColor.translateAlternateColorCodes('&', s));
        }
        return newLore;
    }

    /**
     * Returns the lore of the main menu's Wallet button
     *
     * @return button lore
     */
    public List<String> getButtonWalletLore() {
        List<String> lore = getConfig().getStringList("menu.items.wallet.lore");
        if (lore == null) {
            lore = new ArrayList<>();
        }
        if (lore.isEmpty()) {
            lore.add("{0}");
            lore.add("{1}");
        }
        ArrayList<String> newLore = new ArrayList<>();
        for (String s : lore) {
            newLore.add(ChatColor.translateAlternateColorCodes('&', s));
        }
        return newLore;
    }

    /**
     * Returns name of the main menu's Wallet button
     *
     * @return button name
     */
    public String getButtonWalletName() {
        return getColoredString("menu.items.wallet.name", "Wallet");
    }

    /**
     * Returns name of the main menu's Profit button
     *
     * @return button name
     */
    public String getButtonProfitName() {
        return getColoredString("menu.items.profit.name", "Profit selling now");
    }

    /**
     * Returns name of the main menu's Calendar button
     *
     * @return button name
     */
    public String getButtonCalendarName() {
        return getColoredString("menu.items.calendar", "Calendar");
    }

    /**
     * Returns name of the main menu's Update button
     *
     * @return button name
     */
    public String getButtonUpdateName() {
        return getColoredString("menu.items.update", "Update");
    }

    /**
     * Returns the content is already up-to-date message
     *
     * @return the message
     */
    public String getMessageContentAlreadyUptodate() {
        return getColoredString("messages.already-uptodate",
                "This is not necessary, the content is already up-to-date!");
    }

    /**
     * Returns the updating content message
     *
     * @return the message
     */
    public String getMessageUpdatingContent() {
        return getColoredString("messages.updating-content",
                "Updating content, please, wait {0} minute(s)!");
    }

    /**
     * Returns the name of the Buy action
     *
     * @return the name
     */
    public String getActionBuy() {
        return getColoredString("negotiation-chat.buy-action", "buy");
    }

    /**
     * Returns the name of the Sell action
     *
     * @return the name
     */
    public String getActionSell() {
        return getColoredString("negotiation-chat.sell-action", "sell");
    }

    /**
     * Returns the confirmation prompt message
     *
     * @return the message
     */
    public String getMessageNegotiationConfirmation() {
        return getColoredString("negotiation-chat.confirmation",
                "Do you want {0} {1} {2} for {3} coins?");
    }

    /**
     * Returns the confirmation word of the negotiation
     *
     * @return word
     */
    public String getNegotiationYesWord() {
        return getConfig().getString("negotiation-chat.yes-word", "yes");
    }

    /**
     * Returns the cancellation word of the negotiation
     *
     * @return word
     */
    public String getNegotiationNoWord() {
        return getConfig().getString("negotiation-chat.cancel-word", "cancel");
    }

    /**
     * Returns the successful negotiation message
     *
     * @return the message
     */
    public String getMessageSuccessfulNegotiation() {
        return getColoredString("negotiation-chat.success",
                "Successful negotiation!");
    }

    /**
     * Returns the how to leave the chat warning message
     *
     * @return the messagee
     */
    public String getMessageExitWarning() {
        return getColoredString("negotiation-chat.warning",
                "To exit the negotiation, type {0}");
    }

    /**
     * Returns the outdated data error message
     *
     * @return the message
     */
    public String getMessageOutdatedData() {
        return getColoredString("negotiation-chat.error-outdated-data",
                "The negotiation failed: the data is outdated! Please use the"
                + " Update button/command!");
    }

    /**
     * Returns the Calendar menu name
     *
     * @return the name
     */
    public String getCalendarMenuName() {
        return getColoredString("calendar.name", "Calendar");
    }

    /**
     * Returns the Calendar menu's Back button name
     *
     * @return the name
     */
    public String getCalendarMenuBackButtonName() {
        return getColoredString("calendar.items.back", "Back");
    }

    /**
     * Returns the Calendar menu's Previous Month button name
     *
     * @return the name
     */
    public String getCalendarMenuPreviousMonthButtonName() {
        return getColoredString("calendar.items.previous-month",
                "Previous Month");
    }

    /**
     * Returns the Calendar menu's Next Month button name
     *
     * @return the name
     */
    public String getCalendarMenuNextMonthButtonName() {
        return getColoredString("calendar.items.next-month", "Next Month");
    }

    /**
     * Returns the main menu's Ranking button name
     *
     * @return the name
     */
    public String getButtonRankingName() {
        return getColoredString("menu.items.ranking", "Ranking");
    }

    /**
     * Returns the Ranking menu's Back button name
     *
     * @return the name
     */
    public String getRankingMenuBackButton() {
        return getColoredString("ranking.items.back", "Back");
    }

    /**
     * Returns the Ranking menu name
     *
     * @return the name
     */
    public String getRankingMenuName() {
        return getColoredString("ranking.name", "Ranking");
    }

    /**
     * Returns the messages of the help command
     *
     * @return the messages
     */
    public List<String> getHelpCommandMessages() {
        List<String> fromConfig
                = getConfig().getStringList("messages.help-command");

        List<String> colored = new ArrayList<>();
        for (String a : fromConfig) {
            colored.add(ChatColor.translateAlternateColorCodes('&', a));
        }

        return colored;
    }

    /**
     * Returns the Balance command message
     *
     * @return the message
     */
    public String getMessageBalance() {
        return getColoredString("messages.balance", "Your balance is: ");
    }

    /**
     * Returns the per coin message of the Balance command
     *
     * @return the message
     */
    public String getMessageBalancePerCoin() {
        return getColoredString("messages.balance-per-coin", "&a{0}: &f{1}");
    }

    /**
     * Returns the saving data message of the Save command
     *
     * @return the message
     */
    public String getMessageSavingData() {
        return getColoredString("messages.saving-data", "&fSaving data...");
    }

    /**
     * Return the per coin message of the Today command
     *
     * @return the message
     */
    public String getMessageCurrentExchangeRatePerCoin() {
        return getColoredString("messages.current-rates-per-coin", "&a{0}: &f{1}");
    }

    /**
     * Returns the current exchange rate of the Today command
     *
     * @return the message
     */
    public String getMessageCurrentExchangeRate() {
        return getColoredString("messages.current-rates", "The coins are worth: ");
    }

    /**
     * Returns the lore of the Day item of the Calendar
     *
     * @return the lore
     */
    public List<String> getLoreOfTheDayItem() {
        List<String> lore = getConfig().getStringList("calendar.items.day.lore");
        if (lore == null) {
            lore = new ArrayList<>();
        }

        if (lore.isEmpty()) {
            lore.add("{0}");
        }

        //Coloring
        List<String> newLore = new ArrayList<>();
        for (String s : lore) {
            newLore.add(ChatColor.translateAlternateColorCodes('&', s));
        }

        return newLore;
    }

    /**
     * Returns the Calendar menu's Day item's coin line
     *
     * @return the coin line
     */
    public String getCalendarMenuCoinLine() {
        return getColoredString("calendar.items.day.coin-line", "{0} {1}");
    }

    /**
     * Returns the Calendar menu's Day item's no rates message
     *
     * @return the message
     */
    public String getCalendarMenuNoExchangeRate() {
        return getColoredString("calendar.items.day.no-rates", "No data");
    }

    /**
     * Returns the outdated data message
     *
     * @return the message
     */
    public String getMessageCommandOutdatedData() {
        return getColoredString("messages.outdated-data",
                "Outdated data, please update!");
    }

    /**
     * Returns the Calendar menu's Day item name
     *
     * @return the name
     */
    public String getCalendarMenuDayItemName() {
        return getColoredString("calendar.items.day.name", "Day {0}");
    }

    /**
     * Returns the Ranking menu's Total Investments item name
     *
     * @return the name
     */
    public String getRankingMenuTotalInvestmentsItemName() {
        return getColoredString("ranking.items.total-investments.name",
                "&fTotal investments");
    }

    /**
     * Returns the Ranking menu's Richer item name
     *
     * @return the name
     */
    public String getRankingMenuRicherItemName() {
        return getColoredString("ranking.items.richer.name", "{0} {1}");
    }

    /**
     * Returns the Ranking menu's Richer item name
     *
     * @return the name
     */
    public String getRankingMenuLastUpdatedItemName() {
        return getColoredString("ranking.items.last-updated.name",
                "&fLast updated");
    }

    /**
     * Returns the lore of the Total Investments item of the Ranking menu
     *
     * @return the lore
     */
    public List<String> getRankingMenuLastUpdatedItemLore() {
        List<String> lore = getConfig().getStringList("ranking.items."
                + "last-updated.lore");
        if (lore == null) {
            lore = new ArrayList<>();
        }

        if (lore.isEmpty()) {
            lore.add("{0}");
        }

        List<String> newLore = new ArrayList<>();
        for (String s : lore) {
            newLore.add(ChatColor.translateAlternateColorCodes('&', s));
        }
        return newLore;
    }

    /**
     * Returns the lore of the Total Investments item of the Ranking menu
     *
     * @return the lore
     */
    public List<String> getRankingMenuTotalInvestmentsItemLore() {
        List<String> lore = getConfig().getStringList("ranking.items."
                + "total-investments.lore");
        if (lore == null) {
            lore = new ArrayList<>();
        }

        if (lore.isEmpty()) {
            lore.add("{0}");
        }

        List<String> newLore = new ArrayList<>();
        for (String s : lore) {
            newLore.add(ChatColor.translateAlternateColorCodes('&', s));
        }
        return newLore;
    }

    /**
     * Returns the lore of the Richer item of the Ranking menu
     *
     * @return the lore
     */
    public List<String> getRankingMenuRicherItemLore() {
        List<String> lore = getConfig().getStringList("ranking.items.richer.lore");
        if (lore == null) {
            lore = new ArrayList<>();
        }

        if (lore.isEmpty()) {
            lore.add("{0}");
            lore.add("{1}%");
        }

        List<String> newLore = new ArrayList<>();
        for (String s : lore) {
            newLore.add(ChatColor.translateAlternateColorCodes('&', s));
        }
        return newLore;
    }

    /**
     * Returns the no investor message of the Richer item of the Ranking menu
     *
     * @return the message
     */
    public String getRankingMenuNoRicherLore() {
        return getColoredString("ranking.items.richer.no-investor",
                "{0} No investor");
    }

    /**
     * Returns the accessing ranking data error message
     *
     * @return the message
     */
    public String getMessageErrorAccessingRankingData() {
        return getColoredString("messages.error-ranking-data",
                "There has been an error accessing the Ranking data, "
                + "contact an admin, please!");
    }

    /**
     * Returns the no data error message of the Calendar menu's Day item
     *
     * @return the message
     */
    public String getItemDayError() {
        return getColoredString("calendar.items.day.error-no-data",
                "Error, update!");
    }

    /**
     * Returns the no data error message of the main menu's Coins item
     *
     * @return the message
     */
    public String getButtonCoinsError() {
        return getColoredString("menu.items.coins.error-no-data",
                "Error, update!");
    }
}
