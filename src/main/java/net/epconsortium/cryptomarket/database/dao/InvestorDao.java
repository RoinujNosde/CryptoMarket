package net.epconsortium.cryptomarket.database.dao;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.lang.reflect.Type;
import java.sql.*;
import java.util.*;
import net.epconsortium.cryptomarket.CryptoMarket;
import static net.epconsortium.cryptomarket.CryptoMarket.debug;
import net.epconsortium.cryptomarket.database.ConnectionFactory;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Class used to manage the Investors
 *
 * @author roinujnosde
 */
public class InvestorDao {

    private static final Map<UUID, Investor> INVESTORS_ONLINE = new HashMap<>();
    private final Gson gson = new Gson();
    private static final Type BALANCES_TYPE = TypeToken.getParameterized(
            Map.class, String.class, Balance.class).getType();
    private final CryptoMarket plugin;
    
    public InvestorDao(CryptoMarket plugin) {
        this.plugin = Objects.requireNonNull(plugin);
    }

    /**
     * Creates the table if it does not exist
     *
     * @param plugin
     * @param callback
     */
    public static void configureDatabase(CryptoMarket plugin,
            DatabaseConfigurationCallback callback) {
        Objects.requireNonNull(plugin);
        Objects.requireNonNull(callback);
        new BukkitRunnable() {
            @Override
            public void run() {
                boolean success;
                try (Connection connection = new ConnectionFactory(
                        plugin).getConnection()) {
                    connection.createStatement().execute("CREATE TABLE IF NOT"
                            + " EXISTS investors (uuid VARCHAR(255), "
                            + "balances TEXT);");
                    success = true;
                } catch (SQLException ex) {
                    CryptoMarket.warn("Error configuring the database:");
                    ex.printStackTrace();
                    success = false;
                }
                callback.onDatabaseConfigured(success);
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Saves the Investor data on the database
     *
     * @param investor investor to save
     */
    private void save(Investor investor) {
        Objects.requireNonNull(investor);

        try (Connection connection = new ConnectionFactory(plugin).getConnection()) {
            PreparedStatement s = connection.prepareStatement("INSERT INTO investors (uuid, balances) VALUES (?,?);");
            s.setString(1, investor.getPlayer().getUniqueId().toString());
            s.setString(2, gson.toJson(investor.getBalances(), BALANCES_TYPE));

            s.execute();
            INVESTORS_ONLINE.put(investor.getUniqueId(), investor);
        } catch (SQLException ex) {
            CryptoMarket.warn("Error saving the Investor to the database: " + investor);
            ex.printStackTrace();
        }
    }

    /**
     * Retrieves an Investor from a Player
     *
     * @param player player
     * @param callback callback
     */
    public void getInvestor(Player player, InvestorDataCallback callback) {
        Objects.requireNonNull(player);
        Objects.requireNonNull(callback);
        
        Investor i = INVESTORS_ONLINE.get(player.getUniqueId());
        if (i != null) {
            callback.onInvestorDataReady(i);
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                try (Connection connection = new ConnectionFactory(plugin)
                        .getConnection()) {
                    PreparedStatement statement = connection.
                            prepareStatement("SELECT * FROM investors"
                                    + " WHERE uuid = ?;");
                    statement.setString(1, player.getUniqueId().toString());
                    ResultSet set = statement.executeQuery();
                    Investor investor;
                    if (set.next()) {
                        Map<String, Balance> balances = gson.fromJson(
                                set.getString("balances"), BALANCES_TYPE);

                        investor = new Investor(player, balances);
                        debug("Successfully retrieved data from "
                                + player.getName());
                    } else {
                        debug(player.getName() + " was not an Investor. "
                                + "Creating data...");
                        investor = new Investor(player, new HashMap<>());
                        save(investor);
                    }
                    INVESTORS_ONLINE.put(investor.getUniqueId(), investor);
                    callback.onInvestorDataReady(investor);
                } catch (SQLException ex) {
                    CryptoMarket.warn("An error ocurred while retrieving "
                            + "data from " + player.getName());
                    ex.printStackTrace();
                    callback.onInvestorDataReady(null);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Returns all Investors saved in the database
     *
     * @param callback callback
     */
    public void getInvestors(InvestorsDataCallback callback) {
        Objects.requireNonNull(callback);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                debug("Retrieving investors from the database...");
                try (Connection connection = new ConnectionFactory(plugin)
                        .getConnection()) {
                    Statement statement = connection.createStatement();
                    ResultSet set = statement.executeQuery("SELECT * FROM "
                            + "investors;");
                    
                    Set<Investor> investors = new HashSet<>();
                    while (set.next()) {
                        Map<String, Balance> balances = gson.fromJson(
                                set.getString("balances"), BALANCES_TYPE);
                        UUID uuid = UUID.fromString(set.getString("uuid"));
                        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                        Investor investor = new Investor(player, balances);
                        if (INVESTORS_ONLINE.containsKey(uuid)) {
                            investor = INVESTORS_ONLINE.get(uuid);
                        }
                        investors.add(investor);
                    }
                    callback.onInvestorsDataReady(investors);
                } catch (SQLException ex) {
                    CryptoMarket.warn("Error retrieving all investors "
                            + "from the database:");
                    ex.printStackTrace();
                    callback.onInvestorsDataReady(null);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Saves the last modifications to the database
     * 
     */
    public void saveAll() {
        debug("Saving online investors...");

        try (Connection connection = 
                new ConnectionFactory(plugin).getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE investors SET balances=? WHERE uuid=?;")) {
                Set<UUID> offline = new HashSet<>();
                for (Map.Entry<UUID, Investor> entry : 
                        INVESTORS_ONLINE.entrySet()) {
                    Investor investor = entry.getValue();
                    ps.setString(1, gson.toJson(investor.getBalances(),
                            BALANCES_TYPE));
                    ps.setString(2, investor.getPlayer().getUniqueId()
                            .toString());
                    ps.addBatch();

                    if (!investor.getPlayer().isOnline()) {
                        debug(investor + " is not online. "
                                + "Removing from the map...");
                        offline.add(entry.getKey());
                    }
                }
                ps.executeBatch();
                
                //Removing the offline investors
                INVESTORS_ONLINE.keySet().removeAll(offline);
            }
        } catch (SQLException ex) {
            CryptoMarket.warn("Error saving online investors!");
            ex.printStackTrace();
        }
    }

    public static interface DatabaseConfigurationCallback {

        /**
         * Called when the database configuration is finished
         *
         * @param success true if success
         */
        public void onDatabaseConfigured(boolean success);
    }

    public static interface InvestorsDataCallback {

        /**
         * Notifies when the Investors set is ready
         *
         * @param investors set of all investors, null if an error ocurred
         */
        void onInvestorsDataReady(Set<Investor> investors);
    }

    public static interface InvestorDataCallback {

        /**
         * Notifies when the Investor object is ready
         *
         * @param investor the investor, null if an error ocurred
         */
        void onInvestorDataReady(Investor investor);
    }
}
