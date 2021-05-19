package net.epconsortium.cryptomarket.ui.frames;

import com.cryptomorin.xseries.XMaterial;
import net.epconsortium.cryptomarket.database.dao.Investor;
import net.epconsortium.cryptomarket.finances.Economy;
import net.epconsortium.cryptomarket.finances.ExchangeRate;
import net.epconsortium.cryptomarket.ui.Component;
import net.epconsortium.cryptomarket.ui.ComponentImpl;
import net.epconsortium.cryptomarket.ui.Frame;
import net.epconsortium.cryptomarket.ui.InventoryDrawer;
import net.epconsortium.cryptomarket.util.Formatter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.cryptomorin.xseries.XMaterial.*;
import static net.epconsortium.cryptomarket.ui.Components.addPanels;

public class RankingFrame extends Frame {

    private final Economy econ;
    private final List<Investor> richersList;
    private final double totalInvestments;
    private final ExchangeRate exchangeRate;

    public RankingFrame(@Nullable Frame parent, @NotNull Player viewer, @NotNull ExchangeRate exchangeRate) {
        super(parent, viewer);
        this.exchangeRate = exchangeRate;
        econ = plugin.getEconomy();
        richersList = econ.getTopInvestors(5);
        totalInvestments = econ.getTotalInvestments();
    }

    @Override
    public @NotNull String getTitle() {
        return configuration.getRankingMenuName();
    }

    @Override
    public int getSize() {
        return 45;
    }

    @Override
    public void createComponents() {
        addGlasses();
        add(backButton());
        add(totalInvestments());
        add(lastUpdated());
        for (int i = 0; i < 5; i++) {
            add(richer(i, i + 20));
        }
    }

    private Component richer(int index, int slot) {
        ItemStack head = XMaterial.PLAYER_HEAD.parseItem(true);
        ItemMeta meta = Objects.requireNonNull(head).getItemMeta();
        int rank = index + 1;

        String displayName;
        ArrayList<String> lore = new ArrayList<>();
        if (index >= richersList.size()) {
            displayName = MessageFormat.format(configuration.getRankingMenuNoRicherLore(), rank);
            head.setItemMeta(meta);
        } else {
            Investor richer = richersList.get(index);
            SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
            skullMeta.setOwner(richer.getPlayer().getName());
            displayName = MessageFormat.format(configuration.getRankingMenuRicherItemName(), rank, richer.getPlayer().getName());

            List<String> configLore = configuration.getRankingMenuRicherItemLore();
            BigDecimal patrimony = richer.getConvertedPatrimony(exchangeRate);
            double percentage = 0;
            if (totalInvestments > 0) {
                percentage = (patrimony.doubleValue() / totalInvestments) * 100;
            }
            for (String s : configLore) {
                lore.add(MessageFormat.format(s, Formatter.formatServerCurrency(patrimony),
                        Formatter.formatPercentage(percentage)));
            }
            head.setItemMeta(skullMeta);
        }

        return new ComponentImpl(displayName, lore, head, slot);
    }

    private Component lastUpdated() {
        List<String> lastUpdateLore = new ArrayList<>();
        for (String s : configuration.getRankingMenuLastUpdatedItemLore()) {
            lastUpdateLore.add(MessageFormat.format(s,
                    econ.getRichersLastUpdate().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT))));
        }
        return new ComponentImpl(configuration.getRankingMenuLastUpdatedItemName(), lastUpdateLore, CLOCK, 38);
    }

    private Component totalInvestments() {
        List<String> totalLore = new ArrayList<>();
        for (String s : configuration.getRankingMenuTotalInvestmentsItemLore()) {
            totalLore.add(MessageFormat.format(s, Formatter.formatServerCurrency(totalInvestments)));
        }

        return new ComponentImpl(configuration.getRankingMenuTotalInvestmentsItemName(), totalLore, XMaterial.SUNFLOWER, 36);
    }

    private Component backButton() {
        ComponentImpl component = new ComponentImpl(configuration.getRankingMenuBackButton(), null, ARROW, 4);
        component.setListener(ClickType.LEFT, () -> InventoryDrawer.getInstance().open(getParent()));
        return component;
    }

    private void addGlasses() {
        int[] blackSlots = {0, 3, 5, 8, 9, 11, 12, 14, 15, 17, 18, 19, 25, 26, 27, 29, 30, 32, 33, 35, 41, 39, 44};
        addPanels(this, BLACK_STAINED_GLASS_PANE, blackSlots);

        int[] greySlots = {1, 2, 6, 7, 10, 13, 16, 28, 31, 34, 37, 40, 42, 43};
        addPanels(this, GRAY_STAINED_GLASS_PANE, greySlots);
    }
}
