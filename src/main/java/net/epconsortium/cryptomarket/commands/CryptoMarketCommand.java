package net.epconsortium.cryptomarket.commands;

import java.math.BigDecimal;
import net.epconsortium.cryptomarket.CryptoMarket;
import net.epconsortium.cryptomarket.database.dao.InvestorDao;
import net.epconsortium.cryptomarket.finances.ExchangeRate;
import net.epconsortium.cryptomarket.finances.ExchangeRates;
import net.epconsortium.cryptomarket.ui.Menu;
import net.epconsortium.cryptomarket.util.Configuration;
import net.epconsortium.cryptomarket.util.Formatter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.epconsortium.cryptomarket.finances.Economy;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

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
    public boolean onCommand(CommandSender commandSender, Command command,
            String label, String[] args) {
        if (commandSender instanceof Player) {
            Player player = (Player) commandSender;
            player.sendMessage("Entity ID for "+ player.getName() + "is " +player.getEntityId());
            
            if (args.length != 0) {
                String subCommand = args[0].toLowerCase();
                switch (subCommand) {
                    case "help":
                        return processHelpCommand(player);
                    case "set":
                        return processSetCommand(player, args);
                    case "take":
                        return processTakeCommand(player, args);
                    case "give":
                        return processGiveCommand(player, args);
                    case "save":
                        return processSaveCommand(player);
                    case "today":
                        return processTodayCommand(player);
                    case "balance":
                        return processBalanceCommand(player);
                    case "update":
                        return processUpdateCommand(player);
                }
            } else {
                if (player.hasPermission("cryptomarket.menu")) {
                    new Menu(plugin, player).open();
                } else {
                    player.sendMessage(config.getMessageErrorNoPermission());
                }
                return true;
            }
            return false;
        }
        commandSender.sendMessage("§4Only players can use this command!");
        return true;
    }

    /**
     * Process the update command
     *
     * @param player player
     * @return true if the syntax is ok
     */
    private boolean processUpdateCommand(Player player) {
        if (player.hasPermission("cryptomarket.update")) {
            if (ExchangeRates.errorOcurred()) {
                ExchangeRates rates = new ExchangeRates(plugin);
                rates.updateAll();
                String mensagem = config.getMessageUpdatingContent();
                mensagem = MessageFormat.format(mensagem,
                        rates.getMinutesToUpdate());
                player.sendMessage(mensagem);
            } else {
                player.sendMessage(
                        config.getMessageContentAlreadyUptodate());
            }
        } else {
            player.sendMessage(config.getMessageErrorNoPermission());
        }
        return true;
    }

    /**
     * Process the today command
     *
     * @param player player
     * @return true if the syntax is ok
     */
    private boolean processTodayCommand(Player player) {
        if (player.hasPermission("cryptomarket.today")) {
            ExchangeRate er = new ExchangeRates(plugin).getExchangeRate(
                    LocalDate.now());
            if (er == null) {
                player.sendMessage(config.getMessageCommandOutdatedData());
                return true;
            }
            player.sendMessage(config.getMessageCurrentExchangeRate());
            for (String coin : config.getCoins()) {
                player.sendMessage(MessageFormat.format(
                        config.getMessageCurrentExchangeRatePerCoin(), coin,
                        Formatter.formatCryptocoin(er.getCoinValue(coin))));
            }
        } else {
            player.sendMessage(config.getMessageErrorNoPermission());
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
            new InvestorDao(plugin).getInvestor(player, (investor) -> {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!player.isOnline()) {
                            return;
                        }
                        if (investor == null) {
                            player.sendMessage(config
                                    .getMessageErrorConnectingToDatabase());
                            return;
                        }
                        player.sendMessage(config.getMessageBalance());
                        config.getCoins().forEach((coin) -> {
                            player.sendMessage(MessageFormat.format(
                                    config.getMessageBalancePerCoin(), coin,
                                    Formatter.formatCryptocoin(investor
                                            .getBalance(coin).getValue())));
                        });
                    }
                }.runTask(plugin);

            });
        } else {
            player.sendMessage(config.getMessageErrorNoPermission());
        }
        return true;
    }

    /**
     * Process the save command
     *
     * @param player player
     * @return true if the syntax is ok
     */
    private boolean processSaveCommand(Player player) {
        if (player.hasPermission("cryptomarket.save")) {
            player.sendMessage(config.getMessageSavingData());
            new BukkitRunnable() {
                @Override
                public void run() {
                    new InvestorDao(plugin).saveAll();
                }
            }.runTaskAsynchronously(plugin);
        } else {
            player.sendMessage(config.getMessageErrorNoPermission());
        }
        return true;
    }

    /**
     * Process the give command
     *
     * @param player player
     * @param args args
     * @return true if the syntax is ok
     */
    private boolean processGiveCommand(Player player, String[] args) {
        if (player.hasPermission("cryptomarket.give")) {
            //cm give nick amount coin
            if (args.length < 4) {
                return false;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(config.getMessageErrorPlayerNotFound());
                return false;
            }
            final BigDecimal amount;
            try {
                amount = new BigDecimal(args[2]);
                if (amount.doubleValue() <= 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                player.sendMessage(config.getMessageErrorInvalidAmount());
                return false;
            }
            final String coin = args[3].toUpperCase();
            if (!config.getCoins().contains(coin)) {
                player.sendMessage(config.getMessageErrorInvalidCoin());
                return false;
            }
            new InvestorDao(plugin).getInvestor(target, (investor) -> {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (investor == null) {
                            player.sendMessage(config
                                    .getMessageErrorConnectingToDatabase());
                            return;
                        }
                        Economy economy = new Economy(plugin, coin);
                        economy.deposit(investor, amount);
                        player.sendMessage(MessageFormat.format(
                                config.getMessagePlayerBalanceUpdated(),
                                target.getName()));
                    }
                }.runTask(plugin);
            });
        } else {
            player.sendMessage(config.getMessageErrorNoPermission());
        }

        return true;
    }

    /**
     * Process the take command
     *
     * @param player player
     * @param args args
     * @return true if the syntax is ok
     */
    private boolean processTakeCommand(Player player, String[] args) {
        if (player.hasPermission("cryptomarket.take")) {
            //cm take nick amount coin
            if (args.length < 4) {
                return false;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(config.getMessageErrorPlayerNotFound());
                return false;
            }
            final BigDecimal amount;
            try {
                amount = new BigDecimal(args[2]);
                if (amount.doubleValue() <= 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                player.sendMessage(config.getMessageErrorInvalidAmount());
                return false;
            }
            final String coin = args[3].toUpperCase();
            if (!config.getCoins().contains(coin)) {
                player.sendMessage(config.getMessageErrorInvalidCoin());
                return false;
            }
            new InvestorDao(plugin).getInvestor(target, (investor) -> {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (investor == null) {
                            player.sendMessage(config
                                    .getMessageErrorConnectingToDatabase());
                            return;
                        }
                        Economy economy = new Economy(plugin, coin);
                        if (economy.has(investor, amount)) {
                            economy.withdraw(investor, amount);
                            player.sendMessage(MessageFormat.format(
                                    config.getMessagePlayerBalanceUpdated(),
                                    target.getName()));
                        } else {
                            player.sendMessage(config
                                    .getMessageErrorInsufficientBalance());
                        }
                    }
                }.runTask(plugin);
            });
        } else {
            player.sendMessage(config.getMessageErrorNoPermission());
        }

        return true;
    }

    /**
     * Process the set command
     *
     * @param player player
     * @param args args
     * @return true if the syntax is ok
     */
    private boolean processSetCommand(Player player, String[] args) {
        if (player.hasPermission("cryptomarket.set")) {
            //cm set nick amount coin
            if (args.length < 4) {
                return false;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(config.getMessageErrorPlayerNotFound());
                return false;
            }
            final BigDecimal amount;
            try {
                amount = new BigDecimal(args[2]);
                if (amount.doubleValue() < 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                player.sendMessage(config.getMessageErrorInvalidAmount());
                return false;
            }
            final String coin = args[3].toUpperCase();
            if (!config.getCoins().contains(coin)) {
                player.sendMessage(config.getMessageErrorInvalidCoin());
                return false;
            }
            new InvestorDao(plugin).getInvestor(target, (investor) -> {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (investor == null) {
                            player.sendMessage(config
                                    .getMessageErrorConnectingToDatabase());
                            return;
                        }
                        Economy economy = new Economy(plugin, coin);
                        economy.set(investor, amount);
                        player.sendMessage(MessageFormat.format(
                                config.getMessagePlayerBalanceUpdated(),
                                target.getName()));
                    }
                }.runTask(plugin);
            });
        } else {
            player.sendMessage(config.getMessageErrorNoPermission());
        }

        return true;
    }

    /**
     * Process the help command
     *
     * @param player player
     * @return true if the syntax is ok
     */
    private boolean processHelpCommand(Player player) {
        config.getHelpCommandMessages().forEach(player::sendMessage);
        return true;
    }
}
