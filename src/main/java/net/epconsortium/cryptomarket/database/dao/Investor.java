package net.epconsortium.cryptomarket.database.dao;

import net.epconsortium.cryptomarket.finances.ExchangeRate;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.*;

/**
 * Class representing an Investor
 * It contains a reference to the OfflinePlayer and to the Balance objects
 * 
 * @author roinujnosde
 */
public class Investor {
    private final OfflinePlayer player;
    private final Map<String, Balance> balances;

    Investor(OfflinePlayer player, Map<String, Balance> balances) {
        this.player = Objects.requireNonNull(player);
        this.balances = Objects.requireNonNull(balances);
    }

    /**
     * Returns the OfflinePlayer linked to the Investor
     * 
     * @return the {@link OfflinePlayer}
     */
    public OfflinePlayer getPlayer() {
        return player;
    }

    /**
     * Returns the balance of the Investor in this coin
     *
     * @param coin coin
     * @return balance
     */
    public Balance getBalance(String coin) {
        Balance balance = balances.get(coin);
        if (balance == null) {
            balance = new Balance(BigDecimal.ZERO, BigDecimal.ZERO);
            balances.put(coin, balance);
        }

        return balance;
    }

    /**
     * Converts this Investor's balance in cryptocoins to the server's currency
     *
     * @param rate the {@link ExchangeRate} used to calculate the patrimony
     * @return the converted patrimony or -1 if the rate is null
     */
    public BigDecimal getConvertedPatrimony(@Nullable ExchangeRate rate) {
        if (rate == null) {
            return new BigDecimal(-1);
        }
        BigDecimal patrimony = BigDecimal.ZERO;
        for (Map.Entry<String, Balance> entry : getBalances().entrySet()) {
            patrimony = patrimony.add(rate.getCoinValue(entry.getKey()).multiply(entry.getValue().getValue()));
        }

        return patrimony;
    }

    public static Comparator<Investor> comparator(@NotNull ExchangeRate exchangeRate) {
        return (o1, o2) -> o1.compareTo(o2, exchangeRate);
    }

    public int compareTo(@NotNull Investor other, @NotNull ExchangeRate rate) {
        return getConvertedPatrimony(rate).compareTo(other.getConvertedPatrimony(rate));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Investor that = (Investor) o;

        return player.getUniqueId().equals(that.player.getUniqueId());
    }

    @Override
    public int hashCode() {
        return player.getUniqueId().hashCode();
    }

    /**
     * Returns an unmodifiable Map of the Investor's balances
     * 
     * @return balances
     */
    public Map<String, Balance> getBalances() {
        return Collections.unmodifiableMap(balances);
    }

    @Override
    public String toString() {
        return "[Investor: " + player.getName() + "]";
    }

    /**
     * Returns the UUID of the Investor
     *
     * @return uuid
     */
    public UUID getUniqueId() {
        return player.getUniqueId();
    }
}
