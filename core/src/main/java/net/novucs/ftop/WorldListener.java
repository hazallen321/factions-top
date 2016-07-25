package net.novucs.ftop;

import net.novucs.ftop.hook.event.*;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class WorldListener implements Listener, PluginService {

    private final FactionsTopPlugin plugin;
    private final Map<BlockPos, ChestWorth> chests = new HashMap<>();

    public WorldListener(FactionsTopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void terminate() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void updateWorth(BlockPlaceEvent event) {
        updateWorth(event.getBlock(), RecalculateReason.PLACE, false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void updateWorth(BlockBreakEvent event) {
        updateWorth(event.getBlock(), RecalculateReason.BREAK, true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void updateWorth(EntityExplodeEvent event) {
        event.blockList().forEach(block -> updateWorth(block, RecalculateReason.EXPLODE, true));
    }

    private void updateWorth(Block block, RecalculateReason reason, boolean negate) {
        // Do nothing if this area should not be calculated.
        String factionId = plugin.getFactionsHook().getFactionAt(block);
        if (plugin.getSettings().getIgnoredFactionIds().contains(factionId)) {
            return;
        }

        // Get the worth type and price of this event.
        double price;
        WorthType worthType;
        Map<Material, Integer> materials = new HashMap<>();
        Map<EntityType, Integer> spawners = new HashMap<>();

        switch (block.getType()) {
            case MOB_SPAWNER:
                worthType = WorthType.SPAWNER;
                EntityType spawnType = ((CreatureSpawner) block.getState()).getSpawnedType();
                price = plugin.getSettings().getSpawnerPrice(spawnType);
                spawners.put(spawnType, negate ? -1 : 1);
                break;
            default:
                worthType = WorthType.BLOCK;
                price = plugin.getSettings().getBlockPrice(block.getType());
                materials.put(block.getType(), negate ? -1 : 1);
                break;
        }

        // Add block price to the count.
        plugin.getWorthManager().add(block.getChunk(), reason, worthType, negate ? -price : price, materials, spawners);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void checkWorth(InventoryOpenEvent event) {
        // Do nothing if a player did not open the inventory or if chest events
        // are disabled.
        if (!(event.getPlayer() instanceof Player) || plugin.getSettings().isDisableChestEvents()) {
            return;
        }

        Inventory inventory = event.getInventory();

        // Set all default worth values for this chest.
        if (inventory.getHolder() instanceof DoubleChest) {
            DoubleChest chest = (DoubleChest) inventory.getHolder();
            checkWorth(chest.getLeftSide().getInventory());
            checkWorth(chest.getRightSide().getInventory());
        }

        if (inventory.getHolder() instanceof Chest) {
            checkWorth(inventory);
        }
    }

    private void checkWorth(Inventory inventory) {
        chests.put(BlockPos.of(inventory.getLocation().getBlock()), getWorth(inventory));
    }

    private ChestWorth getWorth(Inventory inventory) {
        double worth = 0;
        Map<Material, Integer> materials = new HashMap<>();
        Map<EntityType, Integer> spawners = new HashMap<>();

        for (ItemStack item : inventory.getStorageContents()) {
            if (item == null) continue;

            if (item.getType() == Material.MOB_SPAWNER) {
                EntityType spawnerType = plugin.getCraftbukkitHook().getSpawnerType(item);
                worth += plugin.getSettings().getSpawnerPrice(spawnerType) * item.getAmount();

                int count = spawners.getOrDefault(spawnerType, 0);
                spawners.put(spawnerType, count + item.getAmount());
                continue;
            }

            worth += plugin.getSettings().getBlockPrice(item.getType()) * item.getAmount();
            int count = materials.getOrDefault(item.getType(), 0);
            materials.put(item.getType(), count + item.getAmount());
        }

        return new ChestWorth(worth, materials, spawners);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void updateWorth(InventoryCloseEvent event) {
        // Do nothing if a player did not close the inventory or if chest
        // events are disabled.
        if (!(event.getPlayer() instanceof Player) || plugin.getSettings().isDisableChestEvents()) {
            return;
        }

        // Get cached values from when chest was opened and add the difference
        // to the worth manager.
        if (event.getInventory().getHolder() instanceof DoubleChest) {
            DoubleChest chest = (DoubleChest) event.getInventory().getHolder();
            updateWorth(chest.getLeftSide().getInventory());
            updateWorth(chest.getRightSide().getInventory());
        }

        if (event.getInventory().getHolder() instanceof Chest) {
            updateWorth(event.getInventory());
        }
    }

    private void updateWorth(Inventory inventory) {
        BlockPos pos = BlockPos.of(inventory.getLocation().getBlock());
        ChestWorth worth = chests.remove(pos);
        if (worth == null) return;

        worth = getDifference(worth, getWorth(inventory));

        plugin.getWorthManager().add(inventory.getLocation().getChunk(), RecalculateReason.CHEST, WorthType.CHEST,
                worth.getTotalWorth(), worth.getMaterials(), worth.getSpawners());
    }

    private ChestWorth getDifference(ChestWorth first, ChestWorth second) {
        double worth = second.getTotalWorth() - first.getTotalWorth();
        Map<Material, Integer> materials = getDifference(first.getMaterials(), second.getMaterials());
        Map<EntityType, Integer> spawners = getDifference(first.getSpawners(), second.getSpawners());
        return new ChestWorth(worth, materials, spawners);
    }

    private <T> Map<T, Integer> getDifference(Map<T, Integer> first, Map<T, Integer> second) {
        Map<T, Integer> target = new HashMap<>();

        for (Map.Entry<T, Integer> entry : first.entrySet()) {
            int difference = second.getOrDefault(entry.getKey(), 0) - entry.getValue();
            target.put(entry.getKey(), difference);
        }

        for (Map.Entry<T, Integer> entry : second.entrySet()) {
            if (target.containsKey(entry.getKey())) continue;
            target.put(entry.getKey(), entry.getValue());
        }

        return target;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void updateWorth(FactionClaimEvent event) {
        String newFactionId = event.getFactionId();
        event.getClaims().asMap().forEach((oldFactionId, claims) -> {
            plugin.getWorthManager().update(newFactionId, claims, false);
            plugin.getWorthManager().update(oldFactionId, claims, true);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void removeFaction(FactionDisbandEvent event) {
        plugin.getWorthManager().remove(event.getFactionId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void renameFaction(FactionRenameEvent event) {
        plugin.getWorthManager().rename(event.getFactionId(), event.getNewName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void updateWorth(FactionEconomyEvent event) {
        plugin.getWorthManager().add(event.getFactionId(), WorthType.FACTION_BALANCE, event.getNewBalance() - event.getOldBalance());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void updateWorth(PlayerEconomyEvent event) {
        String factionId = plugin.getFactionsHook().getFaction(event.getPlayer());
        plugin.getWorthManager().add(factionId, WorthType.PLAYER_BALANCE, event.getNewBalance() - event.getOldBalance());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void updateWorth(FactionJoinEvent event) {
        double balance = plugin.getEconomyHook().getBalance(event.getPlayer());
        plugin.getWorthManager().add(event.getFactionId(), WorthType.PLAYER_BALANCE, balance);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void updateWorth(FactionLeaveEvent event) {
        double balance = plugin.getEconomyHook().getBalance(event.getPlayer());
        plugin.getWorthManager().add(event.getFactionId(), WorthType.PLAYER_BALANCE, -balance);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void recalculate(ChunkUnloadEvent event) {
        plugin.getWorthManager().recalculate(event.getChunk(), RecalculateReason.UNLOAD);
    }
}
