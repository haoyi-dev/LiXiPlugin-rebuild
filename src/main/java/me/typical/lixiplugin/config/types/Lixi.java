package me.typical.lixiplugin.config.types;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
@Getter
public class Lixi {

    @Comment({
            "Các loại gói lì xì. Mỗi key (lixi1, lixi2, ...) là tên loại.",
            "Dùng /lixiadmin givegoilixi <tên> [số lượng] để give.",
            "Right-click để nhận items qua commands. Placeholder: %player%"
    })
    private Map<String, LixiEntry> lixi = defaultEntries();

    private static Map<String, LixiEntry> defaultEntries() {
        Map<String, LixiEntry> map = new LinkedHashMap<>();

        LixiEntry lixi1 = new LixiEntry();
        lixi1.setDisplayName("<gradient:#ffaa00:#ff5500>Gói Lì Xì Vàng</gradient>");
        lixi1.setLore(List.of(
                "<gray>Chứa nhiều vật phẩm may mắn!</gray>",
                "<gray>Click chuột phải để nhận!</gray>"
        ));
        lixi1.setMaterial("PAPER");
        lixi1.setCustomModelData(1000);
        lixi1.setCommands(List.of(
                "give %player% diamond 1",
                "give %player% emerald 2"
        ));
        map.put("lixi1", lixi1);

        LixiEntry lixi2 = new LixiEntry();
        lixi2.setDisplayName("<gradient:#ff0000:#aa0000>Gói Lì Xì Đỏ</gradient>");
        lixi2.setLore(List.of(
                "<gray>Phong bao đỏ may mắn!</gray>",
                "<gray>Click chuột phải để nhận!</gray>"
        ));
        lixi2.setMaterial("RED_CANDLE");
        lixi2.setCustomModelData(1001);
        lixi2.setCommands(List.of(
                "give %player% gold_ingot 2",
                "give %player% diamond 1"
        ));
        map.put("lixi2", lixi2);

        return map;
    }

    @Configuration
    @Getter
    @Setter
    public static class LixiEntry {
        @Comment("Display name (MiniMessage)")
        private String displayName = "<gradient:#ffaa00:#ff5500>Gói Lì Xì</gradient>";

        @Comment("Lore lines (MiniMessage)")
        private List<String> lore = List.of(
                "<gray>Chứa nhiều vật phẩm may mắn!</gray>",
                "<gray>Click chuột phải để nhận!</gray>"
        );

        @Comment({
                "Vanilla material (e.g. PAPER, RED_CANDLE). Ignored if custom item IDs are set below."
        })
        private String material = "PAPER";

        @Comment("Custom Model Data for resource pack (0 = disabled)")
        private int customModelData = 0;

        @Comment("ItemsAdder item ID - overrides material if set")
        private String itemsadder = "";

        @Comment("Oraxen item ID - overrides material if set")
        private String oraxen = "";

        @Comment("Nexo item ID - overrides material if set")
        private String nexo = "";

        @Comment({
                "Commands run when player right-clicks. Placeholder: %player%",
                "Example: give %player% diamond 5"
        })
        private List<String> commands = List.of(
                "give %player% diamond 1",
                "give %player% emerald 2"
        );
    }
}
