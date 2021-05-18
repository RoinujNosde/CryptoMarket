package net.epconsortium.cryptomarket.conversation.prompt;

import net.epconsortium.cryptomarket.CryptoMarket;
import net.epconsortium.cryptomarket.finances.Economy;
import net.epconsortium.cryptomarket.finances.Negotiation;
import net.epconsortium.cryptomarket.database.dao.Investor;
import net.epconsortium.cryptomarket.util.Configuration;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.FixedSetPrompt;
import org.bukkit.conversations.Prompt;

import java.math.BigDecimal;
import java.text.MessageFormat;

/**
 * Prompt that asks the player to confirm the negotiation
 * 
 * @author roinujnosde
 */
public class ConfirmationPrompt extends FixedSetPrompt {

    private final String cancel;

    public ConfirmationPrompt(String yes, String cancel) {
        super(yes, cancel);
        this.cancel = cancel;
    }

    @Override
    protected Prompt acceptValidatedInput(ConversationContext context, String s) {
        if (s.equals(cancel)) {
            return END_OF_CONVERSATION;
        }

        CryptoMarket plugin = (CryptoMarket) context.getPlugin();
        String coin = (String) context.getSessionData("coin");
        Economy economy = plugin.getEconomy();
        Investor investor = (Investor) context.getSessionData("investor");
        
        BigDecimal value = (BigDecimal) context.getSessionData("amount");

        Negotiation negotiation = getNegotiation(context);
        switch (negotiation) {
            case PURCHASE:
                if (!economy.buy(coin, investor, value)) {
                    return new ErrorPrompt();
                }
                break;
            case SELL:
                if (!economy.sell(coin, investor, value)) {
                    return new ErrorPrompt();
                }
                break;
        }

        return new SuccessPrompt();
    }

    @Override
    public String getPromptText(ConversationContext context) {
        CryptoMarket plugin = (CryptoMarket) context.getPlugin();
        String coin = (String) context.getSessionData("coin");
        Configuration config = new Configuration(plugin);
        String message = config.getMessageNegotiationConfirmation();
        String action = null;

        Negotiation negotiation = getNegotiation(context);
        switch (negotiation) {
            case SELL:
                action = config.getActionSell();
                break;
            case PURCHASE:
                action = config.getActionBuy();
                break;
        }

        BigDecimal amount = ((BigDecimal) context.getSessionData("amount"));
        BigDecimal value = plugin.getEconomy().convert(coin, amount);

        return MessageFormat.format(message, action, amount, coin, value) + " " + formatFixedSet();
    }

    private Negotiation getNegotiation(ConversationContext context) {
        return (Negotiation) context.getSessionData("negotiation");
    }
}
