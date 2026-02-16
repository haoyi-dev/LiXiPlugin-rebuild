package me.typical.lixiplugin;

import com.tcoded.folialib.FoliaLib;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPISpigotConfig;
import lombok.Getter;
import me.typical.lixiplugin.commands.AdminCommandHandler;
import me.typical.lixiplugin.commands.CommandHandler;
import me.typical.lixiplugin.config.ConfigManager;
import me.typical.lixiplugin.economy.EconomyProvider;
import me.typical.lixiplugin.economy.LixiCurrency;
import me.typical.lixiplugin.hook.PlayerPointsHook;
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

/** Main plugin class. Orchestrates services. */
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
        CommandAPI.onLoad(new CommandAPISpigotConfig(this));
    }

    @Override
    public void onEnable() {
        instance = this;

        CommandAPI.onEnable();
        this.foliaLib = new FoliaLib(this);
        MessageUtil.info("FoliaLib initialized");
        this.configManager = new ConfigManager(this);
        registerServices();

        MessageUtil.info("LiXiPlugin enabled successfully!");
    }

    @Override
    public void onDisable() {
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

        CommandAPI.onDisable();
        MessageUtil.info("LiXiPlugin disabled");
    }

    private void registerServices() {
        MessageUtil.info("Registering services...");
        registerService(new VaultHook());           // Economy integration first
        registerService(new PlayerPointsHook());    // PlayerPoints (optional)
        registerService(new DatabaseManager());     // Database before envelope operations
        registerService(new UniItemHook());         // Item provider before envelope creation
        registerService(new ChatLixiService());     // Chat lixi service
        registerService(new EnvelopeService());     // Envelope service
        registerService(new CommandHandler());      // User commands
        registerService(new AdminCommandHandler()); // Admin commands last

        MessageUtil.info("All services registered successfully");
    }

    private void registerService(IService service) {
        try {
            service.setup();
            services.put(service.getClass(), service);
            serviceOrder.add(service);
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

    @SuppressWarnings("unchecked")
    public <T extends IService> T getService(Class<T> clazz) {
        return (T) services.get(clazz);
    }

    public EconomyProvider getEconomyProvider(LixiCurrency currency) {
        return switch (currency) {
            case VAULT -> getService(VaultHook.class);
            case POINTS -> getService(PlayerPointsHook.class);
        };
    }
}
