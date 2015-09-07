package me.taien;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class THHunt
{
  private long timestart;
  private int duration;
  private int value;
  private boolean locked = false;
  private Inventory contents;
  private Location location;
  private Player playerfound;
  private Player closestplayer;
  private Material oldblock;

  public THHunt(long timestart, int value, int duration, Location loc, Inventory contents, Material oldblock)
  {
    this.duration = duration;
    this.timestart = timestart;
    this.value = value;
    this.contents = contents;
    this.location = loc;
    this.playerfound = null;
    this.closestplayer = null;
    this.oldblock = null;
  }

  public int getMinutesLeft() {
    return (int)((this.timestart + this.duration * 60000 - System.currentTimeMillis()) / 60000L);
  }

  public Location getLocation() {
    return this.location;
  }

  public String getWorld() {
    return this.location.getWorld().getName();
  }

  public boolean isChestBlock(Block b) {
    Block cb = TreasureHunt.server.getWorld(getWorld()).getBlockAt(this.location.getBlockX(), this.location.getBlockY(), this.location.getBlockZ());
    Block mb = TreasureHunt.server.getWorld(getWorld()).getBlockAt(this.location.getBlockX(), this.location.getBlockY() - 1, this.location.getBlockZ());
    return (b.equals(cb)) || (b.equals(mb));
  }

  public int getDistanceFrom(Location location) {
    int xdiff = Math.abs(this.location.getBlockX() - location.getBlockX());
    int zdiff = Math.abs(this.location.getBlockZ() - location.getBlockZ());
    return (int)Math.sqrt(Math.pow(xdiff, 2.0D) + Math.pow(zdiff, 2.0D));
  }

  public int get3DDistanceFrom(Location location) {
    int xdiff = Math.abs(this.location.getBlockX() - location.getBlockX());
    int ydiff = Math.abs(this.location.getBlockY() - location.getBlockY());
    int zdiff = Math.abs(this.location.getBlockZ() - location.getBlockZ());
    return (int)Math.sqrt(Math.pow(xdiff, 2.0D) + Math.pow(ydiff, 2.0D) + Math.pow(zdiff, 2.0D));
  }

  public int getValue() {
    return this.value;
  }

  public String getRarityString() {
    return this.value < TreasureHunt.epiclevel ? "&9Legendary" : this.value < TreasureHunt.legendarylevel ? "&aRare" : this.value < TreasureHunt.rarelevel ? "&eUncommon" : this.value < TreasureHunt.uncommonlevel ? "&fCommon" : "&5EPIC";
  }

  public Player getPlayerFound() {
    return this.playerfound;
  }

  public void showClosestPlayer() {
    if (!this.locked) {
      Player current = null;
      int currdist = 200;
      World w = this.location.getWorld();
      Player[] aplayer;
      int i = (aplayer = Bukkit.getServer().getOnlinePlayers()).length;

      for (int j = 0; j < i; j++) {
        Player data = aplayer[j];
        if ((data.getWorld() == w) && (!TreasureHunt.nodetectlist.contains(data))) {
          int k = TreasureHunt.threedimensionaldistance ? get3DDistanceFrom(data.getLocation()) : getDistanceFrom(data.getLocation());
          if (k < currdist) {
            currdist = k;
            current = data;
          }
        }
      }

      if ((current == null) && (this.closestplayer != null)) {
        this.closestplayer = null;
      } else if (current != this.closestplayer) {
        HashMap hashmap = new HashMap();
        hashmap.put("rarity", getRarityString());
        hashmap.put("value", Integer.toString(this.value));
        hashmap.put("location", getLocString());
        hashmap.put("pname", current.getName());
        hashmap.put("worldname", this.location.getWorld().getName());
        hashmap.put("distance", Integer.toString(currdist));
        hashmap.put("numhunts", Integer.toString(TreasureHunt.getHuntsInWorld(this.location.getWorld().getName()).size()));
        hashmap.put("timeleft", new StringBuilder().append(getMinutesLeft()).append(" minutes").toString());
        if (this.closestplayer == null)
          TreasureHunt.broadcast(TreasureHunt.colorize(TreasureHunt.convertTags(TreasureHunt.playerclose, hashmap)));
        else {
          this.closestplayer.sendMessage(TreasureHunt.colorize(TreasureHunt.convertTags(TreasureHunt.nolongerclosest, hashmap)));
        }

        this.closestplayer = current;
        if (this.closestplayer != null) {
          this.closestplayer.sendMessage(TreasureHunt.colorize(TreasureHunt.convertTags(TreasureHunt.youareclosest, hashmap)));
        }
      }

      if ((current != null) && (getMinutesLeft() < 10))
        this.timestart = (System.currentTimeMillis() - (this.duration - 10) * 60000);
    }
  }

  public String getLocString()
  {
    return new StringBuilder().append(this.location.getBlockX()).append(",").append(this.location.getBlockY()).append(",").append(this.location.getBlockZ()).toString();
  }

  public boolean isLocked() {
    return this.locked;
  }

  public void chestFoundBy(Player p, boolean stationary) {
    HashMap data = new HashMap();
    data.put("rarity", getRarityString());
    data.put("value", Integer.toString(this.value));
    data.put("location", getLocString());
    data.put("pname", p.getName());
    data.put("worldname", this.location.getWorld().getName());
    data.put("amount", Integer.toString(getMinutesLeft()));
    data.put("numhunts", Integer.toString(TreasureHunt.getHuntsInWorld(this.location.getWorld().getName()).size()));
    data.put("timeleft", new StringBuilder().append(getMinutesLeft()).append(" minutes").toString());
    this.timestart = (System.currentTimeMillis() - (this.duration - TreasureHunt.foundchestfadetime) * 60000);
    int moneyamount = 0;
    THWorldOpts o = (THWorldOpts)TreasureHunt.worlds.get(this.location.getWorld().getName());
    double mult = o.moneymultiplier;
    if ((mult != 0.0D) && (TreasureHunt.economy != null)) {
      int maxmoney = (int)(this.value * mult);
      moneyamount = TreasureHunt.rndGen.nextInt(maxmoney + 1);
      if (moneyamount < o.minmoney) {
        moneyamount = o.minmoney;
      }

      data.put("amount", new StringBuilder().append(moneyamount).append(" ").append(TreasureHunt.economy.currencyNamePlural()).toString());
      TreasureHunt.economy.depositPlayer(p.getName(), moneyamount);
    }

    if (moneyamount > 0) {
      p.sendMessage(TreasureHunt.colorize(TreasureHunt.convertTags(TreasureHunt.moneyfound, data)));
    }

    if (!stationary) {
      TreasureHunt.broadcast(TreasureHunt.colorize(TreasureHunt.convertTags(TreasureHunt.playerfound, data)));
      TreasureHunt.addFound(p, this.value);
    }

    if (TreasureHunt.detaillogs) {
      if (TreasureHunt.economy != null)
        System.out.println(new StringBuilder().append("[THDetails] ").append(p.getName()).append(" found chest: Value ").append(this.value).append(" at ").append(getLocString()).append(". Gained ").append(moneyamount).append(" ").append(TreasureHunt.economy.currencyNamePlural()).append(".").toString());
      else {
        System.out.println(new StringBuilder().append("[THDetails] ").append(p.getName()).append(" found chest: Value ").append(this.value).append(" at ").append(getLocString()).append(".").toString());
      }
    }

    this.locked = true;
    this.playerfound = p;
  }

  public boolean isExpired() {
    return System.currentTimeMillis() >= this.timestart + this.duration * 60000;
  }

  public void removeChest(boolean stationary) {
    this.contents.clear();
    HashMap data = new HashMap();
    data.put("rarity", getRarityString());
    data.put("value", Integer.toString(this.value));
    data.put("location", getLocString());
    data.put("worldname", this.location.getWorld().getName());
    data.put("numhunts", Integer.toString(TreasureHunt.getHuntsInWorld(this.location.getWorld().getName()).size() - 1));
    THWorldOpts o = (THWorldOpts)TreasureHunt.worlds.get(this.location.getWorld().getName());
    Block b = this.location.getBlock();
    if (b.getChunk().isLoaded()) {
      b.setType(Material.AIR);
      if (o.usemarker)
        if (o.fadeblock == null)
          this.location.getWorld().getBlockAt(b.getX(), b.getY() - 1, b.getZ()).setType(this.oldblock);
        else
          this.location.getWorld().getBlockAt(b.getX(), b.getY() - 1, b.getZ()).setType(o.fadeblock);
    }
    else
    {
      b.getChunk().load();
      b.setType(Material.AIR);
      if (o.usemarker) {
        if (o.fadeblock == null)
          this.location.getWorld().getBlockAt(b.getX(), b.getY() - 1, b.getZ()).setType(this.oldblock);
        else {
          this.location.getWorld().getBlockAt(b.getX(), b.getY() - 1, b.getZ()).setType(o.fadeblock);
        }
      }

      b.getChunk().unload();
    }

    this.timestart = 0L;
    if (!stationary) {
      if (!this.locked)
        TreasureHunt.broadcast(TreasureHunt.colorize(TreasureHunt.convertTags(TreasureHunt.unfoundchestfaded, data)));
      else if (TreasureHunt.foundchestfaded.length() > 0) {
        TreasureHunt.broadcast(TreasureHunt.colorize(TreasureHunt.convertTags(TreasureHunt.foundchestfaded, data)));
      }
    }

    if (TreasureHunt.detaillogs)
      System.out.println(new StringBuilder().append("[THDetails] Chest despawned at ").append((String)data.get("location")).append(". ").append(this.locked ? "(Claimed)" : "(Unclaimed)").toString());
  }
}