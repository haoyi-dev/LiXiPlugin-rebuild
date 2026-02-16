package me.typical.lixiplugin.config.types;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import lombok.Getter;

@Configuration
@Getter
public class MessageConfig {

    @Comment("Prefix used in all plugin messages")
    private String prefix = "<gradient:#ff0000:#ffaa00>[Lì Xì]</gradient> <gray>»</gray> ";

    @Comment({
            "Broadcast message when a chat lixi is created",
            "Placeholders: %player%, %amount%, %limit%"
    })
    private String chatLixiBroadcast = "<prefix><gold>%player%</gold> <white>đã gửi lì xì</white> <gold>%amount%</gold> <white>cho</white> <gold>%limit%</gold> <white>người! " +
            "<click:run_command:'/lixi claim %session_id%'><hover:show_text:'<green>Nhấn để nhận!</green>'>" +
            "<gradient:#00ff00:#00aa00>[NHẬN NGAY]</gradient></hover></click></white>";

    @Comment({
            "Message sent to player who successfully claimed from chat lixi",
            "Placeholders: %amount%"
    })
    private String chatLixiClaimSuccess = "<prefix><green>Bạn đã nhận được</green> <gold>%amount%</gold><green>!</green>";

    @Comment("Message when player already claimed this chat lixi")
    private String chatLixiAlreadyClaimed = "<prefix><red>Bạn đã nhận lì xì này rồi!</red>";

    @Comment("Message when chat lixi has expired")
    private String chatLixiExpired = "<prefix><red>Lì xì này đã hết hạn!</red>";

    @Comment("Message when chat lixi has no more slots")
    private String chatLixiNoSlots = "<prefix><red>Lì xì này đã được nhận hết!</red>";

    @Comment({
            "Message when chat lixi expires and refunds to creator",
            "Placeholders: %amount%"
    })
    private String chatLixiRefund = "<prefix><yellow>Lì xì của bạn đã hết hạn. Hoàn lại</yellow> <gold>%amount%</gold>";

    @Comment({
            "Message sent to sender when transferring money",
            "Placeholders: %player%, %amount%"
    })
    private String transferSent = "<prefix><green>Bạn đã gửi</green> <gold>%amount%</gold> <green>cho</green> <gold>%player%</gold>";

    @Comment({
            "Message sent to receiver when receiving money",
            "Placeholders: %player%, %amount%"
    })
    private String transferReceived = "<prefix><green>Bạn đã nhận</green> <gold>%amount%</gold> <green>từ</green> <gold>%player%</gold><green>!</green>";

    @Comment({
            "Message when successfully creating a physical envelope",
            "Placeholders: %amount%"
    })
    private String envelopeCreated = "<prefix><green>Đã tạo phong bao lì xì</green> <gold>%amount%</gold>";

    @Comment({
            "Message when successfully claiming an envelope",
            "Placeholders: %amount%"
    })
    private String envelopeClaimed = "<prefix><green>Bạn đã mở lì xì và nhận được</green> <gold>%amount%</gold><green>!</green>";

    @Comment("Message when envelope has already been claimed")
    private String envelopeAlreadyClaimed = "<prefix><red>Lì Xì này đã được mở rồi!</red>";

    @Comment("Message when player claims gói lì xì (item pack) and receives items")
    private String envelopeItemPackClaimed = "<prefix><green>Bạn đã mở gói lì xì và nhận được vật phẩm!</green>";

    @Comment("Message when successfully setting the envelope item")
    private String adminSetItemSuccess = "<prefix><green>Đã đặt vật phẩm lì xì thành công!</green>";

    @Comment("Message when admin must hold an item to set it")
    private String adminSetItemNoItem = "<prefix><red>Bạn phải cầm một vật phẩm trong tay!</red>";

    @Comment("Message when admin receives gói lì xì. Placeholders: %amount%, %name%")
    private String adminItemPackGiven = "<prefix><green>Đã nhận</green> <gold>%amount%</gold> <green>gói lì xì</green> <gold>%name%</gold><green>!</green>";

    @Comment("Message when lì xì type not found. Placeholder: %name%")
    private String adminLixiNotFound = "<prefix><red>Không tìm thấy loại lì xì</red> <gold>%name%</gold><red>!</red>";

    @Comment("Message when config is reloaded successfully")
    private String adminReloadSuccess = "<prefix><green>Đã tải lại cấu hình thành công!</green>";

    @Comment({
            "Message when player doesn't have enough money",
            "Placeholders: %required%, %balance%"
    })
    private String insufficientBalance = "<prefix><red>Không đủ tiền! Cần</red> <gold>%required%</gold><red>, bạn có</red> <gold>%balance%</gold>";

    @Comment("Message when amount is below minimum")
    private String amountTooLow = "<prefix><red>Số tiền quá thấp! Tối thiểu:</red> <gold>%min%</gold>";

    @Comment("Message when amount is above maximum")
    private String amountTooHigh = "<prefix><red>Số tiền quá cao! Tối đa:</red> <gold>%max%</gold>";

    @Comment("Message when limit is below minimum")
    private String limitTooLow = "<prefix><red>Số lượng người nhận quá thấp! Tối thiểu:</red> <gold>%min%</gold>";

    @Comment("Message when limit is above maximum")
    private String limitTooHigh = "<prefix><red>Số lượng người nhận quá cao! Tối đa:</red> <gold>%max%</gold>";

    @Comment("Message when target player is not online")
    private String playerNotOnline = "<prefix><red>Người chơi</red> <gold>%player%</gold> <red>không online!</red>";

    @Comment("Message when trying to send lixi to yourself")
    private String cannotSendToSelf = "<prefix><red>Bạn không thể gửi lì xì cho chính mình!</red>";

    @Comment("Generic error message")
    private String genericError = "<prefix><red>Đã xảy ra lỗi! Vui lòng thử lại sau.</red>";

    @Comment("Message when Vault economy is not available")
    private String vaultNotAvailable = "<prefix><red>Hệ thống kinh tế không khả dụng!</red>";

    @Comment("Message when PlayerPoints is not available")
    private String pointsNotAvailable = "<prefix><red>PlayerPoints chưa được cài đặt hoặc không khả dụng!</red>";

    public String withPrefix(String message) {
        return message.replace("<prefix>", prefix);
    }
}
