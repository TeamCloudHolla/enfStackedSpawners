package com.enforcedmc.stackedspawners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;

public class StackedSpawners extends JavaPlugin implements Listener
{
    Map<Entity, Integer> stacked;
    List<CreatureSpawner> one;
    List<Spawner> spawners;
    
    public StackedSpawners() {
        this.stacked = new HashMap<Entity, Integer>();
        this.one = new ArrayList<CreatureSpawner>();
        this.spawners = new ArrayList<Spawner>();
    }
    
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents((Listener)this, (Plugin)this);
        this.getConfig().options().copyDefaults(true);
        this.saveDefaultConfig();
        if (this.getConfig().isConfigurationSection("Spawners")) {
            for (final String s : this.getConfig().getConfigurationSection("Spawners").getValues(false).keySet()) {
                if (this.getLocation(s) != null) {
                    this.spawners.add(new Spawner(Util.getEntity(this.getConfig().getString("Spawners." + s + ".Type")), this.getConfig().getInt("Spawners." + s + ".Amount"), this.getLocation(s)));
                }
            }
        }
    }
    
    public void onDisable() {
        if (this.getConfig().isConfigurationSection("Spawners")) {
            for (final String s : this.getConfig().getConfigurationSection("Spawners").getValues(false).keySet()) {
                this.getConfig().set("Spawners." + s, (Object)null);
            }
        }
        for (final Spawner sp : this.spawners) {
            this.getConfig().set("Spawners." + this.getLoc(sp.getLoc()) + ".Type", (Object)sp.getName());
            this.getConfig().set("Spawners." + this.getLoc(sp.getLoc()) + ".Amount", (Object)sp.getAmount());
        }
        this.saveConfig();
    }
    
    Location getLocation(final String s) {
        try {
            final String[] args = s.split(",");
            return new Location(this.getServer().getWorld(args[0]), Double.parseDouble(args[1]), Double.parseDouble(args[2]), Double.parseDouble(args[3]));
        }
        catch (NullPointerException | NumberFormatException | ArrayIndexOutOfBoundsException ex2) {
            final RuntimeException ex = null;
            final RuntimeException e = ex;
            return null;
        }
    }
    
    String getLoc(final Location l) {
        try {
            return String.valueOf(l.getWorld().getName()) + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
        }
        catch (NullPointerException e) {
            return "";
        }
    }
    
    String strip(final String s) {
        return ChatColor.stripColor(s);
    }
    
    Spawner getSpawner(final CreatureSpawner s) {
        for (final Spawner sp : this.spawners) {
            if (sp.getLoc().equals((Object)s.getLocation()) || (sp.getLoc().getBlockX() == s.getLocation().getBlockX() && sp.getLoc().getBlockY() == s.getLocation().getBlockY() && sp.getLoc().getBlockZ() == s.getLocation().getBlockZ() && s.getSpawnedType().equals((Object)sp.getType()))) {
                return sp;
            }
        }
        return null;
    }
    
    Spawner getNear(final Location loc, final EntityType type) {
        for (final Spawner sp : this.spawners) {
            if (loc.distance(sp.getLoc()) <= 5.0 && sp.getType().equals((Object)type)) {
                return sp;
            }
        }
        return null;
    }
    
    @EventHandler
    public void entityDeath(final EntityDeathEvent e) {
        if (this.stacked.containsKey(e.getEntity()) || (e.getEntity().getCustomName() != null && e.getEntity().getCustomName().startsWith("§e"))) {
            final int old = Integer.parseInt(ChatColor.stripColor(e.getEntity().getCustomName()).replaceAll("[^0-9]", ""));
            if (old <= 1) {
                return;
            }
            final LivingEntity en = (LivingEntity)e.getEntity().getWorld().spawnEntity(e.getEntity().getLocation(), e.getEntityType());
            en.setCustomName("§d" + (old - 1) + "X §6" + en.getType().toString());
            en.setCustomNameVisible(true);
            this.stacked.remove(e.getEntity());
            this.stacked.put((Entity)en, old - 1);
        }
    }
    
    @EventHandler
    public void spawn(final SpawnerSpawnEvent e) {
        final CreatureSpawner sp = e.getSpawner();
        if (this.getSpawner(sp) != null) {
            e.setCancelled(true);
            new BukkitRunnable() {
                public void run() {
                    StackedSpawners.this.one.remove(sp);
                }
            }.runTaskLater((Plugin)this, 5L);
            if (this.one.contains(sp)) {
                return;
            }
            final LivingEntity en = (LivingEntity)e.getEntity().getWorld().spawnEntity(e.getLocation(), e.getEntityType());
            en.setCustomName("§d" + this.getSpawner(sp).getAmount() + "X §6" + e.getEntityType().toString());
            en.setCustomNameVisible(true);
            this.stacked.put((Entity)en, this.getSpawner(sp).getAmount());
            this.one.add(sp);
        }
        else {
            this.spawners.add(new Spawner(sp.getSpawnedType(), 1, sp.getLocation()));
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void blockPlace(final BlockPlaceEvent e) {
        final Player p = e.getPlayer();
        final ItemStack b = e.getItemInHand().clone();
        if (b == null || !b.getType().equals((Object)Material.MOB_SPAWNER) || e.isCancelled()) {
            return;
        }
        final CreatureSpawner sp = (CreatureSpawner)e.getBlock().getState();
        final Spawner near = this.getNear(sp.getLocation(), sp.getSpawnedType());
        if (near != null) {
            e.setCancelled(true);
            b.setAmount(1);
            if (p.getItemInHand().getAmount() > 1) {
                p.getItemInHand().setAmount(p.getItemInHand().getAmount() - 1);
            }
            else {
                p.setItemInHand((ItemStack)null);
            }
            near.setAmount(near.getAmount() + 1);
            for (final Hologram h : HologramsAPI.getHolograms((Plugin)this)) {
                if (h.getLocation().distance(near.getLoc().clone().add(0.5, 1.5, 0.5)) < 1.0) {
                    h.delete();
                }
            }
            final Hologram holo = HologramsAPI.createHologram((Plugin)this, near.getLoc().clone().add(0.5, 1.5, 0.5));
            holo.appendTextLine("§e§l" + near.getAmount() + "X §6" + sp.getSpawnedType().toString());
            return;
        }
        if (b.getDurability() != 0) {
            sp.setSpawnedType(EntityType.fromId((int)b.getDurability()));
            this.spawners.add(new Spawner(sp.getSpawnedType(), 1, e.getBlock().getLocation()));
            final Hologram holo = HologramsAPI.createHologram((Plugin)this, sp.getLocation().clone().add(0.5, 1.5, 0.5));
            holo.appendTextLine("§e§l1X §6" + sp.getSpawnedType().toString());
        }
        else if (b.getItemMeta().hasDisplayName() && b.getItemMeta().getDisplayName().startsWith("§e")) {
            final String name = b.getItemMeta().hasLore() ? this.strip(b.getItemMeta().getLore().get(1)) : this.strip(b.getItemMeta().getDisplayName()).replaceAll(" Spawner", "");
            if (Util.getEntity(name) != null) {
                sp.setSpawnedType(Util.getEntity(name));
                this.spawners.add(new Spawner(Util.getEntity(name), 1, e.getBlock().getLocation()));
                final Hologram holo2 = HologramsAPI.createHologram((Plugin)this, sp.getLocation().clone().add(0.5, 1.5, 0.5));
                holo2.appendTextLine("§e§l1X §6" + sp.getSpawnedType().toString());
            }
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void blockBreak(final BlockBreakEvent e) {
        final Block b = e.getBlock();
        if (!b.getType().equals((Object)Material.MOB_SPAWNER) || !(b.getState() instanceof CreatureSpawner)) {
            return;
        }
        final CreatureSpawner sp = (CreatureSpawner)b.getState();
        final Spawner s = this.getSpawner(sp);
        if (s != null) {
            s.setAmount(s.getAmount() - 1);
            for (final Hologram h : HologramsAPI.getHolograms((Plugin)this)) {
                if (h.getLocation().distance(s.getLoc().clone().add(0.5, 1.5, 0.5)) <= 0.5) {
                    h.delete();
                }
            }
            if (s.getAmount() > 0) {
                e.setCancelled(true);
                final Hologram holo = HologramsAPI.createHologram((Plugin)this, s.getLoc().clone().add(0.5, 1.5, 0.5));
                holo.appendTextLine("§e§l" + s.getAmount() + "X §6" + sp.getSpawnedType().toString());
            }
            else {
                this.spawners.remove(s);
            }
        }
    }
}
