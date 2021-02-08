package net.epconsortium.cryptomarket.ui;

import java.math.BigDecimal;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.epconsortium.cryptomarket.CryptoMarket;
import net.epconsortium.cryptomarket.database.dao.Investor;
import net.epconsortium.cryptomarket.database.dao.InvestorDao;
import net.epconsortium.cryptomarket.finances.ExchangeRate;
import net.epconsortium.cryptomarket.finances.ExchangeRates;
import net.epconsortium.cryptomarket.util.Configuration;
import net.epconsortium.cryptomarket.util.Formatter;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Represents the Main menu
 *
 * @author roinujnosde
 */
public class Menu {

    private final CryptoMarket plugin;
    private final Configuration config;
    private final ExchangeRates rates;

    private final Player player;
    private final Inventory inventory;
    private Investor investor;

    public Menu(CryptoMarket plugin, Player player) {
        this.plugin = Objects.requireNonNull(plugin);
        this.player = Objects.requireNonNull(player);
        rates = new ExchangeRates(this.plugin);
        config = new Configuration(plugin);
        inventory = Bukkit.createInventory(null, 54, config.getMenuName());
    }

    /**
     * Opens the Menu
     */
    @SuppressWarnings("LocalVariableHidesMemberVariable")
    public void open() {
        new InvestorDao(plugin).getInvestor(player, (investor) -> {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) {
                        return;
                    }
                    if (investor == null) {
                        player.sendMessage(config.getMessageErrorConnectingToDatabase());
                        return;
                    }
                    Menu.this.investor = investor;
                    configureInventory();
                    player.openInventory(inventory);
                }
            }.runTask(plugin);
        });
    }

    /**
     * Configures the inventory
     */
    private void configureInventory() {
        ItemStack coins = configureItemCoins();
        ItemStack wallet = configureItemWallet();
        ItemStack ranking = configureItemRanking();
        ItemStack profit = configureProfitItem();
        ItemStack calendar = configureGenericItem(XMaterial.FILLED_MAP,
                config.getButtonCalendarName());
        ItemStack update = configureGenericItem(XMaterial.STRUCTURE_VOID,
                config.getButtonUpdateName());
        ItemStack blackGlass = configureGenericItem(
                XMaterial.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack grayGlass = configureGenericItem(XMaterial.GRAY_STAINED_GLASS_PANE, " ");

        setButtonsPositions(coins, wallet, profit, calendar, update, blackGlass,
                grayGlass, ranking);
    }

    /**
     * Configures the Profit item
     *
     * @return the profit item
     */
    private ItemStack configureProfitItem() {
        ItemStack profit = XMaterial.LIME_DYE.parseItem(true);
        if (profit == null) {
            profit = new ItemStack(Material.STONE);
        }
        ItemMeta profitMeta = profit.getItemMeta();
        profitMeta.setDisplayName(config.getButtonProfitName());

        List<String> lore = configureProfitItemLore();

        profitMeta.setLore(lore);
        profit.setItemMeta(profitMeta);

        return profit;
    }

    /**
     * Configures the lore of Profit item
     *
     */
    private List<String> configureProfitItemLore() {
        List<String> coinLines = new ArrayList<>();
        String coinLine = config.getButtonProfitCoinLine();
        ExchangeRate rate = rates.getExchangeRate(LocalDate.now());
        if (rate == null) {
            rate = new ExchangeRate();
        }
        for (String coin : config.getCoins()) {
            BigDecimal profitD = investor.getBalance(coin).getProfitPercentage(
                    rate.getCoinValue(coin));
            String color = config.getButtonProfitNeuterColor();
            if (profitD.compareTo(BigDecimal.ZERO) > 0) {
                color = config.getButtonProfitPositiveColor();
            } else if (profitD.compareTo(BigDecimal.ZERO) < 0) {
                color = config.getButtonProfitNegativeColor();
            }

            coinLines.add(MessageFormat.format(coinLine, coin, color,
                    Formatter.formatServerCurrency(profitD)));
        }
        List<String> lore = config.getButtonProfitLore();
        int indexPlaceholder = 0;
        String tempLine = "{0}";
        for (int i = 0; i < lore.size(); i++) {
            String l = lore.get(i);
            if (l.contains("{0}")) {
                indexPlaceholder = i;
                tempLine = l;
            }
        }
        lore.addAll(indexPlaceholder, coinLines);
        lore.remove(tempLine);
        return lore;
    }

    /**
     * Configures an item with a name and material only
     *
     * @param material material
     * @param name name
     * @return item
     */
    private ItemStack configureGenericItem(XMaterial material, String name) {
        ItemStack stack = material.parseItem(true);
        if (stack == null) {
            stack = new ItemStack(Material.STONE);
        }
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        stack.setItemMeta(meta);

        return stack;
    }

    /**
     * Configures the Wallet item
     *
     * @return Wallet item
     */
    private ItemStack configureItemWallet() {
        ItemStack wallet = XMaterial.BOOK.parseItem(true);
        if (wallet == null) {
            wallet = new ItemStack(Material.STONE);
        }
        ItemMeta meta = wallet.getItemMeta();
        meta.setDisplayName(config.getButtonWalletName());
        configureItemWalletLore(meta);
        wallet.setItemMeta(meta);
        return wallet;
    }

    /**
     * Configures the Coins item
     *
     * @return Coins item
     */
    private ItemStack configureItemCoins() {
        ItemStack coins = XMaterial.SUNFLOWER.parseItem(true);
        if (coins == null) {
            coins = new ItemStack(Material.STONE);
        }
        ItemMeta meta = coins.getItemMeta();
        meta.addEnchant(Enchantment.ARROW_DAMAGE, 1, false);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setDisplayName(config.getButtonCoinsName());

        configureItemCoinsLore(meta);
        coins.setItemMeta(meta);
        return coins;
    }

    /**
     * Configures the lore of Wallet item
     *
     * @param meta Wallet meta
     */
    private void configureItemWalletLore(ItemMeta meta) {
        List<String> lore = config.getButtonWalletLore();
        double vaultBalance = plugin.getEconomy().getBalance(player);

        int balanceIndex = 0;
        String tempBalance = "{0}";
        int coinsIndex = 1;
        String coinsLineTemp = "{1}";
        for (int i = 0; i < lore.size(); i++) {
            String l = lore.get(i);
            if (l.contains("{0}")) {
                balanceIndex = i;
                tempBalance = l;
            }
            if (l.contains("{1}")) {
                coinsIndex = i;
                coinsLineTemp = l;
            }
        }
        lore.add(balanceIndex, MessageFormat.format(tempBalance,
                Formatter.formatServerCurrency(vaultBalance)));
        lore.remove(tempBalance);

        List<String> coinsTemp = new ArrayList<>();
        String coinLine = config.getButtonWalletCoinLine();
        for (String coin : config.getCoins()) {
            String format = MessageFormat.format(coinLine, coin,
                    Formatter.formatCryptocoin(investor.getBalance(coin)
                            .getValue()));
            coinsTemp.add(format);
        }

        lore.addAll(coinsIndex, coinsTemp);
        lore.remove(coinsLineTemp);

        meta.setLore(lore);
    }

    /**
     * Configures the lore of the Coins item
     *
     * @param meta Coins meta
     */
    private void configureItemCoinsLore(ItemMeta meta) {
        ExchangeRate er = rates.getExchangeRate(LocalDate.now());
        List<String> lore = config.getButtonCoinsLore();
        if (er == null) {
            er = new ExchangeRate();
        }

        //Configuring the line of values of the item
        List<String> coinsLine = new ArrayList<>();
        for (String coin : config.getCoins()) {
            BigDecimal coinValue = er.getCoinValue(coin);
            String format;
            if (coinValue.equals(new BigDecimal(-1))) {
                format = MessageFormat.format(config.getButtonCoinsCoinLine(),
                        coin, config.getButtonCoinsError());
            } else {
                format = MessageFormat.format(config.getButtonCoinsCoinLine(),
                        coin, Formatter.formatServerCurrency(coinValue));
            }
            coinsLine.add(format);
        }

        //Adding the line of values to the lore
        int index = 0;
        String remove = null;
        for (int i = 0; i < lore.size(); i++) {
            String s = lore.get(i);
            if (s.contains("{0}")) {
                index = i;
                remove = s;
            }
        }
        lore.addAll(index, coinsLine);
        if (remove != null) {
            lore.remove(remove);
        }
        meta.setLore(lore);
    }

    /**
     * Configures the Ranking item
     *
     * @return Ranking item
     */
    private ItemStack configureItemRanking() {
        ItemStack ranking = XMaterial.PLAYER_HEAD.parseItem(true);
        if (ranking == null) {
            ranking = new ItemStack(Material.STONE);
        }
        ItemMeta rankingMeta = ranking.getItemMeta();
        rankingMeta.setDisplayName(config.getButtonRankingName());
        ranking.setItemMeta(rankingMeta);
        return ranking;
    }

    /**
     * Sets the Buttons on the inventory
     *
     * @param coins
     * @param wallet
     * @param profit
     * @param calendar
     * @param update
     * @param blackGlass
     * @param greyGlass
     * @param ranking
     */
    private void setButtonsPositions(ItemStack coins, ItemStack wallet,
            ItemStack profit, ItemStack calendar, ItemStack update,
            ItemStack blackGlass, ItemStack greyGlass, ItemStack ranking) {

        int[] grey = {1, 2, 4, 6, 7, 9, 11, 13, 15, 17, 18, 20, 24, 26, 22, 27,
            29, 47, 48, 51, 50, 30, 36, 38, 39, 40, 41, 42, 44, 32, 33, 35};
        for (int i : grey) {
            inventory.setItem(i, greyGlass);
        }

        int[] black = {28, 0, 3, 5, 8, 10, 16, 21, 23, 34, 45, 46, 49, 52, 52,
            31};
        for (int i : black) {
            inventory.setItem(i, blackGlass);
        }

        inventory.setItem(37, ranking);
        inventory.setItem(12, coins);
        inventory.setItem(14, wallet);
        inventory.setItem(19, profit);
        inventory.setItem(25, calendar);
        inventory.setItem(43, update);
    }
}
