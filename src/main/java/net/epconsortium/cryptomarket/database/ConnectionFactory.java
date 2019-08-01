package net.epconsortium.cryptomarket.database;

import com.mysql.cj.jdbc.MysqlDataSource;
import net.epconsortium.cryptomarket.CryptoMarket;
import net.epconsortium.cryptomarket.util.Configuration;
import org.sqlite.SQLiteDataSource;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Class used to create a Connection object
 * 
 * @author roinujnosde
 */
public class ConnectionFactory {

    private final CryptoMarket plugin;
    private final Configuration config;

    public ConnectionFactory(CryptoMarket plugin) {
        this.plugin = Objects.requireNonNull(plugin);
        config = new Configuration(plugin);
    }
    
    /**
     * Connects to the database and returns a connection
     * 
     * @return the connection
     * @throws java.sql.SQLException
     */
    public Connection getConnection() throws SQLException {
        Connection connection;
        if (config.isMySQLEnabled()) {
            connection = getMySQLConnection();
        } else {
            connection = getSQLiteConnection();
        }

        return connection;
    }

    /**
     * Returns a SQLite connection
     *
     * @return a connection
     * @throws SQLException
     */
    private Connection getSQLiteConnection() throws SQLException {
        File file = new File(plugin.getDataFolder(), "cryptomarket.db");
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException ex) {
            throw new SQLException(ex);
        }

        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + file.getAbsolutePath());
        Connection connection = dataSource.getConnection();

        return connection;
    }

    /**
     * Returns a MySQL connection
     *
     * @return a connection
     * @throws SQLException
     */
    private Connection getMySQLConnection() throws SQLException {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setUser(config.getMySQLUser());
        dataSource.setPassword(config.getMySQLPassword());
        dataSource.setServerName(config.getMySQLHostname());
        dataSource.setPort(config.getMySQLPort());
        dataSource.setDatabaseName(config.getMySQLDatabaseName());
        dataSource.setUseSSL(false);
        
        Connection connection = dataSource.getConnection();

        return connection;
    }
}
