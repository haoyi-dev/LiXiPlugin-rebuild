package me.typical.lixiplugin.service;

import me.typical.lixiplugin.LXPlugin;
import me.typical.lixiplugin.config.types.MainConfig;
import me.typical.lixiplugin.config.types.MessageConfig;
import me.typical.lixiplugin.economy.EconomyProvider;
import me.typical.lixiplugin.economy.LixiCurrency;
import me.typical.lixiplugin.util.EffectUtil;
import me.typical.lixiplugin.util.MessageUtil;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/** Chat lixi broadcasts: players create sessions, others claim via click. */
public class ChatLixiService implements IService {

    private final LXPlugin plugin = LXPlugin.getInstance();
    private final Map<UUID, ChatLixiSession> activeSessions = new ConcurrentHashMap<>();

    @Override
    public void setup() {
        MessageUtil.info("ChatLixiService initialized");
    }

    @Override
    public void shutdown() {
        activeSessions.clear();
    }

    public boolean createSession(Player creator, double amount, int limit, LixiCurrency currencyType) {
        EconomyProvider provider = plugin.getEconomyProvider(currencyType);
        if (provider == null || !provider.isAvailable()) {
            MessageConfig messages = plugin.getConfigManager().getConfig(MessageConfig.class);
            MessageUtil.send(creator, messages.withPrefix(currencyType == LixiCurrency.POINTS
                    ? messages.getPointsNotAvailable() : messages.getVaultNotAvailable()));
            return false;
        }

        MessageConfig messages = plugin.getConfigManager().getConfig(MessageConfig.class);
        MainConfig.ChatLixiConfig config = plugin.getConfigManager().getConfig(MainConfig.class).getChatLixi();

        double minAmount = currencyType == LixiCurrency.POINTS ? config.getMinPoints() : config.getMinAmount();
        double maxAmount = currencyType == LixiCurrency.POINTS ? config.getMaxPoints() : config.getMaxAmount();

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

        UUID sessionId = UUID.randomUUID();
        ChatLixiSession session = new ChatLixiSession(
                sessionId,
                creator.getUniqueId(),
                creator.getName(),
                amount,
                limit,
                currencyType
        );
        activeSessions.put(sessionId, session);

        String broadcast = messages.withPrefix(messages.getChatLixiBroadcast())
                .replace("%player%", creator.getName())
                .replace("%amount%", provider.format(amount))
                .replace("%limit%", String.valueOf(limit))
                .replace("%session_id%", sessionId.toString());
        MessageUtil.broadcast(broadcast);

        plugin.getFoliaLib().getScheduler().runLater(() -> expireSession(sessionId), config.getExpirySeconds() * 20L);
        return true;
    }

    public void claimSession(Player claimer, UUID sessionId) {
        MessageConfig messages = plugin.getConfigManager().getConfig(MessageConfig.class);

        ChatLixiSession session = activeSessions.get(sessionId);
        if (session == null) {
            MessageUtil.send(claimer, messages.withPrefix(messages.getChatLixiExpired()));
            return;
        }
        if (session.claimedBy.contains(claimer.getUniqueId())) {
            MessageUtil.send(claimer, messages.withPrefix(messages.getChatLixiAlreadyClaimed()));
            return;
        }
        if (session.remainingSlots <= 0) {
            MessageUtil.send(claimer, messages.withPrefix(messages.getChatLixiNoSlots()));
            return;
        }
        // Double Average: last gets remainder; else random in (0.01, 2*avg)
        double claimAmount;
        if (session.remainingSlots == 1) {
            claimAmount = session.remainingAmount;
        } else {
            double maxAmount = (session.remainingAmount / session.remainingSlots) * 2;
            claimAmount = ThreadLocalRandom.current().nextDouble(0.01, Math.max(0.02, maxAmount));
            claimAmount = Math.min(claimAmount, session.remainingAmount);
        }

        session.remainingAmount -= claimAmount;
        session.remainingSlots--;
        session.claimedBy.add(claimer.getUniqueId());

        EconomyProvider provider = plugin.getEconomyProvider(session.currencyType);
        if (provider == null || !provider.isAvailable()) {
            MessageUtil.send(claimer, messages.withPrefix(messages.getGenericError()));
            return;
        }
        provider.deposit(claimer, claimAmount);

        MessageUtil.send(claimer, messages.withPrefix(messages.getChatLixiClaimSuccess())
                .replace("%amount%", provider.format(claimAmount)));
        plugin.getFoliaLib().getScheduler().runAtEntity(claimer, task -> {
            EffectUtil.playLixiEffect(claimer);
        });
        if (session.remainingSlots == 0) {
            activeSessions.remove(sessionId);
        }
    }

    private void expireSession(UUID sessionId) {
        ChatLixiSession session = activeSessions.remove(sessionId);
        if (session == null || session.remainingAmount <= 0) {
            return;
        }

        EconomyProvider provider = plugin.getEconomyProvider(session.currencyType);
        if (provider == null || !provider.isAvailable()) {
            return;
        }
        MessageConfig messages = plugin.getConfigManager().getConfig(MessageConfig.class);

        Player creator = plugin.getServer().getPlayer(session.creatorUuid);
        if (creator != null && creator.isOnline()) {
            provider.deposit(creator, session.remainingAmount);
            MessageUtil.send(creator, messages.withPrefix(messages.getChatLixiRefund())
                    .replace("%amount%", provider.format(session.remainingAmount)));
        } else {
            provider.deposit(plugin.getServer().getOfflinePlayer(session.creatorUuid), session.remainingAmount);
        }
    }

    private static class ChatLixiSession {
        final UUID sessionId;
        final UUID creatorUuid;
        final String creatorName;
        final double totalAmount;
        final int totalSlots;
        final LixiCurrency currencyType;
        double remainingAmount;
        int remainingSlots;
        final Set<UUID> claimedBy;
        final long createdAt;

        ChatLixiSession(UUID sessionId, UUID creatorUuid, String creatorName, double amount, int slots, LixiCurrency currencyType) {
            this.sessionId = sessionId;
            this.creatorUuid = creatorUuid;
            this.creatorName = creatorName;
            this.totalAmount = amount;
            this.totalSlots = slots;
            this.currencyType = currencyType != null ? currencyType : LixiCurrency.VAULT;
            this.remainingAmount = amount;
            this.remainingSlots = slots;
            this.claimedBy = ConcurrentHashMap.newKeySet();
            this.createdAt = System.currentTimeMillis();
        }
    }
}
