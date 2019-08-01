package net.epconsortium.cryptomarket.conversation.prompt;

import net.epconsortium.cryptomarket.util.Configuration;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.FixedSetPrompt;
import org.bukkit.conversations.Prompt;

import java.text.MessageFormat;

/**
 * Prompt that asks the user to choose the coin used in the negotiation
 * If only one coin is configured, this prompt will not be called
 * 
 * @author roinujnosde
 */
public class CoinPrompt extends FixedSetPrompt {

    public CoinPrompt(String... fixedSet) {
        super(fixedSet);
    }
    
    @Override
    protected Prompt acceptValidatedInput(ConversationContext context,
            String input) {
        context.setSessionData("coin", input.toUpperCase());
        return new AmountPrompt();
    }

    @Override
    public String getPromptText(ConversationContext context) {
        Configuration config = Helper.getConfiguration(context);
        return config.getMessageChooseCoin();
    }

    @Override
    protected String getFailedValidationText(ConversationContext context,
            String invalidInput) {
        Configuration config = Helper.getConfiguration(context);

        return MessageFormat.format(config.getMessageValidCoins(),
                formatFixedSet());
    }

    @Override
    protected boolean isInputValid(ConversationContext context, String input) {
        return super.isInputValid(context, input.toUpperCase());
    }
}
