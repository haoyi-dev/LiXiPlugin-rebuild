package me.typical.lixiplugin.hook;

import io.github.projectunified.uniitem.all.AllItemProvider;
import io.github.projectunified.uniitem.api.ItemKey;
import io.github.projectunified.uniitem.api.ItemProvider;
import me.typical.lixiplugin.LXPlugin;
import me.typical.lixiplugin.config.types.Lixi;
import me.typical.lixiplugin.config.types.MainConfig;
import me.typical.lixiplugin.economy.EconomyProvider;
import me.typical.lixiplugin.economy.LixiCurrency;
import me.typical.lixiplugin.service.IService;
import me.typical.lixiplugin.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Hook for custom envelope items (vanilla + ItemsAdder/Oraxen/Nexo). */
public class UniItemHook implements IService {

    private final LXPlugin plugin = LXPlugin.getInstance();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private NamespacedKey lixiIdKey;
    private NamespacedKey lixiTypeKey;
    private ItemProvider uniItemProvider;

    @Override
    public void setup() {
        lixiIdKey = new NamespacedKey(plugin, "lixi-id");
        lixiTypeKey = new NamespacedKey(plugin, "lixi-type");
        MessageUtil.info("UniItem hook initialized");

         uniItemProvider = new AllItemProvider();
    }

    @Override
    public void shutdown() {
    }

    public ItemStack createEnvelopeItem(double amount, UUID id, LixiCurrency currencyType) {
        MainConfig.EnvelopeConfig envelopeConfig = plugin.getConfigManager()
                .getConfig(MainConfig.class)
                .getEnvelope();

        ItemStack item = createItem(envelopeConfig);

        EconomyProvider provider = plugin.getEconomyProvider(currencyType != null ? currencyType : LixiCurrency.VAULT);
        String formattedAmount = provider != null && provider.isAvailable()
                ? provider.format(amount) : String.valueOf(amount);

        item.editMeta(meta -> {
            String displayName = envelopeConfig.getDisplayName();
            Component nameComponent = miniMessage.deserialize(displayName);
            meta.displayName(nameComponent);

            String[] loreTemplate = envelopeConfig.getLore();
            List<Component> loreComponents = new ArrayList<>();
            for (String loreLine : loreTemplate) {
                String processedLine = loreLine.replace("%amount%", formattedAmount);
                loreComponents.add(miniMessage.deserialize(processedLine));
            }
            meta.lore(loreComponents);

            meta.getPersistentDataContainer().set(lixiIdKey, PersistentDataType.STRING, id.toString());
        });

        return item;
    }

    /** Create item pack envelope by lixi type name. Right-click runs commands from lixi.yml. */
    public ItemStack createItemPackEnvelope(String lixiName) {
        Lixi.LixiEntry entry = getLixiEntry(lixiName);
        if (entry == null) {
            return null;
        }

        ItemStack item = createItemFromLixiEntry(entry);
        item.editMeta(meta -> {
            Component nameComponent = miniMessage.deserialize(entry.getDisplayName());
            meta.displayName(nameComponent);

            List<Component> loreComponents = new ArrayList<>();
            for (String loreLine : entry.getLore()) {
                loreComponents.add(miniMessage.deserialize(loreLine));
            }
            meta.lore(loreComponents);

            meta.getPersistentDataContainer().set(lixiTypeKey, PersistentDataType.STRING, lixiName);
        });

        return item;
    }

    private ItemStack createItemFromLixiEntry(Lixi.LixiEntry entry) {
        if (entry.getItemsadder() != null && !entry.getItemsadder().isBlank()) {
            ItemStack item = uniItemProvider.item(new ItemKey("itemsadder", entry.getItemsadder()));
            if (item != null) return item;
        }
        if (entry.getOraxen() != null && !entry.getOraxen().isBlank()) {
            ItemStack item = uniItemProvider.item(new ItemKey("oraxen", entry.getOraxen()));
            if (item != null) return item;
        }
        if (entry.getNexo() != null && !entry.getNexo().isBlank()) {
            ItemStack item = uniItemProvider.item(new ItemKey("nexo", entry.getNexo()));
            if (item != null) return item;
        }
        Material mat = Material.PAPER;
        try {
            mat = Material.valueOf(entry.getMaterial() != null ? entry.getMaterial() : "PAPER");
        } catch (IllegalArgumentException ignored) {
        }
        ItemStack item = new ItemStack(mat);
        if (entry.getCustomModelData() != 0) {
            item.editMeta(meta -> meta.setCustomModelData(entry.getCustomModelData()));
        }
        return item;
    }

    public Lixi.LixiEntry getLixiEntry(String lixiName) {
        Map<String, Lixi.LixiEntry> map = plugin.getConfigManager().getConfig(Lixi.class).getLixi();
        return map != null ? map.get(lixiName) : null;
    }

    public Map<String, Lixi.LixiEntry> getAllLixiTypes() {
        return plugin.getConfigManager().getConfig(Lixi.class).getLixi();
    }

    /** ItemsAdder → Oraxen → Nexo → Vanilla fallback */
    private ItemStack createItem(MainConfig.EnvelopeConfig config) {
        String itemsAdderItemId = config.getItemsadder().getItemId();
        if (itemsAdderItemId != null && !itemsAdderItemId.trim().isEmpty()) {
            ItemStack item = uniItemProvider.item(new ItemKey("itemsadder", itemsAdderItemId));
            if (item != null) {
                return item;
            } else {
            }
        }

        String oraxenItemId = config.getOraxen().getItemId();
        if (oraxenItemId != null && !oraxenItemId.trim().isEmpty()) {
            ItemStack item = uniItemProvider.item(new ItemKey("oraxen", oraxenItemId));
            if (item != null) {
                return item;
            } else {
            }
        }

        String nexoItemId = config.getNexo().getItemId();
        if (nexoItemId != null && !nexoItemId.trim().isEmpty()) {
            ItemStack item = uniItemProvider.item(new ItemKey("nexo", nexoItemId));
            if (item != null) {
                return item;
            }
        }

        return new ItemStack(Material.RED_CANDLE);
    }

    public ItemKey detectItemKey(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        return uniItemProvider.key(item);
    }

    public boolean isLixiItemPack(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(lixiTypeKey, PersistentDataType.STRING);
    }

    public String getLixiItemPackName(ItemStack item) {
        if (!isLixiItemPack(item)) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(lixiTypeKey, PersistentDataType.STRING);
    }

    public boolean isLixiEnvelope(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta()
                .getPersistentDataContainer()
                .has(lixiIdKey, PersistentDataType.STRING);
    }

    public UUID getEnvelopeId(ItemStack item) {
        if (!isLixiEnvelope(item)) {
            return null;
        }
        String idString = item.getItemMeta()
                .getPersistentDataContainer()
                .get(lixiIdKey, PersistentDataType.STRING);
        try {
            return UUID.fromString(idString);
        } catch (IllegalArgumentException e) {
            return null;
        }
        
    }

}

