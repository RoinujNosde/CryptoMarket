package net.epconsortium.cryptomarket.task;

import net.epconsortium.cryptomarket.CryptoMarket;
import net.epconsortium.cryptomarket.util.Configuration;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

public abstract class Task {

    protected final CryptoMarket plugin;
    protected final Configuration configuration;
    protected BukkitTask bukkitTask;

    public Task(@NotNull CryptoMarket plugin) {
        this.plugin = plugin;
        this.configuration = new Configuration(plugin);
    }

    public void start() {
        BukkitScheduler scheduler = plugin.getServer().getScheduler();
        if (!isAsync()) {
            bukkitTask = scheduler.runTaskTimer(plugin, getRunnable(), getDelay(), getPeriod());
        } else {
            bukkitTask = scheduler.runTaskTimerAsynchronously(plugin, getRunnable(), getDelay(), getPeriod());
        }
    }

    public void cancel() {
        if (bukkitTask != null) {
            bukkitTask.cancel();
            bukkitTask = null;
        }
    }

    public abstract @NotNull Runnable getRunnable();

    public abstract boolean isAsync();

    public abstract long getDelay();

    public abstract long getPeriod();
}
