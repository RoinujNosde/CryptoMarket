package net.epconsortium.cryptomarket.finances;

import net.epconsortium.cryptomarket.CryptoMarket;
import net.epconsortium.cryptomarket.database.dao.Investor;
import net.epconsortium.cryptomarket.util.Configuration;
import net.epconsortium.cryptomarket.util.Formatter;
import net.epconsortium.cryptomarket.util.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static net.epconsortium.cryptomarket.CryptoMarket.debug;

/**
 * Class used to perform operations like purchase, sell, etc.
 * Also contains some useful methods like total investments, list of richest investors...
 * 
 * @author roinujnosde
 */
public class Economy {

    private static Economy instance;
    private final CryptoMarket plugin;
    private final net.milkbowl.vault.economy.Economy vaultEconomy;
    private final Configuration config;
    private final Logger logger;

    private List<Investor> investors = new ArrayList<>();
    private long richersUpdate = -1;
    private double totalInvestments = 0;

    private Economy(@NotNull CryptoMarket plugin) {
        this.plugin = plugin;
        config = new Configuration(plugin);
        vaultEconomy = plugin.getVaultEconomy();
        logger = new Logger(plugin);
    }

    public static Economy getInstance(@NotNull CryptoMarket plugin) {
        if (instance == null) {
            instance = new Economy(plugin);
        }
        return instance;
    }

    /**
     * Withdraws the specified amount of the chosen cryptocoin from the
     * investor's account
     *
     * @param investor investor
     * @param amount amount to withdraw
     * @throws IllegalArgumentException if amount is equal or less than 0 or
     * investor does not have the amount
     */
    public void withdraw(String coin, Investor investor, BigDecimal amount) throws IllegalArgumentException {
        Objects.requireNonNull(investor);
        Objects.requireNonNull(amount);

        if (amount.doubleValue() <= 0) {
            throw new IllegalArgumentException("amount cannot be equal or less "
                    + "than 0");
        }
        BigDecimal value = investor.getBalance(coin).getValue();
        if (!(has(coin, investor, amount))) {
            throw new IllegalArgumentException("investor does not have enough "
                    + "balance");
        }
        value = value.subtract(amount);
        investor.getBalance(coin).setValue(value);
        sendNewBalance(coin, investor, value);
    }

    /**
     * Sends the new balance to the Investor
     *
     * @param investor investor
     */
    private void sendNewBalance(String coin, Investor investor, BigDecimal newBalance) {
        Objects.requireNonNull(investor);
        Objects.requireNonNull(newBalance);

        debug("Sending new balance to Investor " + investor);
        debug("New balance: " + newBalance);
        Player player = Bukkit.getPlayer(investor.getPlayer().getUniqueId());
        String newBalanceMsg = config.getMessageNewBalance();
        newBalanceMsg = MessageFormat.format(newBalanceMsg, coin, Formatter.formatCryptocoin(newBalance));
        if (player != null) {
            player.sendMessage(newBalanceMsg);
        } else {
            debug(investor + "is not online");
        }
    }

    /**
     * Sets the specified amount as the investor's balance of the chosen
     * cryptocoin
     *
     * @param investor investor
     * @param amount new balance
     * @throws IllegalArgumentException if amount is negative or equal to 0
     */
    public void set(String coin, Investor investor, BigDecimal amount) {
        Objects.requireNonNull(investor);
        Objects.requireNonNull(amount);

        if (amount.doubleValue() < 0) {
            throw new IllegalArgumentException("amount cannot be negative");
        }
        investor.getBalance(coin).setValue(amount);

        sendNewBalance(coin, investor, amount);
    }

    /**
     * Deposits the specified amount of the chosen cryptocoin in the investor's
     * account
     *
     * @param investor investor
     * @param amount amount to deposit
     * @throws IllegalArgumentException if amount is negative or equal to 0
     */
    public void deposit(String coin, Investor investor, BigDecimal amount)
            throws IllegalArgumentException {
        Objects.requireNonNull(investor);
        Objects.requireNonNull(amount);

        if (amount.doubleValue() <= 0) {
            throw new IllegalArgumentException("amount cannot be negative "
                    + "or equal to 0");
        }
        BigDecimal value = investor.getBalance(coin).getValue();
        value = value.add(amount);
        investor.getBalance(coin).setValue(value);
        debug("Processing deposit of " + amount + " " + coin + ". New balance: " + value);
        sendNewBalance(coin, investor, value);
    }

    /**
     * Processes the purchase of cryptocoins and returns a boolean representing
     * the success of the operation
     *
     * @param investor investor
     * @param amount amount to buy
     * @return true if success
     */
    public boolean buy(String coin, Investor investor, BigDecimal amount) {
        Objects.requireNonNull(investor);
        Objects.requireNonNull(amount);

        debug("Processing the purchase of crypto for " + investor);
        debug("Amount: " + amount);
        double toPay = convert(coin, amount).doubleValue();
        debug("To pay: " + toPay);
        if (vaultEconomy.has(investor.getPlayer(), toPay)) {
            if (amount.doubleValue() > 0) {
                vaultEconomy.withdrawPlayer(investor.getPlayer(), toPay);
                //deposit(investor, amount);
                investor.getBalance(coin).increase(amount, new BigDecimal(toPay));
                logger.log(investor, Negotiation.PURCHASE, amount, coin, toPay);
                return true;
            } else {
                debug("amount is less than 0");
            }
        } else {
            debug(investor + " does not have enough balance.");
        }
        return false;
    }

    /**
     * Processes the sell of cryptocoins and returns a boolean representing the
     * success of the operation
     *
     * @param investor investor
     * @param amount amount to sell
     * @return true if success
     */
    public boolean sell(String coin, Investor investor, BigDecimal amount) {
        Objects.requireNonNull(investor);
        Objects.requireNonNull(amount);

        debug("Processing the sell of crypto for " + investor);
        debug("Amount: " + amount);
        double toReceive = convert(coin, amount).doubleValue();
        debug("To receive: " + toReceive);

        if (amount.doubleValue() > 0) {
            if (has(coin, investor, amount)) {
                vaultEconomy.depositPlayer(investor.getPlayer(), toReceive);
                //withdraw(investor, amount);
                investor.getBalance(coin).decrease(amount, new BigDecimal(toReceive));
                logger.log(investor, Negotiation.SELL, amount, coin, toReceive);
                return true;
            }
        }
        return false;
    }

    /**
     * Converts the amount of the crypto currency to the currency used on the
     * server
     *
     * @param amount amount to convert
     * @return value in the server currency
     */
    public BigDecimal convert(String coin, BigDecimal amount) {
        Objects.requireNonNull(amount);

        ExchangeRate er = plugin.getExchangeRates().getExchangeRate(LocalDate.now());
        if (er == null) {
            er = new ExchangeRate();
        }

        return er.getCoinValue(coin).multiply(amount);
    }

    /**
     * Transfers the specified amount from the debited to the favored
     *
     * @param debited who will pay
     * @param favored who will receive
     * @param amount amount
     * @return true if success
     */
    public boolean transfer(String coin, Investor debited, Investor favored, BigDecimal amount) {
        if (!(amount.doubleValue() <= 0)) {
            if (has(coin, debited, amount)) {
                withdraw(coin, debited, amount);
                deposit(coin, favored, amount);
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the investor has the amount passed in the parameter
     *
     * @param investor investor
     * @param amount amount
     * @return true if he does
     */
    public boolean has(String coin, Investor investor, BigDecimal amount) {
        Objects.requireNonNull(investor);
        Objects.requireNonNull(amount);

        return investor.getBalance(coin).getValue().doubleValue() >= amount.doubleValue();
    }

    /**
     * Returns the total balance of cryptocoins on the server converted to the
     * server coin
     *
     */
    public double getTotalInvestments() {
        return totalInvestments;
    }

    /**
     * Returns an ordered list of the richest investors. The size of list will
     * always be less or equal to the max. This method will return the previous
     * list in case the update interval has not passed yet...
     *
     * @param max max number of investors
     * @throws IllegalArgumentException if max is negative
     * @return the list of richest investors or empty if an error occurred
     */
    public @NotNull List<Investor> getTopInvestors(int max) throws IllegalArgumentException {
        if (max < 0) {
            throw new IllegalArgumentException("max cannot be negative");
        }
        if (max == 0) {
            max = investors.size();
        }

        ExchangeRate rate = plugin.getExchangeRates().getExchangeRate(LocalDate.now());
        if (rate == null) {
            return Collections.emptyList();
        }
        return investors.stream().sorted(Investor.comparator(rate)).limit(max).collect(Collectors.toList());
    }

    /**
     * @return the top 10 richest investors
     */
    public List<Investor> getTopInvestors() {
        return getTopInvestors(10);
    }

    public void setInvestors(@NotNull List<Investor> investors) {
        this.investors = investors;
        ExchangeRate rate = plugin.getExchangeRates().getExchangeRate(LocalDate.now());
        if (rate == null) {
            totalInvestments = 0;
        } else {
            totalInvestments = investors.stream().map(i -> i.getConvertedPatrimony(rate))
                    .mapToDouble(BigDecimal::doubleValue).sum();
        }
    }

    public void setRichersLastUpdate(long timestamp) {
        this.richersUpdate = timestamp;
    }

    /**
     * Returns the time of the last update of the Richers list
     *
     * @return the last update
     */
    public LocalDateTime getRichersLastUpdate() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(richersUpdate), ZoneId.systemDefault());
    }
}
