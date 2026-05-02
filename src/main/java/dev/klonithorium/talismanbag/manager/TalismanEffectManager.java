package dev.klonithorium.talismanbag.manager;

import dev.klonithorium.talismanbag.TalismanBagPlugin;
import dev.klonithorium.talismanbag.model.PlayerBag;
import dev.klonithorium.talismanbag.util.MessageUtil;
import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.player.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.logging.Level;

/**
 * Manages applying and removing MMOItems talisman effects for players.
 *
 * The core mechanism is the {@link TalismanBagInventory} registered via
 * {@code MMOItems.registerPlayerInventory()} — MMOItems will automatically
 * pull items from the bag when recalculating a player's stats. We just need
 * to trigger {@code PlayerData.get(player).updateInventory()} after any
 * bag change to force a recalculation.
 */
public class TalismanEffectManager {

    private final TalismanBagPlugin plugin;

    // Track which talisman keys (type:id) are currently considered active per player
    private final Map<UUID, Set<String>> activeKeys = new HashMap<>();

    public TalismanEffectManager(TalismanBagPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Register the custom bag inventory with MMOItems so it reads bag items
     * as "equipped" during stat calculations.
     * Call this once from onEnable, AFTER confirming MMOItems is present.
     */
    public void register() {
        if (!plugin.isMmoItemsEnabled()) return;
        try {
            MMOItems.plugin.registerPlayerInventory(new TalismanBagInventory(plugin));
            plugin.getLogger().info("Registered TalismanBag as a MMOItems PlayerInventory source.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to register TalismanBagInventory with MMOItems: " + e.getMessage());
        }
    }

    /**
     * Triggers MMOItems to recalculate a player's stats, which will
     * automatically pull bag contents via TalismanBagInventory.
     *
     * Called after any bag modification (add/remove talisman, open bag, login).
     */
    public void applyEffects(Player player) {
        if (!plugin.isMmoItemsEnabled()) return;
        UUID uuid = player.getUniqueId();

        try {
            PlayerData playerData = PlayerData.get(player);
            if (playerData == null) return;

            // Correct method in MMOItems 6.9.x
            playerData.updateInventory();

            // Track active keys for /talismanbag info
            updateActiveKeys(uuid);

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to apply talisman effects for " + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Remove all talisman effects (clears active key tracking and triggers
     * a stat recalculation — after bag unload, TalismanBagInventory returns empty).
     * Called on player logout.
     */
    public void removeEffects(Player player) {
        if (!plugin.isMmoItemsEnabled()) return;
        UUID uuid = player.getUniqueId();
        activeKeys.remove(uuid);

        try {
            PlayerData playerData = PlayerData.get(player);
            if (playerData == null) return;
            playerData.updateInventory();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to remove talisman effects for " + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Returns true if the given ItemStack is a valid talisman for the bag.
     */
    public boolean isTalisman(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        if (!plugin.isMmoItemsEnabled()) return false;

        try {
            NBTItem nbt = NBTItem.get(item);
            if (!nbt.hasType()) return false;

            String mmoType = nbt.getString("MMOITEMS_ITEM_TYPE");
            if (mmoType == null || mmoType.isEmpty()) return false;

            boolean useTypeWhitelist = plugin.getConfig().getBoolean("use-type-whitelist", true);
            String configuredType = plugin.getConfig().getString("talisman-item-type", "TALISMAN");

            if (useTypeWhitelist) {
                return mmoType.equalsIgnoreCase(configuredType);
            } else {
                String mmoId = nbt.getString("MMOITEMS_ITEM_ID");
                List<String> allowed = plugin.getConfig().getStringList("allowed-talismans");
                return mmoId != null && allowed.contains(mmoId.toUpperCase());
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns true if adding this item to the player's bag would exceed the
     * duplicate talisman limit.
     */
    public boolean isDuplicateBlocked(Player player, ItemStack item) {
        int maxDupes = plugin.getConfig().getInt("max-duplicate-talismans", 1);
        String mmoType = getMmoType(item);
        String mmoId = getMmoId(item);
        if (mmoType == null || mmoId == null) return false;

        String key = mmoType + ":" + mmoId;
        PlayerBag bag = plugin.getBagDataManager().getCachedBag(player.getUniqueId());
        if (bag == null) return false;

        long count = bag.getAllItems().stream()
                .filter(i -> i != null && key.equals(getMmoType(i) + ":" + getMmoId(i)))
                .count();

        return count >= maxDupes;
    }

    public void notifyActivation(Player player, ItemStack item) {
        String name = getDisplayName(item);
        String msg = plugin.getConfig().getString("messages.talisman-activated",
                "&aThe talisman &e{name} &ahas been activated!");
        player.sendMessage(plugin.getPrefix() + MessageUtil.colorize(
                MessageUtil.replace(msg, "name", name)));
    }

    public void notifyDeactivation(Player player, ItemStack item) {
        String name = getDisplayName(item);
        String msg = plugin.getConfig().getString("messages.talisman-deactivated",
                "&cThe talisman &e{name} &chas been deactivated.");
        player.sendMessage(plugin.getPrefix() + MessageUtil.colorize(
                MessageUtil.replace(msg, "name", name)));
    }

    /**
     * Remove all effects from all online players (on plugin disable).
     */
    public void removeAllEffects() {
        for (UUID uuid : new HashSet<>(activeKeys.keySet())) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                removeEffects(player);
            }
        }
        activeKeys.clear();
    }

    /**
     * Reapply effects for all online players with loaded bags.
     */
    public void reloadAll() {
        plugin.getBagDataManager().getLoadedBags().forEach(bag -> {
            Player player = plugin.getServer().getPlayer(bag.getPlayerId());
            if (player != null && player.isOnline()) {
                applyEffects(player);
            }
        });
    }

    // ─── Public helpers ─────────────────────────────────────────────────────

    public String getMmoId(ItemStack item) {
        if (item == null) return null;
        try {
            return NBTItem.get(item).getString("MMOITEMS_ITEM_ID");
        } catch (Exception e) {
            return null;
        }
    }

    public String getMmoType(ItemStack item) {
        if (item == null) return null;
        try {
            return NBTItem.get(item).getString("MMOITEMS_ITEM_TYPE");
        } catch (Exception e) {
            return null;
        }
    }

    public String getDisplayName(ItemStack item) {
        if (item == null) return "Unknown";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        String id = getMmoId(item);
        return id != null ? id : item.getType().name();
    }

    public Set<String> getActiveEffects(UUID uuid) {
        return activeKeys.getOrDefault(uuid, Collections.emptySet());
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private void updateActiveKeys(UUID uuid) {
        Set<String> keys = new HashSet<>();
        PlayerBag bag = plugin.getBagDataManager().getCachedBag(uuid);
        if (bag != null) {
            bag.getAllItems().forEach(item -> {
                String type = getMmoType(item);
                String id = getMmoId(item);
                if (type != null && id != null) {
                    keys.add(type + ":" + id);
                }
            });
        }
        activeKeys.put(uuid, keys);
    }
}
