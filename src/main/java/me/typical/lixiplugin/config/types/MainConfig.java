package me.typical.lixiplugin.config.types;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import lombok.Getter;

/**
 * Main configuration file for LiXiPlugin.
 * Contains database settings and feature limits.
 */
@Configuration
@Getter
public class MainConfig {

    @Comment("Database configuration")
    private DatabaseConfig database = new DatabaseConfig();

    @Comment("Chat lixi broadcast settings")
    private ChatLixiConfig chatLixi = new ChatLixiConfig();

    @Comment("Physical envelope item settings")
    private EnvelopeConfig envelope = new EnvelopeConfig();

    @Comment("Visual and sound effects")
    private EffectsConfig effects = new EffectsConfig();

    @Configuration
    @Getter
    public static class DatabaseConfig {
        @Comment({
                "Database type: SQLITE or MARIADB",
                "SQLite is recommended for small servers",
                "MariaDB is recommended for large servers or networks"
        })
        private String type = "SQLITE";

        @Comment("Database host (only for MariaDB)")
        private String host = "localhost";

        @Comment("Database port (only for MariaDB)")
        private int port = 3306;

        @Comment("Database name (only for MariaDB)")
        private String database = "lixi";

        @Comment("Database username (only for MariaDB)")
        private String username = "root";

        @Comment("Database password (only for MariaDB)")
        private String password = "";
    }

    @Configuration
    @Getter
    public static class ChatLixiConfig {
        @Comment("Minimum amount per chat lixi broadcast")
        private double minAmount = 1000.0;

        @Comment("Maximum amount per chat lixi broadcast")
        private double maxAmount = 1000000.0;

        @Comment("Minimum number of claim slots")
        private int minLimit = 1;

        @Comment("Maximum number of claim slots")
        private int maxLimit = 50;

        @Comment("Time in seconds before unclaimed lixi expires and refunds to creator")
        private int expirySeconds = 60;
    }

    @Configuration
    @Getter
    public static class EnvelopeConfig {
        @Comment({
                "Custom item plugin configurations",
                "Set the item ID for each supported plugin",
                "Leave empty or blank to disable that plugin's items",
                "The first enabled plugin found will be used"
        })
        private ItemsAdderSection itemsadder = new ItemsAdderSection();
        private OraxenSection oraxen = new OraxenSection();
        private NexoSection nexo = new NexoSection();

        @Comment("Display name for envelope items (supports MiniMessage)")
        private String displayName = "<color:#e80c00>Lì Xì</color>";

        @Comment({
                "Lore for envelope items (supports MiniMessage)",
                "Use %amount% placeholder for the money amount"
        })
        private String[] lore = {
                "<gray>Chứa <gold>%amount%</gold></gray>",
                "<gray>Click chuột phải để nhận tiền!</gray>"
        };

        @Configuration
        @Getter
        public static class ItemsAdderSection {
            @Comment("ItemsAdder item ID (e.g., 'lixi_envelope')")
            private String itemId = "";

            public void setItemId(String itemId) {
                this.itemId = itemId;
            }
        }

        @Configuration
        @Getter
        public static class OraxenSection {
            @Comment("Oraxen item ID (e.g., 'lixi_envelope')")
            private String itemId = "";

            public void setItemId(String itemId) {
                this.itemId = itemId;
            }
        }

        @Configuration
        @Getter
        public static class NexoSection {
            @Comment("Nexo item ID (e.g., 'lixi_envelope')")
            private String itemId = "";

            public void setItemId(String itemId) {
                this.itemId = itemId;
            }
        }
    }

    @Configuration
    @Getter
    public static class EffectsConfig {
        @Comment("Sound to play when receiving lixi (namespace:sound_name)")
        private String sound = "minecraft:entity.experience_orb.pickup";

        @Comment("Sound volume (0.0 - 1.0)")
        private float volume = 1.0f;

        @Comment("Sound pitch (0.5 - 2.0)")
        private float pitch = 1.0f;

        @Comment("Enable particle effects when receiving lixi")
        private boolean particles = true;
    }
}
