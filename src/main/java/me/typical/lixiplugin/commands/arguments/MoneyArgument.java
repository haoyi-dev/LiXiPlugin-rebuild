package me.typical.lixiplugin.commands.arguments;

import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.CustomArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import me.typical.lixiplugin.util.MoneyUtil;

/**
 * Custom CommandAPI argument for parsing money values with short format support.
 * Supports formats:
 * - Regular numbers: "100", "1500.50"
 * - Thousands: "1k", "1.5k"
 * - Millions: "1M", "2.5M"
 * - Trillions: "1T", "5.5T"
 */
public class MoneyArgument extends CustomArgument<Double, String> {

    public MoneyArgument(String nodeName) {
        super(
                new StringArgument(nodeName).replaceSuggestions(
                        ArgumentSuggestions.strings(info ->
                                new String[]{
                                        "100", "500", "1k", "5k", "10k", "50k", "100k",
                                        "500k", "1M", "5M", "10M", "50M", "100M",
                                        "500M", "1T", "5T", "10T"
                                }
                        )
                ),
                info -> {
                    try {
                        return MoneyUtil.parseMoneyValue(info.input());
                    } catch (IllegalArgumentException e) {
                        throw CustomArgument.CustomArgumentException.fromString(e.getMessage());
                    }
                }
        );
    }
}
