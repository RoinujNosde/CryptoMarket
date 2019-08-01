package net.epconsortium.cryptomarket.conversation.prompt;

import net.epconsortium.cryptomarket.util.Configuration;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.MessagePrompt;
import org.bukkit.conversations.Prompt;

import java.text.MessageFormat;
import java.util.List;

/**
 * Prompt that reminds the player of how to exit the negotiation
 *
 * @author roinujnosde
 */
public class ExitWarningPrompt extends MessagePrompt {

    @Override
    protected Prompt getNextPrompt(ConversationContext context) {
        List<String> coins = Helper.getConfiguration(context).getCoins();
        if (coins.size() == 1) {
            context.setSessionData("coin", coins.get(0).toUpperCase());
            return new AmountPrompt();
        }
        return new CoinPrompt(coins.toArray(new String[0]));
    }

    @Override
    public String getPromptText(ConversationContext context) {
        Configuration config = Helper.getConfiguration(context);

        return MessageFormat.format(config.getMessageExitWarning(),
                config.getConversationWordOfExit());
    }
}
