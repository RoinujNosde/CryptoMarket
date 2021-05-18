package net.epconsortium.cryptomarket.ui;

import net.epconsortium.cryptomarket.CryptoMarket;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InventoryDrawer {
    private static InventoryDrawer instance;
    private final CryptoMarket plugin = CryptoMarket.getInstance();
    private final ConcurrentHashMap<UUID, Frame> OPENING = new ConcurrentHashMap<>();

    private InventoryDrawer() {
    }

    public static InventoryDrawer getInstance() {
        if (instance == null) {
            instance = new InventoryDrawer();
        }
        return instance;
    }

    public void open(@Nullable Frame frame) {
        if (frame == null) {
            return;
        }
        UUID uuid = frame.getViewer().getUniqueId();
        if (frame.equals(OPENING.get(uuid))) {
            return;
        }

        OPENING.put(uuid, frame);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

            Inventory inventory = prepareInventory(frame);

            if (!frame.equals(OPENING.get(uuid))) {
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                frame.getViewer().openInventory(inventory);
                InventoryController.getInstance().register(frame);
                OPENING.remove(uuid);
            });
        });
    }

    @NotNull
    private Inventory prepareInventory(@NotNull Frame frame) {
        Inventory inventory = Bukkit.createInventory(frame.getViewer(), frame.getSize(), frame.getTitle());
        setComponents(inventory, frame);
        return inventory;
    }


    private void setComponents(@NotNull Inventory inventory, @NotNull Frame frame) {
        frame.clear();
        frame.createComponents();

        Set<Component> components = frame.getComponents();
        if (components.isEmpty()) {
            plugin.getLogger().warning(String.format("Frame %s has no components", frame.getTitle()));
            return;
        }
        for (Component c : frame.getComponents()) {
            if (c.getSlot() >= frame.getSize()) {
                continue;
            }
            inventory.setItem(c.getSlot(), c.getItem());
        }
    }

}

