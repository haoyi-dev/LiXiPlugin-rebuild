package me.typical.lixiplugin.service;

import me.typical.lixiplugin.LXPlugin;
import me.typical.lixiplugin.config.types.Lixi;
import me.typical.lixiplugin.config.types.MainConfig;
import me.typical.lixiplugin.config.types.MessageConfig;
import me.typical.lixiplugin.economy.EconomyProvider;
import me.typical.lixiplugin.economy.LixiCurrency;
import me.typical.lixiplugin.hook.UniItemHook;
import me.typical.lixiplugin.util.EffectUtil;
import me.typical.lixiplugin.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

/** Manages physical envelopes and direct transfers. Handles right-click to claim. */
public class EnvelopeService implements IService, Listener {

    private final LXPlugin plugin = LXPlugin.getInstance();

    @Override
    public void setup() {
        MessageUtil.info("EnvelopeService initialized");
    }

    @Override
    public void shutdown() {
    }

    public boolean createEnvelope(Player creator, double amount, LixiCurrency currencyType) {
        EconomyProvider provider = plugin.getEconomyProvider(currencyType);
        if (provider == null || !provider.isAvailable()) {
            MessageConfig messages = plugin.getConfigManager().getConfig(MessageConfig.class);
            MessageUtil.send(creator, messages.withPrefix(currencyType == LixiCurrency.POINTS
                    ? messages.getPointsNotAvailable() : messages.getVaultNotAvailable()));
            return false;
        }

        MessageConfig messages = plugin.getConfigManager().getConfig(MessageConfig.class);
        MainConfig.EnvelopeConfig envelopeConfig = plugin.getConfigManager().getConfig(MainConfig.class).getEnvelope();
        MainConfig.ChatLixiConfig chatConfig = plugin.getConfigManager().getConfig(MainConfig.class).getChatLixi();

        double minAmount = currencyType == LixiCurrency.POINTS ? envelopeConfig.getMinPoints() : chatConfig.getMinAmount();
        double maxAmount = currencyType == LixiCurrency.POINTS ? envelopeConfig.getMaxPoints() : chatConfig.getMaxAmount();

        if (amount < minAmount) {
            MessageUtil.send(creator, messages.withPrefix(messages.getAmountTooLow())
                    .replace("%min%", provider.format(minAmount)));
            return false;
        }
        if (amount > maxAmount) {
            MessageUtil.send(creator, messages.withPrefix(messages.getAmountTooHigh())
                    .replace("%max%", provider.format(maxAmount)));
            return false;
        }

        double balance = provider.getBalance(creator);
        if (balance < amount) {
            MessageUtil.send(creator, messages.withPrefix(messages.getInsufficientBalance())
                    .replace("%required%", provider.format(amount))
                    .replace("%balance%", provider.format(balance)));
            return false;
        }

        if (!provider.withdraw(creator, amount)) {
            MessageUtil.send(creator, messages.withPrefix(messages.getGenericError()));
            return false;
        }

        UUID envelopeId = UUID.randomUUID();
        UniItemHook uniItem = plugin.getService(UniItemHook.class);
        ItemStack envelope = uniItem.createEnvelopeItem(amount, envelopeId, currencyType);

        DatabaseManager db = plugin.getService(DatabaseManager.class);
        db.insertEnvelope(envelopeId, amount, creator.getUniqueId(), currencyType).whenComplete((v, ex) -> {
            if (ex != null) {
                MessageUtil.error("Failed to save envelope to database: " + envelopeId);
                ex.printStackTrace();
                provider.deposit(creator, amount);
                plugin.getFoliaLib().getScheduler().runAtEntity(creator, task -> {
                    MessageUtil.send(creator, messages.withPrefix(messages.getGenericError()));
                });
            }
        });

        creator.getInventory().addItem(envelope);
        MessageUtil.send(creator, messages.withPrefix(messages.getEnvelopeCreated())
                .replace("%amount%", provider.format(amount)));
        return true;
    }

    public boolean directTransfer(Player sender, Player receiver, double amount, LixiCurrency currencyType) {
        EconomyProvider provider = plugin.getEconomyProvider(currencyType);
        if (provider == null || !provider.isAvailable()) {
            MessageConfig messages = plugin.getConfigManager().getConfig(MessageConfig.class);
            MessageUtil.send(sender, messages.withPrefix(currencyType == LixiCurrency.POINTS
                    ? messages.getPointsNotAvailable() : messages.getVaultNotAvailable()));
            return false;
        }

        MessageConfig messages = plugin.getConfigManager().getConfig(MessageConfig.class);
        MainConfig.ChatLixiConfig config = plugin.getConfigManager().getConfig(MainConfig.class).getChatLixi();
        double minAmount = currencyType == LixiCurrency.POINTS ? config.getMinPoints() : config.getMinAmount();
        double maxAmount = currencyType == LixiCurrency.POINTS ? config.getMaxPoints() : config.getMaxAmount();

        if (sender.getUniqueId().equals(receiver.getUniqueId())) {
            MessageUtil.send(sender, messages.withPrefix(messages.getCannotSendToSelf()));
            return false;
        }

        if (amount < minAmount) {
            MessageUtil.send(sender, messages.withPrefix(messages.getAmountTooLow())
                    .replace("%min%", provider.format(minAmount)));
            return false;
        }
        if (amount > maxAmount) {
            MessageUtil.send(sender, messages.withPrefix(messages.getAmountTooHigh())
                    .replace("%max%", provider.format(maxAmount)));
            return false;
        }

        double balance = provider.getBalance(sender);
        if (balance < amount) {
            MessageUtil.send(sender, messages.withPrefix(messages.getInsufficientBalance())
                    .replace("%required%", provider.format(amount))
                    .replace("%balance%", provider.format(balance)));
            return false;
        }

        if (!provider.withdraw(sender, amount)) {
            MessageUtil.send(sender, messages.withPrefix(messages.getGenericError()));
            return false;
        }

        if (!provider.deposit(receiver, amount)) {
            provider.deposit(sender, amount);
            MessageUtil.send(sender, messages.withPrefix(messages.getGenericError()));
            return false;
        }

        MessageUtil.send(sender, messages.withPrefix(messages.getTransferSent())
                .replace("%player%", receiver.getName())
                .replace("%amount%", provider.format(amount)));
        MessageUtil.send(receiver, messages.withPrefix(messages.getTransferReceived())
                .replace("%player%", sender.getName())
                .replace("%amount%", provider.format(amount)));

        plugin.getFoliaLib().getScheduler().runAtEntity(receiver, task -> EffectUtil.playLixiEffect(receiver));
        return true;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) {
            return;
        }

        UniItemHook uniItem = plugin.getService(UniItemHook.class);
        if (uniItem.isLixiItemPack(item)) {
            event.setCancelled(true);
            handleItemPackClaim(player, item);
            return;
        }
        if (!uniItem.isLixiEnvelope(item)) {
            return;
        }
        event.setCancelled(true);
        UUID envelopeId = uniItem.getEnvelopeId(item);
        if (envelopeId == null) {
            MessageUtil.send(player, plugin.getConfigManager().getConfig(MessageConfig.class)
                    .withPrefix(plugin.getConfigManager().getConfig(MessageConfig.class).getGenericError()));
            return;
        }

        DatabaseManager db = plugin.getService(DatabaseManager.class);
        db.claimEnvelope(envelopeId).thenAccept(optionalResult -> {
            plugin.getFoliaLib().getScheduler().runAtEntity(player, task -> {
                handleEnvelopeClaim(player, item, optionalResult);
            });
        });
    }

    private void handleItemPackClaim(Player player, ItemStack item) {
        MessageConfig messages = plugin.getConfigManager().getConfig(MessageConfig.class);
        UniItemHook uniItem = plugin.getService(UniItemHook.class);

        String lixiName = uniItem.getLixiItemPackName(item);
        Lixi.LixiEntry entry = uniItem.getLixiEntry(lixiName);

        if (entry == null || entry.getCommands() == null || entry.getCommands().isEmpty()) {
            MessageUtil.send(player, messages.withPrefix(messages.getGenericError()));
            return;
        }

        String playerName = player.getName();
        for (String cmd : entry.getCommands()) {
            String resolved = cmd.replace("%player%", playerName);
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), resolved);
        }

        if (item.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            item.setAmount(item.getAmount() - 1);
        }

        MessageUtil.send(player, messages.withPrefix(messages.getEnvelopeItemPackClaimed()));
        EffectUtil.playLixiEffect(player);
    }

    private void handleEnvelopeClaim(Player player, ItemStack envelope, Optional<DatabaseManager.ClaimResult> optionalResult) {
        MessageConfig messages = plugin.getConfigManager().getConfig(MessageConfig.class);

        if (optionalResult.isEmpty()) {
            MessageUtil.send(player, messages.withPrefix(messages.getEnvelopeAlreadyClaimed()));
            return;
        }

        DatabaseManager.ClaimResult result = optionalResult.get();
        EconomyProvider provider = plugin.getEconomyProvider(result.currency());
        if (provider == null || !provider.isAvailable()) {
            MessageUtil.send(player, messages.withPrefix(messages.getGenericError()));
            return;
        }

        provider.deposit(player, result.amount());

        if (envelope.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            envelope.setAmount(envelope.getAmount() - 1);
        }

        MessageUtil.send(player, messages.withPrefix(messages.getEnvelopeClaimed())
                .replace("%amount%", provider.format(result.amount())));
        EffectUtil.playLixiEffect(player);
    }
}
