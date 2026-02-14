package me.typical.lixiplugin.service;

import me.typical.lixiplugin.LXPlugin;
import me.typical.lixiplugin.config.types.Lixi;
import me.typical.lixiplugin.config.types.MainConfig;
import me.typical.lixiplugin.config.types.MessageConfig;
import me.typical.lixiplugin.hook.UniItemHook;
import me.typical.lixiplugin.hook.VaultHook;
import me.typical.lixiplugin.util.EffectUtil;
import me.typical.lixiplugin.util.MessageUtil;
import net.milkbowl.vault.economy.EconomyResponse;
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

/**
 * Manages physical envelope items and direct player-to-player transfers.
 * Listens for right-click events to claim envelopes.
 */
public class EnvelopeService implements IService, Listener {

    private final LXPlugin plugin = LXPlugin.getInstance();

    @Override
    public void setup() {
        MessageUtil.info("EnvelopeService initialized");
    }

    @Override
    public void shutdown() {
        // Nothing to clean up
    }

    /**
     * Create a physical envelope item for a player
     *
     * @param creator The player creating the envelope
     * @param amount  The amount of money in the envelope
     * @return true if successful, false otherwise
     */
    public boolean createEnvelope(Player creator, double amount) {
        VaultHook vault = plugin.getService(VaultHook.class);
        MessageConfig messages = plugin.getConfigManager().getConfig(MessageConfig.class);
        MainConfig.ChatLixiConfig config = plugin.getConfigManager()
                .getConfig(MainConfig.class)
                .getChatLixi();

        // Validate amount
        if (amount < config.getMinAmount()) {
            MessageUtil.send(creator, messages.withPrefix(messages.getAmountTooLow())
                    .replace("%min%", vault.format(config.getMinAmount())));
            return false;
        }
        if (amount > config.getMaxAmount()) {
            MessageUtil.send(creator, messages.withPrefix(messages.getAmountTooHigh())
                    .replace("%max%", vault.format(config.getMaxAmount())));
            return false;
        }

        // Check balance
        double balance = vault.getBalance(creator);
        if (balance < amount) {
            MessageUtil.send(creator, messages.withPrefix(messages.getInsufficientBalance())
                    .replace("%required%", vault.format(amount))
                    .replace("%balance%", vault.format(balance)));
            return false;
        }

        // Withdraw money
        EconomyResponse response = vault.withdraw(creator, amount);
        if (!response.transactionSuccess()) {
            MessageUtil.send(creator, messages.withPrefix(messages.getGenericError()));
            return false;
        }

        // Generate unique envelope ID
        UUID envelopeId = UUID.randomUUID();

        // Create item via UniItemHook
        UniItemHook uniItem = plugin.getService(UniItemHook.class);
        ItemStack envelope = uniItem.createEnvelopeItem(amount, envelopeId);

        // Insert into database (async)
        DatabaseManager db = plugin.getService(DatabaseManager.class);
        db.insertEnvelope(envelopeId, amount, creator.getUniqueId()).whenComplete((v, ex) -> {
            if (ex != null) {
                MessageUtil.error("Failed to save envelope to database: " + envelopeId);
                ex.printStackTrace();
                // Refund player on error
                vault.deposit(creator, amount);
                plugin.getFoliaLib().getScheduler().runAtEntity(creator, task -> {
                    MessageUtil.send(creator, messages.withPrefix(messages.getGenericError()));
                });
            }
        });

        // Give item to player
        creator.getInventory().addItem(envelope);

        // Send success message
        MessageUtil.send(creator, messages.withPrefix(messages.getEnvelopeCreated())
                .replace("%amount%", vault.format(amount)));

        return true;
    }

    /**
     * Transfer money directly from one player to another
     *
     * @param sender   The player sending the money
     * @param receiver The player receiving the money
     * @param amount   The amount to transfer
     * @return true if successful, false otherwise
     */
    public boolean directTransfer(Player sender, Player receiver, double amount) {
        VaultHook vault = plugin.getService(VaultHook.class);
        MessageConfig messages = plugin.getConfigManager().getConfig(MessageConfig.class);
        MainConfig.ChatLixiConfig config = plugin.getConfigManager()
                .getConfig(MainConfig.class)
                .getChatLixi();

        // Check if trying to send to self
        if (sender.getUniqueId().equals(receiver.getUniqueId())) {
            MessageUtil.send(sender, messages.withPrefix(messages.getCannotSendToSelf()));
            return false;
        }

        // Validate amount
        if (amount < config.getMinAmount()) {
            MessageUtil.send(sender, messages.withPrefix(messages.getAmountTooLow())
                    .replace("%min%", vault.format(config.getMinAmount())));
            return false;
        }
        if (amount > config.getMaxAmount()) {
            MessageUtil.send(sender, messages.withPrefix(messages.getAmountTooHigh())
                    .replace("%max%", vault.format(config.getMaxAmount())));
            return false;
        }

        // Check balance
        double balance = vault.getBalance(sender);
        if (balance < amount) {
            MessageUtil.send(sender, messages.withPrefix(messages.getInsufficientBalance())
                    .replace("%required%", vault.format(amount))
                    .replace("%balance%", vault.format(balance)));
            return false;
        }

        // Perform transaction
        EconomyResponse withdraw = vault.withdraw(sender, amount);
        if (!withdraw.transactionSuccess()) {
            MessageUtil.send(sender, messages.withPrefix(messages.getGenericError()));
            return false;
        }

        EconomyResponse deposit = vault.deposit(receiver, amount);
        if (!deposit.transactionSuccess()) {
            // Refund sender if deposit fails
            vault.deposit(sender, amount);
            MessageUtil.send(sender, messages.withPrefix(messages.getGenericError()));
            return false;
        }

        // Send messages
        MessageUtil.send(sender, messages.withPrefix(messages.getTransferSent())
                .replace("%player%", receiver.getName())
                .replace("%amount%", vault.format(amount)));

        MessageUtil.send(receiver, messages.withPrefix(messages.getTransferReceived())
                .replace("%player%", sender.getName())
                .replace("%amount%", vault.format(amount)));

        // Play effects for receiver
        plugin.getFoliaLib().getScheduler().runAtEntity(receiver, task -> {
            EffectUtil.playLixiEffect(receiver);
        });

        return true;
    }

    /**
     * Handle right-click events to claim envelopes
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only handle right-click actions
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Only handle main hand to prevent double triggers
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) {
            return;
        }

        UniItemHook uniItem = plugin.getService(UniItemHook.class);

        // Gói lì xì (item pack): nhận vật phẩm qua commands trong lixi.yml
        if (uniItem.isLixiItemPack(item)) {
            event.setCancelled(true);
            handleItemPackClaim(player, item);
            return;
        }

        // Phong bao tiền (money envelope)
        if (!uniItem.isLixiEnvelope(item)) {
            return;
        }

        // Cancel the event to prevent other interactions
        event.setCancelled(true);

        // Get envelope ID
        UUID envelopeId = uniItem.getEnvelopeId(item);
        if (envelopeId == null) {
            MessageUtil.send(player, plugin.getConfigManager().getConfig(MessageConfig.class)
                    .withPrefix(plugin.getConfigManager().getConfig(MessageConfig.class).getGenericError()));
            return;
        }

        // Attempt to claim envelope (async)
        DatabaseManager db = plugin.getService(DatabaseManager.class);
        db.claimEnvelope(envelopeId).thenAccept(optionalAmount -> {
            // Back to main thread for player operations
            plugin.getFoliaLib().getScheduler().runAtEntity(player, task -> {
                handleEnvelopeClaim(player, item, optionalAmount);
            });
        });
    }

    /**
     * Handle right-click on "gói lì xì" (item pack): run commands from lixi.yml, give items to player.
     */
    private void handleItemPackClaim(Player player, ItemStack item) {
        MessageConfig messages = plugin.getConfigManager().getConfig(MessageConfig.class);
        Lixi lixiConfig = plugin.getConfigManager().getConfig(Lixi.class);

        if (lixiConfig.getCommands() == null || lixiConfig.getCommands().isEmpty()) {
            MessageUtil.send(player, messages.withPrefix(messages.getGenericError()));
            return;
        }

        String playerName = player.getName();
        for (String cmd : lixiConfig.getCommands()) {
            String resolved = cmd.replace("%player%", playerName);
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), resolved);
        }

        // Remove one item from hand
        if (item.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            item.setAmount(item.getAmount() - 1);
        }

        MessageUtil.send(player, messages.withPrefix(messages.getEnvelopeItemPackClaimed()));
        EffectUtil.playLixiEffect(player);
    }

    /**
     * Handle the result of an envelope claim attempt
     */
    private void handleEnvelopeClaim(Player player, ItemStack envelope, Optional<Double> optionalAmount) {
        MessageConfig messages = plugin.getConfigManager().getConfig(MessageConfig.class);
        VaultHook vault = plugin.getService(VaultHook.class);

        if (optionalAmount.isEmpty()) {
            // Envelope already claimed or doesn't exist
            MessageUtil.send(player, messages.withPrefix(messages.getEnvelopeAlreadyClaimed()));
            return;
        }

        double amount = optionalAmount.get();

        // Deposit money
        vault.deposit(player, amount);

        // Remove item from hand (1.20-compatible: reduce stack or set hand to air)
        if (envelope.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            envelope.setAmount(envelope.getAmount() - 1);
        }

        // Send success message
        MessageUtil.send(player, messages.withPrefix(messages.getEnvelopeClaimed())
                .replace("%amount%", vault.format(amount)));

        // Play effects
        EffectUtil.playLixiEffect(player);
    }
}
