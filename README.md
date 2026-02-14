# LiXiPlugin

A Paper/Folia Minecraft plugin implementing a Vietnamese lucky money (Lì Xì) distribution system with economy integration.

## Features

- **Chat Broadcasts**: Announce lucky money in chat for players to claim
- **Physical Envelopes**: Create physical envelope items that can be traded and opened
- **Direct Transfers**: Send money directly to players with economy integration
- **Multi-database Support**: SQLite and MariaDB via HikariCP connection pooling
- **Folia Compatible**: Async operations with FoliaLib for both Paper and Folia servers
- **Custom Item Integration**: Support for ItemsAdder, Oraxen, and Nexo via UniItem

## Requirements

- **Java 21** or higher
- **Paper/Folia 1.20+**
- **Vault** (for economy integration)
- **Economy Plugin** (e.g., EssentialsX, CMI)

## Building

```bash
# Build and shade dependencies
./gradlew shadowJar

# Run development server
./gradlew runServer

# Clean build
./gradlew clean build
```

Output JAR: `build/libs/LiXiPlugin-<version>-all.jar`

## Commands

### User Commands (`/lixi`)
- `/lixi chat <amount> <quantity>` - Broadcast lucky money in chat
- `/lixi give <player> <amount>` - Give money directly to a player
- `/lixi envelope <amount> [quantity]` - Create physical envelope items

### Admin Commands (`/lixiadmin`)
- `/lixiadmin reload` - Reload all configurations
- `/lixiadmin stats` - View plugin statistics

### Money Shortcuts
Players can use convenient shortcuts in commands:
- `1k` = 1,000
- `1m` = 1,000,000
- `1b` = 1,000,000,000
- `1t` = 1,000,000,000,000

**Example:** `/lixi chat 10k 5` (broadcasts 10,000 money split into 5 claims)

## Configuration

All configurations are managed via ConfigLib with auto-updating YAML files:

- `main-config.yml` - Core plugin settings
- `message-config.yml` - User-facing messages with MiniMessage support

Configs use **kebab-case** naming and support placeholders via PlaceholderAPI.

## Dependencies

### Runtime (Shaded)
- CommandAPI (Paper shade variant)
- ConfigLib
- FoliaLib
- HikariCP
- SQLite/MariaDB drivers

### External (CompileOnly)
- Vault API
- PlaceholderAPI
- ItemsAdder/Oraxen/Nexo (via UniItem)

All runtime dependencies are relocated under `me.typical.lib.*` to prevent conflicts.

## Architecture

The plugin follows a service-based architecture where all major functionality implements the `IService` interface:

- **VaultHook** - Economy integration
- **DatabaseManager** - Async database operations with connection pooling
- **ChatLixiService** - Chat broadcast system with claim tracking
- **EnvelopeService** - Physical envelope creation and management
- **CommandHandler** - User command registration
- **AdminCommandHandler** - Admin command registration

Services are initialized in dependency order on startup and shut down in reverse order on disable.

## Development

The plugin uses:
- **Java 21** features (records, switch expressions, text blocks)
- **Lombok** for boilerplate reduction
- **Paper API** for modern Minecraft features
- **Async patterns** for all database operations
- **FoliaLib** for cross-platform scheduler compatibility

## License

All rights reserved.

## Author

Made by typical.smc
