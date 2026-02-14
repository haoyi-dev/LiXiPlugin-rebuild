# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

LiXiPlugin is a Paper/Folia Minecraft plugin implementing a Vietnamese lucky money (Lì Xì) distribution system. Features include chat broadcasts, physical envelope items, and direct player transfers with economy integration.

## Build System

**Primary build command:**
```bash
./gradlew shadowJar
```
This compiles, shades dependencies, and outputs to `D:\nexo1.21.11\plugins\update\`.

**Development server:**
```bash
./gradlew runServer
```
Starts a test Paper server (via run-paper plugin) for local testing.

**Standard Gradle tasks:**
```bash
./gradlew build          # Full build with tests
./gradlew clean build    # Clean rebuild
```

## Core Architecture

### Service Pattern
All major functionality implements `IService` interface with `setup()` and `shutdown()` methods. Services are registered in `LXPlugin.registerServices()` in **dependency order** - this order is critical:

1. **VaultHook** - Economy integration (required first)
2. **DatabaseManager** - Database pool initialization
3. **UniItemHook** - Custom item provider integration
4. **ChatLixiService** - Chat broadcast functionality
5. **EnvelopeService** - Physical envelope creation
6. **CommandHandler** - User commands
7. **AdminCommandHandler** - Admin commands

Services are shut down in **reverse order** on disable. Access services via `LXPlugin.getService(ServiceClass.class)`.

### Configuration System (ConfigLib)
- All configs use **kebab-case** naming (e.g., `MainConfig` → `main-config.yml`)
- Config classes registered in `ConfigManager.configClasses` list
- Supports `@Folder` annotation for subfolder organization
- Auto-updates existing files with new fields via `store.update(path)`
- Save programmatically with `ConfigManager.saveConfig(Class, instance)`

### Command System (CommandAPI)
- Uses Paper-shade variant: `commandapi-paper-shade:11.1.0`
- Lifecycle: `CommandAPI.onLoad()` in `onLoad()`, `CommandAPI.onEnable()` in service setup
- Custom arguments extend `CustomArgument` (see `MoneyArgument` for parsing 1k/1M/1T formats)
- All commands organized in tree structure with subcommands
- **Player shorthand**: Players can use money shortcuts in commands: `1k` (1,000), `1m` (1,000,000), `1b` (1,000,000,000), `1t` (1,000,000,000,000)

### Database Layer
- **HikariCP** connection pooling supporting SQLite and MariaDB
- All operations are **async** via `CompletableFuture` to avoid main thread blocking
- Atomic operations (e.g., `claimEnvelope()`) use SQL-level locking to prevent race conditions
- Schema automatically created on first startup

### Async Scheduling (FoliaLib)
- Handles both Paper and Folia server types
- Initialized early in `onEnable()` as `plugin.getFoliaLib()`
- Use instead of Bukkit scheduler for cross-platform compatibility

### Dependency Management
All runtime dependencies are:
- **Shaded** into the final JAR via `shadowJar` task
- **Relocated** under `me.typical.lib.*` to prevent conflicts
- External plugin APIs (Vault, PlaceholderAPI, custom item plugins) are `compileOnly`

## Key Development Patterns

### Adding a New Service
1. Implement `IService` interface with `setup()` and `shutdown()` methods
2. Optionally implement `Listener` for auto-event registration
3. Add to `LXPlugin.registerServices()` in appropriate dependency order
4. Access via `LXPlugin.getService(YourService.class)`

### Adding a New Config
1. Create config class in `config/types/` with ConfigLib annotations
2. Add class to `ConfigManager.configClasses` list
3. Access via `plugin.getConfigManager().getConfig(YourConfig.class)`
4. Use `@Folder("subfolder")` annotation if config should be in subfolder

### Adding Commands
Add new `CommandAPICommand` objects in `CommandHandler` or `AdminCommandHandler`:
- Use `withPermission()` for permission checks
- Use `executesPlayer()` for player-only commands
- Leverage existing custom arguments like `MoneyArgument`

### Database Operations
Always use async patterns:
```java
DatabaseManager db = plugin.getService(DatabaseManager.class);
db.someOperation(args).thenAccept(result -> {
    // Handle result on completion
});
```

## Project Structure

```
src/main/java/me/typical/lixiplugin/
├── LXPlugin.java           # Main plugin class, service orchestration
├── LXPluginLoader.java     # Paper plugin loader
├── commands/               # CommandAPI command handlers
│   ├── CommandHandler.java       # User commands (/lixi)
│   ├── AdminCommandHandler.java  # Admin commands (/lixiadmin)
│   └── arguments/               # Custom argument types
├── config/
│   ├── ConfigManager.java       # ConfigLib manager
│   ├── annotations/            # Custom config annotations
│   └── types/                  # Config data classes
├── hook/                   # External plugin integrations
│   ├── VaultHook.java          # Economy API
│   └── UniItemHook.java        # ItemsAdder/Oraxen/Nexo
├── service/               # Core business logic (IService implementations)
│   ├── IService.java          # Service interface
│   ├── DatabaseManager.java   # HikariCP database layer
│   ├── ChatLixiService.java   # Chat broadcast system
│   └── EnvelopeService.java   # Physical envelope creation
└── util/                  # Utility classes
```

## Technical Constraints

- **Java 21** required (uses records, switch expressions, text blocks)
- **Paper API 1.20+** - supports Paper 1.20 and above
- **Folia compatible** - all scheduling via FoliaLib
- Manifest must include: `'paperweight-mappings-namespace': 'mojang'`
- Database dependencies are `compileOnly` (server provides at runtime)

## Important Notes

- **Service registration order matters** - dependencies must be registered before dependents
- Config file names are auto-generated from class names via kebab-case conversion
- All messages should reference config files, not be hard-coded in services
- The plugin uses a Paper loader (`LXPluginLoader`) for bootstrapping
- ShadowJar outputs directly to test server's plugin update folder for rapid iteration
