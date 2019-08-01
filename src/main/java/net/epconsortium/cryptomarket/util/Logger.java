package net.epconsortium.cryptomarket.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import net.epconsortium.cryptomarket.CryptoMarket;
import net.epconsortium.cryptomarket.finances.Negotiation;
import net.epconsortium.cryptomarket.database.dao.Investor;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * This class logs the negociations to a file
 *
 * @author roinujnosde
 */
public class Logger {

    private final CryptoMarket plugin;
    private final File file;
    private final File logsFolder;

    public Logger(CryptoMarket plugin) {
        this.plugin = Objects.requireNonNull(plugin);
        logsFolder = new File(plugin.getDataFolder() + File.separator
                + "logs");
        file = new File(logsFolder,
                LocalDate.now() + ".txt");
    }

    /**
     * Logs this data to the log file
     *
     * @param investor
     * @param negotiation
     * @param cryptoValue
     * @param coin
     * @param vaultValue
     */
    public void log(Investor investor, Negotiation negotiation,
            BigDecimal cryptoValue, String coin, double vaultValue) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (!logsFolder.exists() && !logsFolder.mkdir()) {
                        throw new IOException("Error creating the logs folder!");
                    }
                    file.createNewFile();
                    try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                            new FileOutputStream(file, true)))) {
                        writer.printf("%s-> investor: %s type: %s cryptocoin: %s %f "
                                + "servercoin: %f new balance: %f",
                                LocalTime.now(), investor.getPlayer().getName(),
                                negotiation, coin, cryptoValue, vaultValue,
                                investor.getBalance(coin).getValue());
                        writer.println();
                        writer.flush();
                    }
                } catch (IOException ex) {
                    CryptoMarket.warn("Error logging a negociation to file!");
                    ex.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }
}
