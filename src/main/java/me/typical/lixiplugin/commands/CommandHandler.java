package me.typical.lixiplugin.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import me.typical.lixiplugin.LXPlugin;
import me.typical.lixiplugin.commands.arguments.MoneyArgument;
import me.typical.lixiplugin.config.types.MessageConfig;
import me.typical.lixiplugin.economy.LixiCurrency;
import me.typical.lixiplugin.service.ChatLixiService;
import me.typical.lixiplugin.service.EnvelopeService;
import me.typical.lixiplugin.service.IService;
import me.typical.lixiplugin.util.MessageUtil;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;

/** User commands: chat, give, phongbao, claim. */
public class CommandHandler implements IService {

    private final LXPlugin plugin = LXPlugin.getInstance();

    @Override
    public void setup() {
        registerCommands();
        MessageUtil.info("Commands registered successfully");
    }

    @Override
    public void shutdown() {
    }

    private void registerCommands() {
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
                        MessageUtil.send(player, "<yellow>/lixi chat <số tiền> <số người></yellow> <gray>- Lì xì chat (tiền)</gray>");
                        MessageUtil.send(player, "<yellow>/lixi chat <số điểm> <số người> points</yellow> <gray>- Lì xì chat (PlayerPoints)</gray>");
                        MessageUtil.send(player, "<yellow>/lixi give <người chơi> <số tiền></yellow> <gray>- Chuyển tiền</gray>");
                        MessageUtil.send(player, "<yellow>/lixi give <người chơi> <số điểm> points</yellow> <gray>- Chuyển points</gray>");
                        MessageUtil.send(player, "<yellow>/lixi phongbao <số tiền></yellow> <gray>- Tạo phong bì tiền</gray>");
                        MessageUtil.send(player, "<yellow>/lixi phongbao <số điểm> points</yellow> <gray>- Tạo phong bì points</gray>");
                        MessageUtil.send(player, "<dark_gray>Tiền: 100, 1k, 1.5M | Points: số nguyên</dark_gray>");
                    } else {
                        sender.sendMessage("=== LiXi Commands ===");
                        sender.sendMessage("/lixi chat <amount> <limit> [points] - Chat broadcast");
                        sender.sendMessage("/lixi give <player> <amount> [points] - Direct transfer");
                        sender.sendMessage("/lixi phongbao <amount> [points] - Create envelope");
                    }
                })
                .register();
    }

    private CommandAPICommand chatCommand() {
        return new CommandAPICommand("chat")
                .withPermission("lixi.chat")
                .withArguments(
                        new MoneyArgument("chatAmount"),
                        new IntegerArgument("chatLimit", 1)
                )
                .withOptionalArguments(
                        new StringArgument("chatCurrency").replaceSuggestions(ArgumentSuggestions.strings("points"))
                )
                .executesPlayer((player, args) -> {
                    double amount = ((Number) Objects.requireNonNull(args.get("chatAmount"))).doubleValue();
                    int limit = ((Number) Objects.requireNonNull(args.get("chatLimit"))).intValue();
                    Object currencyArg = args.getOptional("chatCurrency").orElse(null);
                    boolean usePoints = currencyArg != null && "points".equalsIgnoreCase(currencyArg.toString());
                    LixiCurrency currency = usePoints ? LixiCurrency.POINTS : LixiCurrency.VAULT;
                    double amt = usePoints ? (int) amount : amount;
                    plugin.getService(ChatLixiService.class).createSession(player, amt, limit, currency);
                });
    }

    private CommandAPICommand directTransferCommand() {
        return new CommandAPICommand("give")
                .withPermission("lixi.give")
                .withArguments(
                        new EntitySelectorArgument.OnePlayer("giveTarget"),
                        new MoneyArgument("giveAmount")
                )
                .withOptionalArguments(
                        new StringArgument("giveCurrency").replaceSuggestions(ArgumentSuggestions.strings("points"))
                )
                .executesPlayer((sender, args) -> {
                    Player target = (Player) args.get("giveTarget");
                    double amount = ((Number) Objects.requireNonNull(args.get("giveAmount"))).doubleValue();
                    Object currencyArg = args.getOptional("giveCurrency").orElse(null);
                    boolean usePoints = currencyArg != null && "points".equalsIgnoreCase(currencyArg.toString());
                    LixiCurrency currency = usePoints ? LixiCurrency.POINTS : LixiCurrency.VAULT;
                    double amt = usePoints ? (int) amount : amount;
                    if (target == null || !target.isOnline()) {
                        MessageConfig messages = plugin.getConfigManager().getConfig(MessageConfig.class);
                        String targetStr = args.getRaw("giveTarget");
                        MessageUtil.send(sender, messages.withPrefix(messages.getPlayerNotOnline())
                                .replace("%player%", targetStr != null ? targetStr : "?"));
                        return;
                    }
                    plugin.getService(EnvelopeService.class).directTransfer(sender, target, amt, currency);
                });
    }

    private CommandAPICommand envelopeCommand() {
        return new CommandAPICommand("phongbao")
                .withAliases("envelope", "pb")
                .withPermission("lixi.phongbao")
                .withArguments(new MoneyArgument("pbAmount"))
                .withOptionalArguments(
                        new StringArgument("pbCurrency").replaceSuggestions(ArgumentSuggestions.strings("points"))
                )
                .executesPlayer((player, args) -> {
                    double amount = ((Number) Objects.requireNonNull(args.get("pbAmount"))).doubleValue();
                    Object currencyArg = args.getOptional("pbCurrency").orElse(null);
                    boolean usePoints = currencyArg != null && "points".equalsIgnoreCase(currencyArg.toString());
                    LixiCurrency currency = usePoints ? LixiCurrency.POINTS : LixiCurrency.VAULT;
                    double amt = usePoints ? (int) amount : amount;
                    plugin.getService(EnvelopeService.class).createEnvelope(player, amt, currency);
                });
    }

    private CommandAPICommand claimCommand() {
        return new CommandAPICommand("claim")
                .withArguments(new StringArgument("id"))
                .executesPlayer((player, args) -> {
                    String idString = (String) args.get("id");
                    if (idString == null || idString.isBlank()) {
                        MessageConfig messages = plugin.getConfigManager().getConfig(MessageConfig.class);
                        MessageUtil.send(player, messages.withPrefix(messages.getGenericError()));
                        return;
                    }
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
