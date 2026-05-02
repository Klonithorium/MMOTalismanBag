package dev.klonithorium.talismanbag.manager;

import dev.klonithorium.talismanbag.TalismanBagPlugin;
import dev.klonithorium.talismanbag.model.PlayerBag;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages loading/saving of PlayerBag data to YAML files per player.
 */
public class BagDataManager {

    private final TalismanBagPlugin plugin;
    private final File dataFolder;
    private final Map<UUID, PlayerBag> loadedBags = new HashMap<>();

    public BagDataManager(TalismanBagPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) dataFolder.mkdirs();
    }

    public int getBagSize() {
        return plugin.getConfig().getInt("bag-size", 27);
    }

    /**
     * Load a player's bag (from disk or cache).
     */
    public PlayerBag loadBag(UUID playerId) {
        if (loadedBags.containsKey(playerId)) {
            return loadedBags.get(playerId);
        }

        PlayerBag bag = new PlayerBag(playerId, getBagSize());
        File file = getFile(playerId);

        if (file.exists()) {
            FileConfiguration data = YamlConfiguration.loadConfiguration(file);
            ItemStack[] contents = new ItemStack[getBagSize()];
            for (int i = 0; i < getBagSize(); i++) {
                if (data.contains("slots." + i)) {
                    contents[i] = data.getItemStack("slots." + i);
                }
            }
            bag.loadFromArray(contents);
        }

        loadedBags.put(playerId, bag);
        return bag;
    }

    /**
     * Save a player's bag to disk.
     */
    public void saveBag(UUID playerId) {
        PlayerBag bag = loadedBags.get(playerId);
        if (bag == null) return;

        File file = getFile(playerId);
        FileConfiguration data = new YamlConfiguration();

        ItemStack[] contents = bag.toArray();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                data.set("slots." + i, contents[i]);
            }
        }

        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save bag data for " + playerId + ": " + e.getMessage());
        }
    }

    /**
     * Unload a bag from cache (call after saving on logout).
     */
    public void unloadBag(UUID playerId) {
        saveBag(playerId);
        loadedBags.remove(playerId);
    }

    /**
     * Save and unload all cached bags (on plugin disable).
     */
    public void saveAll() {
        new HashSet<>(loadedBags.keySet()).forEach(this::saveBag);
        loadedBags.clear();
    }

    public void reload() {
        // Re-load all currently cached bags from disk
        Set<UUID> ids = new HashSet<>(loadedBags.keySet());
        loadedBags.clear();
        ids.forEach(this::loadBag);
    }

    public PlayerBag getCachedBag(UUID playerId) {
        return loadedBags.get(playerId);
    }

    public Collection<PlayerBag> getLoadedBags() {
        return loadedBags.values();
    }

    private File getFile(UUID playerId) {
        return new File(dataFolder, playerId.toString() + ".yml");
    }
}
