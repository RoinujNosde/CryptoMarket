package net.epconsortium.cryptomarket.ui;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public abstract class Component {

    private final HashMap<ClickType, Runnable> listeners = new HashMap<>();
    private final HashMap<ClickType, String> permissions = new HashMap<>();

    @NotNull
    public abstract ItemStack getItem();

    public abstract int getSlot();

    @Nullable
    public ItemMeta getItemMeta() {
        return getItem().getItemMeta();
    }

    public void setItemMeta(@NotNull ItemMeta itemMeta) {
        getItem().setItemMeta(itemMeta);
    }

    public void setPermission(@NotNull ClickType click, @Nullable String permission) {
        permissions.put(click, permission);
    }

    @Nullable
    public String getPermission(@NotNull ClickType click) {
        return permissions.get(click);
    }

    public void setListener(@NotNull ClickType click, @Nullable Runnable listener) {
        listeners.put(click, listener);
    }

    @Nullable
    public Runnable getListener(@NotNull ClickType click) {
        return listeners.get(click);
    }
}

