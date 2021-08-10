package net.epconsortium.cryptomarket;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.epconsortium.cryptomarket.database.dao.Investor;
import net.epconsortium.cryptomarket.database.dao.InvestorDao;
import net.epconsortium.cryptomarket.finances.ExchangeRate;
import net.epconsortium.cryptomarket.finances.ExchangeRates;
import net.epconsortium.cryptomarket.util.Formatter;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CMExpansion extends PlaceholderExpansion {

    private static final Pattern INVESTOR_BALANCE_PATTERN = Pattern.compile("balance_(?<coin>\\w+)");
    private static final Pattern COIN_PRICE_PATTERN = Pattern.compile("price_(?<coin>\\w+)");
    private static final Pattern INVESTOR_PATRIMONY_PATTERN = Pattern.compile("investor_patrimony");

    private final CryptoMarket plugin;
    private final InvestorDao investorDao;
    private final ExchangeRates exchangeRates;

    public CMExpansion(CryptoMarket plugin) {
        this.plugin = plugin;
        investorDao = plugin.getInvestorDao();
        exchangeRates = plugin.getExchangeRates();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "cryptomarket";
    }

    @Override
    public @NotNull String getAuthor() {
        return "RoinujNosde";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @NotNull List<String> getPlaceholders() {
        return Arrays.asList("%cryptomarket_investor_patrimony%", "%cryptomarket_balance_<coin>%",
                "%cryptomarket_price_<coin>%");
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        Matcher priceMatcher = COIN_PRICE_PATTERN.matcher(params);
        if (priceMatcher.matches()) {
            String coin = priceMatcher.group("coin");
            ExchangeRate exchangeRate = exchangeRates.getExchangeRate(LocalDate.now());
            if (exchangeRate != null) {
                return Formatter.formatCryptocoin(exchangeRate.getCoinValue(coin.toUpperCase(Locale.ROOT)));
            }
        }
        Investor investor = investorDao.getInvestor(player);
        if (investor == null) {
            plugin.getLogger().info(String.format("%s is offline", player.getName()));
            return "";
        }
        if (INVESTOR_PATRIMONY_PATTERN.matcher(params).matches()) {
            ExchangeRate exchangeRate = exchangeRates.getExchangeRate(LocalDate.now());
            if (exchangeRate != null) {
                return Formatter.formatServerCurrency(investor.getConvertedPatrimony(exchangeRate));
            }
        }
        Matcher balanceMatcher = INVESTOR_BALANCE_PATTERN.matcher(params);
        if (balanceMatcher.matches()) {
            String coin = balanceMatcher.group("coin");
            return Formatter.formatCryptocoin(investor.getBalance(coin.toUpperCase(Locale.ROOT)).getValue());
        }
        return "";
    }
}
