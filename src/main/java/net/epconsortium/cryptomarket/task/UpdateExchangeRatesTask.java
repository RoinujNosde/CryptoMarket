package net.epconsortium.cryptomarket.task;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import net.epconsortium.cryptomarket.CryptoMarket;
import net.epconsortium.cryptomarket.util.Configuration;
import net.epconsortium.cryptomarket.finances.ExchangeRates;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Task that updates the exchanges rates
 *
 * @author roinujnosde
 */
public class UpdateExchangeRatesTask extends BukkitRunnable {

    private final ExchangeRates exchangeRates;

    public UpdateExchangeRatesTask(ExchangeRates exchangeRates) {
        this.exchangeRates = Objects.requireNonNull(exchangeRates);
    }

    /**
     * Used internally!
     */
    @Override
    public void run() {
        //Se ocorreu erro, atualiza as cotações diárias e atual
        if (ExchangeRates.errorOcurred()) {
            exchangeRates.updateAll();
            //Se não, atualiza apenas a atual
        } else {
            exchangeRates.updateCurrentExchangeRate();
        }
    }

    /**
     * Starts the repetitive task
     *
     * @param plugin plugin
     */
    public void start(CryptoMarket plugin) {
        Objects.requireNonNull(plugin);

        Configuration config = new Configuration(plugin);
        long period = config.getIntervalExchangeRatesUpdateInTicks();

        LocalDateTime tomorrow = LocalDate.now().plusDays(1).atTime(0, 1);
        long delay = LocalDateTime.now().until(tomorrow, ChronoUnit.SECONDS) * 20;
        delay = delay % period;

        this.runTaskTimer(plugin, delay, period);
    }
}
