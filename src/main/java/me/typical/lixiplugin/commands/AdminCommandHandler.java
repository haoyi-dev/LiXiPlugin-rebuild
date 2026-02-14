package me.typical.lixiplugin.commands;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import io.github.projectunified.uniitem.api.ItemKey;
import dev.jorel.commandapi.arguments.IntegerArgument;
import me.typical.lixiplugin.LXPlugin;
import me.typical.lixiplugin.config.types.MainConfig;
import me.typical.lixiplugin.config.types.MessageConfig;
import me.typical.lixiplugin.hook.UniItemHook;
import me.typical.lixiplugin.service.IService;
import me.typical.lixiplugin.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Handles admin commands using CommandAPI.
 */
public class AdminCommandHandler implements IService {

    private final LXPlugin plugin = LXPlugin.getInstance();

    @Override
    public void setup() {
        registerCommands();
        MessageUtil.info("Admin commands registered successfully");
    }

    @Override
    public void shutdown() {
        // CommandAPI handles unregistration
    }

    private void registerCommands() {
        // Admin command tree - separate from main /lixi command
        new CommandAPICommand("lixiadmin")
                .withAliases("lxa")
                .withPermission("lixi.admin")
                .withSubcommands(
                        setItemCommand(),
                        giveItemPackCommand(),
                        reloadCommand()
                )
                .executes((sender, args) -> {
                    if (sender instanceof Player player) {
                        MessageUtil.send(player, "<gold>=== LiXi Admin Commands ===</gold>");
                        MessageUtil.send(player, "<yellow>/lixiadmin setitem</yellow> <gray>- Set held item as envelope</gray>");
                        MessageUtil.send(player, "<yellow>/lixiadmin givegoilixi [số lượng]</yellow> <gray>- Give gói lì xì (vật phẩm)</gray>");
                        MessageUtil.send(player, "<yellow>/lixiadmin reload</yellow> <gray>- Reload configurations</gray>");
                    } else {
                        sender.sendMessage("=== LiXi Admin Commands ===");
                        sender.sendMessage("/lixiadmin setitem - Set held item as envelope");
                        sender.sendMessage("/lixiadmin givegoilixi [amount] - Give item pack envelope");
                        sender.sendMessage("/lixiadmin reload - Reload configurations");
                    }
                })
                .register();
    }

    /**
     * /lixiadmin setitem - Set the held item as envelope item
     */
    private CommandAPICommand setItemCommand() {
        return new CommandAPICommand("setitem")
                .withPermission("lixi.admin")
                .executesPlayer((player, args) -> {
                    ItemStack item = player.getInventory().getItemInMainHand();
                    MessageConfig messages = plugin.getConfigManager().getConfig(MessageConfig.class);

                    if (item.getType().isAir()) {
                        MessageUtil.send(player, messages.withPrefix(messages.getAdminSetItemNoItem()));
                        return;
                    }

                    // Detect item using UniItem API
                    UniItemHook uniItem = plugin.getService(UniItemHook.class);
                    ItemKey itemKey = uniItem.detectItemKey(item);

                    if (itemKey == null) {
                        MessageUtil.send(player, messages.withPrefix("<red>Could not detect item type! Item may be vanilla or unsupported.</red>"));
                        return;
                    }

                    // Get the plugin type and item ID
                    String pluginType = itemKey.type();
                    String itemId = itemKey.id();

                    MessageUtil.send(player, "<gray>Detected: <yellow>" + pluginType + ":" + itemId + "</yellow></gray>");

                    // Update config based on plugin type
                    MainConfig config = plugin.getConfigManager().getConfig(MainConfig.class);
                    MainConfig.EnvelopeConfig envelopeConfig = config.getEnvelope();

                    // Clear all other plugin IDs
                    envelopeConfig.getItemsadder().setItemId("");
                    envelopeConfig.getOraxen().setItemId("");
                    envelopeConfig.getNexo().setItemId("");

                    // Set the appropriate plugin ID
                    switch (pluginType.toLowerCase()) {
                        case "itemsadder" -> {
                            envelopeConfig.getItemsadder().setItemId(itemId);
                            MessageUtil.send(player, "<green>Set ItemsAdder item: <white>" + itemId + "</white></green>");
                        }
                        case "oraxen" -> {
                            envelopeConfig.getOraxen().setItemId(itemId);
                            MessageUtil.send(player, "<green>Set Oraxen item: <white>" + itemId + "</white></green>");
                        }
                        case "nexo" -> {
                            envelopeConfig.getNexo().setItemId(itemId);
                            MessageUtil.send(player, "<green>Set Nexo item: <white>" + itemId + "</white></green>");
                        }
                        default -> {
                            MessageUtil.send(player, "<red>Unsupported plugin type: <white>" + pluginType + "</white></red>");
                            MessageUtil.send(player, "<gray>Supported: itemsadder, oraxen, nexo</gray>");
                            return;
                        }
                    }

                    // Save config to disk
                    plugin.getConfigManager().saveConfig(MainConfig.class, config);

                    MessageUtil.send(player, messages.withPrefix(messages.getAdminSetItemSuccess()));
                });
    }

    /**
     * /lixiadmin givegoilixi [amount] - Give gói lì xì (item pack), items from lixi.yml
     */
    private CommandAPICommand giveItemPackCommand() {
        return new CommandAPICommand("givegoilixi")
                .withAliases("goilixi", "itemphongbao")
                .withPermission("lixi.admin")
                .withOptionalArguments(new IntegerArgument("amount", 1, 64))
                .executesPlayer((player, args) -> {
                    int amount = (Integer) args.getOptional("amount").orElse(1);
                    UniItemHook uniItem = plugin.getService(UniItemHook.class);
                    ItemStack pack = uniItem.createItemPackEnvelope();
                    pack.setAmount(amount);
                    player.getInventory().addItem(pack);
                    MessageConfig messages = plugin.getConfigManager().getConfig(MessageConfig.class);
                    MessageUtil.send(player, messages.withPrefix(messages.getAdminItemPackGiven()).replace("%amount%", String.valueOf(amount)));
                });
    }

    /**
     * /lixiadmin reload - Reload configurations
     */
    private CommandAPICommand reloadCommand() {
        return new CommandAPICommand("reload")
                .withPermission("lixi.admin")
                .executes((sender, args) -> {
                    plugin.getConfigManager().reloadAll();
                    MessageConfig messages = plugin.getConfigManager().getConfig(MessageConfig.class);

                    if (sender instanceof Player player) {
                        MessageUtil.send(player, messages.withPrefix(messages.getAdminReloadSuccess()));
                    } else {
                        sender.sendMessage("Configuration reloaded successfully!");
                    }
                });
    }
}
