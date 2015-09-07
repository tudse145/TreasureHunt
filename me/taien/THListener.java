package me.taien;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;

public class THListener
  implements Listener
{
  public THListener(TreasureHunt plugin)
  {
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player p = event.getPlayer();
    if ((TreasureHunt.useperms) && ((TreasureHunt.permission.has(p, "taien.th.nodetect." + p.getWorld().getName())) || (TreasureHunt.permission.has(p, "taien.th.nodetect.*"))))
      TreasureHunt.nodetectlist.add(p);
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event)
  {
    Player p = event.getPlayer();
    if (TreasureHunt.nodetectlist.contains(p))
      TreasureHunt.nodetectlist.remove(p);
  }

  @EventHandler
  public void onBlockBreak(BlockBreakEvent event)
  {
    if (TreasureHunt.protectbreak) {
      Block eb = event.getBlock();
      Iterator iterator = TreasureHunt.huntList.iterator();

      while (iterator.hasNext()) {
        THHunt b = (THHunt)iterator.next();
        if (b.isChestBlock(eb)) {
          event.getPlayer().sendMessage(ChatColor.DARK_RED + "You can't break treasure chests or the blocks under them!");
          event.setCancelled(true);
          return;
        }
      }

      iterator = TreasureHunt.stationaryList.iterator();

      while (iterator.hasNext()) {
        THStationaryChest b1 = (THStationaryChest)iterator.next();
        if ((b1.hunt != null) && (b1.hunt.isChestBlock(eb))) {
          event.getPlayer().sendMessage(ChatColor.DARK_RED + "You can't break treasure chests or the blocks under them!");
          event.setCancelled(true);
          return;
        }
      }

      iterator = TreasureHunt.compassblocks.iterator();

      while (iterator.hasNext()) {
        Block b2 = (Block)iterator.next();
        if (b2.equals(eb)) {
          event.getPlayer().sendMessage(ChatColor.DARK_RED + "You can't break compass blocks!");
          event.setCancelled(true);
          return;
        }
      }
    }
  }

  @EventHandler
  public void onBlockBurn(BlockBurnEvent e)
  {
    if (TreasureHunt.protectburn) {
      Block b = e.getBlock();
      Iterator iterator = TreasureHunt.huntList.iterator();

      while (iterator.hasNext()) {
        THHunt bb = (THHunt)iterator.next();
        if (bb.isChestBlock(b)) {
          e.setCancelled(true);
          return;
        }
      }

      iterator = TreasureHunt.stationaryList.iterator();

      while (iterator.hasNext()) {
        THStationaryChest bb1 = (THStationaryChest)iterator.next();
        if ((bb1.hunt != null) && (bb1.hunt.isChestBlock(b))) {
          e.setCancelled(true);
          return;
        }
      }

      iterator = TreasureHunt.compassblocks.iterator();

      while (iterator.hasNext()) {
        Block bb2 = (Block)iterator.next();
        if (b.equals(bb2)) {
          e.setCancelled(true);
          return;
        }
      }
    }
  }

  @EventHandler
  public void onEntityExplodeEvent(EntityExplodeEvent e)
  {
    if (TreasureHunt.protectexplode) {
      Iterator iterator = TreasureHunt.huntList.iterator();

      while (iterator.hasNext()) {
        THHunt bb = (THHunt)iterator.next();
        Iterator iterator1 = e.blockList().iterator();

        while (iterator1.hasNext()) {
          Block b = (Block)iterator1.next();
          if (bb.isChestBlock(b)) {
            e.setCancelled(true);
            return;
          }
        }
      }

      iterator = TreasureHunt.stationaryList.iterator();

      while (iterator.hasNext()) {
        THStationaryChest bb1 = (THStationaryChest)iterator.next();
        Iterator iterator1 = e.blockList().iterator();

        while (iterator1.hasNext()) {
          Block b = (Block)iterator1.next();
          if ((bb1.hunt != null) && (bb1.hunt.isChestBlock(b))) {
            e.setCancelled(true);
            return;
          }
        }
      }

      iterator = TreasureHunt.compassblocks.iterator();

      while (iterator.hasNext()) {
        Block bb2 = (Block)iterator.next();
        if (e.blockList().contains(bb2)) {
          e.setCancelled(true);
          return;
        }
      }
    }
  }

  @EventHandler
  public void onBlockPistonExtendEvent(BlockPistonExtendEvent e)
  {
    if (TreasureHunt.protectpiston) {
      Iterator iterator = TreasureHunt.huntList.iterator();

      while (iterator.hasNext()) {
        THHunt bb = (THHunt)iterator.next();
        Iterator iterator1 = e.getBlocks().iterator();

        while (iterator1.hasNext()) {
          Block b = (Block)iterator1.next();
          if (bb.isChestBlock(b)) {
            e.setCancelled(true);
            return;
          }
        }
      }

      iterator = TreasureHunt.stationaryList.iterator();

      while (iterator.hasNext()) {
        THStationaryChest bb1 = (THStationaryChest)iterator.next();
        Iterator iterator1 = e.getBlocks().iterator();

        while (iterator1.hasNext()) {
          Block b = (Block)iterator1.next();
          if ((bb1.hunt != null) && (bb1.hunt.isChestBlock(b))) {
            e.setCancelled(true);
            return;
          }
        }
      }

      iterator = TreasureHunt.compassblocks.iterator();

      while (iterator.hasNext()) {
        Block bb2 = (Block)iterator.next();
        if (e.getBlocks().contains(bb2)) {
          e.setCancelled(true);
          return;
        }
      }
    }
  }

  @EventHandler
  public void onBlockPistonRetractEvent(BlockPistonRetractEvent e)
  {
    if (TreasureHunt.protectpiston) {
      Block b = e.getBlock();
      Iterator iterator = TreasureHunt.huntList.iterator();

      while (iterator.hasNext()) {
        THHunt bb = (THHunt)iterator.next();
        if (bb.isChestBlock(b)) {
          e.setCancelled(true);
          return;
        }
      }

      iterator = TreasureHunt.stationaryList.iterator();

      while (iterator.hasNext()) {
        THStationaryChest bb1 = (THStationaryChest)iterator.next();
        if ((bb1.hunt != null) && (bb1.hunt.isChestBlock(b))) {
          e.setCancelled(true);
          return;
        }
      }

      iterator = TreasureHunt.compassblocks.iterator();

      while (iterator.hasNext()) {
        Block bb2 = (Block)iterator.next();
        if (b.equals(bb2)) {
          e.setCancelled(true);
          return;
        }
      }
    }
  }

  @EventHandler
  public void onBlockPlace(BlockPlaceEvent event)
  {
    Block eb = event.getBlock();
    Player p = event.getPlayer();
    if ((TreasureHunt.selections.containsKey(p)) && (event.getBlockPlaced().getType() == Material.OBSIDIAN)) {
      THToolSettings ts = (THToolSettings)TreasureHunt.selections.get(p);
      THStationaryChest removal = null;
      Iterator iterator = TreasureHunt.stationaryList.iterator();

      while (iterator.hasNext()) {
        THStationaryChest c = (THStationaryChest)iterator.next();
        if (c.chest.equals(eb)) {
          if (c.hunt != null) {
            c.removeChest();
          }

          removal = c;
          p.sendMessage(ChatColor.DARK_PURPLE + "Stationary chest location removed.");
          break;
        }
      }

      if (removal != null) {
        TreasureHunt.stationaryList.remove(removal);
        event.setCancelled(true);
      } else {
        TreasureHunt.stationaryList.add(new THStationaryChest(ts.itemlist, ts.minminutes, ts.maxminutes, ts.value, eb));
        p.sendMessage(ChatColor.DARK_PURPLE + "Created stationary chest at Loc: " + eb.getX() + "," + eb.getY() + "," + eb.getZ() + " Val: " + ts.value + " MinMinutes: " + ts.minminutes + " MaxMinutes: " + ts.maxminutes);
        event.setCancelled(true);
      }
    }
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    Player p = event.getPlayer();
    THWorldOpts o = null;
    if (TreasureHunt.worlds.containsKey(p.getWorld().getName())) {
      o = (THWorldOpts)TreasureHunt.worlds.get(p.getWorld().getName());
      if ((event.getAction() == Action.RIGHT_CLICK_BLOCK) && (event.getClickedBlock().getType() == Material.CHEST)) {
        THHunt thhunt = TreasureHunt.getCurrentHunt(event.getClickedBlock().getLocation());
        if (thhunt != null) {
          if (thhunt.isLocked()) {
            if (p != thhunt.getPlayerFound()) {
              HashMap hashmap = new HashMap();
              hashmap.put("rarity", thhunt.getRarityString());
              hashmap.put("value", Integer.toString(thhunt.getValue()));
              hashmap.put("location", thhunt.getLocString());
              hashmap.put("worldname", p.getWorld().getName());
              hashmap.put("numhunts", Integer.toString(TreasureHunt.getHuntsInWorld(p.getWorld().getName()).size()));
              hashmap.put("timeleft", thhunt.getMinutesLeft() + " minutes");
              hashmap.put("pname", thhunt.getPlayerFound().getName());
              p.sendMessage(TreasureHunt.colorize(TreasureHunt.convertTags(TreasureHunt.alreadyclaimed, hashmap)));
              event.setCancelled(true);
            }
          } else if ((TreasureHunt.useperms) && ((!TreasureHunt.useperms) || ((!TreasureHunt.permission.has(p, "taien.th.claim." + p.getWorld().getName())) && (!TreasureHunt.permission.has(p, "taien.th.claim.*"))))) {
            p.sendMessage(ChatColor.DARK_RED + "You are not allowed to claim chests!");
            event.setCancelled(true);
          } else {
            thhunt.chestFoundBy(p, false);
          }
        } else {
          THStationaryChest thstationarychest = TreasureHunt.getStationaryChest(event.getClickedBlock());
          if ((thstationarychest != null) && (thstationarychest.hunt != null))
            if (thstationarychest.hunt.isLocked()) {
              if (p != thstationarychest.hunt.getPlayerFound()) {
                HashMap hashmap1 = new HashMap();
                hashmap1.put("rarity", thstationarychest.hunt.getRarityString());
                hashmap1.put("value", Integer.toString(thstationarychest.hunt.getValue()));
                hashmap1.put("location", thstationarychest.hunt.getLocString());
                hashmap1.put("worldname", p.getWorld().getName());
                hashmap1.put("numhunts", Integer.toString(TreasureHunt.getHuntsInWorld(p.getWorld().getName()).size()));
                hashmap1.put("timeleft", thstationarychest.hunt.getMinutesLeft() + " minutes");
                hashmap1.put("pname", thstationarychest.hunt.getPlayerFound().getName());
                p.sendMessage(TreasureHunt.colorize(TreasureHunt.convertTags(TreasureHunt.alreadyclaimed, hashmap1)));
                event.setCancelled(true);
              }
            } else if ((TreasureHunt.useperms) && ((!TreasureHunt.useperms) || ((!TreasureHunt.permission.has(p, "taien.th.claimstat." + p.getWorld().getName())) && (!TreasureHunt.permission.has(p, "taien.th.claimstat.*"))))) {
              p.sendMessage(ChatColor.DARK_RED + "You are not allowed to claim stationary chests!");
              event.setCancelled(true);
            } else {
              thstationarychest.hunt.chestFoundBy(p, true);
            }
        }
      }
      else if (((event.getAction() == Action.LEFT_CLICK_AIR) || (event.getAction() == Action.LEFT_CLICK_BLOCK)) && (event.hasItem()) && (event.getItem().getType() == o.hunttool)) {
        if ((!TreasureHunt.useperms) || ((TreasureHunt.useperms) && ((TreasureHunt.permission.has(p, "taien.th.tool." + p.getWorld().getName())) || (TreasureHunt.permission.has(p, "taien.th.tool.*")))))
          if ((TreasureHunt.lastcheck.containsKey(p)) && (((Long)TreasureHunt.lastcheck.get(p)).longValue() >= System.currentTimeMillis() - 1000 * TreasureHunt.checksec)) {
            p.sendMessage(ChatColor.DARK_RED + "You can only check for the closest chest once every " + TreasureHunt.checksec + " seconds.");
          } else {
            int i = o.consumechance;
            if ((i > 0) && ((i >= 100) || (TreasureHunt.rndGen.nextInt(100) < i)) && ((!TreasureHunt.useperms) || ((!TreasureHunt.permission.has(p, "taien.th.noconsume." + p.getWorld().getName())) && (!TreasureHunt.permission.has(p, "taien.th.noconsume.*"))))) {
              ItemStack itemstack = null;
              ItemStack[] aitemstack;
              int j = (aitemstack = p.getInventory().getContents()).length;

              for (int k = 0; k < j; k++) {
                ItemStack itemstack1 = aitemstack[k];
                if ((itemstack1 != null) && (itemstack1.getType() == o.hunttool)) {
                  itemstack = itemstack1;
                }
              }

              if (itemstack != null) {
                if (itemstack.getAmount() == 1)
                  p.getInventory().clear(p.getInventory().first(itemstack));
                else {
                  p.getInventory().setItem(p.getInventory().first(itemstack), new ItemStack(o.hunttool, itemstack.getAmount() - 1));
                }
              }
            }

            TreasureHunt.getClosestHunt(p, true);
            TreasureHunt.lastcheck.put(p, Long.valueOf(System.currentTimeMillis()));
          }
      }
      else if ((event.getAction() == Action.LEFT_CLICK_BLOCK) && (TreasureHunt.usecompass) && (event.hasItem()) && (event.getItem().getType() == o.offeringtool)) {
        Block b = event.getClickedBlock();
        Iterator i = TreasureHunt.compassblocks.iterator();

        while (i.hasNext()) {
          Block cb = (Block)i.next();
          if ((b.getWorld().getName().equalsIgnoreCase(cb.getWorld().getName())) && (b.getX() == cb.getX()) && (b.getY() == cb.getY()) && (b.getZ() == cb.getZ())) {
            event.setCancelled(true);
            if ((TreasureHunt.useperms) && (!TreasureHunt.permission.has(p, "taien.th.compass." + p.getWorld().getName())) && (!TreasureHunt.permission.has(p, "taien.th.compass.*"))) {
              p.sendMessage(ChatColor.DARK_RED + "You aren't allowed to use compass blocks!");
              return;
            }

            if (TreasureHunt.getAmountInInventory(p.getInventory(), o.offeringtool, (short)0) < o.offeramount) {
              p.sendMessage(ChatColor.DARK_RED + "You don't have enough " + o.offeringtool.name() + " to make an offering!");
              return;
            }

            HashMap data = new HashMap();
            data.put("pname", p.getName());
            data.put("worldname", p.getWorld().getName());
            data.put("numhunts", Integer.toString(TreasureHunt.getHuntsInWorld(p.getWorld().getName()).size()));
            data.put("item", o.offeringtool.name());
            p.sendMessage(TreasureHunt.colorize(TreasureHunt.convertTags(TreasureHunt.offeritem, data)));
            TreasureHunt.takeItemFromPlayer(p.getInventory(), o.offeringtool, (short)0, o.offeramount);
            THHunt h = TreasureHunt.getClosestHunt(p, false);
            if (h == null) {
              p.sendMessage(TreasureHunt.colorize(TreasureHunt.convertTags(TreasureHunt.compassnochange, data))); break;
            }
            int distance = TreasureHunt.threedimensionaldistance ? h.get3DDistanceFrom(p.getLocation()) : h.getDistanceFrom(p.getLocation());
            if (distance > o.maxcompassdistance) {
              p.sendMessage(TreasureHunt.colorize(TreasureHunt.convertTags(TreasureHunt.compassnochange, data)));
            } else {
              data.put("distance", Integer.toString(distance));
              data.put("rarity", h.getRarityString());
              data.put("value", Integer.toString(h.getValue()));
              data.put("location", h.getLocString());
              data.put("timeleft", h.getMinutesLeft() + " minutes");
              p.setCompassTarget(h.getLocation());
              p.sendMessage(TreasureHunt.colorize(TreasureHunt.convertTags(TreasureHunt.compasschange, data)));
            }

            break;
          }
        }
      }
    }
  }
}