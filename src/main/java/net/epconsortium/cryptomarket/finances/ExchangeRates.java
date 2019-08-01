package net.epconsortium.cryptomarket.finances;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.epconsortium.cryptomarket.CryptoMarket;
import net.epconsortium.cryptomarket.util.Configuration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;

import static net.epconsortium.cryptomarket.CryptoMarket.warn;

/**
 * Class used to have access to the exchange rates of the cryptocoins
 * 
 */
public class ExchangeRates {

    private final CryptoMarket plugin;
    private final Configuration config;

    private static int requests = 0;
    private static final String USER_AGENT = "Mozilla/5.0";
    private static final Map<LocalDate, ExchangeRate> RATES = new HashMap<>();
    private static LocalDate lastCurrentDay = LocalDate.now();
    
    private static boolean dailyError;
    private static boolean currentError;

    public ExchangeRates(CryptoMarket plugin) {
        this.plugin = Objects.requireNonNull(plugin);
        config = new Configuration(plugin);
    }

    /**
     * Updates all exchange rates and allocates them on memory
     */
    public void updateAll() {
        //Reseting the errors
        setCurrentError(false);
        setDailyError(false);
        //Updating
        updateCurrentExchangeRate();
        updateDailyRates();
    }

    /**
     * Updates today's rate
     */
    public void updateCurrentExchangeRate() {
        new BukkitRunnable() {
            @Override
            @SuppressWarnings("UseSpecificCatch")
            public void run() {
                boolean error = false;
                for (String coin : config.getCoins()) {
                    awaitServerLimit();
                    try {
                        HttpURLConnection connection
                                = openHttpConnection(getExchangeRateUrl(coin));

                        int responseCode = connection.getResponseCode();
                        if (responseCode >= 200 && responseCode <= 299) {
                            JsonObject json = extractJsonFrom(connection);

                            BigDecimal exchangeRate = json
                                    .get("Realtime Currency Exchange Rate")
                                    .getAsJsonObject()
                                    .get("5. Exchange Rate").getAsBigDecimal();

                            LocalDate date = LocalDate.now();
                            ExchangeRate er = getExchangeRate(date);
                            if (er == null) {
                                er = new ExchangeRate();
                            }

                            er.update(coin, exchangeRate);
                            RATES.put(date, er);
                        } else {
                            error = true;
                        }
                    } catch (Exception e) {
                        warn("Error updating the coins values. "
                                + "Wait a few minutes and try again!");
                        error = true;
                    }
                }
                setCurrentError(error);
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Updates previous days' rates
     */
    private void updateDailyRates() {
        new BukkitRunnable() {
            @Override
            @SuppressWarnings("UseSpecificCatch")
            public void run() {
                HashMap<String, Boolean> errors = new HashMap<>();
                for (String coin : config.getCoins()) {
                    awaitServerLimit();
                    try {
                        URL url = getCurrencyDailyUrl(coin);
                        HttpURLConnection connection = openHttpConnection(url);

                        int responseCode = connection.getResponseCode();
                        if (responseCode >= 200 && responseCode <= 299) {
                            JsonObject json = extractJsonFrom(connection);

                            JsonObject jo = json.getAsJsonObject(
                                    "Time Series (Digital Currency Daily)");
                            Set<Map.Entry<String, JsonElement>> entries
                                    = jo.entrySet();

                            entries.forEach(entry -> {
                                LocalDate date
                                        = LocalDate.parse(entry.getKey());
                                BigDecimal value = entry.getValue()
                                        .getAsJsonObject()
                                        .get("4a. close ("
                                                + config.getPhysicalCurrency()
                                                + ")")
                                        .getAsBigDecimal();
                                ExchangeRate er = getExchangeRate(date);
                                if (er == null) {
                                    er = new ExchangeRate();
                                }
                                er.update(coin, value);
                                RATES.put(date, er);
                            });
                        } else {
                            errors.put(coin, true);
                        }
                    } catch (Exception ex) {
                        warn("Error updating the coins values. "
                                + "Wait a few minutes and try again!");
                        errors.put(coin, true);
                    }
                }
                for (Map.Entry<String, Boolean> entry : errors.entrySet()) {
                    if (entry.getValue()) {
                        setDailyError(true);
                        return;
                    }
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Returns the URL to the ExchangeRate function
     *
     * @param coin coin
     * @return the URL
     * @throws MalformedURLException
     */
    private URL getExchangeRateUrl(String coin) throws MalformedURLException {
        URL url = new URL("https://www.alphavantage.co/query?function="
                + "CURRENCY_EXCHANGE_RATE&from_currency=" + coin
                + "&to_currency=" + config.getPhysicalCurrency()
                + "&apikey=" + config.getApiKey());
        return url;
    }

    /**
     * Returns the URL to the CurrencyDaily function
     *
     * @param coin coin
     * @return the URL
     * @throws MalformedURLException
     */
    private URL getCurrencyDailyUrl(String coin) throws MalformedURLException {
        URL url = new URL("https://www.alphavantage.co/query?function="
                + "DIGITAL_CURRENCY_DAILY&symbol=" + coin + "&market="
                + config.getPhysicalCurrency() + "&apikey="
                + config.getApiKey());
        return url;
    }

    /**
     * Returns the Exchange Rate from the date
     *
     * @param date date
     * @return Exchange Rate, or null if an error ocurred
     * @throws IllegalArgumentException if date is after today
     */
    public ExchangeRate getExchangeRate(LocalDate date) throws
            IllegalArgumentException {
        Objects.requireNonNull(date);
        final LocalDate now = LocalDate.now();
        if (date.isAfter(now)) {
            throw new IllegalArgumentException("date cannot be after today");
        }
        if (now.isAfter(lastCurrentDay)) {
            setCurrentError(true);
            lastCurrentDay = now;
        }
        
        return RATES.get(date);
    }

    /**
     * Returns an unmodifiable map of the Exchange Rates
     *
     * @return as exchange rates
     */
    public static Map<LocalDate, ExchangeRate> getExchangeRates() {
        return Collections.unmodifiableMap(RATES);
    }

    /**
     * Causes the Async Thread to sleep so that the 5 requests per minute limit
     * (of the AlphaVantage API) is not trespassed
     */
    @SuppressWarnings("SleepWhileHoldingLock")
    private static synchronized void awaitServerLimit() {
        if (requests < 5) {
            //Requests are less than 5, so you don't need to sleep
            requests++;
            return;
        }
        //Requests exceeded, sleeping...
        try {
            Thread.sleep(70 * 1000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        //Reseting requests count
        requests = 1;
    }

    /**
     * Returns the connection response in a JsonObject
     *
     * @param connection connection
     * @return json object
     * @throws IOException if an error ocurred reading the response
     */
    private JsonObject extractJsonFrom(HttpURLConnection connection) throws IOException {
        JsonObject json;
        try (InputStream in = connection.getInputStream()) {
            Gson gson = new Gson();
            json = gson.fromJson(new InputStreamReader(in), JsonObject.class
            );
        }

        return json;
    }

    /**
     * Opens and returns a HTTP connection
     *
     * @param url url
     * @return HTTP connection
     * @throws IOException if an error happened on the connection
     */
    private HttpURLConnection openHttpConnection(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", USER_AGENT);
        return connection;
    }

    /**
     * Returns the amount of minutes the plugin takes to update the Exchange
     * Rates
     *
     * @return the minutes
     */
    public int getMinutesToUpdate() {
        double minutes = config.getCoins().size();
        minutes = minutes * 2 / 5;

        return (int) Math.ceil(minutes);
    }

    /**
     * Verifies if an error happened during an update
     *
     * @return true if an error ocurred
     */
    public static boolean errorOcurred() {
        return dailyError || currentError;
    }

    /**
     * Sets if an error ocurred during the daily exchange rate update
     * 
     * @param error true if ocurred
     */
    private void setDailyError(boolean error) {
        dailyError = error;
    }
    
    /**
     * Sets if an error ocurred during the current exchange rate update
     * 
     * @param error true if ocurred
     */
    private void setCurrentError(boolean error) {
        currentError = error;
    }
}
