package me.typical.lixiplugin.config.types;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import lombok.Getter;

import java.util.List;

/**
 * Configuration for "gói lì xì" (item pack envelope).
 * File: lixi.yml
 * When a player right-clicks this item, the commands below are run (as console).
 * Use %player% placeholder for the player name.
 */
@Configuration
@Getter
public class Lixi {

    @Comment("Display name for the item pack envelope (supports MiniMessage)")
    private String displayName = "<gradient:#ffaa00:#ff5500>Gói Lì Xì</gradient>";

    @Comment("Lore for the item pack envelope (supports MiniMessage)")
    private List<String> lore = List.of(
            "<gray>Chứa nhiều vật phẩm may mắn!</gray>",
            "<gray>Click chuột phải để nhận!</gray>"
    );

    @Comment({
            "Commands to run when a player right-clicks the item pack.",
            "Executed as console. Placeholder: %player% = player name",
            "Example: give %player% diamond 5"
    })
    private List<String> commands = List.of(
            "give %player% diamond 1",
            "give %player% emerald 2"
    );
}
