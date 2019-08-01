package net.epconsortium.cryptomarket.conversation.prompt;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.MessagePrompt;
import org.bukkit.conversations.Prompt;

/**
 * Prompt used when the data is not up-to-date and the negotiation cannot proceed
 * 
 * @author roinujnosde
 */
public class OutdatedDataPrompt extends MessagePrompt {
    @Override
    protected Prompt getNextPrompt(ConversationContext conversationContext) {
        return END_OF_CONVERSATION;
    }

    @Override
    public String getPromptText(ConversationContext context) {
        return Helper.getConfiguration(context).getMessageOutdatedData();
    }
}
