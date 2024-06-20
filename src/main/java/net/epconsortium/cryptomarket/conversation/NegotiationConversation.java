package net.epconsortium.cryptomarket.conversation;

import net.epconsortium.cryptomarket.CryptoMarket;
import net.epconsortium.cryptomarket.conversation.prompt.ExitWarningPrompt;
import net.epconsortium.cryptomarket.database.dao.Investor;
import net.epconsortium.cryptomarket.finances.Negotiation;
import net.epconsortium.cryptomarket.util.Configuration;
import org.bukkit.ChatColor;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.ConversationPrefix;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Class used to have a conversation with the player 
 *
 * @author roinujnosde
 */
public class NegotiationConversation implements ConversationPrefix {

    private final CryptoMarket plugin;
    private final Player player;
    private final Negotiation negotiation;
    private final Configuration config;

    public NegotiationConversation(CryptoMarket plugin, Negotiation negotiation, Player player) {
        this.negotiation = Objects.requireNonNull(negotiation);
        this.plugin = Objects.requireNonNull(plugin);
        this.player = Objects.requireNonNull(player);
        config = new Configuration(plugin);
    }

    /**
     * Starts the Negotiation Conversation with the Player
     */
    public void start() {
        if (!player.isOnline()) {
            return;
        }
        Investor investor = plugin.getInvestorDao().getInvestor(player);
        if (investor == null) {
            player.sendMessage(config.getMessageErrorConnectingToDatabase());
            return;
        }
        createConversation(investor).begin();
    }

    /**
     * Creates the Conversation object
     * 
     * @param investor the investor
     * @return the Conversation
     */
    private Conversation createConversation(Investor investor) {
        Map<Object, Object> data = new HashMap<>();
        data.put("negotiation", negotiation);
        data.put("investor", investor);

        return new ConversationFactory(plugin)
                .withFirstPrompt(new ExitWarningPrompt())
                .withLocalEcho(true)
                .withInitialSessionData(data)
                .withPrefix(this)
                .withEscapeSequence(ChatColor.stripColor(
                        config.getConversationWordOfExit()))
                .buildConversation(player);
    }

    @Override
    public String getPrefix(ConversationContext conversationContext) {
        return config.getNegotiationChatPrefix();
    }
}
