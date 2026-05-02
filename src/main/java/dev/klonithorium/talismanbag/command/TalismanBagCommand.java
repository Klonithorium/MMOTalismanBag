package dev.klonithorium.talismanbag.command;

import dev.klonithorium.talismanbag.TalismanBagPlugin;
import dev.klonithorium.talismanbag.gui.TalismanBagGui;
import dev.klonithorium.talismanbag.model.PlayerBag;
import dev.klonithorium.talismanbag.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * /talismanbag [open|reload|info]
 * Aliases: /tbag /talisman
 * Author: klonithorium
 */
public class TalismanBagCommand implements CommandExecutor, TabCompleter {

    private final TalismanBagPlugin plugin;

    public TalismanBagCommand(TalismanBagPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Default: open the bag
        if (args.length == 0 || args[0].equalsIgnoreCase("open")) {
            return handleOpen(sender);
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                return handleReload(sender);
            }
            case "info" -> {
                return handleInfo(sender);
            }
            default -> {
                sendUsage(sender, label);
                return true;
            }
        }
    }

    private boolean handleOpen(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getPrefix() + MessageUtil.colorize("&cThis command can only be used by players."));
            return true;
        }
        if (!player.hasPermission("talismanbag.open")) {
            sendMsg(player, "no-permission", "&cYou don't have permission to do that.");
            return true;
        }

        TalismanBagGui gui = new TalismanBagGui(plugin, player);
        gui.open();
        sendMsg(player, "bag-opened", "&aYour Talisman Bag has been opened!");
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("talismanbag.reload")) {
            if (sender instanceof Player p) sendMsg(p, "no-permission", "&cYou don't have permission to do that.");
            else sender.sendMessage("No permission.");
            return true;
        }
        plugin.reload();
        String msg = plugin.getConfig().getString("messages.reloaded", "&aConfiguration reloaded successfully!");
        sender.sendMessage(plugin.getPrefix() + MessageUtil.colorize(msg));
        return true;
    }

    private boolean handleInfo(CommandSender sender) {
        if (!sender.hasPermission("talismanbag.info")) {
            if (sender instanceof Player p) sendMsg(p, "no-permission", "&cYou don't have permission to do that.");
            return true;
        }

        sender.sendMessage(MessageUtil.colorize("&#FFD700&l✦ TalismanBag ✦"));
        sender.sendMessage(MessageUtil.colorize("&7Version: &e" + plugin.getDescription().getVersion()));
        sender.sendMessage(MessageUtil.colorize("&7Author: &eklonithorium"));
        sender.sendMessage(MessageUtil.colorize("&7MMOItems: " + (plugin.isMmoItemsEnabled() ? "&aEnabled" : "&cNot Found")));
        sender.sendMessage(MessageUtil.colorize("&7Bag Size: &e" + plugin.getConfig().getInt("bag-size", 27) + " slots"));

        if (sender instanceof Player player) {
            PlayerBag bag = plugin.getBagDataManager().loadBag(player.getUniqueId());
            Set<String> active = plugin.getTalismanEffectManager().getActiveEffects(player.getUniqueId());
            sender.sendMessage(MessageUtil.colorize("&7Your Talismans: &e" + bag.usedSlots() + " stored, &a" + active.size() + " active"));
        }

        sender.sendMessage(MessageUtil.colorize("&7Commands: &e/talismanbag open&7, &e/talismanbag reload&7, &e/talismanbag info"));
        return true;
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(plugin.getPrefix() + MessageUtil.colorize(
                "&eUsage: &f/" + label + " [open|reload|info]"));
    }

    private void sendMsg(Player player, String key, String fallback) {
        String raw = plugin.getConfig().getString("messages." + key, fallback);
        player.sendMessage(plugin.getPrefix() + MessageUtil.colorize(raw));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("open", "reload", "info")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
