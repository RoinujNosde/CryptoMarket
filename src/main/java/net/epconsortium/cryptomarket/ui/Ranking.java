package net.epconsortium.cryptomarket.ui;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.epconsortium.cryptomarket.CryptoMarket;
import net.epconsortium.cryptomarket.finances.Economy;
import net.epconsortium.cryptomarket.database.dao.Investor;
import net.epconsortium.cryptomarket.util.Configuration;
import net.epconsortium.cryptomarket.util.Formatter;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import static net.epconsortium.cryptomarket.CryptoMarket.debug;

/**
 * Represents the Ranking menu
 * 
 * @author roinujnosde
 */
public class Ranking {

    private final Player player;
    private final CryptoMarket plugin;
    private final Inventory inventory;
    private final Configuration config;
    private List<Investor> richersList;
    private double totalInvestments;

    public Ranking(CryptoMarket plugin, Player player) {
        this.player = Objects.requireNonNull(player);
        this.plugin = Objects.requireNonNull(plugin);

        config = new Configuration(plugin);
        inventory = Bukkit.createInventory(null, 45,
                config.getRankingMenuName());
    }

    /**
     * Open the Ranking menu
     */
    public void open() {
        Economy eco = new Economy(plugin, "");
        eco.getRichestInvestors(5, (investors) -> {
            eco.getTotalInvestments(total -> {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (player.isOnline()) {
                            if (investors == null || total == -1) {
                                player.sendMessage(config
                                        .getMessageErrorAccessingRankingData());
                                return;
                            }
                            debug("Ranking: investors size: " + investors.size());
                            richersList = investors;
                            totalInvestments = total;
                            configureInventory();
                            player.openInventory(inventory);
                        }
                    }
                }.runTask(plugin);
            });
        });
    }

    /**
     * Configures the Richer on the index
     *
     * @param index index
     * @return the Richer item
     */
    private ItemStack getRicher(int index) {
        Economy econ = new Economy(plugin, "");

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = head.getItemMeta();

        int rank = index + 1;

        if (index >= richersList.size()) {
            meta.setDisplayName(MessageFormat.format(
                    config.getRankingMenuNoRicherLore(), rank));
            head.setItemMeta(meta);
        } else {
            Investor richer = richersList.get(index);
            SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
            skullMeta.setOwningPlayer(richer.getPlayer());
            skullMeta.setDisplayName(MessageFormat.format(
                    config.getRankingMenuRicherItemName(), rank,
                    richer.getPlayer().getName()));

            List<String> lore1 = config.getRankingMenuRicherItemLore();
            ArrayList<String> lore2 = new ArrayList<>();
            BigDecimal patrimony = econ.getConvertedPatrimony(richer);
            double percentage = 0;
            if (totalInvestments > 0) {
                percentage = (patrimony.doubleValue() / totalInvestments) * 100;
            }
            for (String s : lore1) {
                lore2.add(MessageFormat.format(s,
                        Formatter.formatServerCurrency(patrimony),
                        Formatter.formatPercentage(percentage)));
            }

            skullMeta.setLore(lore2);
            head.setItemMeta(skullMeta);
        }

        return head;
    }

    /**
     * Configures the inventory
     */
    private void configureInventory() {
        List<ItemStack> richers = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            richers.add(getRicher(i));
        }

        ItemStack total = configureTotalInvestmentsItem();
        ItemStack lastUpdated = configureLastUpdated();
        ItemStack back = configureBackButton();
        ItemStack black = configureGenericItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack grey = configureGenericItem(Material.GRAY_STAINED_GLASS_PANE, " ");

        setItemsPositions(black, grey, back, richers, total, lastUpdated);
    }

    private ItemStack configureLastUpdated() {
        ItemStack lastUpdated = new ItemStack(Material.CLOCK);
        ItemMeta lastUpdatedMeta = lastUpdated.getItemMeta();
        lastUpdatedMeta.setDisplayName(config.getRankingMenuLastUpdatedItemName());
        List<String> lastUpdateLore = new ArrayList<>();
        for (String s : config.getRankingMenuLastUpdatedItemLore()) {
            lastUpdateLore.add(MessageFormat.format(s,
                    Economy.getRichersLastUpdate().format(DateTimeFormatter
                            .ofLocalizedDateTime(FormatStyle.SHORT))));
        }
        lastUpdatedMeta.setLore(lastUpdateLore);
        lastUpdated.setItemMeta(lastUpdatedMeta);
        return lastUpdated;
    }

    private ItemStack configureTotalInvestmentsItem() {
        ItemStack total = new ItemStack(Material.SUNFLOWER);
        ItemMeta totalMeta = total.getItemMeta();
        totalMeta.setDisplayName(config.getRankingMenuTotalInvestmentsItemName());
        List<String> totalLore = new ArrayList<>();
        for (String s : config.getRankingMenuTotalInvestmentsItemLore()) {
            totalLore.add(MessageFormat.format(s,
                    Formatter.formatServerCurrency(totalInvestments)));
        }
        totalMeta.setLore(totalLore);
        total.setItemMeta(totalMeta);

        return total;
    }

    /**
     * Configures a generic item
     *
     * @param material
     * @param name
     * @return the item
     */
    private ItemStack configureGenericItem(Material material, String name) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        stack.setItemMeta(meta);

        return stack;
    }

    /**
     * Configures the Back button
     *
     * @return the back button
     */
    private ItemStack configureBackButton() {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.setDisplayName(config.getRankingMenuBackButton());
        back.setItemMeta(meta);
        return back;
    }

    /**
     * Sets the items' positions
     *
     * @param blackGlass
     * @param grayGlass
     * @param back
     * @param richers
     */
    private void setItemsPositions(ItemStack blackGlass, ItemStack grayGlass,
            ItemStack back, List<ItemStack> richers, ItemStack total,
            ItemStack lastUpdated) {
        //black glasses
        int black[] = {0, 3, 5, 8, 9, 11, 12, 14, 15, 17, 18, 19, 25, 26, 27,
            29, 30, 32, 33, 35, 41, 39, 44};
        for (int b : black) {
            inventory.setItem(b, blackGlass);
        }
        //grey glasses
        int grey[] = {1, 2, 6, 7, 10, 13, 16, 28, 31, 34, 37, 40, 42, 43};
        for (int g : grey) {
            inventory.setItem(g, grayGlass);
        }
        //back button
        inventory.setItem(4, back);
        //total investments
        inventory.setItem(36, total);
        //last updated
        inventory.setItem(38, lastUpdated);
        //richers
        int index = 20;
        for (ItemStack richer : richers) {
            inventory.setItem(index, richer);
            index++;
        }
    }
}
