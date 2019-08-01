package net.epconsortium.cryptomarket.database.dao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Class used to represent the Balance of one coin
 * 
 * @author roinujnosde
 */
public class Balance {

    private BigDecimal totalPurchased;
    private BigDecimal totalPaid;

    Balance(BigDecimal totalPurchased, BigDecimal totalPaid)
            throws IllegalArgumentException {
        Objects.requireNonNull(totalPaid);
        Objects.requireNonNull(totalPurchased);

        if (totalPaid.doubleValue() < 0 || totalPurchased.doubleValue() < 0) {
            throw new IllegalArgumentException("amounts cannot be less than 0");
        }

        this.totalPaid = totalPaid;
        this.totalPurchased = totalPurchased;
    }

    /**
     * Returns the balance value
     *
     * @return the value
     */
    public BigDecimal getValue() {
        return totalPurchased;
    }

    /**
     * Returns the purchase average
     *
     * @return the average (totalPaid / totalPurchased)
     */
    public BigDecimal getPurchaseAverage() {
        if (totalPurchased.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return totalPaid.divide(totalPurchased, RoundingMode.FLOOR);
    }

    /**
     * Sets the value of the balance and maintains the average purchase returned
     * by {@link #getPurchaseAverage()}
     *
     * @param value the new value
     * @throws IllegalArgumentException if value is less than 0
     * @throws NullPointerException if value is null
     */
    public void setValue(BigDecimal value) throws IllegalArgumentException {
        Objects.requireNonNull(value);

        if (value.doubleValue() < 0) {
            throw new IllegalArgumentException("value cannot be less than 0");
        }

        if (value.compareTo(BigDecimal.ZERO) == 0) {
            totalPaid = BigDecimal.ZERO;
        } else {
            totalPaid = value.multiply(getPurchaseAverage());
        }
        totalPurchased = value;
    }

    /**
     * Decreases the balance value
     *
     * @param sold cryptocoin sold amount
     * @param received amount received in server coin
     * @throws IllegalArgumentException if sold is bigger than
     * {@link #getValue()}; if sold or received are less or equal to 0
     */
    public void decrease(BigDecimal sold, BigDecimal received)
            throws IllegalArgumentException {
        Objects.requireNonNull(sold);
        Objects.requireNonNull(received);

        if (sold.doubleValue() <= 0 || received.doubleValue() <= 0) {
            throw new IllegalArgumentException("sold and received cannot be"
                    + "less or equal to 0");
        }

        if (sold.doubleValue() > totalPurchased.doubleValue()) {
            throw new IllegalArgumentException("sold cannot be bigger than the"
                    + "purchased amount");
        }

        if (received.doubleValue() >= totalPaid.doubleValue()) {
            totalPurchased = new BigDecimal(0);
            totalPaid = new BigDecimal(0);
        } else {
            totalPurchased = totalPurchased.subtract(sold);
            totalPaid = totalPaid.subtract(received);
        }
    }

    /**
     * Increases the balance value
     *
     * @param purchased cryptocoin purchased amount
     * @param paid amount paid in server coin
     * @throws IllegalArgumentException if purchased or paid are not positive
     */
    public void increase(BigDecimal purchased, BigDecimal paid)
            throws IllegalArgumentException {
        Objects.requireNonNull(paid);
        Objects.requireNonNull(purchased);

        if (purchased.doubleValue() <= 0 || paid.doubleValue() <= 0) {
            throw new IllegalArgumentException("the parameters cannot be "
                    + "less or equal to 0");
        }

        totalPaid = totalPaid.add(paid);
        totalPurchased = totalPurchased.add(purchased);
    }

    /**
     * Returns the profit percentage if the investor sells his balance at this
     * exchange rate. This can return a negative value meaning a loss.
     *
     * @param exchangeRate the exchange rate
     * @return the percentage (100 = 100%)
     */
    public BigDecimal getProfitPercentage(BigDecimal exchangeRate) {
        Objects.requireNonNull(exchangeRate);

        BigDecimal sell = exchangeRate.multiply(totalPurchased);

        if (sell.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal divisor = totalPaid;
        if (totalPaid.doubleValue() == 0) {
            divisor = BigDecimal.ONE;
        }

        return sell.divide(divisor, RoundingMode.FLOOR)
                .subtract(new BigDecimal(1)).multiply(new BigDecimal(100));
    }
}
