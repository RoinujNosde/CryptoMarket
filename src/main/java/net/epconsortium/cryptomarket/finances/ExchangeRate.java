package net.epconsortium.cryptomarket.finances;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Class used to store the exchange rates of all coins on a day
 * 
 * @author roinujnosde
 */
public class ExchangeRate {

    private final Map<String, BigDecimal> values;

    public ExchangeRate() {
        values = new HashMap<>();
    }

    /**
     * Returns the coin value
     *
     * @param coin coin symbol
     * @return the value, or -1, if there is not data
     */
    public BigDecimal getCoinValue(String coin) {
        Objects.requireNonNull(coin);
        coin = coin.toUpperCase();
        BigDecimal decimal = values.get(coin);
        if (decimal == null) {
            decimal = new BigDecimal(-1);
        }
        return decimal;
    }

    /**
     * Updates the coin value
     * 
     * @param coin coin
     * @param value new value
     */
    public void update(String coin, BigDecimal value) {
        Objects.requireNonNull(coin);
        values.put(coin, value);
    }
}
