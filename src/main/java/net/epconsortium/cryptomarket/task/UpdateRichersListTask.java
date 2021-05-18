package net.epconsortium.cryptomarket.task;

import net.epconsortium.cryptomarket.CryptoMarket;
import net.epconsortium.cryptomarket.database.dao.Investor;
import net.epconsortium.cryptomarket.finances.Economy;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class UpdateRichersListTask extends Task {

    public UpdateRichersListTask(@NotNull CryptoMarket plugin) {
        super(plugin);
    }

    @Override
    public @NotNull Runnable getRunnable() {
        return () -> {
            List<Investor> investors = plugin.getInvestorDao().getInvestors();
            if (investors == null) {
                return;
            }
            Economy economy = plugin.getEconomy();
            economy.setInvestors(investors);
            economy.setRichersLastUpdate(System.currentTimeMillis());
        };
    }

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    public long getDelay() {
        return 0;
    }

    @Override
    public long getPeriod() {
        return configuration.getIntervalRichersUpdateInTicks();
    }
}
