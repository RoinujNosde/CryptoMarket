package net.epconsortium.cryptomarket.database;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

import net.epconsortium.cryptomarket.CryptoMarket;
import net.epconsortium.cryptomarket.util.Configuration;

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
        
        Connection connection = null;
        
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        } catch (ClassNotFoundException e) {
        	throw new SQLException(e);
        }

        return connection;
    }

    /**
     * Returns a MySQL connection
     *
     * @return a connection
     * @throws SQLException
     */
    private Connection getMySQLConnection() throws SQLException {
        Connection connection = null;
        
        Properties properties = new Properties();
        properties.setProperty("user", config.getMySQLUser());
        properties.setProperty("password", config.getMySQLPassword());
        properties.setProperty("useSSL", "false");
        properties.setProperty("characterEncoding", "utf-8");
        properties.setProperty("autoReconnect", "true");
        
    	try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://" + config.getMySQLHostname() + ":" + config.getMySQLPort() + "/" + config.getMySQLDatabaseName(), properties);
        } catch (ClassNotFoundException e) {
        	throw new SQLException(e);
        }

        return connection;
    }
}
