package net.epconsortium.cryptomarket.ui;

import com.cryptomorin.xseries.XMaterial;

public class Components {

    private Components() {
    }

    public static Component generic(XMaterial material, String displayName, int slot) {
        return new ComponentImpl(displayName, null, material, slot);
    }

    public static void addPanels(Frame frame, XMaterial material, int[] slots) {
        for (int slot : slots) {
            frame.add(generic(material, " ", slot));
        }
    }
}
