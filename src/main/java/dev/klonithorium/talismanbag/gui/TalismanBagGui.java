package dev.klonithorium.talismanbag.gui;

import dev.klonithorium.talismanbag.TalismanBagPlugin;
import dev.klonithorium.talismanbag.model.PlayerBag;
import dev.klonithorium.talismanbag.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Creates and manages the Talisman Bag GUI inventory.
 */
public class TalismanBagGui implements InventoryHolder {

    private final TalismanBagPlugin plugin;
    private final Player player;
    private final Inventory inventory;
    private final int bagSize;

    // The last row of slots is reserved for a info display item
    private static final int INFO_SLOT_OFFSET = 4; // center of last row

    public TalismanBagGui(TalismanBagPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;

        int configSize = plugin.getConfig().getInt("bag-size", 27);
        // Ensure it's a valid chest size (multiple of 9, max 54)
        configSize = Math.min(54, Math.max(9, configSize));
        configSize = (int) (Math.ceil(configSize / 9.0) * 9);
        this.bagSize = configSize;

        String title = MessageUtil.colorize(
                plugin.getConfig().getString("gui.title", "&6✦ Talisman Bag ✦"));
        this.inventory = Bukkit.createInventory(this, bagSize, title);
    }

    /**
     * Populate inventory with the player's stored talismans and decoration.
     */
    public void build() {
        PlayerBag bag = plugin.getBagDataManager().loadBag(player.getUniqueId());
        inventory.clear();

        // Fill talisman item slots from saved bag data
        for (int i = 0; i < bagSize; i++) {
            ItemStack stored = bag.getSlot(i);
            if (stored != null) {
                inventory.setItem(i, stored);
            }
        }
    }

    /**
     * Create a decorative filler item.
     */
    public static ItemStack buildFiller(TalismanBagPlugin plugin) {
        String matName = plugin.getConfig().getString("gui.filler-material", "GRAY_STAINED_GLASS_PANE");
        Material mat;
        try {
            mat = Material.valueOf(matName.toUpperCase());
        } catch (IllegalArgumentException e) {
            mat = Material.GRAY_STAINED_GLASS_PANE;
        }

        String name = plugin.getConfig().getString("gui.filler-name", " ");
        ItemStack filler = new ItemStack(mat);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtil.colorize(name));
            filler.setItemMeta(meta);
        }
        return filler;
    }

    /**
     * Build the info/status item shown in the GUI.
     */
    public ItemStack buildInfoItem(int activeTalismans) {
        ItemStack item = new ItemStack(Material.ENDER_CHEST);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtil.colorize("&#FFD700✦ Talisman Bag"));
            meta.setLore(List.of(
                    MessageUtil.colorize("&7Author: &eklonithorium"),
                    MessageUtil.colorize("&7Active talismans: &a" + activeTalismans),
                    MessageUtil.colorize("&7Slots: &e" + bagSize),
                    "",
                    MessageUtil.colorize("&7Place &eTalisman &7items to"),
                    MessageUtil.colorize("&7keep their effects active!")
            ));
        }
        item.setItemMeta(meta);
        return item;
    }

    public void open() {
        build();
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }

    public int getBagSize() {
        return bagSize;
    }
}
