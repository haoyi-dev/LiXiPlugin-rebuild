package me.typical.lixiplugin.config;

import de.exlll.configlib.NameFormatters;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurationStore;
import lombok.Getter;
import me.typical.lixiplugin.LXPlugin;
import me.typical.lixiplugin.config.annotations.Folder;
import me.typical.lixiplugin.config.types.Lixi;
import me.typical.lixiplugin.config.types.MainConfig;
import me.typical.lixiplugin.config.types.MessageConfig;
import me.typical.lixiplugin.util.MessageUtil;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages all configuration files for LiXiPlugin.
 * Uses ConfigLib for YAML serialization with kebab-case naming.
 */
public class ConfigManager {
    // Holds loaded config instances
    private static final Map<Class<?>, Object> configs = new HashMap<>();

    @Getter
    private static ConfigManager instance;

    private final LXPlugin plugin;

    private final List<Class<?>> configClasses = List.of(
            MainConfig.class,
            MessageConfig.class,
            Lixi.class
    );

    // Holds file paths for each config type
    private final Map<Class<?>, Path> configPaths = new HashMap<>();

    private final YamlConfigurationProperties properties = YamlConfigurationProperties.newBuilder()
            .setNameFormatter(NameFormatters.LOWER_KEBAB_CASE)
            .header("""
                    LiXiPlugin Configuration @ %d
                    Made by typical.smc
                    """.formatted(Year.now().getValue()))
            .build();

    public ConfigManager(LXPlugin plugin) {
        this.plugin = plugin;
        instance = this;
        // Prepare paths and load all configs
        initPaths();
        registerAll();
    }

    private void initPaths() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        for (Class<?> clazz : configClasses) {
            configPaths.put(clazz, getConfigPath(clazz));
        }
    }

    private Path getConfigPath(Class<?> clazz) {
        String fileName = getConfigFileName(clazz.getSimpleName()) + ".yml";

        if (clazz.isAnnotationPresent(Folder.class)) {
            // If the class is annotated with @Folder, create a subfolder
            String folderName = clazz.getAnnotation(Folder.class).value();
            File folder = new File(plugin.getDataFolder(), folderName);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            return Paths.get(folder.getPath(), fileName);
        }
        return Paths.get(plugin.getDataFolder().getPath(), fileName);
    }

    @SuppressWarnings("unchecked")
    private void registerAll() {
        MessageUtil.info("Loading all configurations");
        for (Class<?> rawClass : configClasses) {
            // Capture wildcard and process each
            registerConfig((Class<Object>) rawClass);
        }
        MessageUtil.info("All configurations loaded successfully");
    }

    private <T> void registerConfig(Class<T> cfgClass) {
        Path path = configPaths.get(cfgClass);

        YamlConfigurationStore<T> store = new YamlConfigurationStore<>(cfgClass, properties);

        // Update existing config file with any new fields
        store.update(path);

        // Load config
        T loaded = store.load(path);
        configs.put(cfgClass, loaded);
    }

    /**
     * Reload all configs (e.g. on /lixi admin reload)
     */
    public void reloadAll() {
        configs.clear();
        MessageUtil.info("Reloading all configurations");
        for (Class<?> rawClass : configClasses) {
            registerConfig(rawClass);
        }
        MessageUtil.info("All configurations reloaded successfully");
    }

    /**
     * Retrieve a loaded configuration instance by its class
     */
    @SuppressWarnings("unchecked")
    public <T> T getConfig(Class<T> cls) {
        return (T) configs.get(cls);
    }

    /**
     * Save a configuration instance to disk
     *
     * @param cls    The configuration class
     * @param config The configuration instance to save
     */
    public <T> void saveConfig(Class<T> cls, T config) {
        Path path = configPaths.get(cls);
        if (path == null) {
            MessageUtil.error("No path found for config class: " + cls.getSimpleName());
            return;
        }

        YamlConfigurationStore<T> store = new YamlConfigurationStore<>(cls, properties);
        store.save(config, path);
        configs.put(cls, config);
        MessageUtil.info("Saved configuration: " + cls.getSimpleName());
    }

    /**
     * Convert class name to kebab-case file name
     */
    private String getConfigFileName(String name) {
        StringBuilder builder = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i != 0) {
                    builder.append('-');
                }
                builder.append(Character.toLowerCase(c));
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }
}
