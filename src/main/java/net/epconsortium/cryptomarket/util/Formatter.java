package net.epconsortium.cryptomarket.util;

import java.text.NumberFormat;
import java.util.Objects;

/**
 * Utility class to format numbers
 *
 * @author roinujnosde
 */
public class Formatter {

    /**
     * Formats and returns a number in the local format with eight numbers after
     * the decimal separator
     *
     * @param number number to format
     * @return formatted number
     */
    public static String formatCryptocoin(Object number) {
        Objects.requireNonNull(number);
        NumberFormat format = NumberFormat.getNumberInstance();
        format.setMaximumFractionDigits(8);
        format.setMinimumFractionDigits(8);

        return format.format(number);
    }

    /**
     * Formats and returns a number in the local format with two numbers after
     * the decimal separator
     *
     * @param number number to format
     * @return formatted number
     */
    public static String formatServerCurrency(Object number) {
        Objects.requireNonNull(number);
        NumberFormat format = NumberFormat.getNumberInstance();
        format.setMaximumFractionDigits(2);
        format.setMinimumFractionDigits(2);

        return format.format(number);
    }

    /**
     * Formats and returns a number in the local format without fraction digits
     *
     * @param number number to format
     * @return formatted number
     */
    public static String formatPercentage(Object number) {
        Objects.requireNonNull(number);
        NumberFormat format = NumberFormat.getNumberInstance();
        format.setMaximumFractionDigits(0);

        return format.format(number);
    }
}
