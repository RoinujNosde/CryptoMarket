package net.epconsortium.cryptomarket.database.dao;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.epconsortium.cryptomarket.CryptoMarket;
import net.epconsortium.cryptomarket.database.ConnectionFactory;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static net.epconsortium.cryptomarket.CryptoMarket.debug;

/**
 * Class used to manage the Investors
 *
 * @author roinujnosde
 */
public class InvestorDao {

    private static InvestorDao instance;
    private static final List<Investor> ONLINE_INVESTORS = new CopyOnWriteArrayList<>();
    private static ConnectionFactory connectionFactory;
    private final Gson gson = new Gson();
    private static final Type BALANCES_TYPE = TypeToken.getParameterized(Map.class, String.class, Balance.class)
            .getType();
    private final CryptoMarket plugin;

    private InvestorDao(CryptoMarket plugin) {
        this.plugin = Objects.requireNonNull(plugin);
        connectionFactory = new ConnectionFactory(plugin);
    }

    public static InvestorDao getInstance(@NotNull CryptoMarket plugin) {
        if (instance == null) {
            instance = new InvestorDao(plugin);
        }
        return instance;
    }

    /**
     * Creates the table if it does not exist
     */
    public void configureDatabase(CryptoMarket plugin, DatabaseConfigurationCallback callback) {
        Objects.requireNonNull(plugin);
        Objects.requireNonNull(callback);
        new BukkitRunnable() {
            @Override
            public void run() {
                boolean success;
                try (Connection connection = connectionFactory.getConnection()) {
                    connection.createStatement().execute("CREATE TABLE IF NOT EXISTS investors (uuid VARCHAR(255), balances TEXT);");
                    success = true;
                } catch (SQLException ex) {
                    CryptoMarket.warn("Error configuring the database:");
                    ex.printStackTrace();
                    success = false;
                }
                boolean finalSuccess = success;
                Bukkit.getScheduler().runTask(plugin, () -> callback.onDatabaseConfigured(finalSuccess));
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Inserts the Investor data into the database
     *
     * @param investor investor to insert
     */
    private void insert(@NotNull Investor investor) {
        try (Connection connection = new ConnectionFactory(plugin).getConnection()) {
            PreparedStatement s = connection.prepareStatement("INSERT INTO investors (uuid, balances) VALUES (?,?);");
            s.setString(1, investor.getPlayer().getUniqueId().toString());
            s.setString(2, gson.toJson(investor.getBalances(), BALANCES_TYPE));

            s.execute();
            ONLINE_INVESTORS.add(investor);
        } catch (SQLException ex) {
            CryptoMarket.warn("Error saving the Investor to the database: " + investor);
            ex.printStackTrace();
        }
    }

    public @Nullable Investor getInvestor(@NotNull final OfflinePlayer player) {
        for (Investor investor : ONLINE_INVESTORS) {
            if (investor.getUniqueId().equals(player.getUniqueId())) {
                return investor;
            }
        }
        return null;
    }

    public void unloadInvestor(@NotNull final OfflinePlayer player) {
        ONLINE_INVESTORS.removeIf(i -> i.getUniqueId().equals(player.getUniqueId()));
    }

    public void loadInvestor(@NotNull final OfflinePlayer player) {
        try (Connection connection = new ConnectionFactory(plugin).getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM investors WHERE uuid = ?;");
            statement.setString(1, player.getUniqueId().toString());
            ResultSet set = statement.executeQuery();
            Investor investor;
            if (set.next()) {
                Map<String, Balance> balances = gson.fromJson(set.getString("balances"), BALANCES_TYPE);

                investor = new Investor(player, balances);
                debug("Successfully retrieved data for " + player.getName());
                ONLINE_INVESTORS.add(investor);
            } else {
                debug(player.getName() + " was not an Investor. Creating data...");
                investor = new Investor(player, new HashMap<>());
                insert(investor);
            }
        } catch (SQLException ex) {
            CryptoMarket.warn("An error occurred while retrieving data for " + player.getName());
            ex.printStackTrace();
        }
    }

    public @Nullable List<Investor> getInvestors() {
        List<Investor> investors = new ArrayList<>();
        try (Connection connection = new ConnectionFactory(plugin).getConnection()) {
            Statement statement = connection.createStatement();
            ResultSet set = statement.executeQuery("SELECT * FROM investors;");
            while (set.next()) {
                Map<String, Balance> balances = gson.fromJson(set.getString("balances"), BALANCES_TYPE);
                UUID uuid = UUID.fromString(set.getString("uuid"));
                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                Investor investor = getInvestor(player);
                if (investor == null) {
                    investor = new Investor(player, balances);
                }
                investors.add(investor);
            }
        } catch (SQLException ex) {
            CryptoMarket.warn("Error retrieving all investors from the database:");
            ex.printStackTrace();
            return null;
        }
        
        return investors;
    }

    /**
     * Saves the last modifications to the database
     */
    public void saveAll() {
        debug("Saving online investors...");

        try (Connection connection = new ConnectionFactory(plugin).getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement("UPDATE investors SET balances=? WHERE uuid=?;")) {
                for (Investor investor : ONLINE_INVESTORS) {
                    ps.setString(1, gson.toJson(investor.getBalances(), BALANCES_TYPE));
                    ps.setString(2, investor.getPlayer().getUniqueId().toString());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        } catch (SQLException ex) {
            CryptoMarket.warn("Error saving online investors!");
            ex.printStackTrace();
        }
    }

    public interface DatabaseConfigurationCallback {

        /**
         * Called when the database configuration is finished
         *
         * @param success true if success
         */
        void onDatabaseConfigured(boolean success);
    }
}
