package me.typical.lixiplugin.util;

import me.typical.lixiplugin.LXPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/** MiniMessage send/broadcast utilities. */
public class MessageUtil {
    private static final Logger LOGGER = LXPlugin.getInstance().getLogger();
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    public static void info(String message) {
        LOGGER.log(Level.INFO, message);
    }

    public static void warn(String message) {
        LOGGER.log(Level.WARNING, message);
    }

    public static void error(String message) {
        LOGGER.log(Level.SEVERE, message);
    }

    public static void send(Player player, String message) {
        if (message == null || message.isEmpty()) return;
        Component component = MINI_MESSAGE.deserialize(message);
        player.sendMessage(component);
    }

    public static void send(Player player, String message, Map<String, String> placeholders) {
        if (message == null || message.isEmpty()) return;
        String processed = replacePlaceholders(message, placeholders);
        send(player, processed);
    }

    public static void broadcast(String message) {
        if (message == null || message.isEmpty()) return;
        Component component = MINI_MESSAGE.deserialize(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(component);
        }
        Bukkit.getConsoleSender().sendMessage(component);
    }

    public static void broadcast(String message, Map<String, String> placeholders) {
        if (message == null || message.isEmpty()) return;
        String processed = replacePlaceholders(message, placeholders);
        broadcast(processed);
    }

    public static String replacePlaceholders(String message, Map<String, String> placeholders) {
        if (placeholders == null || placeholders.isEmpty()) return message;
        String result = message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
