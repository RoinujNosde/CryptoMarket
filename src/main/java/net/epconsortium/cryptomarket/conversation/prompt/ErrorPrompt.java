package net.epconsortium.cryptomarket.conversation.prompt;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.MessagePrompt;
import org.bukkit.conversations.Prompt;

/**
 * Prompt used when the player has not egough balance
 *
 * @author roinujnosde
 */
public class ErrorPrompt extends MessagePrompt {

    @Override
    protected Prompt getNextPrompt(ConversationContext context) {
        return new AmountPrompt();
    }

    @Override
    public String getPromptText(ConversationContext context) {
        return Helper.getConfiguration(context).getMessageErrorNotEnoughBalance();
    }
}
