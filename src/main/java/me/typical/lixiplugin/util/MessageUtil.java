package me.typical.lixiplugin.util;

import me.typical.lixiplugin.LXPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for sending messages to players and console.
 * Handles MiniMessage parsing and placeholder replacement.
 */
public class MessageUtil {
    private static final Logger LOGGER = LXPlugin.getInstance().getLogger();
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    /**
     * Log an info message to console
     */
    public static void info(String message) {
        LOGGER.log(Level.INFO, message);
    }

    /**
     * Log a warning message to console
     */
    public static void warn(String message) {
        LOGGER.log(Level.WARNING, message);
    }

    /**
     * Log an error message to console
     */
    public static void error(String message) {
        LOGGER.log(Level.SEVERE, message);
    }

    /**
     * Send a MiniMessage formatted message to a player
     */
    public static void send(Player player, String message) {
        if (message == null || message.isEmpty()) return;
        Component component = MINI_MESSAGE.deserialize(message);
        player.sendMessage(component);
    }

    /**
     * Send a MiniMessage formatted message to a player with placeholder replacements
     */
    public static void send(Player player, String message, Map<String, String> placeholders) {
        if (message == null || message.isEmpty()) return;
        String processed = replacePlaceholders(message, placeholders);
        send(player, processed);
    }

    /**
     * Broadcast a MiniMessage formatted message to all online players and console.
     * Compatible with Paper 1.20+ (sends to each player and console explicitly).
     */
    public static void broadcast(String message) {
        if (message == null || message.isEmpty()) return;
        Component component = MINI_MESSAGE.deserialize(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(component);
        }
        Bukkit.getConsoleSender().sendMessage(component);
    }

    /**
     * Broadcast a MiniMessage formatted message with placeholder replacements
     */
    public static void broadcast(String message, Map<String, String> placeholders) {
        if (message == null || message.isEmpty()) return;
        String processed = replacePlaceholders(message, placeholders);
        broadcast(processed);
    }

    /**
     * Replace placeholders in a message string
     */
    public static String replacePlaceholders(String message, Map<String, String> placeholders) {
        if (placeholders == null || placeholders.isEmpty()) return message;
        String result = message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
