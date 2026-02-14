package me.typical.lixiplugin.commands;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import me.typical.lixiplugin.LXPlugin;
import me.typical.lixiplugin.commands.arguments.MoneyArgument;
import me.typical.lixiplugin.config.types.MessageConfig;
import me.typical.lixiplugin.service.ChatLixiService;
import me.typical.lixiplugin.service.EnvelopeService;
import me.typical.lixiplugin.service.IService;
import me.typical.lixiplugin.util.MessageUtil;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Handles all command registration using CommandAPI.
 */
public class CommandHandler implements IService {

    private final LXPlugin plugin = LXPlugin.getInstance();

    @Override
    public void setup() {
        registerCommands();
        CommandAPI.onEnable();
        MessageUtil.info("Commands registered successfully");
    }

    @Override
    public void shutdown() {
        // CommandAPI handles unregistration
        CommandAPI.onDisable();
    }

    private void registerCommands() {
        // Main command tree
        new CommandAPICommand("lixi")
                .withAliases("lx", "luckmoney")
                .withSubcommands(
                        chatCommand(),
                        directTransferCommand(),
                        envelopeCommand(),
                        claimCommand()
                )
                .executes((sender, args) -> {
                    if (sender instanceof Player player) {
                        MessageUtil.send(player, "<gold>LiXi Commands</gold>");
                        MessageUtil.send(player, "<yellow>/lixi chat <money> <limit></yellow> <gray>- Create chat broadcast</gray>");
                        MessageUtil.send(player, "<yellow>/lixi give <player> <money></yellow> <gray>- Direct transfer</gray>");
                        MessageUtil.send(player, "<yellow>/lixi phongbao <money></yellow> <gray>- Create physical envelope</gray>");
                        MessageUtil.send(player, "<dark_gray>Money formats: 100, 1k, 1.5M, 2T</dark_gray>");
                    } else {
                        sender.sendMessage("=== LiXi Commands ===");
                        sender.sendMessage("/lixi chat <money> <limit> - Create chat broadcast");
                        sender.sendMessage("/lixi give <player> <money> - Direct transfer");
                        sender.sendMessage("/lixi phongbao <money> - Create physical envelope");
                        sender.sendMessage("Money formats: 100, 1k, 1.5M, 2T");
                    }
                })
                .register();
    }

    /**
     * /lixi chat <money> <limit> - Create a chat broadcast lixi
     */
    private CommandAPICommand chatCommand() {
        return new CommandAPICommand("chat")
                .withPermission("lixi.chat")
                .withArguments(
                        new MoneyArgument("money"),
                        new IntegerArgument("limit")
                )
                .executesPlayer((player, args) -> {
                    double amount = (double) args.get("money");
                    int limit = (int) args.get("limit");

                    ChatLixiService service = plugin.getService(ChatLixiService.class);
                    service.createSession(player, amount, limit);
                });
    }

    /**
     * /lixi <player> <money> - Direct transfer to another player
     */
    private CommandAPICommand directTransferCommand() {
        return new CommandAPICommand("give")
                .withPermission("lixi.give")
                .withArguments(
                        new EntitySelectorArgument.OnePlayer("player"),
                        new MoneyArgument("money")
                )
                .executesPlayer((sender, args) -> {
                    Player target = (Player) args.get("player");
                    double amount = (double) args.get("money");

                    if (target == null || !target.isOnline()) {
                        MessageConfig messages = plugin.getConfigManager().getConfig(MessageConfig.class);
                        MessageUtil.send(sender, messages.withPrefix(messages.getPlayerNotOnline())
                                .replace("%player%", args.getRaw("player")));
                        return;
                    }

                    EnvelopeService service = plugin.getService(EnvelopeService.class);
                    service.directTransfer(sender, target, amount);
                });
    }

    /**
     * /lixi phongbao <money> - Create a physical envelope item
     */
    private CommandAPICommand envelopeCommand() {
        return new CommandAPICommand("phongbao")
                .withAliases("envelope", "pb")
                .withPermission("lixi.phongbao")
                .withArguments(new MoneyArgument("money"))
                .executesPlayer((player, args) -> {
                    double amount = (double) args.get("money");

                    EnvelopeService service = plugin.getService(EnvelopeService.class);
                    service.createEnvelope(player, amount);
                });
    }

    /**
     * /lixi claim <id> - Hidden command for claiming chat lixi (triggered by click)
     */
    private CommandAPICommand claimCommand() {
        return new CommandAPICommand("claim")
                .withArguments(new StringArgument("id"))
                .executesPlayer((player, args) -> {
                    String idString = (String) args.get("id");

                    try {
                        UUID sessionId = UUID.fromString(idString);
                        ChatLixiService service = plugin.getService(ChatLixiService.class);
                        service.claimSession(player, sessionId);
                    } catch (IllegalArgumentException e) {
                        MessageConfig messages = plugin.getConfigManager().getConfig(MessageConfig.class);
                        MessageUtil.send(player, messages.withPrefix(messages.getGenericError()));
                    }
                });
    }

}
