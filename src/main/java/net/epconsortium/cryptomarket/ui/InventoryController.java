package net.epconsortium.cryptomarket.ui;

import net.epconsortium.cryptomarket.CryptoMarket;
import net.epconsortium.cryptomarket.util.Configuration;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class InventoryController implements Listener {
    private static InventoryController instance;

    private final Map<UUID, Frame> frames = new HashMap<>();
    private final Configuration configuration = new Configuration(CryptoMarket.getInstance());

    private InventoryController() {}

    public static InventoryController getInstance() {
        if (instance == null) {
            instance = new InventoryController();
        }
        return instance;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClose(InventoryCloseEvent event) {
        HumanEntity entity = event.getPlayer();
        if (!(entity instanceof Player)) {
            return;
        }

        frames.remove(entity.getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(InventoryClickEvent event) {
        HumanEntity entity = event.getWhoClicked();
        if (!(entity instanceof Player)) {
            return;
        }

        Frame frame = frames.get(entity.getUniqueId());
        if (frame == null) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null || event.getClickedInventory().getType() == InventoryType.PLAYER) {
            return;
        }

        Component component = frame.getComponent(event.getSlot());
        if (component == null) {
            return;
        }

        ClickType click = event.getClick();
        Runnable listener = component.getListener(click);
        if (listener == null) {
            return;
        }

        String permission = component.getPermission(click);
        if (permission != null) {
            if (!entity.hasPermission(permission)) {
                entity.sendMessage(configuration.getMessageErrorNoPermission());
                return;
            }
        }

        Bukkit.getScheduler().runTask(CryptoMarket.getInstance(), () -> {
            ItemStack currentItem = event.getCurrentItem();
            if (currentItem == null) return;

            ItemMeta itemMeta = currentItem.getItemMeta();
            Objects.requireNonNull(itemMeta).setLore(Collections.singletonList("..."));
            currentItem.setItemMeta(itemMeta);

            listener.run();
        });
    }

    /**
     * Registers the frame in the InventoryController
     * @param frame the frame
     *
     * @author RoinujNosde
     */
    public void register(@NotNull Frame frame) {
        frames.put(frame.getViewer().getUniqueId(), frame);
    }

    /**
     * Checks if the Player is registered
     *
     * @param player the Player
     * @return if they are registered
     */
    public boolean isRegistered(@NotNull Player player) {
        return frames.containsKey(player.getUniqueId());
    }
}

