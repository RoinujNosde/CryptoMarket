package net.epconsortium.cryptomarket.commands;

import net.epconsortium.cryptomarket.CryptoMarket;
import net.epconsortium.cryptomarket.database.dao.Investor;
import net.epconsortium.cryptomarket.finances.Economy;
import net.epconsortium.cryptomarket.finances.ExchangeRate;
import net.epconsortium.cryptomarket.finances.ExchangeRates;
import net.epconsortium.cryptomarket.ui.InventoryDrawer;
import net.epconsortium.cryptomarket.ui.frames.MenuFrame;
import net.epconsortium.cryptomarket.util.Configuration;
import net.epconsortium.cryptomarket.util.Formatter;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Class used to process the commands of the plugin
 * 
 * @author roinujnosde
 */
public class CryptoMarketCommand implements CommandExecutor, TabCompleter {

    private final CryptoMarket plugin;
    private final Configuration config;

    public CryptoMarketCommand(CryptoMarket plugin) {
        this.plugin = Objects.requireNonNull(plugin);
        config = new Configuration(plugin);
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        final String subCommand = args.length > 0 ? args[0].toLowerCase() : "";
        switch (subCommand) {
            case "help":
                return processHelpCommand(commandSender);
            case "set":
                return processSetCommand(commandSender, args);
            case "take":
                return processTakeCommand(commandSender, args);
            case "give":
                return processGiveCommand(commandSender, args);
            case "save":
                return processSaveCommand(commandSender);
            case "today":
                return processTodayCommand(commandSender);
            case "update":
                return processUpdateCommand(commandSender);
        }

        if (commandSender instanceof Player) {
            Player player = (Player) commandSender;
            switch (subCommand) {
                case "":
                    return processMenuCommand(player);
                case "balance":
                    return processBalanceCommand(player);
            }
        } else {
            commandSender.sendMessage("§4Only players can use this command!");
            return true;
        }
        return false;
    }

    private boolean processMenuCommand(Player player) {
        if (player.hasPermission("cryptomarket.menu")) {
            if (plugin.getInvestorDao().getInvestor(player) == null) {
                player.sendMessage(config.getMessageErrorConnectingToDatabase());
                return true;
            }
            InventoryDrawer.getInstance().open(new MenuFrame(null, player));
        } else {
            player.sendMessage(config.getMessageErrorNoPermission());
        }
        return true;
    }

    /**
     * Process the update command
     *
     * @param sender sender
     * @return true if the syntax is ok
     */
    private boolean processUpdateCommand(CommandSender sender) {
        if (sender.hasPermission("cryptomarket.update")) {
            if (ExchangeRates.errorOccurred()) {
                ExchangeRates rates = plugin.getExchangeRates();
                rates.updateAll();
                String mensagem = config.getMessageUpdatingContent();
                mensagem = MessageFormat.format(mensagem, rates.getMinutesToUpdate());
                sender.sendMessage(mensagem);
            } else {
                sender.sendMessage(config.getMessageContentAlreadyUptodate());
            }
        } else {
            sender.sendMessage(config.getMessageErrorNoPermission());
        }
        return true;
    }

    /**
     * Process the today command
     *
     * @param sender sender
     * @return true if the syntax is ok
     */
    private boolean processTodayCommand(CommandSender sender) {
        if (sender.hasPermission("cryptomarket.today")) {
            ExchangeRate er = plugin.getExchangeRates().getExchangeRate(LocalDate.now());
            if (er == null) {
                sender.sendMessage(config.getMessageCommandOutdatedData());
                return true;
            }
            sender.sendMessage(config.getMessageCurrentExchangeRate());
            for (String coin : config.getCoins()) {
                sender.sendMessage(MessageFormat.format(config.getMessageCurrentExchangeRatePerCoin(), coin,
                        Formatter.formatCryptocoin(er.getCoinValue(coin))));
            }
        } else {
            sender.sendMessage(config.getMessageErrorNoPermission());
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender,
            Command command, String label, String[] args) {
        List<String> subCommands = Arrays.asList("set", "give", "take", "save",
                "balance", "update", "today", "help");
        if (args.length == 0) {
            return subCommands;
        }

        if (args.length == 1) {
            List<String> combinations = new ArrayList<>();
            for (String sub : subCommands) {
                if (sub.startsWith(args[args.length - 1])) {
                    combinations.add(sub);
                }
            }
            return combinations;
        }

        return null;
    }

    /**
     * Process the balance command
     *
     * @param player player
     * @return true if the syntax is ok
     */
    private boolean processBalanceCommand(Player player) {
        if (player.hasPermission("cryptomarket.balance")) {
            Investor investor = plugin.getInvestorDao().getInvestor(player);

            if (!player.isOnline()) {
                return true;
            }
            if (investor == null) {
                player.sendMessage(config.getMessageErrorConnectingToDatabase());
                return true;
            }
            player.sendMessage(config.getMessageBalance());
            config.getCoins().forEach((coin) -> {
                player.sendMessage(MessageFormat.format(config.getMessageBalancePerCoin(), coin,
                        Formatter.formatCryptocoin(investor.getBalance(coin).getValue())));
            });

        } else {
            player.sendMessage(config.getMessageErrorNoPermission());
        }
        return true;
    }

    /**
     * Process the save command
     *
     * @param sender sender
     * @return true if the syntax is ok
     */
    private boolean processSaveCommand(CommandSender sender) {
        if (sender.hasPermission("cryptomarket.save")) {
            sender.sendMessage(config.getMessageSavingData());
            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getInvestorDao().saveAll();
                }
            }.runTaskAsynchronously(plugin);
        } else {
            sender.sendMessage(config.getMessageErrorNoPermission());
        }
        return true;
    }

    /**
     * Process the give command
     *
     * @param sender sender
     * @param args   args
     * @return true if the syntax is ok
     */
    private boolean processGiveCommand(CommandSender sender, String[] args) {
        if (sender.hasPermission("cryptomarket.give")) {
            //cm give nick amount coin
            if (args.length < 4) {
                return false;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(config.getMessageErrorPlayerNotFound());
                return false;
            }
            final BigDecimal amount;
            try {
                amount = new BigDecimal(args[2]);
                if (amount.doubleValue() <= 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                sender.sendMessage(config.getMessageErrorInvalidAmount());
                return false;
            }
            final String coin = args[3].toUpperCase();
            if (!config.getCoins().contains(coin)) {
                sender.sendMessage(config.getMessageErrorInvalidCoin());
                return false;
            }
            Investor investor = plugin.getInvestorDao().getInvestor(target);
            if (investor == null) {
                sender.sendMessage(config.getMessageErrorConnectingToDatabase());
                return true;
            }
            plugin.getEconomy().deposit(coin, investor, amount);
            sender.sendMessage(MessageFormat.format(config.getMessagePlayerBalanceUpdated(), target.getName()));


        } else {
            sender.sendMessage(config.getMessageErrorNoPermission());
        }

        return true;
    }

    /**
     * Process the take command
     *
     * @param sender sender
     * @param args   args
     * @return true if the syntax is ok
     */
    private boolean processTakeCommand(CommandSender sender, String[] args) {
        if (sender.hasPermission("cryptomarket.take")) {
            //cm take nick amount coin
            if (args.length < 4) {
                return false;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(config.getMessageErrorPlayerNotFound());
                return false;
            }
            final BigDecimal amount;
            try {
                amount = new BigDecimal(args[2]);
                if (amount.doubleValue() <= 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                sender.sendMessage(config.getMessageErrorInvalidAmount());
                return false;
            }
            final String coin = args[3].toUpperCase();
            if (!config.getCoins().contains(coin)) {
                sender.sendMessage(config.getMessageErrorInvalidCoin());
                return false;
            }
            Investor investor = plugin.getInvestorDao().getInvestor(target);

            if (investor == null) {
                sender.sendMessage(config.getMessageErrorConnectingToDatabase());
                return true;
            }
            Economy economy = plugin.getEconomy();
            if (economy.has(coin, investor, amount)) {
                economy.withdraw(coin, investor, amount);
                sender.sendMessage(MessageFormat.format(config.getMessagePlayerBalanceUpdated(), target.getName()));
            } else {
                sender.sendMessage(config.getMessageErrorInsufficientBalance());
            }
        } else {
            sender.sendMessage(config.getMessageErrorNoPermission());
        }

        return true;
    }

    /**
     * Process the set command
     *
     * @param sender sender
     * @param args   args
     * @return true if the syntax is ok
     */
    private boolean processSetCommand(CommandSender sender, String[] args) {
        if (sender.hasPermission("cryptomarket.set")) {
            //cm set nick amount coin
            if (args.length < 4) {
                return false;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(config.getMessageErrorPlayerNotFound());
                return false;
            }
            final BigDecimal amount;
            try {
                amount = new BigDecimal(args[2]);
                if (amount.doubleValue() < 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                sender.sendMessage(config.getMessageErrorInvalidAmount());
                return false;
            }
            final String coin = args[3].toUpperCase();
            if (!config.getCoins().contains(coin)) {
                sender.sendMessage(config.getMessageErrorInvalidCoin());
                return false;
            }
            Investor investor = plugin.getInvestorDao().getInvestor(target);
            if (investor == null) {
                sender.sendMessage(config.getMessageErrorConnectingToDatabase());
                return true;
            }
            plugin.getEconomy().set(coin, investor, amount);
            sender.sendMessage(MessageFormat.format(config.getMessagePlayerBalanceUpdated(), target.getName()));
        } else {
            sender.sendMessage(config.getMessageErrorNoPermission());
        }

        return true;
    }

    /**
     * Process the help command
     *
     * @param sender sender
     * @return true if the syntax is ok
     */
    private boolean processHelpCommand(CommandSender sender) {
        config.getHelpCommandMessages().forEach(sender::sendMessage);
        return true;
    }
}
