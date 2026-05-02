package dev.klonithorium.talismanbag.model;

import org.bukkit.inventory.ItemStack;

/**
 * Represents a talisman stored inside a player's Talisman Bag.
 */
public class TalismanItem {

    private final ItemStack itemStack;
    private final String mmoItemsType;
    private final String mmoItemsId;
    private final String displayName;

    public TalismanItem(ItemStack itemStack, String mmoItemsType, String mmoItemsId, String displayName) {
        this.itemStack = itemStack.clone();
        this.mmoItemsType = mmoItemsType;
        this.mmoItemsId = mmoItemsId;
        this.displayName = displayName;
    }

    public ItemStack getItemStack() {
        return itemStack.clone();
    }

    public String getMmoItemsType() {
        return mmoItemsType;
    }

    public String getMmoItemsId() {
        return mmoItemsId;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns a unique key for this talisman (type:id).
     */
    public String getKey() {
        return mmoItemsType + ":" + mmoItemsId;
    }
}
