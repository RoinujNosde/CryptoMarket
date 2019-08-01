package net.epconsortium.cryptomarket.ui;

import java.util.Objects;
import org.bukkit.event.inventory.InventoryClickEvent;

class Helper {

    /**
     * Checks if this is not a regular inventory (like a Chest or something)
     *
     * @param event event
     * @return true if it is a custom inventory
     */
    static boolean isCustomInventory(InventoryClickEvent event) {
        Objects.requireNonNull(event);

        return event.getInventory().getName() != null
                && event.getCurrentItem() != null
                && event.getCurrentItem().hasItemMeta()
                && event.getCurrentItem().getItemMeta().hasDisplayName();
    }
}
