package dev.klonithorium.talismanbag;

import dev.klonithorium.talismanbag.command.TalismanBagCommand;
import dev.klonithorium.talismanbag.listener.BagGuiListener;
import dev.klonithorium.talismanbag.listener.PlayerListener;
import dev.klonithorium.talismanbag.manager.BagDataManager;
import dev.klonithorium.talismanbag.manager.TalismanEffectManager;
import dev.klonithorium.talismanbag.util.MessageUtil;
import org.bukkit.plugin.java.JavaPlugin;

public class TalismanBagPlugin extends JavaPlugin {

    private static TalismanBagPlugin instance;

    private BagDataManager bagDataManager;
    private TalismanEffectManager talismanEffectManager;
    private boolean mmoItemsEnabled = false;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        saveDefaultConfig();

        // Check MMOItems
        if (getServer().getPluginManager().getPlugin("MMOItems") != null
                && getServer().getPluginManager().isPluginEnabled("MMOItems")) {
            mmoItemsEnabled = true;
            getLogger().info("MMOItems found! Talisman effects integration enabled.");
        } else {
            getLogger().warning("MMOItems not found! Talisman effect detection will be limited.");
        }

        // Init managers
        bagDataManager = new BagDataManager(this);
        talismanEffectManager = new TalismanEffectManager(this);

        // Register listeners
        getServer().getPluginManager().registerEvents(new BagGuiListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // Register command
        TalismanBagCommand cmd = new TalismanBagCommand(this);
        getCommand("talismanbag").setExecutor(cmd);
        getCommand("talismanbag").setTabCompleter(cmd);

        getLogger().info("TalismanBag v" + getDescription().getVersion() + " by klonithorium has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save all open bags and remove active effects
        if (bagDataManager != null) {
            bagDataManager.saveAll();
        }
        if (talismanEffectManager != null) {
            talismanEffectManager.removeAllEffects();
        }
        getLogger().info("TalismanBag has been disabled. All data saved.");
    }

    public void reload() {
        reloadConfig();
        bagDataManager.reload();
        talismanEffectManager.reloadAll();
    }

    public static TalismanBagPlugin getInstance() {
        return instance;
    }

    public BagDataManager getBagDataManager() {
        return bagDataManager;
    }

    public TalismanEffectManager getTalismanEffectManager() {
        return talismanEffectManager;
    }

    public boolean isMmoItemsEnabled() {
        return mmoItemsEnabled;
    }

    public String getPrefix() {
        return MessageUtil.colorize(getConfig().getString("messages.prefix", "&6[TalismanBag] &r"));
    }
}
