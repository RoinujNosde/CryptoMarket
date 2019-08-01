package net.epconsortium.cryptomarket.conversation.prompt;

import net.epconsortium.cryptomarket.CryptoMarket;
import net.epconsortium.cryptomarket.finances.Economy;
import net.epconsortium.cryptomarket.finances.Negotiation;
import net.epconsortium.cryptomarket.util.Configuration;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.NumericPrompt;
import org.bukkit.conversations.Prompt;

import java.math.BigDecimal;

/**
 * Prompt that asks the amount to be negotiated
 * @author roinujnosde
 */
public class AmountPrompt extends NumericPrompt {
    @Override
    protected Prompt acceptValidatedInput(ConversationContext context, Number number) {
        CryptoMarket plugin = (CryptoMarket) context.getPlugin();
        Configuration config = new Configuration(plugin);
        String coin = ((String) context.getSessionData("coin"));
        Economy economia = new Economy(plugin, coin);
        BigDecimal amount = new BigDecimal(number.toString());

        if (economia.convert(amount).doubleValue() < 0) {
            return new OutdatedDataPrompt();
        }
        context.setSessionData("amount", amount);

        return new ConfirmationPrompt(config.getNegotiationYesWord(), config.getNegotiationNoWord());
    }

    @Override
    protected boolean isNumberValid(ConversationContext context, Number input) {
        return !(input.doubleValue() <= 0);
    }

    @Override
    protected String getFailedValidationText(ConversationContext context, Number number) {
        Configuration config = Helper.getConfiguration(context);
        return config.getMessageErrorInvalidValue();
    }

    @Override
    public String getPromptText(ConversationContext context) {
        Configuration config = Helper.getConfiguration(context);
        Negotiation negotiation = (Negotiation) context.getSessionData("negotiation");
        switch (negotiation) {
            case PURCHASE:
                return config.getMessageBuyNegotiation();
            case SELL:
                return config.getMessageSellNegotiation();
        }

        return null;
    }
}
