package net.epconsortium.cryptomarket.ui.frames;

import com.cryptomorin.xseries.XMaterial;
import net.epconsortium.cryptomarket.finances.ExchangeRate;
import net.epconsortium.cryptomarket.ui.*;
import net.epconsortium.cryptomarket.ui.ComponentImpl.Builder;
import net.epconsortium.cryptomarket.util.Formatter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CalendarFrame extends Frame {

    private @NotNull YearMonth period = YearMonth.now();

    public CalendarFrame(@Nullable Frame parent, @NotNull Player viewer) {
        super(parent, viewer);
    }

    @Override
    public @NotNull String getTitle() {
        return configuration.getCalendarMenuName();
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public void createComponents() {
        addDays();
        add(back());
        add(previousMonthItem());
        add(nextMonthItem());
        add(period());
        addPanels();
    }

    /**
     * Shows data from the next month
     * It does nothing if the {@link Player} is viewing the current month
     */
    public void nextMonth() {
        if (period.plusMonths(1).isAfter(YearMonth.now())) {
            return;
        }
        period = period.plusMonths(1);
        InventoryDrawer.getInstance().open(this);
    }

    /**
     * Shows data from the previous month
     */
    public void previousMonth() {
        period = period.minusMonths(1);
        InventoryDrawer.getInstance().open(this);
    }

    private void addPanels() {
        int[] blackSlots = {0, 2, 4, 8};
        int[] graySlots = {3};
        Components.addPanels(this, XMaterial.BLACK_STAINED_GLASS_PANE, blackSlots);
        Components.addPanels(this, XMaterial.GRAY_STAINED_GLASS_PANE, graySlots);
    }

    private Component period() {
        return new ComponentImpl(period.toString(), null, XMaterial.FILLED_MAP, 6);
    }

    private Component nextMonthItem() {
        Component component = new Builder(XMaterial.OAK_BUTTON)
                .withDisplayName(configuration.getCalendarMenuNextMonthButtonName()).withSlot(7).build();
        component.setListener(ClickType.LEFT, this::nextMonth);
        return component;
    }

    private Component previousMonthItem() {
        Component component = new Builder(XMaterial.OAK_BUTTON)
                .withDisplayName(configuration.getCalendarMenuPreviousMonthButtonName()).withSlot(5).build();
        component.setListener(ClickType.LEFT, this::previousMonth);
        return component;
    }

    private Component back() {
        Component component = new Builder(XMaterial.ARROW).withSlot(1)
                .withDisplayName(configuration.getCalendarMenuBackButtonName()).build();
        component.setListener(ClickType.LEFT, () -> InventoryDrawer.getInstance().open(getParent()));
        return component;
    }

    private void addDays() {
        Month month = period.getMonth();
        int slot = 18;
        int days = month.length(period.isLeapYear());

        for (int day = 1; day <= days; day++) {
            LocalDate date = LocalDate.of(period.getYear(), month, day);

            List<String> lore;
            if (date.isAfter(LocalDate.now())) {
                lore = Collections.singletonList(configuration.getCalendarMenuNoExchangeRate());
            } else {
                ExchangeRate er = plugin.getExchangeRates().getExchangeRate(date);
                if (er == null) {
                    er = new ExchangeRate();
                }
                lore = getDayItemLore(er);
            }

            Component component = new Builder(XMaterial.GRAY_STAINED_GLASS_PANE).withAmount(day).withSlot(slot)
                    .withLore(lore).withDisplayName(getDayItemName(day)).build();
            add(component);
            slot++;
        }
    }

    @NotNull
    private String getDayItemName(int day) {
        return MessageFormat.format(configuration.getCalendarMenuDayItemName(), day);
    }

    private List<String> getDayItemLore(ExchangeRate rate) {
        List<String> lore = configuration.getLoreOfTheDayItem();
        //Configurando a linha de valores das moedas
        List<String> coinsLine = new ArrayList<>();
        List<String> coins = configuration.getCoins();

        for (String coin : coins) {
            BigDecimal coinValue = rate.getCoinValue(coin);
            String format;
            if (coinValue.equals(new BigDecimal(-1))) {
                format = MessageFormat.format(configuration.getCalendarMenuCoinLine(), coin,
                        configuration.getItemDayError());
            } else {
                format = MessageFormat.format(configuration.getCalendarMenuCoinLine(), coin,
                        Formatter.formatServerCurrency(coinValue));
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

        return lore;
    }
}
