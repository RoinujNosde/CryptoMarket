package net.epconsortium.cryptomarket.ui.frames;

import com.cryptomorin.xseries.XMaterial;
import net.epconsortium.cryptomarket.conversation.NegotiationConversation;
import net.epconsortium.cryptomarket.database.dao.Investor;
import net.epconsortium.cryptomarket.finances.ExchangeRate;
import net.epconsortium.cryptomarket.finances.ExchangeRates;
import net.epconsortium.cryptomarket.finances.Negotiation;
import net.epconsortium.cryptomarket.ui.Component;
import net.epconsortium.cryptomarket.ui.ComponentImpl;
import net.epconsortium.cryptomarket.ui.ComponentImpl.Builder;
import net.epconsortium.cryptomarket.ui.Frame;
import net.epconsortium.cryptomarket.ui.InventoryDrawer;
import net.epconsortium.cryptomarket.util.Formatter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.cryptomorin.xseries.XMaterial.*;
import static net.epconsortium.cryptomarket.ui.Components.addPanels;

public class MenuFrame extends Frame {
    private final Investor investor;
    private final ExchangeRates rates;

    public MenuFrame(@Nullable Frame parent, @NotNull Player viewer) {
        super(parent, viewer);
        investor = plugin.getInvestorDao().getInvestor(viewer);
        rates = plugin.getExchangeRates();
    }

    @Override
    public @NotNull String getTitle() {
        return configuration.getMenuName();
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public void createComponents() {
        add(profit());
        add(coins());
        add(wallet());
        add(ranking());
        add(calendar());
        add(update());
        addGlasses();
    }

    @NotNull
    private Component update() {
        ComponentImpl update = new ComponentImpl(configuration.getButtonUpdateName(), null, STRUCTURE_VOID, 43);
        update.setPermission(ClickType.LEFT, "cryptomarket.update");
        update.setListener(ClickType.LEFT, () -> {
            if (ExchangeRates.errorOccurred()) {
                ExchangeRates er = plugin.getExchangeRates();
                er.updateAll();
                getViewer().closeInventory();

                String msg = configuration.getMessageUpdatingContent();
                msg = MessageFormat.format(msg, er.getMinutesToUpdate());
                getViewer().sendMessage(msg);
            } else {
                getViewer().sendMessage(configuration.getMessageContentAlreadyUptodate());
                getViewer().closeInventory();
            }
        });
        return update;
    }

    @NotNull
    private Component calendar() {
        ComponentImpl calendar = new ComponentImpl(configuration.getButtonCalendarName(), null, FILLED_MAP, 25);
        calendar.setPermission(ClickType.LEFT, "cryptomarket.calendar");
        calendar.setListener(ClickType.LEFT, () ->
                InventoryDrawer.getInstance().open(new CalendarFrame(this, getViewer())));
        return calendar;
    }

    private void addGlasses() {
        int[] blackSlots = {28, 0, 3, 5, 8, 10, 16, 21, 23, 34, 45, 46, 49, 52, 52, 31};
        addPanels(this, BLACK_STAINED_GLASS_PANE, blackSlots);

        int[] greySlots = {1, 2, 4, 6, 7, 9, 11, 13, 15, 17, 18, 20, 24, 26, 22, 27, 29, 47, 48, 51, 50, 30, 36, 38, 39,
                40, 41, 42, 44, 32, 33, 35};
        addPanels(this, GRAY_STAINED_GLASS_PANE, greySlots);
    }

    private Component profit() {
        return new Builder(XMaterial.LIME_DYE).withDisplayName(configuration.getButtonProfitName())
                .withLore(getProfitItemLore()).withSlot(19).build();
    }

    private List<String> getProfitItemLore() {
        List<String> coinLines = new ArrayList<>();
        String coinLine = configuration.getButtonProfitCoinLine();
        ExchangeRate rate = rates.getExchangeRate(LocalDate.now());
        if (rate == null) {
            rate = new ExchangeRate();
        }
        for (String coin : configuration.getCoins()) {
            BigDecimal profitD = investor.getBalance(coin).getProfitPercentage(
                    rate.getCoinValue(coin));
            String color = configuration.getButtonProfitNeuterColor();
            if (profitD.compareTo(BigDecimal.ZERO) > 0) {
                color = configuration.getButtonProfitPositiveColor();
            } else if (profitD.compareTo(BigDecimal.ZERO) < 0) {
                color = configuration.getButtonProfitNegativeColor();
            }

            coinLines.add(MessageFormat.format(coinLine, coin, color,
                    Formatter.formatServerCurrency(profitD)));
        }
        List<String> lore = configuration.getButtonProfitLore();
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

    private Component wallet() {
        return new Builder(XMaterial.BOOK).withDisplayName(configuration.getButtonWalletName())
                .withLore(getWalletLore()).withSlot(14).build();
    }

    private Component coins() {
        Component coins = new ComponentImpl(configuration.getButtonCoinsName(), getCoinsLore(), SUNFLOWER, 12);

        ItemMeta meta = Objects.requireNonNull(coins.getItemMeta());
        meta.addEnchant(Enchantment.ARROW_DAMAGE, 1, false);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        coins.setItemMeta(meta);

        coins.setPermission(ClickType.RIGHT, "cryptomarket.negotiate");
        coins.setListener(ClickType.RIGHT, () -> {
            getViewer().closeInventory();
            new NegotiationConversation(plugin, Negotiation.PURCHASE, getViewer()).start();
        });
        coins.setPermission(ClickType.LEFT, "cryptomarket.negotiate");
        coins.setListener(ClickType.LEFT, () -> {
            getViewer().closeInventory();
            new NegotiationConversation(plugin, Negotiation.SELL, getViewer()).start();
        });

        return coins;
    }

    private List<String> getWalletLore() {
        List<String> lore = configuration.getButtonWalletLore();
        double vaultBalance = plugin.getVaultEconomy().getBalance(getViewer());

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
        String coinLine = configuration.getButtonWalletCoinLine();
        for (String coin : configuration.getCoins()) {
            String format = MessageFormat.format(coinLine, coin, Formatter.formatCryptocoin(investor.getBalance(coin)
                            .getValue()));
            coinsTemp.add(format);
        }

        lore.addAll(coinsIndex, coinsTemp);
        lore.remove(coinsLineTemp);

        return lore;
    }

    private List<String> getCoinsLore() {
        ExchangeRate er = rates.getExchangeRate(LocalDate.now());
        List<String> lore = configuration.getButtonCoinsLore();
        if (er == null) {
            er = new ExchangeRate();
        }

        //Configuring the line of values of the item
        List<String> coinsLine = new ArrayList<>();
        for (String coin : configuration.getCoins()) {
            BigDecimal coinValue = er.getCoinValue(coin);
            String format;
            if (coinValue.equals(new BigDecimal(-1))) {
                format = MessageFormat.format(configuration.getButtonCoinsCoinLine(), coin,
                        configuration.getButtonCoinsError());
            } else {
                format = MessageFormat.format(configuration.getButtonCoinsCoinLine(), coin,
                        Formatter.formatServerCurrency(coinValue));
            }
            coinsLine.add(format);
        }

        //Adding the line of values to the lore
        int index = 0; // FIXME: 16/05/2021 Duplicate code
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
        return lore;
    }

    private Component ranking() {
        Component component = new Builder(PLAYER_HEAD).withDisplayName(configuration.getButtonRankingName())
                .withSlot(37).build();
        component.setPermission(ClickType.LEFT, "cryptomarket.ranking");
        component.setListener(ClickType.LEFT, () -> {
            ExchangeRate exchangeRate = plugin.getExchangeRates().getExchangeRate(LocalDate.now());
            if (exchangeRate == null) {
                getViewer().sendMessage(configuration.getMessageErrorAccessingRankingData());
                getViewer().closeInventory();
                return;
            }
            InventoryDrawer.getInstance().open(new RankingFrame(this, getViewer(), exchangeRate));
        });
        return component;
    }

}
