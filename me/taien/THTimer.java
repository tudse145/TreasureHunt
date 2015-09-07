package me.taien;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;

public class THTimer
  implements Runnable
{
  public void run()
  {
    if (TreasureHunt.worlds.size() != 0) {
      long hunts = System.currentTimeMillis();
      Iterator iterator = TreasureHunt.worlds.entrySet().iterator();

      while (iterator.hasNext()) {
        Map.Entry e = (Map.Entry)iterator.next();
        String wn = (String)e.getKey();
        THWorldOpts o = (THWorldOpts)e.getValue();
        if (o.enabled) {
          if (o.searchingloc) {
            World world = Bukkit.getServer().getWorld(wn);
            Block block = THChestGenerator.findLocation(world, o, TreasureHunt.maxattemptspertick);
            o.searchingattempts += TreasureHunt.maxattemptspertick;
            if (block != null) {
              if (TreasureHunt.detaillogs) {
                System.out.println("[THDetails] Location selection took about " + o.searchingattempts + " runs.");
              }

              THChestGenerator.startHunt(world, o.searchingvalue, block, false, (String)null);
              o.searchingloc = false;
              o.searchingattempts = 0;
              o.searchingvalue = 0;
            } else if ((block == null) && (o.searchingattempts >= TreasureHunt.maxspawnattempts)) {
              System.out.println("[TreasureHunt] Failed to find suitable location in world '" + wn + "' after " + TreasureHunt.maxspawnattempts + " runs.");
              o.searchingloc = false;
              o.searchingattempts = 0;
              o.searchingvalue = 0;
            }
          } else if (hunts >= o.lastcheck + o.interval * 1000) {
            if ((TreasureHunt.getHuntsInWorld(wn).size() < o.minchests) && ((o.overrideminplayers) || (TreasureHunt.server.getOnlinePlayers().length >= TreasureHunt.minplayers))) {
              o.searchingloc = true;
              o.searchingvalue = THChestGenerator.findValue(o);
              o.searchingattempts = 0;
            }

            if (o.chance < 1)
              System.out.println("[TreasureHunt] Settings for world '" + (String)e.getKey() + "' are incorrect:  ChestChance cannot be less than 1.");
            else if ((o.chance == 1) || (TreasureHunt.rndGen.nextInt(o.chance) == 0)) {
              if (TreasureHunt.server.getOnlinePlayers().length >= TreasureHunt.minplayers) {
                o.searchingloc = true;
                o.searchingvalue = THChestGenerator.findValue(o);
                o.searchingattempts = 0;
              } else if (TreasureHunt.detaillogs) {
                System.out.println("[THDetails] Chest would have spawned, but insufficient players online.");
              }
            }

            Iterator target = TreasureHunt.stationaryList.iterator();

            while (target.hasNext()) {
              THStationaryChest c = (THStationaryChest)target.next();
              c.tick(hunts);
            }

            o.lastcheck = hunts;
          }
        }
      }

      THHunt[] athhunt = new THHunt[TreasureHunt.huntList.size()];
      athhunt = (THHunt[])TreasureHunt.huntList.toArray(athhunt);
      THHunt[] athhunt1 = athhunt;
      int i = athhunt.length;

      for (int j = 0; j < i; j++) {
        THHunt h = athhunt1[j];
        if (h.isExpired())
          if ((h.isLocked()) && (!((THWorldOpts)TreasureHunt.worlds.get(h.getWorld())).fadefoundchests)) {
            TreasureHunt.huntList.remove(h);
          } else {
            h.removeChest(false);
            TreasureHunt.huntList.remove(h);
          }
      }
    }
  }
}