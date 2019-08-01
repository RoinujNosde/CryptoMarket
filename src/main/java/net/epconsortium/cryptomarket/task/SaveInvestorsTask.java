package net.epconsortium.cryptomarket.task;

import java.util.Objects;
import net.epconsortium.cryptomarket.CryptoMarket;
import net.epconsortium.cryptomarket.database.dao.InvestorDao;
import net.epconsortium.cryptomarket.util.Configuration;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Task that save all investors to the database
 * 
 * @author roinujnosde
 */
public class SaveInvestorsTask extends BukkitRunnable {

    private final CryptoMarket plugin;

    public SaveInvestorsTask(CryptoMarket plugin) {
        this.plugin = Objects.requireNonNull(plugin);
    }

    /**
     * Used internally!
     */
    @Override
    public void run() {
        InvestorDao dao = new InvestorDao(plugin);
        dao.saveAll();
    }

    /**
     * Starts the repetitive task
     */
    public void start() {
        Configuration config = new Configuration(plugin);
        long delay = config.getIntervalSavingInvestorsInTicks();

        this.runTaskTimerAsynchronously(plugin, delay, delay);
    }
}
