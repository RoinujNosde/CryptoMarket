package net.epconsortium.cryptomarket.ui;

import net.epconsortium.cryptomarket.CryptoMarket;
import net.epconsortium.cryptomarket.util.Configuration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Frame {

    protected final CryptoMarket plugin;
    protected final Configuration configuration;
    private final Frame parent;
    private final Player viewer;
    private final Set<Component> components = ConcurrentHashMap.newKeySet();

    public Frame(@Nullable Frame parent, @NotNull Player viewer) {
        this.parent = parent;
        this.viewer = viewer;
        plugin = CryptoMarket.getInstance();
        configuration = new Configuration(plugin);
    }

    @NotNull
    public abstract String getTitle();

    @NotNull
    public Player getViewer() {
        return viewer;
    }

    @Nullable
    public Frame getParent() {
        return parent;
    }

    public abstract int getSize();

    public abstract void createComponents();

    @Nullable
    public Component getComponent(int slot) {
        for (Component c : getComponents()) {
            if (c.getSlot() == slot) {
                return c;
            }
        }
        return null;
    }

    public void add(@NotNull Component c) {
        components.add(c);
    }

    public void clear() {
        components.clear();
    }

    @NotNull
    public Set<Component> getComponents() {
        return components;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Frame) {
            Frame otherFrame = (Frame) other;
            return getSize() == otherFrame.getSize() && getTitle().equals(otherFrame.getTitle())
                    && getComponents().equals(otherFrame.getComponents());
        }

        return false;
    }

    @Override
    public int hashCode() {
        return getTitle().hashCode() + Integer.hashCode(getSize()) + getComponents().hashCode();
    }

}
