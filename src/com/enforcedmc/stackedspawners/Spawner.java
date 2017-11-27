package com.enforcedmc.stackedspawners;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;

public class Spawner
{
    private EntityType type;
    private int amount;
    private Location loc;
    
    public Spawner(final EntityType type, final int amt, final Location loc) {
        this.type = type;
        this.amount = amt;
        this.loc = loc;
    }
    
    public EntityType getType() {
        return this.type;
    }
    
    public int getAmount() {
        return this.amount;
    }
    
    public void setAmount(final int amt) {
        this.amount = amt;
    }
    
    public Location getLoc() {
        return this.loc;
    }
    
    public String getName() {
        return Util.getName(this.type);
    }
}
