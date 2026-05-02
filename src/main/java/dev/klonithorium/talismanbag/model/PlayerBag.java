package dev.klonithorium.talismanbag.model;

import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Holds the talisman bag data for a single player.
 */
public class PlayerBag {

    private final UUID playerId;
    // Slot index → ItemStack (the raw item stored, so it can be restored)
    private final Map<Integer, ItemStack> slots;
    private final int bagSize;

    public PlayerBag(UUID playerId, int bagSize) {
        this.playerId = playerId;
        this.bagSize = bagSize;
        this.slots = new LinkedHashMap<>();
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public int getBagSize() {
        return bagSize;
    }

    public Map<Integer, ItemStack> getSlots() {
        return Collections.unmodifiableMap(slots);
    }

    public boolean setSlot(int slot, ItemStack item) {
        if (slot < 0 || slot >= bagSize) return false;
        if (item == null) {
            slots.remove(slot);
        } else {
            slots.put(slot, item.clone());
        }
        return true;
    }

    public ItemStack getSlot(int slot) {
        return slots.getOrDefault(slot, null);
    }

    public void clearSlot(int slot) {
        slots.remove(slot);
    }

    public void clearAll() {
        slots.clear();
    }

    /**
     * Returns all stored items as a list (for effect application).
     */
    public List<ItemStack> getAllItems() {
        return new ArrayList<>(slots.values());
    }

    public boolean isFull() {
        return slots.size() >= bagSize;
    }

    public int usedSlots() {
        return slots.size();
    }

    /**
     * Load slots from a flat ItemStack array (from deserialization).
     */
    public void loadFromArray(ItemStack[] contents) {
        slots.clear();
        for (int i = 0; i < Math.min(contents.length, bagSize); i++) {
            if (contents[i] != null) {
                slots.put(i, contents[i]);
            }
        }
    }

    /**
     * Export slots to a flat ItemStack array for serialization.
     */
    public ItemStack[] toArray() {
        ItemStack[] arr = new ItemStack[bagSize];
        slots.forEach((slot, item) -> arr[slot] = item.clone());
        return arr;
    }
}
