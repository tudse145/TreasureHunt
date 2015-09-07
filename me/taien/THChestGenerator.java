package me.taien;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.BukkitUtil;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class THChestGenerator
{
  public static int findValue(THWorldOpts o)
  {
    int value = o.maxvalue;
    int current = value;

    for (int i = 1; i <= o.drawweight; i++) {
      value = TreasureHunt.rndGen.nextInt(o.maxvalue);
      current = value < current ? value : current;
    }

    return current;
  }

  public static Block findLocation(World w, THWorldOpts o, int amtofruns) {
    Block target = null;
    boolean found = false;
    int attempt = 0;
    boolean x = false;
    boolean y = false;
    boolean z = false;
    int minxpos = o.centerx + o.mindistance;
    int minxneg = o.centerx - o.mindistance;
    int minzpos = o.centerz + o.mindistance;
    int minzneg = o.centerz - o.mindistance;
    do
    {
      attempt++;
      int i = TreasureHunt.rndGen.nextInt(o.maxdistance * 2) - o.maxdistance + o.centerx;
      int j = TreasureHunt.rndGen.nextInt(o.maxdistance * 2) - o.maxdistance + o.centerz;
      do
      {
        i = TreasureHunt.rndGen.nextInt(o.maxdistance * 2) - o.maxdistance + o.centerx;
        j = TreasureHunt.rndGen.nextInt(o.maxdistance * 2) - o.maxdistance + o.centerz;
      }while (((i < minxpos) && (i > minxneg)) || (
        (j < minzpos) && (j > minzneg)));
      int k;
      do
      {
        int k;
        if (o.searchingvalue < 2500)
          k = TreasureHunt.rndGen.nextInt(o.maxelevation);
        else
          k = TreasureHunt.rndGen.nextInt(o.maxelevationrare);
      }
      while (k < o.minelevation);

      target = w.getBlockAt(i, k, j);
      if ((target.getType() == Material.AIR) && (target.getLightLevel() <= o.maxlight) && (target.getLightLevel() >= o.minlight)) {
        target = w.getBlockAt(i, k - 1, j);
        if (o.spawnableblocks.contains(target.getType())) {
          if (TreasureHunt.worldguard != null) {
            THFakePlayer p = new THFakePlayer();
            LocalPlayer lp = TreasureHunt.worldguard.wrapPlayer(p);

            ApplicableRegionSet rs = TreasureHunt.worldguard.getRegionManager(w).getApplicableRegions(BukkitUtil.toVector(target));
            if ((!rs.canBuild(lp)) && (!rs.allows(DefaultFlag.CHEST_ACCESS, lp))) {
              System.out.println("[TreasureHuntDebug] Potential location denied due to WorldGuard.");
              continue;
            }
          }

          found = true;
          target = w.getBlockAt(i, k, j);
        }
      }
    }
    while ((!found) && (attempt <= amtofruns));

    return found ? target : null;
  }

  public static int populateItems(Inventory contents, THWorldOpts o, int current, boolean stationary, String stationaryitemlist) {
    int generatedvalue = 0;
    HashMap all = new HashMap();
    HashMap allvals = new HashMap();
    int i = 0;

    if (!stationary) {
      if (TreasureHunt.worldlists.get(o.itemlist) == null) {
        System.out.println("[TreasureHunt] Couldn't fill chest!  WorldList '" + o.itemlist + "' doesn't exist!");
        return -1;
      }

      if (!o.strictitems) {
        if (current >= TreasureHunt.epiclevel) {
          for (Iterator curnbr = ((THWorldList)TreasureHunt.worldlists.get(o.itemlist)).epic.entrySet().iterator(); curnbr.hasNext(); i++) {
            Map.Entry nbr = (Map.Entry)curnbr.next();
            all.put(Integer.valueOf(i), (ItemStack)nbr.getKey());
            allvals.put((ItemStack)nbr.getKey(), (Integer)nbr.getValue());
          }
        }
        if (current >= TreasureHunt.legendarylevel) {
          for (Iterator curnbr = ((THWorldList)TreasureHunt.worldlists.get(o.itemlist)).legendary.entrySet().iterator(); curnbr.hasNext(); i++) {
            Map.Entry nbr = (Map.Entry)curnbr.next();
            all.put(Integer.valueOf(i), (ItemStack)nbr.getKey());
            allvals.put((ItemStack)nbr.getKey(), (Integer)nbr.getValue());
          }
        }

        if (current >= TreasureHunt.rarelevel) {
          for (Iterator curnbr = ((THWorldList)TreasureHunt.worldlists.get(o.itemlist)).rare.entrySet().iterator(); curnbr.hasNext(); i++) {
            Map.Entry nbr = (Map.Entry)curnbr.next();
            all.put(Integer.valueOf(i), (ItemStack)nbr.getKey());
            allvals.put((ItemStack)nbr.getKey(), (Integer)nbr.getValue());
          }
        }

        if (current >= TreasureHunt.uncommonlevel) {
          for (Iterator curnbr = ((THWorldList)TreasureHunt.worldlists.get(o.itemlist)).uncommon.entrySet().iterator(); curnbr.hasNext(); i++) {
            Map.Entry nbr = (Map.Entry)curnbr.next();
            all.put(Integer.valueOf(i), (ItemStack)nbr.getKey());
            allvals.put((ItemStack)nbr.getKey(), (Integer)nbr.getValue());
          }
        }

        for (Iterator curnbr = ((THWorldList)TreasureHunt.worldlists.get(o.itemlist)).common.entrySet().iterator(); curnbr.hasNext(); i++) {
          Map.Entry nbr = (Map.Entry)curnbr.next();
          all.put(Integer.valueOf(i), (ItemStack)nbr.getKey());
          allvals.put((ItemStack)nbr.getKey(), (Integer)nbr.getValue());
        }
      }
      if (current >= TreasureHunt.epiclevel)
        for (Iterator curnbr = ((THWorldList)TreasureHunt.worldlists.get(o.itemlist)).epic.entrySet().iterator(); curnbr.hasNext(); i++) {
          Map.Entry nbr = (Map.Entry)curnbr.next();
          all.put(Integer.valueOf(i), (ItemStack)nbr.getKey());
          allvals.put((ItemStack)nbr.getKey(), (Integer)nbr.getValue());
        }
      if (current >= TreasureHunt.legendarylevel)
        for (Iterator curnbr = ((THWorldList)TreasureHunt.worldlists.get(o.itemlist)).legendary.entrySet().iterator(); curnbr.hasNext(); i++) {
          Map.Entry nbr = (Map.Entry)curnbr.next();
          all.put(Integer.valueOf(i), (ItemStack)nbr.getKey());
          allvals.put((ItemStack)nbr.getKey(), (Integer)nbr.getValue());
        }
      if (current >= TreasureHunt.rarelevel)
        for (Iterator curnbr = ((THWorldList)TreasureHunt.worldlists.get(o.itemlist)).rare.entrySet().iterator(); curnbr.hasNext(); i++) {
          Map.Entry nbr = (Map.Entry)curnbr.next();
          all.put(Integer.valueOf(i), (ItemStack)nbr.getKey());
          allvals.put((ItemStack)nbr.getKey(), (Integer)nbr.getValue());
        }
      if (current >= TreasureHunt.uncommonlevel) {
        for (Iterator curnbr = ((THWorldList)TreasureHunt.worldlists.get(o.itemlist)).uncommon.entrySet().iterator(); curnbr.hasNext(); i++) {
          Map.Entry nbr = (Map.Entry)curnbr.next();
          all.put(Integer.valueOf(i), (ItemStack)nbr.getKey());
          allvals.put((ItemStack)nbr.getKey(), (Integer)nbr.getValue());
        }
      }
      for (Iterator curnbr = ((THWorldList)TreasureHunt.worldlists.get(o.itemlist)).common.entrySet().iterator(); curnbr.hasNext(); i++) {
        Map.Entry nbr = (Map.Entry)curnbr.next();
        all.put(Integer.valueOf(i), (ItemStack)nbr.getKey());
        allvals.put((ItemStack)nbr.getKey(), (Integer)nbr.getValue());
      }
    }
    else {
      boolean flag = true;
      if (TreasureHunt.customlists.get(stationaryitemlist) == null) {
        flag = false;
      }

      if ((!flag) && (TreasureHunt.worldlists.get(stationaryitemlist) == null)) {
        System.out.println("[TreasureHunt] Couldn't fill stationary chest!  WorldList/CustomList '" + stationaryitemlist + "' doesn't exist!");
        return -1;
      }

      if (flag)
        for (Iterator item = ((Map)TreasureHunt.customlists.get(stationaryitemlist)).entrySet().iterator(); item.hasNext(); i++) {
          Map.Entry entry = (Map.Entry)item.next();
          all.put(Integer.valueOf(i), (ItemStack)entry.getKey());
          allvals.put((ItemStack)entry.getKey(), (Integer)entry.getValue());
        }
      if (!o.strictitems) {
        if (current >= TreasureHunt.epiclevel) {
          for (Iterator item = ((THWorldList)TreasureHunt.worldlists.get(o.itemlist)).epic.entrySet().iterator(); item.hasNext(); i++) {
            Map.Entry entry = (Map.Entry)item.next();
            all.put(Integer.valueOf(i), (ItemStack)entry.getKey());
            allvals.put((ItemStack)entry.getKey(), (Integer)entry.getValue());
          }
        }

        if (current >= TreasureHunt.legendarylevel) {
          for (Iterator item = ((THWorldList)TreasureHunt.worldlists.get(o.itemlist)).legendary.entrySet().iterator(); item.hasNext(); i++) {
            Map.Entry entry = (Map.Entry)item.next();
            all.put(Integer.valueOf(i), (ItemStack)entry.getKey());
            allvals.put((ItemStack)entry.getKey(), (Integer)entry.getValue());
          }
        }

        if (current >= TreasureHunt.rarelevel) {
          for (Iterator item = ((THWorldList)TreasureHunt.worldlists.get(o.itemlist)).rare.entrySet().iterator(); item.hasNext(); i++) {
            Map.Entry entry = (Map.Entry)item.next();
            all.put(Integer.valueOf(i), (ItemStack)entry.getKey());
            allvals.put((ItemStack)entry.getKey(), (Integer)entry.getValue());
          }
        }

        if (current >= TreasureHunt.uncommonlevel) {
          for (Iterator item = ((THWorldList)TreasureHunt.worldlists.get(o.itemlist)).uncommon.entrySet().iterator(); item.hasNext(); i++) {
            Map.Entry entry = (Map.Entry)item.next();
            all.put(Integer.valueOf(i), (ItemStack)entry.getKey());
            allvals.put((ItemStack)entry.getKey(), (Integer)entry.getValue());
          }
        }

        for (Iterator item = ((THWorldList)TreasureHunt.worldlists.get(o.itemlist)).common.entrySet().iterator(); item.hasNext(); i++) {
          Map.Entry entry = (Map.Entry)item.next();
          all.put(Integer.valueOf(i), (ItemStack)entry.getKey());
          allvals.put((ItemStack)entry.getKey(), (Integer)entry.getValue());
        }
      }
      if (current >= TreasureHunt.epiclevel)
        for (Iterator item = ((THWorldList)TreasureHunt.worldlists.get(o.itemlist)).epic.entrySet().iterator(); item.hasNext(); i++) {
          Map.Entry entry = (Map.Entry)item.next();
          all.put(Integer.valueOf(i), (ItemStack)entry.getKey());
          allvals.put((ItemStack)entry.getKey(), (Integer)entry.getValue());
        }
      if (current >= TreasureHunt.legendarylevel)
        for (Iterator item = ((THWorldList)TreasureHunt.worldlists.get(o.itemlist)).legendary.entrySet().iterator(); item.hasNext(); i++) {
          Map.Entry entry = (Map.Entry)item.next();
          all.put(Integer.valueOf(i), (ItemStack)entry.getKey());
          allvals.put((ItemStack)entry.getKey(), (Integer)entry.getValue());
        }
      if (current >= TreasureHunt.rarelevel)
        for (Iterator item = ((THWorldList)TreasureHunt.worldlists.get(o.itemlist)).rare.entrySet().iterator(); item.hasNext(); i++) {
          Map.Entry entry = (Map.Entry)item.next();
          all.put(Integer.valueOf(i), (ItemStack)entry.getKey());
          allvals.put((ItemStack)entry.getKey(), (Integer)entry.getValue());
        }
      if (current >= TreasureHunt.uncommonlevel) {
        for (Iterator item = ((THWorldList)TreasureHunt.worldlists.get(o.itemlist)).uncommon.entrySet().iterator(); item.hasNext(); i++) {
          Map.Entry entry = (Map.Entry)item.next();
          all.put(Integer.valueOf(i), (ItemStack)entry.getKey());
          allvals.put((ItemStack)entry.getKey(), (Integer)entry.getValue());
        }
      }
      for (Iterator item = ((THWorldList)TreasureHunt.worldlists.get(o.itemlist)).common.entrySet().iterator(); item.hasNext(); i++) {
        Map.Entry entry = (Map.Entry)item.next();
        all.put(Integer.valueOf(i), (ItemStack)entry.getKey());
        allvals.put((ItemStack)entry.getKey(), (Integer)entry.getValue());
      }
    }

    do
    {
      boolean flag = false;
      int i1 = all.size();
      if (all.size() < 1) {
        System.out.println("[TreasureHunt] There appear to be no items in the selection list...chest spawn failed.");
        return -1;
      }

      for (int j = 1; j <= o.gooditemweight; j++) {
        int k = TreasureHunt.rndGen.nextInt(all.size());
        i1 = k < i1 ? k : i1;
      }

      ItemStack itemstack = (ItemStack)all.get(Integer.valueOf(i1));
      if (itemstack.getMaxStackSize() == 1) {
        contents.addItem(new ItemStack[] { itemstack });
        generatedvalue += ((Integer)allvals.get(itemstack)).intValue();
      } else {
        int maxamt = (current - generatedvalue) / ((Integer)allvals.get(itemstack)).intValue();
        if (maxamt <= 0) {
          if (itemstack.getDurability() == 0)
            contents.addItem(new ItemStack[] { new ItemStack(itemstack.getType(), 1) });
          else {
            contents.addItem(new ItemStack[] { new ItemStack(itemstack.getType(), 1, itemstack.getDurability()) });
          }

          generatedvalue += ((Integer)allvals.get(itemstack)).intValue();
        } else {
          int amt = TreasureHunt.rndGen.nextInt(maxamt) + 1;

          for (int ii = 1; ii < o.amountweight; ii++) {
            int newamt = TreasureHunt.rndGen.nextInt(maxamt) + 1;
            if (newamt < amt) {
              amt = newamt;
            }
          }

          if (itemstack.getDurability() == 0)
            contents.addItem(new ItemStack[] { new ItemStack(itemstack.getType(), amt) });
          else {
            contents.addItem(new ItemStack[] { new ItemStack(itemstack.getType(), amt, itemstack.getDurability()) });
          }

          generatedvalue += ((Integer)allvals.get(itemstack)).intValue() * amt;
        }
      }
    }
    while ((generatedvalue < current) && (contents.firstEmpty() >= 0));

    return generatedvalue;
  }

  public static THHunt startHunt(World worldtouse, int setvalue, Block block, boolean stationary, String stationaryitemlist) {
    if (TreasureHunt.worlds.size() == 0) {
      System.out.println("[TreasureHunt] Unable to start hunt!  No worlds set!");
      return null;
    }
    THWorldOpts o = (THWorldOpts)TreasureHunt.worlds.get(worldtouse.getName());
    if (o == null) {
      System.out.println("[TreasureHunt] Unable to start hunt!  World not set up!");
      return null;
    }
    int maxvalue = o.maxvalue + 1;
    boolean x = false;
    boolean y = false;
    boolean z = false;
    Inventory contents = null;
    Material oldblock = null;
    int current;
    int current;
    if (setvalue == -1) {
      int value = findValue(o);
      current = value;
    } else {
      current = setvalue;
    }

    if (block == null) {
      o.searchingloc = true;
      o.searchingattempts = 0;
      o.searchingvalue = current;
      return null;
    }
    int i = block.getX();
    int j = block.getY() - 1;
    int k = block.getZ();
    if (o.usemarker) {
      block = worldtouse.getBlockAt(i, j, k);
      oldblock = block.getType();
      block.setType(o.markerblock);
    }

    j++;
    block = worldtouse.getBlockAt(i, j, k);
    block.setType(Material.CHEST);
    InventoryHolder location = (InventoryHolder)block.getState();
    contents = location.getInventory();
    Location wLocation = new Location(worldtouse, i, j, k);
    if (stationary)
      current = populateItems(contents, o, current, true, stationaryitemlist);
    else {
      current = populateItems(contents, o, current, false, (String)null);
    }

    if (current == -1) {
      return null;
    }
    THHunt hunt = new THHunt(System.currentTimeMillis(), current, o.duration, wLocation, contents, oldblock);
    if (!stationary) {
      TreasureHunt.huntList.add(hunt);
      HashMap data = new HashMap();
      data.put("worldname", worldtouse.getName());
      data.put("value", Integer.toString(current));
      data.put("rarity", hunt.getRarityString());
      data.put("timeleft", o.duration + " minutes");
      data.put("numhunts", Integer.toString(TreasureHunt.getHuntsInWorld(worldtouse.getName()).size()));
      data.put("location", hunt.getLocString());
      if (TreasureHunt.spawnedchest.length() > 0) {
        TreasureHunt.broadcast(TreasureHunt.colorize(TreasureHunt.convertTags(TreasureHunt.spawnedchest, data)));
      }

      if (TreasureHunt.detaillogs)
        System.out.println("[THDetails] Hunt started in world " + worldtouse.getName() + " at " + hunt.getLocString() + " - Value: " + hunt.getValue());
    }
    else if (TreasureHunt.detaillogs) {
      System.out.println("[THDetails] Stationary chest respawned (in " + worldtouse.getName() + ")!  Loc: " + hunt.getLocString() + " - Val: " + hunt.getValue());
    }

    return hunt;
  }
}