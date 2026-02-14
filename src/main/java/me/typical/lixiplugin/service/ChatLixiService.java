package me.typical.lixiplugin.service;

import me.typical.lixiplugin.LXPlugin;
import me.typical.lixiplugin.config.types.MainConfig;
import me.typical.lixiplugin.config.types.MessageConfig;
import me.typical.lixiplugin.hook.VaultHook;
import me.typical.lixiplugin.util.EffectUtil;
import me.typical.lixiplugin.util.MessageUtil;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages chat lixi broadcasts.
 * Players create sessions that others can claim from via chat click events.
 */
public class ChatLixiService implements IService {

    private final LXPlugin plugin = LXPlugin.getInstance();
    private final Map<UUID, ChatLixiSession> activeSessions = new ConcurrentHashMap<>();

    @Override
    public void setup() {
        MessageUtil.info("ChatLixiService initialized");
    }

    @Override
    public void shutdown() {
        // Cancel all active sessions
        activeSessions.clear();
    }

    /**
     * Create a new chat lixi session
     *
     * @param creator The player creating the lixi
     * @param amount  The total amount to distribute
     * @param limit   The number of people who can claim
     * @return true if successful, false otherwise
     */
    public boolean createSession(Player creator, double amount, int limit) {
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

        // Validate limit
        if (limit < config.getMinLimit()) {
            MessageUtil.send(creator, messages.withPrefix(messages.getLimitTooLow())
                    .replace("%min%", String.valueOf(config.getMinLimit())));
            return false;
        }
        if (limit > config.getMaxLimit()) {
            MessageUtil.send(creator, messages.withPrefix(messages.getLimitTooHigh())
                    .replace("%max%", String.valueOf(config.getMaxLimit())));
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

        // Create session
        UUID sessionId = UUID.randomUUID();
        ChatLixiSession session = new ChatLixiSession(
                sessionId,
                creator.getUniqueId(),
                creator.getName(),
                amount,
                limit
        );
        activeSessions.put(sessionId, session);

        // Broadcast message
        String broadcast = messages.withPrefix(messages.getChatLixiBroadcast())
                .replace("%player%", creator.getName())
                .replace("%amount%", vault.format(amount))
                .replace("%limit%", String.valueOf(limit))
                .replace("%session_id%", sessionId.toString());
        MessageUtil.broadcast(broadcast);

        // Schedule expiry
        plugin.getFoliaLib().getScheduler().runLater(() -> {
            expireSession(sessionId);
        }, config.getExpirySeconds() * 20L); // Convert seconds to ticks

        return true;
    }

    /**
     * Attempt to claim from a chat lixi session
     *
     * @param claimer   The player attempting to claim
     * @param sessionId The session UUID
     */
    public void claimSession(Player claimer, UUID sessionId) {
        MessageConfig messages = plugin.getConfigManager().getConfig(MessageConfig.class);
        VaultHook vault = plugin.getService(VaultHook.class);

        ChatLixiSession session = activeSessions.get(sessionId);

        // Check if session exists
        if (session == null) {
            MessageUtil.send(claimer, messages.withPrefix(messages.getChatLixiExpired()));
            return;
        }

        // Check if already claimed
        if (session.claimedBy.contains(claimer.getUniqueId())) {
            MessageUtil.send(claimer, messages.withPrefix(messages.getChatLixiAlreadyClaimed()));
            return;
        }

        // Check if slots available
        if (session.remainingSlots <= 0) {
            MessageUtil.send(claimer, messages.withPrefix(messages.getChatLixiNoSlots()));
            return;
        }

        // Calculate amount using Double Average algorithm
        double claimAmount;
        if (session.remainingSlots == 1) {
            // Last person gets all remaining
            claimAmount = session.remainingAmount;
        } else {
            // Random amount between 0.01 and (remaining / remainingSlots) * 2
            double maxAmount = (session.remainingAmount / session.remainingSlots) * 2;
            claimAmount = ThreadLocalRandom.current().nextDouble(0.01, Math.max(0.02, maxAmount));
            claimAmount = Math.min(claimAmount, session.remainingAmount);
        }

        // Update session
        session.remainingAmount -= claimAmount;
        session.remainingSlots--;
        session.claimedBy.add(claimer.getUniqueId());

        // Deposit money
        vault.deposit(claimer, claimAmount);

        // Send success message
        MessageUtil.send(claimer, messages.withPrefix(messages.getChatLixiClaimSuccess())
                .replace("%amount%", vault.format(claimAmount)));

        // Play effects
        plugin.getFoliaLib().getScheduler().runAtEntity(claimer, task -> {
            EffectUtil.playLixiEffect(claimer);
        });

        // Remove session if all slots claimed
        if (session.remainingSlots == 0) {
            activeSessions.remove(sessionId);
        }
    }

    /**
     * Expire a session and refund remaining amount to creator
     */
    private void expireSession(UUID sessionId) {
        ChatLixiSession session = activeSessions.remove(sessionId);
        if (session == null || session.remainingAmount <= 0) {
            return;
        }

        // Refund remaining to creator
        VaultHook vault = plugin.getService(VaultHook.class);
        MessageConfig messages = plugin.getConfigManager().getConfig(MessageConfig.class);

        Player creator = plugin.getServer().getPlayer(session.creatorUuid);
        if (creator != null && creator.isOnline()) {
            vault.deposit(creator, session.remainingAmount);
            MessageUtil.send(creator, messages.withPrefix(messages.getChatLixiRefund())
                    .replace("%amount%", vault.format(session.remainingAmount)));
        } else {
            // Creator offline, deposit to offline player
            vault.deposit(plugin.getServer().getOfflinePlayer(session.creatorUuid), session.remainingAmount);
        }
    }

    /**
     * Data class for a chat lixi session
     */
    private static class ChatLixiSession {
        final UUID sessionId;
        final UUID creatorUuid;
        final String creatorName;
        final double totalAmount;
        final int totalSlots;
        double remainingAmount;
        int remainingSlots;
        final Set<UUID> claimedBy;
        final long createdAt;

        ChatLixiSession(UUID sessionId, UUID creatorUuid, String creatorName, double amount, int slots) {
            this.sessionId = sessionId;
            this.creatorUuid = creatorUuid;
            this.creatorName = creatorName;
            this.totalAmount = amount;
            this.totalSlots = slots;
            this.remainingAmount = amount;
            this.remainingSlots = slots;
            this.claimedBy = ConcurrentHashMap.newKeySet();
            this.createdAt = System.currentTimeMillis();
        }
    }
}
