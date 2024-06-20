package net.epconsortium.cryptomarket.task;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import net.epconsortium.cryptomarket.CryptoMarket;
import net.epconsortium.cryptomarket.util.Configuration;
import net.epconsortium.cryptomarket.finances.ExchangeRates;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

/**
 * Task that updates the exchanges rates
 *
 * @author roinujnosde
 */
public class UpdateExchangeRatesTask extends Task {

    public UpdateExchangeRatesTask(@NotNull CryptoMarket plugin) {
        super(plugin);
    }

    @Override
    public @NotNull Runnable getRunnable() {
        ExchangeRates exchangeRates = plugin.getExchangeRates();
        return () -> {
            //Se ocorreu erro, atualiza as cotações diárias e atual
            if (ExchangeRates.errorOccurred()) {
                exchangeRates.updateAll();
                //Se não, atualiza apenas a atual
            } else {
                exchangeRates.updateCurrentExchangeRate();
            }
        };
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public long getDelay() {
        return 0;
    }

    @Override
    public long getPeriod() {
        return 60 * 20; //every minute
    }
}
