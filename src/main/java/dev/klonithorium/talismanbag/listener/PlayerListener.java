package dev.klonithorium.talismanbag.listener;

import dev.klonithorium.talismanbag.TalismanBagPlugin;
import dev.klonithorium.talismanbag.model.PlayerBag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player login/logout to persist talisman effects across sessions.
 */
public class PlayerListener implements Listener {

    private final TalismanBagPlugin plugin;

    public PlayerListener(TalismanBagPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Load bag data and re-apply effects after a short delay
        // (delay ensures MMOItems has initialized the player's data first)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            PlayerBag bag = plugin.getBagDataManager().loadBag(player.getUniqueId());
            if (!bag.getAllItems().isEmpty()) {
                plugin.getTalismanEffectManager().applyEffects(player);
                plugin.getLogger().info("Restored " + bag.usedSlots() +
                        " talisman(s) for " + player.getName());
            }
        }, 20L); // 1 second delay
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Remove effects and save data
        plugin.getTalismanEffectManager().removeEffects(player);
        plugin.getBagDataManager().unloadBag(player.getUniqueId());
    }
}
