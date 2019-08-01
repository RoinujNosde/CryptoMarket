package net.epconsortium.cryptomarket.conversation.prompt;

import java.util.Objects;
import net.epconsortium.cryptomarket.CryptoMarket;
import net.epconsortium.cryptomarket.util.Configuration;
import org.bukkit.conversations.ConversationContext;

class Helper {

    static Configuration getConfiguration(ConversationContext context) {
        Objects.requireNonNull(context);
        
        return new Configuration(((CryptoMarket) context.getPlugin()));
    }
}
