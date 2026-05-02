package dev.klonithorium.talismanbag.manager;

import dev.klonithorium.talismanbag.TalismanBagPlugin;
import io.lumine.mythic.lib.api.player.EquipmentSlot;
import net.Indyuce.mmoitems.api.player.inventory.EquippedItem;
import net.Indyuce.mmoitems.comp.inventory.PlayerInventory;
import net.Indyuce.mmoitems.comp.inventory.SlotEquippedItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implements MMOItems' PlayerInventory interface so that talismans stored in
 * the bag are treated as "equipped" items by MMOItems' stat engine.
 *
 * Registered once on plugin enable via MMOItems.plugin.registerPlayerInventory().
 * MMOItems will call getInventory(player) every time it recalculates stats.
 */
public class TalismanBagInventory implements PlayerInventory {

    private final TalismanBagPlugin plugin;

    public TalismanBagInventory(TalismanBagPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Returns all talisman items stored in the player's bag as EquippedItems.
     * MMOItems will apply the stats/abilities of these items as if they were equipped.
     */
    @Override
    public List<EquippedItem> getInventory(Player player) {
        UUID uuid = player.getUniqueId();
        var bag = plugin.getBagDataManager().getCachedBag(uuid);
        if (bag == null) {
            bag = plugin.getBagDataManager().loadBag(uuid);
        }

        List<EquippedItem> result = new ArrayList<>();
        List<ItemStack> items = bag.getAllItems();
        for (int i = 0; i < items.size(); i++) {
            ItemStack item = items.get(i);
            if (item != null && !item.getType().isAir()) {
                result.add(new SlotEquippedItem(player, i, item, EquipmentSlot.OTHER));
            }
        }
        return result;
    }
}
