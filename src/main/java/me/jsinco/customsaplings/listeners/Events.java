package me.jsinco.customsaplings.listeners;

import me.jsinco.customsaplings.CustomSaplings;
import me.jsinco.customsaplings.manager.SaplingsManager;
import me.jsinco.customsaplings.util.TextUtils;
import me.jsinco.customsaplings.util.Util;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Random;

public class Events implements Listener {

    private final NamespacedKey saplingKey;
    private final CustomSaplings plugin;

    public Events(CustomSaplings plugin) {
        this.plugin = plugin;
        this.saplingKey = new NamespacedKey(plugin, "sapling");
    }

    // Disable leaf decay
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onLeafDecay(LeavesDecayEvent event) {
        if (plugin.getConfig().getBoolean("disable-leaf-decay")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();

        if (!item.hasItemMeta() || !item.getItemMeta().getPersistentDataContainer().has(saplingKey, PersistentDataType.STRING)) {
            return; // Not a custom sapling
        }

        if (plugin.getConfig().getBoolean("require-permission-to-place") && !event.getPlayer().hasPermission("customsaplings.place")) {
            event.getPlayer().sendMessage(TextUtils.prefix + "You do not have permission to place this sapling!");
            event.setCancelled(true);
            return;
        }

        if (plugin.getConfig().getStringList("disabled-worlds").contains(event.getBlockPlaced().getWorld().getName())) {
            event.getPlayer().sendMessage(TextUtils.prefix + "You cannot place saplings in this world!");
            event.setCancelled(true);
            return;
        }


        String sapling = item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "sapling"), PersistentDataType.STRING);
        event.getBlockPlaced().setMetadata("sapling", new FixedMetadataValue(plugin, sapling));
        Util.debugLog("&a" + event.getPlayer().getName() + " placed a " + sapling + " sapling!");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event) {
        if (plugin.getConfig().getStringList("disabled-worlds").contains(event.getWorld().getName())) {
            return; // Do not allow sapling growth in disabled worlds
        }

        List<BlockState> blocks = event.getBlocks();

        for (BlockState blockState : blocks) {
            if (!blockState.hasMetadata("sapling")) {
                continue;
            }
            Block block = blockState.getBlock();
            String sapling = block.getMetadata("sapling").get(0).asString();
            String schematic = SaplingsManager.getSaplingSchematic(sapling);
            if (schematic == null) {
                Util.debugLog("&cCould not find schematic for sapling! " + sapling);
                return;
            }
            block.removeMetadata("sapling", plugin);
            block.setType(Material.AIR);

            SaplingsManager.setSchematic(schematic, block.getLocation());
            event.setCancelled(true);

            Util.debugLog("&aA " + schematic + " tree has grown!");
            break;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!block.hasMetadata("sapling")) {
            return; // Not a custom sapling
        } else if (!plugin.getConfig().getBoolean("drop-sapling-item-on-break")) {
            return; // Drop sapling is disabled
        }
        List<MetadataValue> metadataValues = block.getMetadata("sapling");
        ItemStack sapling = SaplingsManager.getSaplingItem(metadataValues.get(0).asString());
        if (sapling == null) return;
        else if (!block.getType().equals(sapling.getType())) {
            block.removeMetadata("sapling", plugin);
            return; // Should never happen, but to prevent any possible exploits
        }

        if (plugin.getConfig().getBoolean("require-permission-to-break") && !event.getPlayer().hasPermission("customsaplings.break")) {
            event.getPlayer().sendMessage(TextUtils.prefix + "You do not have permission to break this sapling!");
            Util.debugLog("&e" + event.getPlayer().getName() + " tried to break a sapling but did not have permission! Location: " + event.getPlayer().getLocation());
            event.setCancelled(true);
            return;
        }

        event.setDropItems(false);
        block.getWorld().dropItemNaturally(block.getLocation(), sapling);
        Util.debugLog("&a" + event.getPlayer().getName() + " broke a " + metadataValues.get(0).asString() + " sapling!");
    }

    @EventHandler(ignoreCancelled = true) // For custom sapling boxes
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().equals(Action.RIGHT_CLICK_AIR) && !event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;


        if (event.getItem() == null || !event.getItem().hasItemMeta() || !event.getItem().getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "box"), PersistentDataType.STRING)) {
            return; // Not a custom sapling box
        }

        String rarity = event.getItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "box"), PersistentDataType.STRING);
        List<String> saplings = SaplingsManager.getAllSaplingsOfRarity(rarity);
        if (saplings.isEmpty()) {
            event.getPlayer().sendMessage(TextUtils.prefix + "There are no saplings in this box!");
            Util.debugLog("&eSomeone tried to open a box with no saplings in it! Rarity: " + rarity + " Location: " + event.getPlayer().getLocation());
        } else {
            event.getItem().setAmount(event.getItem().getAmount() - 1);
            String sapling = saplings.get(new Random().nextInt(saplings.size()));
            Util.giveItem(event.getPlayer(), SaplingsManager.getSaplingItem(sapling));
            Util.debugLog("&a" + event.getPlayer().getName() + " opened a " + rarity + " box and got a " + sapling + " sapling!");

            if (plugin.getConfig().get("rarity-boxes." + rarity + ".open-sound") != null) {
                event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.valueOf(plugin.getConfig().getString("rarity-boxes." + rarity + ".open-sound")), 1, 1);
            }
        }
        event.setCancelled(true);
    }

}
