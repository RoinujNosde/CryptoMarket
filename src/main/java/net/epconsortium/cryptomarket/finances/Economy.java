package net.epconsortium.cryptomarket.finances;

import net.epconsortium.cryptomarket.CryptoMarket;
import net.epconsortium.cryptomarket.util.Configuration;
import net.epconsortium.cryptomarket.util.Formatter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static net.epconsortium.cryptomarket.CryptoMarket.debug;
import net.epconsortium.cryptomarket.database.dao.InvestorDao;
import net.epconsortium.cryptomarket.database.dao.Investor;
import net.epconsortium.cryptomarket.util.Logger;

/**
 * Class used to perform operations like purchase, sell, etc.
 * Also contains some useful methods like total investments, list of richest investors...
 * 
 * @author roinujnosde
 */
public class Economy {

    private final CryptoMarket plugin;
    private final net.milkbowl.vault.economy.Economy economy;
    private final Configuration config;
    private final String coin;
    private final Logger logger;

    private static List<Investor> richers = new ArrayList<>();
    private static long richersUpdate = -1;
    private static double totalInvestments = 0;
    private static long totalInvestmentsUpdate = -1;

    public Economy(CryptoMarket plugin, String coin) {
        this.plugin = Objects.requireNonNull(plugin);
        config = new Configuration(plugin);
        economy = plugin.getEconomy();
        logger = new Logger(plugin);

        if (coin == null) {
            coin = "BTC";
        }
        this.coin = coin;
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
    public void withdraw(Investor investor, BigDecimal amount)
            throws IllegalArgumentException {
        Objects.requireNonNull(investor);
        Objects.requireNonNull(amount);

        if (amount.doubleValue() <= 0) {
            throw new IllegalArgumentException("amount cannot be equal or less "
                    + "than 0");
        }
        BigDecimal value = investor.getBalance(coin).getValue();
        if (!(has(investor, amount))) {
            throw new IllegalArgumentException("investor does not have enough "
                    + "balance");
        }
        value = value.subtract(amount);
        investor.getBalance(coin).setValue(value);
        sendNewBalance(investor, value);
    }

    /**
     * Sends the new balance to the Investor
     *
     * @param investor investor
     */
    private void sendNewBalance(Investor investor, BigDecimal newBalance) {
        Objects.requireNonNull(investor);
        Objects.requireNonNull(newBalance);

        debug("Sending new balance to Investor " + investor);
        debug("New balance: " + newBalance);
        Player player = Bukkit.getPlayer(investor.getPlayer().getUniqueId());
        String newBalanceMsg = config.getMessageNewBalance();
        newBalanceMsg = MessageFormat.format(newBalanceMsg, coin, Formatter
                .formatCryptocoin(newBalance));
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
    public void set(Investor investor, BigDecimal amount) {
        Objects.requireNonNull(investor);
        Objects.requireNonNull(amount);

        if (amount.doubleValue() < 0) {
            throw new IllegalArgumentException("amount cannot be negative");
        }
        investor.getBalance(coin).setValue(amount);

        sendNewBalance(investor, amount);
    }

    /**
     * Deposits the specified amount of the chosen cryptocoin in the investor's
     * account
     *
     * @param investor investor
     * @param amount amount to deposit
     * @throws IllegalArgumentException if amount is negative or equal to 0
     */
    public void deposit(Investor investor, BigDecimal amount)
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
        debug("Processing deposit of " + amount + " " + coin + ". New balance:"
                + " " + value);
        sendNewBalance(investor, value);
    }

    /**
     * Processes the purchase of cryptocoins and returns a boolean representing
     * the success of the operation
     *
     * @param investor investor
     * @param amount amount to buy
     * @return true if success
     */
    public boolean buy(Investor investor, BigDecimal amount) {
        Objects.requireNonNull(investor);
        Objects.requireNonNull(amount);

        debug("Processing the purchase of crypto for " + investor);
        debug("Amount: " + amount);
        double toPay = convert(amount).doubleValue();
        debug("To pay: " + toPay);
        if (economy.has(investor.getPlayer(), toPay)) {
            if (amount.doubleValue() > 0) {
                economy.withdrawPlayer(investor.getPlayer(), toPay);
                //deposit(investor, amount);
                investor.getBalance(coin).increase(amount,
                        new BigDecimal(toPay));
                logger.log(investor, Negotiation.PURCHASE, amount, coin,
                        toPay);
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
     * @return true if sucess
     */
    public boolean sell(Investor investor, BigDecimal amount) {
        Objects.requireNonNull(investor);
        Objects.requireNonNull(amount);

        debug("Processing the sell of crypto for " + investor);
        debug("Amount: " + amount);
        double toReceive = convert(amount).doubleValue();
        debug("To receive: " + toReceive);

        if (amount.doubleValue() > 0) {
            if (has(investor, amount)) {
                economy.depositPlayer(investor.getPlayer(), toReceive);
                //withdraw(investor, amount);
                investor.getBalance(coin).decrease(amount,
                        new BigDecimal(toReceive));
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
    public BigDecimal convert(BigDecimal amount) {
        Objects.requireNonNull(amount);

        ExchangeRate er = new ExchangeRates(plugin).getExchangeRate(
                LocalDate.now());
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
    public boolean transfer(Investor debited, Investor favored,
            BigDecimal amount) {
        if (!(amount.doubleValue() <= 0)) {
            if (has(debited, amount)) {
                withdraw(debited, amount);
                deposit(favored, amount);
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
    public boolean has(Investor investor, BigDecimal amount) {
        Objects.requireNonNull(investor);
        Objects.requireNonNull(amount);

        return investor.getBalance(coin).getValue().doubleValue()
                >= amount.doubleValue();
    }

    /**
     * Returns the total investments of the Investor converted to the server
     * currency
     *
     * @param investor investor
     * @return patrimony
     */
    @SuppressWarnings("LocalVariableHidesMemberVariable")
    public BigDecimal getConvertedPatrimony(Investor investor) {
        Objects.requireNonNull(investor);

        BigDecimal patrimony = new BigDecimal(0);
        for (String coin : investor.getBalances().keySet()) {
            patrimony = patrimony.add(new Economy(plugin, coin)
                    .convert(investor.getBalance(coin).getValue()));
        }

        return patrimony;
    }

    /**
     * Returns the total balance of cryptocoins on the server converted to the
     * server coin
     *
     * @param callback callback
     */
    public void getTotalInvestments(InvestmentsCallback callback) {
        Objects.requireNonNull(callback);

        if (totalInvestmentsUpdate == -1 || System.currentTimeMillis()
                > (totalInvestmentsUpdate + config
                        .getIntervalRichersUpdateInMillis())) {
            new InvestorDao(plugin).getInvestors((investors) -> {
                updateTotalInvestments(investors);
                callback.onTotalInvestmentsReady(totalInvestments);
            });
        } else {
            callback.onTotalInvestmentsReady(totalInvestments);
        }
    }

    /**
     * Updates the total balance of cryptocoins and its last update time
     *
     * @param investors investors to calculate, can be null
     */
    private void updateTotalInvestments(Set<Investor> investors) {
        if (investors != null) {
            totalInvestments = investors
                    .stream()
                    .map(this::getConvertedPatrimony)
                    .mapToDouble(BigDecimal::doubleValue)
                    .sum();
        } else {
            totalInvestments = -1;
        }
        totalInvestmentsUpdate = System.currentTimeMillis();
    }

    /**
     * Returns an ordered list of the richest investors. The size of list will
     * always be less or equal to the max. This method will return the previous
     * list in case the update interval has not passed yet...
     *
     * @param max max number of investors
     * @param callback callback
     * @throws IllegalArgumentException if max is equal or less than 0
     */
    public void getRichestInvestors(final int max,
            RichestInvestorsCallback callback) throws IllegalArgumentException {
        Objects.requireNonNull(callback);

        if (max < 1) {
            throw new IllegalArgumentException("max cannot be equal or less "
                    + "than 0");
        }
        //Checking if the update interval has passed or not
        if (richersUpdate == -1
                || (System.currentTimeMillis() - config
                .getIntervalRichersUpdateInMillis())
                >= richersUpdate) {

            //Getting all investors
            new InvestorDao(plugin).getInvestors((investors) -> {
                updateTotalInvestments(investors);

                if (investors == null) {
                    callback.onRichestInvestorsDataReady(null);
                    debug("Economy: investors list returned null");
                    return;
                }
                Economy eco = new Economy(plugin, "");
                debug("Economy: investors list empty? " + investors.isEmpty());
                richers = investors
                        .stream()
                        .sorted((i1, i2) -> (eco.getConvertedPatrimony(i1)
                        .compareTo(eco.getConvertedPatrimony(i2)) * -1))
                        .limit(max).collect(Collectors.toList());

                richersUpdate = System.currentTimeMillis();

                debug("Richers updated");
                callback.onRichestInvestorsDataReady(richers);
            });
        } else {
            debug("The interval has not passed,"
                    + " the previous list is going to be returned.");
            //todo: isto deveria levar em consideração o parâmetro max
            callback.onRichestInvestorsDataReady(richers);
        }
    }

    /**
     * Returns the time of the last update of the Richers list
     *
     * @return the last update
     */
    public static LocalDateTime getRichersLastUpdate() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(richersUpdate),
                ZoneId.systemDefault());
    }

    public static interface InvestmentsCallback {

        /**
         * Notifies when the total investments is ready
         *
         * @param total total investments
         */
        void onTotalInvestmentsReady(double total);
    }

    public static interface RichestInvestorsCallback {

        /**
         * Notifies when the ordered list of richest investors is ready
         *
         * @param investors richer investors, null if an error ocurred
         */
        void onRichestInvestorsDataReady(List<Investor> investors);
    }
}
