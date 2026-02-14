package me.typical.lixiplugin;

import com.tcoded.folialib.FoliaLib;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIPaperConfig;
import lombok.Getter;
import me.typical.lixiplugin.commands.AdminCommandHandler;
import me.typical.lixiplugin.commands.CommandHandler;
import me.typical.lixiplugin.config.ConfigManager;
import me.typical.lixiplugin.hook.UniItemHook;
import me.typical.lixiplugin.hook.VaultHook;
import me.typical.lixiplugin.service.ChatLixiService;
import me.typical.lixiplugin.service.DatabaseManager;
import me.typical.lixiplugin.service.EnvelopeService;
import me.typical.lixiplugin.service.IService;
import me.typical.lixiplugin.util.MessageUtil;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main plugin class for LiXiPlugin.
 * Manages all services and the plugin lifecycle.
 */
public final class LXPlugin extends JavaPlugin {

    @Getter
    private static LXPlugin instance;

    @Getter
    private FoliaLib foliaLib;

    @Getter
    private ConfigManager configManager;

    private final Map<Class<? extends IService>, IService> services = new HashMap<>();
    private final List<IService> serviceOrder = new ArrayList<>();

    @Override
    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIPaperConfig(this));
    }

    @Override
    public void onEnable() {
        instance = this;

        // Initialize FoliaLib for cross-platform scheduler support
        this.foliaLib = new FoliaLib(this);
        MessageUtil.info("FoliaLib initialized");

        // Load configurations
        this.configManager = new ConfigManager(this);

        // Register all services
        registerServices();

        MessageUtil.info("LiXiPlugin enabled successfully!");
    }

    @Override
    public void onDisable() {
        // Shutdown services in reverse order
        MessageUtil.info("Shutting down services...");
        for (int i = serviceOrder.size() - 1; i >= 0; i--) {
            IService service = serviceOrder.get(i);
            try {
                service.shutdown();
                MessageUtil.info("Service " + service.getClass().getSimpleName() + " shut down");
            } catch (Exception e) {
                MessageUtil.error("Error shutting down service " + service.getClass().getSimpleName());
                e.printStackTrace();
            }
        }

        MessageUtil.info("LiXiPlugin disabled");
    }

    /**
     * Register all plugin services in the correct order.
     * Services are initialized in order and shut down in reverse order.
     */
    private void registerServices() {
        MessageUtil.info("Registering services...");

        // Register services in dependency order
        registerService(new VaultHook());           // Economy integration first
        registerService(new DatabaseManager());     // Database before envelope operations
        registerService(new UniItemHook());         // Item provider before envelope creation
        registerService(new ChatLixiService());     // Chat lixi service
        registerService(new EnvelopeService());     // Envelope service
        registerService(new CommandHandler());      // User commands
        registerService(new AdminCommandHandler()); // Admin commands last

        MessageUtil.info("All services registered successfully");
    }

    /**
     * Register and initialize a single service.
     * If the service implements Listener, it will be automatically registered.
     */
    private void registerService(IService service) {
        try {
            service.setup();
            services.put(service.getClass(), service);
            serviceOrder.add(service);

            // Auto-register as listener if applicable
            if (service instanceof Listener) {
                getServer().getPluginManager().registerEvents((Listener) service, this);
                MessageUtil.info("Service " + service.getClass().getSimpleName() + " registered as listener");
            }

            MessageUtil.info("Service " + service.getClass().getSimpleName() + " initialized");
        } catch (Exception e) {
            MessageUtil.error("Failed to initialize service: " + service.getClass().getSimpleName());
            e.printStackTrace();
        }
    }

    /**
     * Get a service instance by its class.
     *
     * @param clazz The service class
     * @param <T>   The service type
     * @return The service instance, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T extends IService> T getService(Class<T> clazz) {
        return (T) services.get(clazz);
    }
}
