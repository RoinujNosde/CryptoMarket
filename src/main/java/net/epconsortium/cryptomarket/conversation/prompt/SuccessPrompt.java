package net.epconsortium.cryptomarket.conversation.prompt;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.MessagePrompt;
import org.bukkit.conversations.Prompt;

/**
 * Prompt used when the negotiation is over and well succeeded
 * 
 * @author roinujnosde
 */
public class SuccessPrompt extends MessagePrompt {
    @Override
    protected Prompt getNextPrompt(ConversationContext conversationContext) {
        return END_OF_CONVERSATION;
    }

    @Override
    public String getPromptText(ConversationContext context) {
        return Helper.getConfiguration(context).getMessageSuccessfulNegotiation();
    }

}
