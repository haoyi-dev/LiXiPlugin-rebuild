# LiXiPlugin

## Tính năng

- **Lì xì chat**: Phát thông báo lì xì trong chat, người chơi click để nhận
- **Phong bì vật lý**: Tạo item phong bì có thể trao đổi, mở nhận tiền/points
- **Chuyển trực tiếp**: Gửi tiền hoặc points cho người chơi khác
- **Gói lì xì (item pack)**: Nhiều loại lì xì (lixi1, lixi2...) với material và custom model riêng
- **Hai loại tiền tệ**: Vault (tiền) và PlayerPoints (điểm)
- **Hỗ trợ Folia**: Tương thích Paper và Folia
- **ItemsAdder / Oraxen / Nexo**: Custom item cho phong bì

---

## Yêu cầu

- **Java 21+**
- **Paper / Folia 1.20+**
- **Vault** (bắt buộc cho tiền)
- **Plugin kinh tế** (EssentialsX, CMI...)
- **PlayerPoints** (tùy chọn, cho lì xì bằng points)

---

## Cài đặt

1. Tải file JAR từ [Releases](https://github.com/SimpMC-Studio/LiXiPlugin/releases)
2. Đặt `LiXiPlugin-*.jar` vào thư mục `plugins/`
3. Cài **Vault** và plugin kinh tế
4. (Tùy chọn) Cài **PlayerPoints** để dùng lì xì bằng points
5. Khởi động server, sửa config trong `plugins/LiXiPlugin/`

---

## Lệnh

### Prefix lệnh chính

- **Lệnh gốc:** `/lixi` hoặc `/lx` hoặc `/luckmoney`
- Tất cả lệnh đều bắt đầu bằng `/lixi`

### Lệnh người chơi

| Lệnh | Quyền | Mô tả |
|------|-------|-------|
| `/lixi` | — | Xem danh sách lệnh |
| `/lixi chat <số> <limit>` | `lixi.chat` | Lì xì chat (tiền) |
| `/lixi chat <số> <limit> points` | `lixi.chat` | Lì xì chat (PlayerPoints) |
| `/lixi give <người chơi> <số>` | `lixi.give` | Chuyển tiền trực tiếp |
| `/lixi give <người chơi> <số> points` | `lixi.give` | Chuyển points trực tiếp |
| `/lixi phongbao <số>` | `lixi.phongbao` | Tạo phong bì tiền |
| `/lixi phongbao <số> points` | `lixi.phongbao` | Tạo phong bì points |
| `/lixi phongbao` / `/lixi envelope` / `/lixi pb` | — | Bí danh của phongbao |

**Lưu ý:** Thêm `points` ở **cuối** lệnh để dùng PlayerPoints thay vì tiền Vault.

### Lệnh Admin

| Lệnh | Quyền | Mô tả |
|------|-------|-------|
| `/lixiadmin` hoặc `/lxa` | `lixi.admin` | Xem lệnh admin |
| `/lixiadmin setitem` | `lixi.admin` | Đặt item cầm tay làm phong bì tiền |
| `/lixiadmin givegoilixi <tên> [số lượng]` | `lixi.admin` | Give gói lì xì theo tên (lixi1, lixi2...) |
| `/lixiadmin reload` | `lixi.admin` | Reload config |

### Định dạng số tiền / số điểm

| Ví dụ | Giá trị |
|-------|---------|
| `100` | 100 |
| `1k` | 1,000 |
| `1.5k` | 1,500 |
| `1M` | 1,000,000 |
| `2.5M` | 2,500,000 |
| `1T` | 1,000,000,000,000 |

### Ví dụ lệnh

```bash
# Lì xì chat 10,000 cho 5 người (tiền)
/lixi chat 10k 5

# Lì xì chat 100 points cho 3 người
/lixi chat 100 3 points

# Chuyển 50k cho Steve
/lixi give Steve 50k

# Chuyển 500 points cho Alex
/lixi give Alex 500 points

# Tạo phong bì 1M
/lixi phongbao 1M

# Tạo phong bì 100 points
/lixi phongbao 100 points

# Admin: give 5 gói lì xì loại lixi1
/lixiadmin givegoilixi lixi1 5
```

---

## Quyền hạn (Permissions)

| Permission | Mặc định | Mô tả |
|------------|----------|-------|
| `lixi.chat` | OP | Tạo lì xì chat |
| `lixi.give` | OP | Chuyển tiền/points trực tiếp |
| `lixi.phongbao` | OP | Tạo phong bì |
| `lixi.admin` | OP | Lệnh admin |

---

## Cấu hình

### File config

- `main-config.yml` — Database, giới hạn số tiền/points, hiệu ứng
- `message-config.yml` — Tin nhắn và **prefix**
- `lixi.yml` — Các loại gói lì xì (lixi1, lixi2...) với material, custom model, commands

### Đổi Prefix tin nhắn

Trong `message-config.yml`:

```yaml
prefix: "<gradient:#ff0000:#ffaa00>[Lì Xì]</gradient> <gray>»</gray> "
```

Sửa chuỗi này theo MiniMessage (gradient, color, click, hover...). Sau đó dùng `/lixiadmin reload`.

### Placeholders trong tin nhắn

Plugin thay thế các placeholder sau trong tin nhắn:

| Placeholder | Mô tả |
|-------------|-------|
| `<prefix>` | Prefix từ config |
| `%player%` | Tên người chơi |
| `%amount%` | Số tiền/points đã format |
| `%limit%` | Số người được nhận |
| `%session_id%` | ID phiên chat lì xì (cho nút click) |
| `%required%` | Số tiền cần có |
| `%balance%` | Số dư hiện tại |
| `%min%` | Giá trị tối thiểu |
| `%max%` | Giá trị tối đa |
| `%name%` | Tên loại lì xì (trong admin messages) |

### PlaceholderAPI

Plugin có `PlaceholderAPI` trong `compileOnly`. Để dùng placeholders của PlaceholderAPI trong **tin nhắn của plugin**, cần:

1. Cài PlaceholderAPI
2. Gọi `PlaceholderAPI.setPlaceholders(player, message)` trước khi gửi (hiện plugin chưa tích hợp sẵn)

Nếu dùng plugin khác (scoreboard, tab...) hiển thị tin nhắn từ LiXiPlugin, plugin đó có thể tự parse PlaceholderAPI. Tin nhắn trong `message-config.yml` dùng MiniMessage và placeholders của plugin (`%player%`, `%amount%`...).

### Gói lì xì (lixi.yml)

Cấu trúc:

```yaml
lixi:
  lixi1:
    display-name: "<gradient:#ffaa00:#ff5500>Gói Lì Xì Vàng</gradient>"
    lore:
      - "<gray>Chứa vật phẩm may mắn!</gray>"
    material: PAPER
    custom-model-data: 1000
    itemsadder: ""   # hoặc ID item ItemsAdder
    oraxen: ""
    nexo: ""
    commands:
      - "give %player% diamond 1"
      - "give %player% emerald 2"
  lixi2:
    display-name: "<gradient:#ff0000:#aa0000>Gói Lì Xì Đỏ</gradient>"
    material: RED_CANDLE
    custom-model-data: 1001
    commands: [...]
```

- `material`: Vanilla (PAPER, RED_CANDLE...)
- `custom-model-data`: Resource pack
- `itemsadder` / `oraxen` / `nexo`: Ưu tiên hơn material nếu có
- `commands`: Chạy khi người chơi right-click, `%player%` = tên người nhận

Give gói lì xì: `/lixiadmin givegoilixi lixi1 [số lượng]`

---

## Build

```bash
./gradlew shadowJar
```

File JAR: `build/libs/LiXiPlugin-<version>-all.jar`

---

## Cấu trúc dự án

```
src/main/java/me/typical/lixiplugin/
├── LXPlugin.java
├── commands/       # CommandHandler, AdminCommandHandler
├── config/         # ConfigManager, MainConfig, MessageConfig, Lixi
├── economy/        # LixiCurrency, EconomyProvider
├── hook/           # VaultHook, PlayerPointsHook, UniItemHook
├── service/        # ChatLixiService, EnvelopeService, DatabaseManager
└── util/           # MessageUtil, MoneyUtil, EffectUtil
```

---

## Tác giả

typical.smc — rebuild by haoyi-dev
