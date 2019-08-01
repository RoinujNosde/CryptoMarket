package net.epconsortium.cryptomarket.database.dao;

import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

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
     * @return offlineplayer
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Investor that = (Investor) o;

        return player.equals(that.player);
    }

    @Override
    public int hashCode() {
        return player.hashCode();
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
