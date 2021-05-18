package net.epconsortium.cryptomarket.task;

import net.epconsortium.cryptomarket.CryptoMarket;
import org.jetbrains.annotations.NotNull;

/**
 * Task that save all investors to the database
 * 
 * @author roinujnosde
 */
public class SaveInvestorsTask extends Task {

    public SaveInvestorsTask(CryptoMarket plugin) {
        super(plugin);
    }

    @Override
    public @NotNull Runnable getRunnable() {
        return () -> plugin.getInvestorDao().saveAll();
    }

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    public long getDelay() {
        return configuration.getIntervalSavingInvestorsInTicks();
    }

    @Override
    public long getPeriod() {
        return configuration.getIntervalSavingInvestorsInTicks();
    }
}
