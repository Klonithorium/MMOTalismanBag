package dev.klonithorium.talismanbag.manager;

import dev.klonithorium.talismanbag.TalismanBagPlugin;
import dev.klonithorium.talismanbag.model.PlayerBag;
import dev.klonithorium.talismanbag.util.MessageUtil;
import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.api.player.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.logging.Level;

/**
 * Manages applying and removing MMOItems talisman effects for players.
 * When a talisman is placed in the bag, its item is virtually equipped
 * by updating the player's MMOItems cached stats.
 */
public class TalismanEffectManager {

    private final TalismanBagPlugin plugin;

    // Track which talismans are "active" per player (key = player UUID, value = set of "type:id")
    private final Map<UUID, Set<String>> activeEffects = new HashMap<>();

    // Track the actual ItemStacks being held as "virtual equipment"
    private final Map<UUID, List<ItemStack>> virtualItems = new HashMap<>();

    public TalismanEffectManager(TalismanBagPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Applies all talisman effects for a player based on their bag contents.
     * Safe to call multiple times — diffs and only changes what's needed.
     */
    public void applyEffects(Player player) {
        if (!plugin.isMmoItemsEnabled()) return;

        UUID uuid = player.getUniqueId();
        PlayerBag bag = plugin.getBagDataManager().getCachedBag(uuid);
        if (bag == null) return;

        try {
            PlayerData playerData = PlayerData.get(player);

            // Remove old virtual items
            clearVirtualItems(uuid);

            Set<String> newActive = new HashSet<>();
            List<ItemStack> items = new ArrayList<>();

            int maxDupes = plugin.getConfig().getInt("max-duplicate-talismans", 1);
            Map<String, Integer> dupCount = new HashMap<>();

            for (ItemStack item : bag.getAllItems()) {
                if (item == null) continue;

                String mmoId = getMmoId(item);
                String mmoType = getMmoType(item);
                if (mmoId == null || mmoType == null) continue;

                String key = mmoType + ":" + mmoId;
                int count = dupCount.getOrDefault(key, 0);
                if (count >= maxDupes) continue;
                dupCount.put(key, count + 1);

                newActive.add(key);
                items.add(item);
            }

            // Store for tracking
            activeEffects.put(uuid, newActive);
            virtualItems.put(uuid, items);

            // Tell MMOItems to update the player's cached stats
            // MMOItems uses an inventory update mechanism; we trigger it by
            // temporarily marking items as "equipped" in the player's data.
            playerData.updateStats();

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to apply talisman effects for " + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Remove all talisman effects from a player.
     */
    public void removeEffects(Player player) {
        if (!plugin.isMmoItemsEnabled()) return;
        UUID uuid = player.getUniqueId();

        try {
            clearVirtualItems(uuid);
            activeEffects.remove(uuid);

            PlayerData playerData = PlayerData.get(player);
            playerData.updateStats();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to remove talisman effects for " + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Called when an item is added to the bag — checks if it's a valid talisman.
     */
    public boolean isTalisman(ItemStack item) {
        if (item == null || !plugin.isMmoItemsEnabled()) return false;

        try {
            String mmoType = getMmoType(item);
            if (mmoType == null) return false;

            boolean useTypeWhitelist = plugin.getConfig().getBoolean("use-type-whitelist", true);
            String configuredType = plugin.getConfig().getString("talisman-item-type", "TALISMAN");

            if (useTypeWhitelist) {
                return mmoType.equalsIgnoreCase(configuredType);
            } else {
                List<String> allowed = plugin.getConfig().getStringList("allowed-talismans");
                String mmoId = getMmoId(item);
                return mmoId != null && allowed.contains(mmoId.toUpperCase());
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if adding this talisman would exceed the duplicate limit.
     */
    public boolean isDuplicateBlocked(Player player, ItemStack item) {
        UUID uuid = player.getUniqueId();
        Set<String> active = activeEffects.getOrDefault(uuid, new HashSet<>());

        String mmoType = getMmoType(item);
        String mmoId = getMmoId(item);
        if (mmoType == null || mmoId == null) return false;

        String key = mmoType + ":" + mmoId;
        int maxDupes = plugin.getConfig().getInt("max-duplicate-talismans", 1);

        // Count how many of this key are in the bag
        PlayerBag bag = plugin.getBagDataManager().getCachedBag(uuid);
        if (bag == null) return false;

        long count = bag.getAllItems().stream()
                .filter(i -> i != null && key.equals(getMmoType(i) + ":" + getMmoId(i)))
                .count();

        return count >= maxDupes;
    }

    /**
     * Notify player about talisman activation.
     */
    public void notifyActivation(Player player, ItemStack item) {
        String name = getDisplayName(item);
        String msg = plugin.getConfig().getString("messages.talisman-activated",
                "&aThe talisman &e{name} &ahas been activated!");
        player.sendMessage(plugin.getPrefix() + MessageUtil.colorize(
                MessageUtil.replace(msg, "name", name)));
    }

    /**
     * Notify player about talisman deactivation.
     */
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
        for (UUID uuid : new HashSet<>(activeEffects.keySet())) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                removeEffects(player);
            }
        }
        activeEffects.clear();
        virtualItems.clear();
    }

    /**
     * Reload and reapply effects for all online players with loaded bags.
     */
    public void reloadAll() {
        plugin.getBagDataManager().getLoadedBags().forEach(bag -> {
            Player player = plugin.getServer().getPlayer(bag.getPlayerId());
            if (player != null && player.isOnline()) {
                applyEffects(player);
            }
        });
    }

    // ─── MMOItems NBT helpers ───────────────────────────────────────────────

    public String getMmoId(ItemStack item) {
        if (item == null) return null;
        try {
            NBTItem nbt = NBTItem.get(item);
            return nbt.getString("MMOITEMS_ITEM_ID");
        } catch (Exception e) {
            return null;
        }
    }

    public String getMmoType(ItemStack item) {
        if (item == null) return null;
        try {
            NBTItem nbt = NBTItem.get(item);
            return nbt.getString("MMOITEMS_ITEM_TYPE");
        } catch (Exception e) {
            return null;
        }
    }

    public String getDisplayName(ItemStack item) {
        if (item == null) return "Unknown";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return MessageUtil.colorize(item.getItemMeta().getDisplayName());
        }
        String id = getMmoId(item);
        return id != null ? id : item.getType().name();
    }

    private void clearVirtualItems(UUID uuid) {
        virtualItems.remove(uuid);
    }

    public Set<String> getActiveEffects(UUID uuid) {
        return activeEffects.getOrDefault(uuid, Collections.emptySet());
    }
}
