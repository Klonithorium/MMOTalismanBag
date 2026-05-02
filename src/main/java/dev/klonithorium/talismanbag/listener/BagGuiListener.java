package dev.klonithorium.talismanbag.listener;

import dev.klonithorium.talismanbag.TalismanBagPlugin;
import dev.klonithorium.talismanbag.gui.TalismanBagGui;
import dev.klonithorium.talismanbag.model.PlayerBag;
import dev.klonithorium.talismanbag.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Handles all inventory click events inside the Talisman Bag GUI.
 */
public class BagGuiListener implements Listener {

    private final TalismanBagPlugin plugin;

    public BagGuiListener(TalismanBagPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof TalismanBagGui bagGui)) return;

        // Ensure it's this player's bag
        if (!bagGui.getPlayer().equals(player)) {
            event.setCancelled(true);
            return;
        }

        Inventory clickedInv = event.getClickedInventory();
        int slot = event.getRawSlot();

        // ── Handle shift-clicks from player's own inventory into the bag ──
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            // Shift-click from player inventory
            if (clickedInv != null && !clickedInv.equals(event.getInventory())) {
                ItemStack item = event.getCurrentItem();
                if (item == null || item.getType().isAir()) return;

                if (!plugin.getTalismanEffectManager().isTalisman(item)) {
                    event.setCancelled(true);
                    sendMsg(player, "messages.not-a-talisman", "&cThat item is not a recognized Talisman!");
                    return;
                }

                if (plugin.getTalismanEffectManager().isDuplicateBlocked(player, item)) {
                    event.setCancelled(true);
                    sendMsg(player, "messages.duplicate-blocked", "&cYou already have that talisman active!");
                    return;
                }

                PlayerBag bag = plugin.getBagDataManager().loadBag(player.getUniqueId());
                if (bag.isFull()) {
                    event.setCancelled(true);
                    sendMsg(player, "messages.bag-full", "&cYour Talisman Bag is full!");
                    return;
                }

                // Allow the shift-click; sync after event
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    syncBagFromGui(bagGui, player);
                    plugin.getTalismanEffectManager().applyEffects(player);
                    plugin.getTalismanEffectManager().notifyActivation(player, item);
                });
                return;
            }

            // Shift-click FROM bag inventory to player inventory (removing talisman)
            if (clickedInv != null && clickedInv.equals(event.getInventory())) {
                ItemStack item = event.getCurrentItem();
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    syncBagFromGui(bagGui, player);
                    plugin.getTalismanEffectManager().applyEffects(player);
                    if (item != null && !item.getType().isAir()) {
                        plugin.getTalismanEffectManager().notifyDeactivation(player, item);
                    }
                });
                return;
            }
        }

        // ── Click inside the bag inventory ──
        if (clickedInv != null && clickedInv.equals(event.getInventory())) {
            ItemStack cursor = event.getCursor();
            ItemStack current = event.getCurrentItem();

            // Placing an item into the bag
            if (cursor != null && !cursor.getType().isAir()) {
                if (!plugin.getTalismanEffectManager().isTalisman(cursor)) {
                    event.setCancelled(true);
                    sendMsg(player, "messages.not-a-talisman", "&cThat item is not a recognized Talisman!");
                    return;
                }
                if (plugin.getTalismanEffectManager().isDuplicateBlocked(player, cursor)) {
                    // Check if we're swapping out the same type
                    if (current == null || current.getType().isAir() ||
                            !sameKey(cursor, current)) {
                        event.setCancelled(true);
                        sendMsg(player, "messages.duplicate-blocked", "&cYou already have that talisman active!");
                        return;
                    }
                }

                // Sync after the click resolves
                ItemStack placed = cursor.clone();
                ItemStack removed = (current != null && !current.getType().isAir()) ? current.clone() : null;
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    syncBagFromGui(bagGui, player);
                    plugin.getTalismanEffectManager().applyEffects(player);
                    plugin.getTalismanEffectManager().notifyActivation(player, placed);
                    if (removed != null) {
                        plugin.getTalismanEffectManager().notifyDeactivation(player, removed);
                    }
                });
                return;
            }

            // Taking an item out of the bag
            if (current != null && !current.getType().isAir()) {
                ItemStack removed = current.clone();
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    syncBagFromGui(bagGui, player);
                    plugin.getTalismanEffectManager().applyEffects(player);
                    plugin.getTalismanEffectManager().notifyDeactivation(player, removed);
                });
            }
            return;
        }

        // ── Click in player's own inventory while bag is open — allow freely ──
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof TalismanBagGui bagGui)) return;

        // Check if any dragged slot is inside the bag
        boolean draggedIntoBag = event.getRawSlots().stream()
                .anyMatch(s -> s < bagGui.getBagSize());

        if (!draggedIntoBag) return;

        ItemStack item = event.getOldCursor();
        if (!plugin.getTalismanEffectManager().isTalisman(item)) {
            event.setCancelled(true);
            sendMsg(player, "messages.not-a-talisman", "&cThat item is not a recognized Talisman!");
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            syncBagFromGui(bagGui, player);
            plugin.getTalismanEffectManager().applyEffects(player);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof TalismanBagGui bagGui)) return;

        // Final sync on close
        syncBagFromGui(bagGui, player);
        plugin.getBagDataManager().saveBag(player.getUniqueId());
        plugin.getTalismanEffectManager().applyEffects(player);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    /**
     * Reads the current GUI inventory state and writes it back to the PlayerBag model.
     */
    private void syncBagFromGui(TalismanBagGui bagGui, Player player) {
        PlayerBag bag = plugin.getBagDataManager().loadBag(player.getUniqueId());
        bag.clearAll();
        Inventory inv = bagGui.getInventory();
        for (int i = 0; i < bagGui.getBagSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && !item.getType().isAir()) {
                bag.setSlot(i, item);
            }
        }
    }

    private boolean sameKey(ItemStack a, ItemStack b) {
        String keyA = plugin.getTalismanEffectManager().getMmoType(a)
                + ":" + plugin.getTalismanEffectManager().getMmoId(a);
        String keyB = plugin.getTalismanEffectManager().getMmoType(b)
                + ":" + plugin.getTalismanEffectManager().getMmoId(b);
        return keyA.equals(keyB);
    }

    private void sendMsg(Player player, String configKey, String fallback) {
        String raw = plugin.getConfig().getString("messages." + configKey.replace("messages.", ""), fallback);
        player.sendMessage(plugin.getPrefix() + MessageUtil.colorize(raw));
    }
}
