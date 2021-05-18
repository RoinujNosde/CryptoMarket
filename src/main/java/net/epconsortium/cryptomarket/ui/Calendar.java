package net.epconsortium.cryptomarket.ui;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.epconsortium.cryptomarket.CryptoMarket;
import net.epconsortium.cryptomarket.finances.ExchangeRate;
import net.epconsortium.cryptomarket.finances.ExchangeRates;
import net.epconsortium.cryptomarket.util.Configuration;
import net.epconsortium.cryptomarket.util.Formatter;

import static net.epconsortium.cryptomarket.CryptoMarket.debug;

/**
 * Represents the Calendar menu
 * 
 * @author roinujnosde
 */
public class Calendar {

    private final CryptoMarket plugin;

    private final Player player;
    private final Configuration config;
    private final Inventory inventory;
    private YearMonth period;

    private static final Map<UUID, YearMonth> DATA = new HashMap<>();

    public Calendar(CryptoMarket plugin, Player player) {
        this.player = Objects.requireNonNull(player);
        this.plugin = Objects.requireNonNull(plugin);
        config = new Configuration(plugin);

        //if contains, the inventory is open
        if (!DATA.containsKey(player.getUniqueId())) {
            DATA.put(player.getUniqueId(), YearMonth.now());
            inventory = Bukkit.createInventory(null, 54,
                    config.getCalendarMenuName());
        } else {
            inventory = player.getOpenInventory().getTopInventory();
        }
        period = DATA.get(player.getUniqueId());
    }

    /**
     * Removes the Player from the Calendar data, so that when he reopens the
     * Calendar the period will be the current one
     *
     * @param player player
     */
    public static void remove(Player player) {
        DATA.remove(player.getUniqueId());
    }

    /**
     * Goes to the next month
     */
    public void nextMonth() {
        debug("Current: " + period);
        if (period.plusMonths(1).isAfter(YearMonth.now())) {
            return;
        }
        period = period.plusMonths(1);
        debug("Changed: " + period);
        DATA.put(player.getUniqueId(), period);

        loadValues();
    }

    /**
     * Goes to the previous month
     */
    public void previousMonth() {
        debug("Current: " + period);
        period = period.minusMonths(1);
        debug("Changed: " + period);
        DATA.put(player.getUniqueId(), period);
        loadValues();
    }

    /**
     * Opens the Calendar
     */
    public void open() {
        configureInventory();
        loadValues();
        player.openInventory(inventory);
    }

    /**
     * Cleans the inventory so that it can be reused
     */
    private void cleanInventory() {
        int index = 18;
        ItemStack air = new ItemStack(Objects.requireNonNull(XMaterial.AIR.parseMaterial()), 1);
        for (int i = index; i < (index + 31); i++) {
            inventory.setItem(i, air);
        }
    }

    /**
     * Load the cryptocoins values and display them on inventory
     */
    private void loadValues() {
        debug("Loading values for period: " + period);
        cleanInventory();
        ItemStack day = XMaterial.GRAY_STAINED_GLASS_PANE.parseItem();

        ItemStack periodItem = configurePeriodItem(period.toString());
        inventory.setItem(6, periodItem);

        Month month = period.getMonth();
        int index = 18;
        int days = month.length(period.isLeapYear());

        for (int x = 1; x <= days; x++) {
            ItemMeta dayMeta = day.getItemMeta();
            dayMeta.setDisplayName(MessageFormat.format(
                    config.getCalendarMenuDayItemName(), x));
            LocalDate date = LocalDate.of(period.getYear(), month, x);
            LocalDate today = LocalDate.now();

            if (date.isAfter(today)) {
                ArrayList< String> lore = new ArrayList<>();
                lore.add(config.getCalendarMenuNoExchangeRate());
                dayMeta.setLore(lore);
            } else {
                ExchangeRate er = plugin.getExchangeRates().getExchangeRate(date);

                if (er == null) {
                    er = new ExchangeRate();
                }
                configureItemDayLore(er, dayMeta, day);
            }

            day.setAmount(x);
            day.setItemMeta(dayMeta);
            inventory.setItem(index, day);
            index++;
        }
        debug("Finished loading for period " + period);
    }

    /**
     * Configures the lore of item Day
     *
     * @param meta
     * @param itemStack
     */
    private void configureItemDayLore(ExchangeRate er, ItemMeta meta,
            ItemStack itemStack) {
        List< String> lore = config.getLoreOfTheDayItem();
        //Configurando a linha de valores das moedas
        List< String> coinsLine = new ArrayList<>();
        List< String> coins = config.getCoins();

        for (String coin : coins) {
            BigDecimal coinValue = er.getCoinValue(coin);
            String format;
            if (coinValue.equals(new BigDecimal(-1))) {
                format = MessageFormat.format(config.getCalendarMenuCoinLine(),
                        coin, config.getItemDayError());
            } else {
                format = MessageFormat.format(config.getCalendarMenuCoinLine(),
                        coin, Formatter.formatServerCurrency(coinValue));
            }
            coinsLine.add(format);
        }

        //Adicionando a linha de valores à lore
        int indexB = 0;
        String remove = null;
        for (int i = 0; i < lore.size(); i++) {
            String s = lore.get(i);
            if (s.contains("{0}")) {
                indexB = i;
                remove = s;
            }
        }

        lore.addAll(indexB, coinsLine);
        if (remove != null) {
            lore.remove(remove);
        }

        meta.setLore(lore);
        itemStack.setItemMeta(meta);
    }

    /**
     * Configures the inventory
     */
    private void configureInventory() {
        ItemStack back = configureGenericItem(XMaterial.ARROW,
                config.getCalendarMenuBackButtonName());
        ItemStack nextMonth = configureGenericItem(XMaterial.OAK_BUTTON,
                config.getCalendarMenuNextMonthButtonName());
        ItemStack previousMonth = configureGenericItem(XMaterial.OAK_BUTTON,
                config.getCalendarMenuPreviousMonthButtonName());
        ItemStack periodItem = configurePeriodItem(period.toString());
        ItemStack blackGlass = configureGenericItem(
                XMaterial.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack greyGlass = configureGenericItem(
                XMaterial.GRAY_STAINED_GLASS_PANE, " ");
        setButtonsPositions(back, nextMonth, previousMonth, periodItem,
                blackGlass, greyGlass);
    }

    /**
     * Configures the Period item
     *
     * @param date date
     * @return Period item
     */
    private ItemStack configurePeriodItem(String date) {
        ItemStack item = new ItemStack(Objects.requireNonNull(XMaterial.FILLED_MAP.parseMaterial(true)));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(date);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Configures a generic item
     *
     * @param material material
     * @param name name
     * @return the item
     */
    private ItemStack configureGenericItem(XMaterial material, String name) {
        ItemStack stack = material.parseItem();
        if (stack == null) {
            stack = new ItemStack(Material.STONE);
        }
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        stack.setItemMeta(meta);
        return stack;
    }

    /**
     * Sets the buttons' positions
     *
     * @param back
     * @param nextMonth
     * @param previousMonth
     * @param periodItem
     * @param blackGlass
     * @param greyGlass
     */
    private void setButtonsPositions(ItemStack back, ItemStack nextMonth,
            ItemStack previousMonth, ItemStack periodItem, ItemStack blackGlass,
            ItemStack greyGlass) {
        inventory.setItem(0, blackGlass);
        inventory.setItem(1, back);
        inventory.setItem(2, blackGlass);
        inventory.setItem(3, greyGlass);
        inventory.setItem(4, blackGlass);
        inventory.setItem(5, previousMonth);
        inventory.setItem(6, periodItem);
        inventory.setItem(7, nextMonth);
        inventory.setItem(8, blackGlass);
    }
}
