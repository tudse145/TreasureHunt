package me.taien;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import me.taien.config.Config;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

public class TreasureHunt extends JavaPlugin
{
  static TreasureHunt plugin;
  static String ptag;
  static String spawnedchest;
  static String playerclose;
  static String youareclosest;
  static String nolongerclosest;
  static String playerfound;
  static String moneyfound;
  static String foundchestfaded;
  static String unfoundchestfaded;
  static String alreadyclaimed;
  static String closestchest;
  static String nochests;
  static String offeritem;
  static String compasschange;
  static String compassnochange;
  static String directional;
  static String forwardtext;
  static String backwardtext;
  static String abovetext;
  static String belowtext;
  static String lefttext;
  static String righttext;
  private static THListener listener = null;
  private final THTimer timer;
  private int timerid;
  public static boolean useperms = false;
  public static WorldGuardPlugin worldguard = null;
  public static Economy economy = null;
  public static Permission permission = null;
  public static String version = "0.10.2";
  public static Server server = Bukkit.getServer();
  public static int checksec = 5;
  public static int maxspawnattempts = 1000;
  public static int minplayers = 4;
  public static int foundchestfadetime = 5;
  public static int maxattemptspertick = 20;
  public static int uncommonlevel = 1500;
  public static int rarelevel = 2500;
  public static int legendarylevel = 3500;
  public static int epiclevel = 4500;
  public static boolean usecompass = false;
  public static boolean threedimensionaldistance = false;
  public static boolean directionaltext = false;
  public static boolean detaillogs = false;
  public static boolean protectbreak = true;
  public static boolean protectburn = true;
  public static boolean protectpiston = true;
  public static boolean protectexplode = true;
  public static Random rndGen = new Random();
  public static Map worldlists = new HashMap();
  public static Map customlists = new HashMap();
  public static List compassblocks = new LinkedList();
  public static Map playerdata = new HashMap();
  public static List huntList = new LinkedList();
  public static List stationaryList = new LinkedList();
  public static List nodetectlist = new LinkedList();
  public static Map selections = new HashMap();
  public static Map lastcheck = new HashMap();
  public static Map worlds = new HashMap();
  public static Map enchanted = new HashMap();
  private DecimalFormat ratiopercent;

  public TreasureHunt()
  {
    this.timer = new THTimer();
    this.timerid = 0;

    this.ratiopercent = new DecimalFormat("#.###");
  }
  public void onDisable() {
    Iterator iterator = huntList.iterator();

    while (iterator.hasNext()) {
      THHunt c = (THHunt)iterator.next();
      c.removeChest(false);
    }

    iterator = stationaryList.iterator();

    while (iterator.hasNext()) {
      THStationaryChest c1 = (THStationaryChest)iterator.next();
      c1.removeChest();
    }

    huntList.clear();
    saveProcedure();
    server.getScheduler().cancelTask(this.timerid);
    System.out.println("[TreasureHunt] Deactivated.");
  }

  public void onEnable() {
    System.out.println("[TreasureHunt] Activating...");
    RegisteredServiceProvider pm;
    try {
      pm = server.getServicesManager().getRegistration(Economy.class);
      if (pm != null)
        economy = (Economy)pm.getProvider();
      else
        System.out.println("[TreasureHunt] Vault not found.  No money will be found in chests.");
    }
    catch (NoClassDefFoundError noclassdeffounderror) {
      System.out.println("[TreasureHunt] Vault not found.  No money will be found in chests.");
    }
    try
    {
      pm = server.getServicesManager().getRegistration(Permission.class);
      if (pm != null)
        permission = (Permission)pm.getProvider();
      else
        System.out.println("[TreasureHunt] Vault not found.  OP-only permissions will be used.");
    }
    catch (NoClassDefFoundError noclassdeffounderror1) {
      System.out.println("[TreasureHunt] Vault not found.  OP-only permissions will be used.");
    }

    if (permission != null) {
      useperms = true;
    }

    PluginManager pm1 = Bukkit.getServer().getPluginManager();
    worldguard = (WorldGuardPlugin)pm1.getPlugin("WorldGuard");
    if (worldguard != null) {
      System.out.println("[TreasureHunt] Hooked into WorldGuard.");
    }
    plugin = this;

    loadProcedure();
    listener = new THListener(this);
    this.timerid = server.getScheduler().scheduleSyncRepeatingTask(this, this.timer, 200L, 1L);
    System.out.println("[TreasureHunt] Activated.");
  }

  public void loadProcedure() {
    Config messages = new Config(this, "messages.yml");
    Config config = new Config(this, "config.yml");
    Config players = new Config(this, "players.yml");
    System.out.println("[TreasureHunt] Loading messages...");
    ptag = messages.getConfig().getString("Options.PluginTag", "&e[&6TreasureHunt&e]");
    spawnedchest = messages.getConfig().getString("Messages.SpawnedChest", "<tag> &fA treasure chest of <rarity> &frarity appeared in &9<worldname>&f!");
    playerclose = messages.getConfig().getString("Messages.PlayerCloseToChest", "<tag> &fA player is very close to the <rarity> &fchest!");
    youareclosest = messages.getConfig().getString("Messages.YouAreClosest", "<tag> &aYou are now the closest player to the <rarity> &achest!");
    nolongerclosest = messages.getConfig().getString("Messages.NoLongerClosest", "<tag> &cYou are no longer the closest player to the <rarity> &cchest!");
    playerfound = messages.getConfig().getString("Messages.PlayerFoundChest", "<tag> &fThe chest of value &a<value> &fhas been found by &2<pname> &fat &a<location>&f!");
    moneyfound = messages.getConfig().getString("Messages.MoneyFound", "<tag> &aYou found <amount> in the chest!");
    foundchestfaded = messages.getConfig().getString("Messages.FoundChestFaded", "");
    unfoundchestfaded = messages.getConfig().getString("Messages.UnfoundChestFaded", "<tag> &fThe chest of value &a<value> &fhas &cfaded &fwithout being found!");
    alreadyclaimed = messages.getConfig().getString("Messages.AlreadyClaimed", "&7This chest has already been claimed by &a<pname>&7!");
    closestchest = messages.getConfig().getString("Messages.ClosestChest", "&7The closest chest (of <numhunts>) is currently &9<distance> &7blocks away.");
    nochests = messages.getConfig().getString("Messages.NoChests", "&7No hunts are currently active in this world!");
    offeritem = messages.getConfig().getString("Messages.OfferItem", "&7You offer the altar a &9<item>&7...");
    compasschange = messages.getConfig().getString("Messages.CompassChange", "&7...and your compass needle starts pointing madly in a certain direction!");
    compassnochange = messages.getConfig().getString("Messages.CompassNoChange", "&7...but your compass needle doesn't change.");
    directional = messages.getConfig().getString("Messages.Directional", "&7The chest seems to be somewhere <direction>.");
    forwardtext = messages.getConfig().getString("Directions.Forward", "ahead of you");
    backwardtext = messages.getConfig().getString("Directions.Backward", "behind you");
    abovetext = messages.getConfig().getString("Directions.Above", "above you");
    belowtext = messages.getConfig().getString("Directions.Below", "below you");
    lefttext = messages.getConfig().getString("Directions.Left", "to your left");
    righttext = messages.getConfig().getString("Directions.Right", "to your right");
    System.out.println("[TreasureHunt] Loading configuration...");
    customlists.clear();
    worldlists.clear();
    compassblocks.clear();
    nodetectlist.clear();
    enchanted.clear();
    stationaryList.clear();
    playerdata.clear();
    List stationarystrings = config.getConfig().getStringList("StationaryChests");
    checksec = config.getConfig().getInt("Options.SecondsBetweenChecks", 5);
    maxspawnattempts = config.getConfig().getInt("Options.MaxSpawnAttempts", 1000);
    minplayers = config.getConfig().getInt("Options.MinPlayersOnline", 4);
    foundchestfadetime = config.getConfig().getInt("Options.FoundChestFadeTime", 5);
    maxattemptspertick = config.getConfig().getInt("Options.MaxAttemptsPerTick", 20);
    usecompass = config.getConfig().getBoolean("Options.UseCompass", false);
    threedimensionaldistance = config.getConfig().getBoolean("Options.3DDistances", false);
    directionaltext = config.getConfig().getBoolean("Options.DirectionalText", false);
    detaillogs = config.getConfig().getBoolean("Options.DetailLogs", false);
    protectbreak = config.getConfig().getBoolean("Options.Protection.Break", true);
    protectburn = config.getConfig().getBoolean("Options.Protection.Burn", true);
    protectexplode = config.getConfig().getBoolean("Options.Protection.Explode", true);
    protectpiston = config.getConfig().getBoolean("Options.Protection.Piston", true);
    uncommonlevel = config.getConfig().getInt("Options.ChestLevels.Uncommon", 1500);
    rarelevel = config.getConfig().getInt("Options.ChestLevels.Rare", 2500);
    legendarylevel = config.getConfig().getInt("Options.ChestLevels.Legendary", 3500);
    epiclevel = config.getConfig().getInt("Options.ChestLevels.Epic", 4500);
    new LinkedList();
    List incdata = config.getConfig().getStringList("CompassBlocks");
    int cb = 0;
    Iterator enchs = incdata.iterator();

    while (enchs.hasNext()) {
      String worldlist = (String)enchs.next();
      String[] enchantedlist = worldlist.split(":");
      if (enchantedlist.length < 4) {
        System.out.println("[TreasureHunt] Incorrect data value found in CompassBlocks(" + worldlist + "), ignoring...");
      } else {
        String worldstrings = enchantedlist[0];
        int listnum = Integer.parseInt(enchantedlist[1]);
        int totalitems = Integer.parseInt(enchantedlist[2]);
        int customstrings = Integer.parseInt(enchantedlist[3]);
        World stats = server.getWorld(worldstrings);
        Block pcount = null;
        if (stats != null) {
          pcount = stats.getBlockAt(listnum, totalitems, customstrings);
        }

        if (pcount != null) {
          cb++;
          compassblocks.add(pcount);
        } else {
          System.out.println("[TreasureHunt] Incorrect data value found in CompassBlocks(" + worldlist + "), ignoring...");
        }
      }
    }

    System.out.println("[TreasureHunt] Loaded " + cb + " CompassBlocks.");
    if (!config.getConfig().isConfigurationSection("WorldOptions")) {
      config.getConfig().createSection("WorldOptions");
    }

    Set set = config.getConfig().getConfigurationSection("WorldOptions").getKeys(false);

    int i = 0;

    if (set.size() == 0) {
      String s = ((World)server.getWorlds().get(0)).getName();
      worlds.put(s, new THWorldOpts());
      System.out.println("[TreasureHunt] No worlds found in WorldOptions.  Added world '" + s + "' by default.");
    } else {
      Iterator iterator = set.iterator();

      while (iterator.hasNext()) {
        String s = (String)iterator.next();
        String worldstrings = config.getConfig().getString("WorldOptions." + s + ".ItemList", "Default");
        int listnum = config.getConfig().getInt("WorldOptions." + s + ".ChestChance", 100);
        int totalitems = config.getConfig().getInt("WorldOptions." + s + ".ChestInterval", 60);
        int customstrings = config.getConfig().getInt("WorldOptions." + s + ".ChestDuration", 60);
        i = config.getConfig().getInt("WorldOptions." + s + ".MaxDistance", 3000);
        int j = config.getConfig().getInt("WorldOptions." + s + ".MinDistance", 0);
        int playerstrings = config.getConfig().getInt("WorldOptions." + s + ".MaxCompassDistance", 1000);
        int centerX = config.getConfig().getInt("WorldOptions." + s + ".CenterX", 0);
        int w = config.getConfig().getInt("WorldOptions." + s + ".CenterZ", 0);
        int split = config.getConfig().getInt("WorldOptions." + s + ".DrawWeight", 2);
        int split1 = config.getConfig().getInt("WorldOptions." + s + ".GoodItemWeight", 2);
        int itemdat = config.getConfig().getInt("WorldOptions." + s + ".AmountWeight", 3);
        int intval = config.getConfig().getInt("WorldOptions." + s + ".MaxValue", 5000);
        int epiclist = config.getConfig().getInt("WorldOptions." + s + ".MinLightLevel", 0);
        int epicitemlist = config.getConfig().getInt("WorldOptions." + s + ".MaxLightLevel", 4);
        int maxElevation = config.getConfig().getInt("WorldOptions." + s + ".MaxElevation", 50);
        int itemdat1 = config.getConfig().getInt("WorldOptions." + s + ".MaxElevationRare", 25);
        int split2 = config.getConfig().getInt("WorldOptions." + s + ".MinElevation", 4);
        int itemdat2 = config.getConfig().getInt("WorldOptions." + s + ".ConsumeChance", 50);
        int intval1 = config.getConfig().getInt("WorldOptions." + s + ".MinMoney", 100);
        int offeramount = config.getConfig().getInt("WorldOptions." + s + ".OfferAmount", 1);
        int minchests = config.getConfig().getInt("WorldOptions." + s + ".MinChests", 0);
        long lastcheck = config.getConfig().getLong("WorldOptions." + s + ".LastCheck", 0L);
        double moneymultiplier = config.getConfig().getDouble("WorldOptions." + s + ".MoneyMultiplier", 1.0D);
        boolean usemarker = config.getConfig().getBoolean("WorldOptions." + s + ".UseMarker", true);
        boolean enabled = config.getConfig().getBoolean("WorldOptions." + s + ".Enabled", true);
        boolean strictitems = config.getConfig().getBoolean("WorldOptions." + s + ".StrictItems", false);
        boolean fadefoundchests = config.getConfig().getBoolean("WorldOptions." + s + ".FadeFoundChests", true);
        boolean overrideminplayers = config.getConfig().getBoolean("WorldOptions." + s + ".OverrideMinPlayers", false);
        String s2 = config.getConfig().getString("WorldOptions." + s + ".HuntTool", "ROTTEN_FLESH");
        Material hunttool = Material.ROTTEN_FLESH;
        if ((Material.getMaterial(s2.toUpperCase()) == null) && (Material.getMaterial(Integer.parseInt(s2)) == null))
          System.out.println("'" + s2 + "' is not a valid item name or id. (HuntTool) Using default.");
        else if (Material.getMaterial(s2.toUpperCase()) != null)
          hunttool = Material.getMaterial(s2.toUpperCase());
        else {
          hunttool = Material.getMaterial(Integer.parseInt(s2));
        }

        s2 = config.getConfig().getString("WorldOptions." + s + ".OfferingTool", "BONE");
        Material offeringtool = Material.BONE;
        if ((Material.getMaterial(s2.toUpperCase()) == null) && (Material.getMaterial(Integer.parseInt(s2)) == null))
          System.out.println("'" + s2 + "' is not a valid item name or id. (HuntTool) Using default.");
        else if (Material.getMaterial(s2.toUpperCase()) != null)
          offeringtool = Material.getMaterial(s2.toUpperCase());
        else {
          offeringtool = Material.getMaterial(Integer.parseInt(s2));
        }

        s2 = config.getConfig().getString("WorldOptions." + s + ".MarkerBlock", "GLOWSTONE");
        Material markerblock = Material.GLOWSTONE;
        if ((Material.getMaterial(s2.toUpperCase()) == null) && (Material.getMaterial(Integer.parseInt(s2)) == null))
          System.out.println("'" + s2 + "' is not a valid item name or id. (MarkerBlock) Using default.");
        else if (Material.getMaterial(s2.toUpperCase()) != null)
          markerblock = Material.getMaterial(s2.toUpperCase());
        else {
          markerblock = Material.getMaterial(Integer.parseInt(s2));
        }

        s2 = config.getConfig().getString("WorldOptions." + s + ".FadeBlock", "SOUL_SAND");
        Material fadeblock = Material.SOUL_SAND;
        if ((Material.getMaterial(s2.toUpperCase()) == null) && (!s2.equalsIgnoreCase("RETURN")) && (Material.getMaterial(Integer.parseInt(s2)) == null))
          System.out.println("'" + s2 + "' is not a valid item name or id. (FadeBlock) Using default.");
        else if (Material.getMaterial(s2.toUpperCase()) != null)
          fadeblock = Material.getMaterial(s2.toUpperCase());
        else if (s2.equalsIgnoreCase("RETURN"))
          fadeblock = null;
        else {
          fadeblock = Material.getMaterial(Integer.parseInt(s2));
        }

        new LinkedList();
        List spawnblocks = config.getConfig().getStringList("WorldOptions." + s + ".CanSpawnOn");
        LinkedList spawnableblocks = new LinkedList();
        if (spawnblocks.size() == 0) {
          System.out.println("[TreasureHunt] No spawning blocks found for world " + s + ".  Using default.");
          spawnableblocks.add(Material.STONE);
          spawnableblocks.add(Material.SMOOTH_BRICK);
          spawnableblocks.add(Material.MOSSY_COBBLESTONE);
          spawnableblocks.add(Material.OBSIDIAN);
        } else {
          Iterator iterator1 = spawnblocks.iterator();

          while (iterator1.hasNext()) {
            String world = (String)iterator1.next();
            if ((Material.getMaterial(world.toUpperCase()) == null) && (Material.getMaterial(Integer.parseInt(world)) == null))
              System.out.println("'" + world + "' is not a valid item name or id. (SpawnableBlocks, World " + s + ")");
            else if (Material.getMaterial(world.toUpperCase()) != null)
              spawnableblocks.add(Material.getMaterial(world.toUpperCase()));
            else {
              spawnableblocks.add(Material.getMaterial(Integer.parseInt(world)));
            }
          }

          if (spawnableblocks.size() == 0) {
            System.out.println("[TreasureHunt] No usable spawning blocks found for world " + s + ".  Using default.");
            spawnableblocks.add(Material.STONE);
            spawnableblocks.add(Material.SMOOTH_BRICK);
            spawnableblocks.add(Material.MOSSY_COBBLESTONE);
            spawnableblocks.add(Material.OBSIDIAN);
          }
        }

        World world = server.getWorld(s);
        if (world != null) {
          worlds.put(s, new THWorldOpts(worldstrings, customstrings, totalitems, i, j, playerstrings, listnum, intval, epiclist, epicitemlist, maxElevation, itemdat1, split2, centerX, w, split, split1, itemdat, itemdat2, intval1, offeramount, minchests, lastcheck, moneymultiplier, usemarker, enabled, strictitems, fadefoundchests, overrideminplayers, markerblock, hunttool, offeringtool, fadeblock, spawnableblocks));
          System.out.println("[TreasureHunt] Loaded world '" + s + "'");
        } else {
          System.out.println("[TreasureHunt] Failed to load world '" + s + "' - world does not appear to exist");
        }
      }
    }

    System.out.println("[TreasureHunt] Settings for " + worlds.size() + " worlds loaded.");
    int k = 0;
    if (!config.getConfig().isConfigurationSection("EnchantedItems")) {
      config.getConfig().createSection("EnchantedItems");
    }

    Set set1 = config.getConfig().getConfigurationSection("EnchantedItems").getKeys(false);
    Iterator iterator2 = set1.iterator();

    while (iterator2.hasNext()) {
      String worldstrings = (String)iterator2.next();
      int totalitems = config.getConfig().getInt("EnchantedItems." + worldstrings + ".ID", 307);
      short short0 = (short)config.getConfig().getInt("EnchantedItems." + worldstrings + ".Damage", 0);
      ItemStack itemstack = new ItemStack(totalitems, 1, short0);
      if (!config.getConfig().isConfigurationSection("EnchantedItems." + worldstrings + ".Effects")) {
        config.getConfig().createSection("EnchantedItems." + worldstrings + ".Effects");
      }

      Iterator iterator3 = config.getConfig().getConfigurationSection("EnchantedItems." + worldstrings + ".Effects").getKeys(false).iterator();

      while (iterator3.hasNext()) {
        String s1 = (String)iterator3.next();
        Enchantment enchantment = Enchantment.getByName(s1);
        if (enchantment != null) {
          if (!enchantment.canEnchantItem(itemstack)) {
            System.out.println("[TreasureHunt] Cannot enchant " + itemstack.getType().name() + " with " + enchantment.getName() + "! Enchantment dropped.");
          } else {
            boolean flag = false;
            Iterator iterator4 = itemstack.getEnchantments().keySet().iterator();

            while (iterator4.hasNext()) {
              Enchantment enchantment1 = (Enchantment)iterator4.next();
              if (enchantment.conflictsWith(enchantment1)) {
                System.out.println("[TreasureHunt] Enchant " + enchantment.getName() + " conflicts with " + enchantment1.getName() + " on " + itemstack.getType().name() + "! Enchantment dropped.");
                flag = true;
                break;
              }
            }

            if (!flag) {
              int split = enchantment.getMaxLevel() < config.getConfig().getInt("EnchantedItems." + worldstrings + ".Effects." + s1, 1) ? enchantment.getMaxLevel() : config.getConfig().getInt("EnchantedItems." + worldstrings + ".Effects." + s1, 1);
              itemstack.addEnchantment(enchantment, split);
            }
          }
        }
      }

      if (itemstack.getEnchantments().size() < 1) {
        System.out.println("[TreasureHunt] No valid enchants found for " + itemstack.getType().name() + "! Item dropped from enchanted list.");
      } else {
        enchanted.put(worldstrings, itemstack);
        k++;
      }
    }

    System.out.println("[TreasureHunt] Loaded " + k + " Enchanted Item Setups.");
    if (!config.getConfig().isConfigurationSection("WorldLists")) {
      config.getConfig().createSection("WorldLists");
    }

    Set set2 = config.getConfig().getConfigurationSection("WorldLists").getKeys(false);
    int listnum = 0;
    int totalitems = 0;

    for (Iterator iterator6 = set2.iterator(); iterator6.hasNext(); i = 0) {
      String s3 = (String)iterator6.next();
      listnum++;
      if (!config.getConfig().isConfigurationSection("WorldLists." + s3 + ".Common")) {
        config.getConfig().createSection("WorldLists." + s3 + ".Common");
      }

      Set set3 = config.getConfig().getConfigurationSection("WorldLists." + s3 + ".Common").getKeys(false);
      HashMap hashmap = new HashMap();
      Iterator iterator5 = set3.iterator();

      while (iterator5.hasNext()) {
        String s2 = (String)iterator5.next();
        String[] astring = s2.split(":");
        if (astring.length < 2) {
          int split1 = 0;
          try
          {
            split1 = Integer.parseInt(s2);
          }
          catch (NumberFormatException numberformatexception)
          {
          }
          if ((Material.getMaterial(s2.toUpperCase()) == null) && (!enchanted.containsKey(s2)) && ((split1 == 0) || (Material.getMaterial(split1) == null))) {
            System.out.println("'" + s2 + "' is not a valid item name or id. (List '" + s3 + "' Commons)");
          } else {
            if (Material.getMaterial(s2.toUpperCase()) != null)
              hashmap.put(new ItemStack(Material.getMaterial(s2.toUpperCase())), Integer.valueOf(config.getConfig().getInt("WorldLists." + s3 + ".Common." + s2, 1)));
            else if (enchanted.containsKey(s2))
              hashmap.put((ItemStack)enchanted.get(s2), Integer.valueOf(config.getConfig().getInt("WorldLists." + s3 + ".Common." + s2, 1)));
            else {
              hashmap.put(new ItemStack(Material.getMaterial(split1)), Integer.valueOf(config.getConfig().getInt("WorldLists." + s3 + ".Common." + s2, 1)));
            }

            i++;
          }
        } else {
          short short1 = Short.parseShort(astring[1]);
          int itemdat = 0;
          try
          {
            itemdat = Integer.parseInt(astring[0]);
          }
          catch (NumberFormatException numberformatexception1)
          {
          }
          if ((Material.getMaterial(s2.toUpperCase()) == null) && ((itemdat == 0) || (Material.getMaterial(itemdat) == null))) {
            System.out.println("'" + astring[0] + "' is not a valid item name or id. (List '" + s3 + "' Commons)");
          } else {
            if (Material.getMaterial(astring[0].toUpperCase()) != null)
              hashmap.put(new ItemStack(Material.getMaterial(astring[0].toUpperCase()), 1, short1), Integer.valueOf(config.getConfig().getInt("WorldLists." + s3 + ".Common." + s2, 1)));
            else {
              hashmap.put(new ItemStack(Material.getMaterial(itemdat), 1, short1), Integer.valueOf(config.getConfig().getInt("WorldLists." + s3 + ".Common." + s2, 1)));
            }

            i++;
          }
        }
      }

      if (!config.getConfig().isConfigurationSection("WorldLists." + s3 + ".Uncommon")) {
        config.getConfig().createSection("WorldLists." + s3 + ".Uncommon");
      }

      Set set4 = config.getConfig().getConfigurationSection("WorldLists." + s3 + ".Uncommon").getKeys(false);
      HashMap hashmap1 = new HashMap();
      Iterator iterator4 = set4.iterator();

      while (iterator4.hasNext()) {
        String s4 = (String)iterator4.next();
        String[] astring1 = s4.split(":");
        if (astring1.length < 2) {
          int intval = 0;
          try
          {
            intval = Integer.parseInt(s4);
          }
          catch (NumberFormatException numberformatexception2)
          {
          }
          if ((Material.getMaterial(s4.toUpperCase()) == null) && (!enchanted.containsKey(s4)) && ((intval == 0) || (Material.getMaterial(intval) == null))) {
            System.out.println("'" + s4 + "' is not a valid item name or id. (List '" + s3 + "' Uncommons)");
          } else {
            if (Material.getMaterial(s4.toUpperCase()) != null)
              hashmap1.put(new ItemStack(Material.getMaterial(s4.toUpperCase())), Integer.valueOf(config.getConfig().getInt("WorldLists." + s3 + ".Uncommon." + s4, 1)));
            else if (enchanted.containsKey(s4))
              hashmap1.put((ItemStack)enchanted.get(s4), Integer.valueOf(config.getConfig().getInt("WorldLists." + s3 + ".Uncommon." + s4, 1)));
            else {
              hashmap1.put(new ItemStack(Material.getMaterial(intval)), Integer.valueOf(config.getConfig().getInt("WorldLists." + s3 + ".Uncommon." + s4, 1)));
            }

            i++;
          }
        } else {
          short short2 = Short.parseShort(astring1[1]);
          int epiclist = 0;
          try
          {
            epiclist = Integer.parseInt(astring1[0]);
          }
          catch (NumberFormatException numberformatexception3)
          {
          }
          if ((Material.getMaterial(s4.toUpperCase()) == null) && ((epiclist == 0) || (Material.getMaterial(epiclist) == null))) {
            System.out.println("'" + astring1[0] + "' is not a valid item name or id. (List '" + s3 + "' Uncommons)");
          } else {
            if (Material.getMaterial(astring1[0].toUpperCase()) != null)
              hashmap1.put(new ItemStack(Material.getMaterial(astring1[0].toUpperCase()), 1, short2), Integer.valueOf(config.getConfig().getInt("WorldLists." + s3 + ".Uncommon." + s4, 1)));
            else {
              hashmap1.put(new ItemStack(Material.getMaterial(epiclist), 1, short2), Integer.valueOf(config.getConfig().getInt("WorldLists." + s3 + ".Uncommon." + s4, 1)));
            }

            i++;
          }
        }
      }

      if (!config.getConfig().isConfigurationSection("WorldLists." + s3 + ".Rare")) {
        config.getConfig().createSection("WorldLists." + s3 + ".Rare");
      }

      Set set5 = config.getConfig().getConfigurationSection("WorldLists." + s3 + ".Rare").getKeys(false);
      HashMap hashmap2 = new HashMap();
      Iterator iterator7 = set5.iterator();

      while (iterator7.hasNext()) {
        String s5 = (String)iterator7.next();
        String[] astring2 = s5.split(":");
        if (astring2.length < 2) {
          int epicitemlist = 0;
          try
          {
            epicitemlist = Integer.parseInt(s5);
          }
          catch (NumberFormatException numberformatexception4)
          {
          }
          if ((Material.getMaterial(s5.toUpperCase()) == null) && (!enchanted.containsKey(s5)) && ((epicitemlist == 0) || (Material.getMaterial(epicitemlist) == null))) {
            System.out.println("'" + s5 + "' is not a valid item name or id. (List '" + s3 + "' Rares)");
          } else {
            if (Material.getMaterial(s5.toUpperCase()) != null)
              hashmap2.put(new ItemStack(Material.getMaterial(s5.toUpperCase())), Integer.valueOf(config.getConfig().getInt("WorldLists." + s3 + ".Rare." + s5, 1)));
            else if (enchanted.containsKey(s5))
              hashmap2.put((ItemStack)enchanted.get(s5), Integer.valueOf(config.getConfig().getInt("WorldLists." + s3 + ".Rare." + s5, 1)));
            else {
              hashmap2.put(new ItemStack(Material.getMaterial(epicitemlist)), Integer.valueOf(config.getConfig().getInt("WorldLists." + s3 + ".Rare." + s5, 1)));
            }

            i++;
          }
        } else {
          short short3 = Short.parseShort(astring2[1]);
          int s11 = 0;
          try
          {
            s11 = Integer.parseInt(astring2[0]);
          }
          catch (NumberFormatException numberformatexception5)
          {
          }
          if ((Material.getMaterial(s5.toUpperCase()) == null) && ((s11 == 0) || (Material.getMaterial(s11) == null))) {
            System.out.println("'" + astring2[0] + "' is not a valid item name or id. (List '" + s3 + "' Rares)");
          } else {
            if (Material.getMaterial(astring2[0].toUpperCase()) != null)
              hashmap2.put(new ItemStack(Material.getMaterial(astring2[0].toUpperCase()), 1, short3), Integer.valueOf(config.getConfig().getInt("WorldLists." + s3 + ".Rare." + s5, 1)));
            else {
              hashmap2.put(new ItemStack(Material.getMaterial(s11), 1, short3), Integer.valueOf(config.getConfig().getInt("WorldLists." + s3 + ".Rare." + s5, 1)));
            }

            i++;
          }
        }
      }

      if (!config.getConfig().isConfigurationSection("WorldLists." + s3 + ".Legendary")) {
        config.getConfig().createSection("WorldLists." + s3 + ".Legendary");
      }

      Set set6 = config.getConfig().getConfigurationSection("WorldLists." + s3 + ".Legendary").getKeys(false);
      HashMap hashmap3 = new HashMap();
      Iterator iterator8 = set6.iterator();

      while (iterator8.hasNext()) {
        String s6 = (String)iterator8.next();
        String[] astring3 = s6.split(":");
        if (astring3.length < 2) {
          int itemdat1 = 0;
          try
          {
            itemdat1 = Integer.parseInt(s6);
          }
          catch (NumberFormatException numberformatexception6)
          {
          }
          if ((Material.getMaterial(s6.toUpperCase()) == null) && (!enchanted.containsKey(s6)) && ((itemdat1 == 0) || (Material.getMaterial(itemdat1) == null))) {
            System.out.println("'" + s6 + "' is not a valid item name or id. (List '" + s3 + "' Legendaries)");
          } else {
            if (Material.getMaterial(s6.toUpperCase()) != null)
              hashmap3.put(new ItemStack(Material.getMaterial(s6.toUpperCase())), Integer.valueOf(config.getConfig().getInt("WorldLists." + s3 + ".Legendary." + s6, 1)));
            else if (enchanted.containsKey(s6))
              hashmap3.put((ItemStack)enchanted.get(s6), Integer.valueOf(config.getConfig().getInt("WorldLists." + s3 + ".Legendary." + s6, 1)));
            else {
              hashmap3.put(new ItemStack(Material.getMaterial(itemdat1)), Integer.valueOf(config.getConfig().getInt("WorldLists." + s3 + ".Legendary." + s6, 1)));
            }

            i++;
          }
        } else {
          short short4 = Short.parseShort(astring3[1]);
          int split2 = 0;
          try
          {
            split2 = Integer.parseInt(astring3[0]);
          }
          catch (NumberFormatException numberformatexception7)
          {
          }
          if ((Material.getMaterial(s6.toUpperCase()) == null) && ((split2 == 0) || (Material.getMaterial(split2) == null))) {
            System.out.println("'" + astring3[0] + "' is not a valid item name or id. (List '" + s3 + "' Legendaries)");
          } else {
            if (Material.getMaterial(astring3[0].toUpperCase()) != null)
              hashmap3.put(new ItemStack(Material.getMaterial(astring3[0].toUpperCase()), 1, short4), Integer.valueOf(config.getConfig().getInt("WorldLists." + s3 + ".Legendary." + s6, 1)));
            else {
              hashmap3.put(new ItemStack(Material.getMaterial(split2), 1, short4), Integer.valueOf(config.getConfig().getInt("WorldLists." + s3 + ".Legendary." + s6, 1)));
            }

            i++;
          }
        }
      }

      if (!config.getConfig().isConfigurationSection("WorldLists." + s3 + ".Epic")) {
        config.getConfig().createSection("WorldLists." + s3 + ".Epic");
      }

      Set set7 = config.getConfig().getConfigurationSection("WorldLists." + s3 + ".Epic").getKeys(false);
      HashMap hashmap4 = new HashMap();
      Iterator iterator9 = set7.iterator();

      while (iterator9.hasNext()) {
        String s7 = (String)iterator9.next();
        String[] astring4 = s7.split(":");
        if (astring4.length < 2) {
          int itemdat2 = 0;
          try
          {
            itemdat2 = Integer.parseInt(s7);
          }
          catch (NumberFormatException numberformatexception8)
          {
          }
          if ((Material.getMaterial(s7.toUpperCase()) == null) && (!enchanted.containsKey(s7)) && ((itemdat2 == 0) || (Material.getMaterial(itemdat2) == null))) {
            System.out.println("'" + s7 + "' is not a valid item name or id. (List '" + s3 + "' Epics)");
          } else {
            if (Material.getMaterial(s7.toUpperCase()) != null)
              hashmap4.put(new ItemStack(Material.getMaterial(s7.toUpperCase())), Integer.valueOf(config.getConfig().getInt("WorldLists." + s3 + ".Epic." + s7, 1)));
            else if (enchanted.containsKey(s7))
              hashmap4.put((ItemStack)enchanted.get(s7), Integer.valueOf(config.getConfig().getInt("WorldLists." + s3 + ".Epic." + s7, 1)));
            else {
              hashmap4.put(new ItemStack(Material.getMaterial(itemdat2)), Integer.valueOf(config.getConfig().getInt("WorldLists." + s3 + ".Epic." + s7, 1)));
            }

            i++;
          }
        } else {
          short short5 = Short.parseShort(astring4[1]);
          int intval1 = 0;
          try
          {
            intval1 = Integer.parseInt(astring4[0]);
          }
          catch (NumberFormatException numberformatexception9)
          {
          }
          if ((Material.getMaterial(s7.toUpperCase()) == null) && ((intval1 == 0) || (Material.getMaterial(intval1) == null))) {
            System.out.println("'" + s7 + "' is not a valid item name or id. (List '" + s3 + "' Epics)");
          } else {
            if (Material.getMaterial(astring4[0].toUpperCase()) != null)
              hashmap4.put(new ItemStack(Material.getMaterial(astring4[0].toUpperCase()), 1, short5), Integer.valueOf(config.getConfig().getInt("WorldLists." + s3 + ".Epic." + s7, 1)));
            else {
              hashmap4.put(new ItemStack(Material.getMaterial(intval1), 1, short5), Integer.valueOf(config.getConfig().getInt("WorldLists." + s3 + ".Epic." + s7, 1)));
            }

            i++;
          }
        }
      }

      System.out.println("[TreasureHunt] Loaded " + i + " items successfully from list '" + s3 + "'.");
      if ((hashmap.size() != 0) && (hashmap1.size() != 0) && (hashmap2.size() != 0) && (hashmap3.size() != 0) && (hashmap4.size() != 0)) {
        totalitems += i;
        worldlists.put(s3, new THWorldList(hashmap, hashmap1, hashmap2, hashmap3, hashmap4));
      } else {
        listnum--;
        System.out.println("[TreasureHunt] A subsection of WorldList '" + s3 + "' has no items!  As this would BREAK TreasureHunt, this list is being ignored!");
      }
    }

    System.out.println("[TreasureHunt] Loaded a total of " + totalitems + " items successfully from " + listnum + " WorldLists!");
    if (!config.getConfig().isConfigurationSection("CustomLists")) {
      config.getConfig().createSection("CustomLists");
    }

    Set set8 = config.getConfig().getConfigurationSection("CustomLists").getKeys(false);
    listnum = 0;
    totalitems = 0;

    for (Iterator iterator10 = set8.iterator(); iterator10.hasNext(); i = 0) {
      String s8 = (String)iterator10.next();
      listnum++;
      Set set9 = config.getConfig().getConfigurationSection("CustomLists." + s8).getKeys(false);
      HashMap hashmap5 = new HashMap();
      Iterator iterator11 = set9.iterator();

      while (iterator11.hasNext()) {
        String s9 = (String)iterator11.next();
        String[] astring5 = s9.split(":");
        if (astring5.length < 2) {
          int itemdat = 0;
          try
          {
            itemdat = Integer.parseInt(s9);
          }
          catch (NumberFormatException numberformatexception10)
          {
          }
          if ((Material.getMaterial(s9.toUpperCase()) == null) && (!enchanted.containsKey(s9)) && ((itemdat == 0) || (Material.getMaterial(itemdat) == null))) {
            System.out.println("'" + s9 + "' is not a valid item name or id. (CustomList '" + s8 + "')");
          } else {
            if (Material.getMaterial(s9.toUpperCase()) != null)
              hashmap5.put(new ItemStack(Material.getMaterial(s9.toUpperCase())), Integer.valueOf(config.getConfig().getInt("CustomLists." + s8 + "." + s9, 1)));
            else if (enchanted.containsKey(s9))
              hashmap5.put((ItemStack)enchanted.get(s9), Integer.valueOf(config.getConfig().getInt("CustomLists." + s8 + "." + s9, 1)));
            else {
              hashmap5.put(new ItemStack(Material.getMaterial(itemdat)), Integer.valueOf(config.getConfig().getInt("CustomLists." + s8 + "." + s9, 1)));
            }

            i++;
          }
        } else {
          short short6 = Short.parseShort(astring5[1]);
          int intval = 0;
          try
          {
            intval = Integer.parseInt(astring5[0]);
          }
          catch (NumberFormatException numberformatexception11)
          {
          }
          if ((Material.getMaterial(s9.toUpperCase()) == null) && ((intval == 0) || (Material.getMaterial(intval) == null))) {
            System.out.println("'" + astring5[0] + "' is not a valid item name or id. (CustomList '" + s8 + "')");
          } else {
            if (Material.getMaterial(astring5[0].toUpperCase()) != null)
              hashmap5.put(new ItemStack(Material.getMaterial(astring5[0].toUpperCase()), 1, short6), Integer.valueOf(config.getConfig().getInt("CustomLists." + s8 + "." + s9, 1)));
            else {
              hashmap5.put(new ItemStack(Material.getMaterial(intval), 1, short6), Integer.valueOf(config.getConfig().getInt("CustomLists." + s8 + "." + s9, 1)));
            }

            i++;
          }
        }
      }

      if (hashmap5.size() == 0) {
        listnum--;
        System.out.println("[TreasureHunt] CustomList '" + s8 + "' has no items!  The list is being ignored!");
      } else {
        totalitems += i;
        customlists.put(s8, hashmap5);
      }
    }

    System.out.println("[TreasureHunt] Loaded a total of " + totalitems + " items successfully from " + listnum + " CustomLists!");
    i = 0;
    Iterator iterator3 = stationarystrings.iterator();

    while (iterator3.hasNext()) {
      String s1 = (String)iterator3.next();
      String[] astring6 = s1.split(":");
      if (astring6.length >= 9) {
        World world1 = server.getWorld(astring6[0]);
        if (world1 != null)
        {
          if (astring6.length == 9) {
            THStationaryChest thstationarychest = new THStationaryChest("Default", Integer.parseInt(astring6[5]), Integer.parseInt(astring6[6]), Integer.parseInt(astring6[4]), Long.parseLong(astring6[7]), Integer.parseInt(astring6[8]), world1.getBlockAt(Integer.parseInt(astring6[1]), Integer.parseInt(astring6[2]), Integer.parseInt(astring6[3])));
            stationaryList.add(thstationarychest);
            i++;
          } else if (astring6.length == 10) {
            THStationaryChest thstationarychest = new THStationaryChest(astring6[9], Integer.parseInt(astring6[5]), Integer.parseInt(astring6[6]), Integer.parseInt(astring6[4]), Long.parseLong(astring6[7]), Integer.parseInt(astring6[8]), world1.getBlockAt(Integer.parseInt(astring6[1]), Integer.parseInt(astring6[2]), Integer.parseInt(astring6[3])));
            stationaryList.add(thstationarychest);
            i++;
          }
        }
      }
    }

    System.out.println("[TreasureHunt] Loaded " + i + " stationary chests.");
    iterator3 = worlds.keySet().iterator();

    while (iterator3.hasNext()) {
      String s1 = (String)iterator3.next();
      THWorldOpts thworldopts = (THWorldOpts)worlds.get(s1);
      if (!worldlists.containsKey(thworldopts.itemlist)) {
        System.out.println("[TreasureHunt] World '" + s1 + "' has '" + thworldopts.itemlist + "' listed as its WorldList, but this WorldList doesn't seem to exist.");
        System.out.println("[TreasureHunt] Chest generation will not work in this world until this is resolved!");
      }
    }

    iterator3 = stationaryList.iterator();

    while (iterator3.hasNext()) {
      THStationaryChest thstationarychest1 = (THStationaryChest)iterator3.next();
      if ((!worldlists.containsKey(thstationarychest1.itemlist)) && (!customlists.containsKey(thstationarychest1.itemlist))) {
        System.out.println("[TreasureHunt] Stationary chest at " + thstationarychest1.chest.getX() + "," + thstationarychest1.chest.getY() + "," + thstationarychest1.chest.getZ() + " in world '" + thstationarychest1.chest.getWorld().getName() + "' has '" + thstationarychest1.itemlist + "' listed as its WorldList/CustomList, but this list doesn't seem to exist.");
        System.out.println("[TreasureHunt] This chest will not respawn properly until this is resolved!");
      }
    }

    int j = 0;
    if (!players.getConfig().isConfigurationSection("PlayerData")) {
      players.getConfig().createSection("PlayerData");
    }

    Set set9 = players.getConfig().getConfigurationSection("PlayerData").getKeys(false);
    Iterator iterator5 = set9.iterator();

    while (iterator5.hasNext()) {
      String s2 = (String)iterator5.next();
      String[] astring = players.getConfig().getString("PlayerData." + s2, "0:0").split(":");
      if (astring.length >= 2) {
        j++;
        playerdata.put(s2, new THPlayer(Integer.parseInt(astring[0]), Integer.parseInt(astring[1])));
      }
    }

    System.out.println("[TreasureHunt] Loaded data for " + j + " players.");
    config.saveDefaultConfig();
    players.saveDefaultConfig();
    messages.saveDefaultConfig();
  }

  public void saveProcedure() {
    System.out.println("[TreasureHunt] Saving data...");
    int[] config = updateConfig();
    System.out.println("[TreasureHunt] Saved " + config[0] + " worlds.");
    System.out.println("[TreasureHunt] Saved " + config[1] + " items in " + config[2] + " WorldLists and " + config[3] + "CustomLists.");
    System.out.println("[TreasureHunt] Saved " + config[4] + " enchanted item setups.");

    updateMessages();
    System.out.println("[TreasureHunt] Saved messages.");

    updatePlayerData();
    System.out.println("[TreasureHunt] Saved player data.");
  }

  private int[] updateConfig() {
    Config config = new Config(this, "config.yml");
    config.loadConfig();
    boolean i = false;
    int wc = 0;
    int wl = 0;
    int cl = 0;
    config.getConfig().set("Options.SecondsBetweenChecks", Integer.valueOf(checksec));
    config.getConfig().set("Options.MaxSpawnAttempts", Integer.valueOf(maxspawnattempts));
    config.getConfig().set("Options.MaxAttemptsPerTick", Integer.valueOf(maxattemptspertick));
    config.getConfig().set("Options.MinPlayersOnline", Integer.valueOf(minplayers));
    config.getConfig().set("Options.UseCompass", Boolean.valueOf(usecompass));
    config.getConfig().set("Options.3DDistances", Boolean.valueOf(threedimensionaldistance));
    config.getConfig().set("Options.FoundChestFadeTime", Integer.valueOf(foundchestfadetime));
    config.getConfig().set("Options.DirectionalText", Boolean.valueOf(directionaltext));
    config.getConfig().set("Options.DetailLogs", Boolean.valueOf(detaillogs));
    config.getConfig().set("Options.Protection.Break", Boolean.valueOf(protectbreak));
    config.getConfig().set("Options.Protection.Burn", Boolean.valueOf(protectburn));
    config.getConfig().set("Options.Protection.Explode", Boolean.valueOf(protectexplode));
    config.getConfig().set("Options.Protection.Piston", Boolean.valueOf(protectpiston));
    config.getConfig().set("Options.ChestLevels.Uncommon", Integer.valueOf(uncommonlevel));
    config.getConfig().set("Options.ChestLevels.Rare", Integer.valueOf(rarelevel));
    config.getConfig().set("Options.ChestLevels.Legendary", Integer.valueOf(legendarylevel));
    config.getConfig().set("Options.ChestLevels.Epic", Integer.valueOf(epiclevel));
    LinkedList entries = new LinkedList();
    Iterator chestlist = compassblocks.iterator();

    while (chestlist.hasNext()) {
      Block enchs = (Block)chestlist.next();
      String messages = enchs.getWorld().getName() + ":" + enchs.getX() + ":" + enchs.getY() + ":" + enchs.getZ();
      entries.add(messages);
    }

    config.getConfig().set("CompassBlocks", entries);

    for (chestlist = worlds.entrySet().iterator(); chestlist.hasNext(); wc++) {
      Map.Entry entry = (Map.Entry)chestlist.next();
      String messages = (String)entry.getKey();
      THWorldOpts players = (THWorldOpts)entry.getValue();
      config.getConfig().set("WorldOptions." + messages + ".ItemList", players.itemlist);
      config.getConfig().set("WorldOptions." + messages + ".HuntTool", players.hunttool.name());
      config.getConfig().set("WorldOptions." + messages + ".OfferingTool", players.offeringtool.name());
      config.getConfig().set("WorldOptions." + messages + ".MarkerBlock", players.markerblock.name());
      if (players.fadeblock == null)
        config.getConfig().set("WorldOptions." + messages + ".FadeBlock", "RETURN");
      else {
        config.getConfig().set("WorldOptions." + messages + ".FadeBlock", players.fadeblock.name());
      }

      config.getConfig().set("WorldOptions." + messages + ".ChestChance", Integer.valueOf(players.chance));
      config.getConfig().set("WorldOptions." + messages + ".ChestInterval", Integer.valueOf(players.interval));
      config.getConfig().set("WorldOptions." + messages + ".ChestDuration", Integer.valueOf(players.duration));
      config.getConfig().set("WorldOptions." + messages + ".MaxDistance", Integer.valueOf(players.maxdistance));
      config.getConfig().set("WorldOptions." + messages + ".MinDistance", Integer.valueOf(players.mindistance));
      config.getConfig().set("WorldOptions." + messages + ".MaxCompassDistance", Integer.valueOf(players.maxcompassdistance));
      config.getConfig().set("WorldOptions." + messages + ".CenterX", Integer.valueOf(players.centerx));
      config.getConfig().set("WorldOptions." + messages + ".CenterZ", Integer.valueOf(players.centerz));
      config.getConfig().set("WorldOptions." + messages + ".DrawWeight", Integer.valueOf(players.drawweight));
      config.getConfig().set("WorldOptions." + messages + ".AmountWeight", Integer.valueOf(players.amountweight));
      config.getConfig().set("WorldOptions." + messages + ".GoodItemWeight", Integer.valueOf(players.gooditemweight));
      config.getConfig().set("WorldOptions." + messages + ".MinLightLevel", Integer.valueOf(players.minlight));
      config.getConfig().set("WorldOptions." + messages + ".MaxLightLevel", Integer.valueOf(players.maxlight));
      config.getConfig().set("WorldOptions." + messages + ".UseMarker", Boolean.valueOf(players.usemarker));
      config.getConfig().set("WorldOptions." + messages + ".MinElevation", Integer.valueOf(players.minelevation));
      config.getConfig().set("WorldOptions." + messages + ".MaxElevation", Integer.valueOf(players.maxelevation));
      config.getConfig().set("WorldOptions." + messages + ".MaxElevationRare", Integer.valueOf(players.maxelevationrare));
      config.getConfig().set("WorldOptions." + messages + ".ConsumeChance", Integer.valueOf(players.consumechance));
      config.getConfig().set("WorldOptions." + messages + ".MinMoney", Integer.valueOf(players.minmoney));
      config.getConfig().set("WorldOptions." + messages + ".MoneyMultiplier", Double.valueOf(players.moneymultiplier));
      config.getConfig().set("WorldOptions." + messages + ".Enabled", Boolean.valueOf(players.enabled));
      config.getConfig().set("WorldOptions." + messages + ".MaxValue", Integer.valueOf(players.maxvalue));
      config.getConfig().set("WorldOptions." + messages + ".LastCheck", Long.valueOf(players.lastcheck));
      config.getConfig().set("WorldOptions." + messages + ".OverrideMinPlayers", Boolean.valueOf(players.overrideminplayers));
      config.getConfig().set("WorldOptions." + messages + ".FadeFoundChests", Boolean.valueOf(players.fadefoundchests));
      config.getConfig().set("WorldOptions." + messages + ".MinChests", Integer.valueOf(players.minchests));
      config.getConfig().set("WorldOptions." + messages + ".OfferAmount", Integer.valueOf(players.offeramount));
      config.getConfig().set("WorldOptions." + messages + ".StrictItems", Boolean.valueOf(players.strictitems));
      LinkedList s = new LinkedList();
      Iterator iterator = players.spawnableblocks.iterator();

      while (iterator.hasNext()) {
        Material itemdat = (Material)iterator.next();
        s.add(itemdat.name());
      }

      config.getConfig().set("WorldOptions." + messages + ".CanSpawnOn", s);
    }

    int i2 = 0;
    chestlist = worldlists.keySet().iterator();

    while (chestlist.hasNext()) {
      String s = (String)chestlist.next();
      wl++;

      for (Iterator iterator1 = ((THWorldList)worldlists.get(s)).common.entrySet().iterator(); iterator1.hasNext(); i2++) {
        Map.Entry entry1 = (Map.Entry)iterator1.next();
        ItemStack itemstack = (ItemStack)entry1.getKey();
        if (enchanted.containsValue(itemstack)) {
          Iterator iterator = enchanted.entrySet().iterator();

          while (iterator.hasNext()) {
            Map.Entry entry2 = (Map.Entry)iterator.next();
            if (entry2.getValue() == itemstack)
              config.getConfig().set("WorldLists." + s + ".Common." + (String)entry2.getKey(), entry1.getValue());
          }
        }
        if (itemstack.getDurability() != 0) {
          String s1 = itemstack.getType().name() + ":" + itemstack.getDurability();
          config.getConfig().set("WorldLists." + s + ".Common." + s1, entry1.getValue());
        } else {
          config.getConfig().set("WorldLists." + s + ".Common." + itemstack.getType().name(), entry1.getValue());
        }
      }

      for (iterator1 = ((THWorldList)worldlists.get(s)).uncommon.entrySet().iterator(); iterator1.hasNext(); i2++) {
        Map.Entry entry1 = (Map.Entry)iterator1.next();
        ItemStack itemstack = (ItemStack)entry1.getKey();
        if (enchanted.containsValue(itemstack)) {
          Iterator iterator = enchanted.entrySet().iterator();

          while (iterator.hasNext()) {
            Map.Entry entry2 = (Map.Entry)iterator.next();
            if (entry2.getValue() == itemstack)
              config.getConfig().set("WorldLists." + s + ".Uncommon." + (String)entry2.getKey(), entry1.getValue());
          }
        }
        if (itemstack.getDurability() != 0) {
          String s1 = itemstack.getType().name() + ":" + itemstack.getDurability();
          config.getConfig().set("WorldLists." + s + ".Uncommon." + s1, entry1.getValue());
        } else {
          config.getConfig().set("WorldLists." + s + ".Uncommon." + itemstack.getType().name(), entry1.getValue());
        }
      }

      for (iterator1 = ((THWorldList)worldlists.get(s)).rare.entrySet().iterator(); iterator1.hasNext(); i2++) {
        Map.Entry entry1 = (Map.Entry)iterator1.next();
        ItemStack itemstack = (ItemStack)entry1.getKey();
        if (enchanted.containsValue(itemstack)) {
          Iterator iterator = enchanted.entrySet().iterator();

          while (iterator.hasNext()) {
            Map.Entry entry2 = (Map.Entry)iterator.next();
            if (entry2.getValue() == itemstack)
              config.getConfig().set("WorldLists." + s + ".Rare." + (String)entry2.getKey(), entry1.getValue());
          }
        }
        if (itemstack.getDurability() != 0) {
          String s1 = itemstack.getType().name() + ":" + itemstack.getDurability();
          config.getConfig().set("WorldLists." + s + ".Rare." + s1, entry1.getValue());
        } else {
          config.getConfig().set("WorldLists." + s + ".Rare." + itemstack.getType().name(), entry1.getValue());
        }
      }

      for (iterator1 = ((THWorldList)worldlists.get(s)).legendary.entrySet().iterator(); iterator1.hasNext(); i2++) {
        Map.Entry entry1 = (Map.Entry)iterator1.next();
        ItemStack itemstack = (ItemStack)entry1.getKey();
        if (enchanted.containsValue(itemstack)) {
          Iterator iterator = enchanted.entrySet().iterator();

          while (iterator.hasNext()) {
            Map.Entry entry2 = (Map.Entry)iterator.next();
            if (entry2.getValue() == itemstack)
              config.getConfig().set("WorldLists." + s + ".Legendary." + (String)entry2.getKey(), entry1.getValue());
          }
        }
        if (itemstack.getDurability() != 0) {
          String s1 = itemstack.getType().name() + ":" + itemstack.getDurability();
          config.getConfig().set("WorldLists." + s + ".Legendary." + s1, entry1.getValue());
        } else {
          config.getConfig().set("WorldLists." + s + ".Legendary." + itemstack.getType().name(), entry1.getValue());
        }
      }

      for (iterator1 = ((THWorldList)worldlists.get(s)).epic.entrySet().iterator(); iterator1.hasNext(); i2++) {
        Map.Entry entry1 = (Map.Entry)iterator1.next();
        ItemStack itemstack = (ItemStack)entry1.getKey();
        if (enchanted.containsValue(itemstack)) {
          Iterator iterator = enchanted.entrySet().iterator();

          while (iterator.hasNext()) {
            Map.Entry entry2 = (Map.Entry)iterator.next();
            if (entry2.getValue() == itemstack)
              config.getConfig().set("WorldLists." + s + ".Epic." + (String)entry2.getKey(), entry1.getValue());
          }
        }
        if (itemstack.getDurability() != 0) {
          String s1 = itemstack.getType().name() + ":" + itemstack.getDurability();
          config.getConfig().set("WorldLists." + s + ".Epic." + s1, entry1.getValue());
        } else {
          config.getConfig().set("WorldLists." + s + ".Epic." + itemstack.getType().name(), entry1.getValue());
        }
      }
    }

    chestlist = customlists.keySet().iterator();

    while (chestlist.hasNext()) {
      String s = (String)chestlist.next();
      cl++;

      for (Iterator iterator1 = ((Map)customlists.get(s)).entrySet().iterator(); iterator1.hasNext(); i2++) {
        Map.Entry entry1 = (Map.Entry)iterator1.next();
        ItemStack itemstack = (ItemStack)entry1.getKey();
        if (enchanted.containsValue(itemstack)) {
          Iterator iterator = enchanted.entrySet().iterator();

          while (iterator.hasNext()) {
            Map.Entry entry2 = (Map.Entry)iterator.next();
            if (entry2.getValue() == itemstack)
              config.getConfig().set("CustomLists." + s + "." + (String)entry2.getKey(), entry1.getValue());
          }
        }
        if (itemstack.getDurability() != 0) {
          String s1 = itemstack.getType().name() + ":" + itemstack.getDurability();
          config.getConfig().set("CustomLists." + s + "." + s1, entry1.getValue());
        } else {
          config.getConfig().set("CustomLists." + s + "." + itemstack.getType().name(), entry1.getValue());
        }
      }
    }

    int j = 0;
    Iterator iterator2 = enchanted.entrySet().iterator();

    while (iterator2.hasNext()) {
      Map.Entry entry3 = (Map.Entry)iterator2.next();
      j++;
      config.getConfig().set("EnchantedItems." + (String)entry3.getKey() + ".ID", Integer.valueOf(((ItemStack)entry3.getValue()).getTypeId()));
      config.getConfig().set("EnchantedItems." + (String)entry3.getKey() + ".Damage", Short.valueOf(((ItemStack)entry3.getValue()).getDurability()));
      Iterator iterator3 = ((ItemStack)entry3.getValue()).getEnchantments().entrySet().iterator();

      while (iterator3.hasNext()) {
        Map.Entry entry4 = (Map.Entry)iterator3.next();
        config.getConfig().set("EnchantedItems." + (String)entry3.getKey() + ".Effects." + ((Enchantment)entry4.getKey()).getName(), entry4.getValue());
      }
    }

    LinkedList linkedlist = new LinkedList();
    Iterator iterator1 = stationaryList.iterator();

    while (iterator1.hasNext()) {
      THStationaryChest thstationarychest = (THStationaryChest)iterator1.next();
      linkedlist.add(thstationarychest.chest.getWorld().getName() + ":" + thstationarychest.chest.getX() + ":" + thstationarychest.chest.getY() + ":" + thstationarychest.chest.getZ() + ":" + thstationarychest.value + ":" + thstationarychest.respawnmintime + ":" + thstationarychest.respawnmaxtime + ":" + thstationarychest.lastrespawn + ":" + thstationarychest.currentrespawntime + ":" + thstationarychest.itemlist);
    }

    config.getConfig().set("StationaryChests", linkedlist);
    config.saveConfig();
    int[] result = { wc, i2, wl, cl, j };
    return result;
  }

  private void updateMessages() {
    Config conf = new Config(this, "messages.yml");
    conf.loadConfig();
    conf.getConfig().set("Options.PluginTag", ptag);
    conf.getConfig().set("Messages.SpawnedChest", spawnedchest);
    conf.getConfig().set("Messages.PlayerCloseToChest", playerclose);
    conf.getConfig().set("Messages.YouAreClosest", youareclosest);
    conf.getConfig().set("Messages.NoLongerClosest", nolongerclosest);
    conf.getConfig().set("Messages.PlayerFoundChest", playerfound);
    conf.getConfig().set("Messages.MoneyFound", moneyfound);
    conf.getConfig().set("Messages.FoundChestFaded", foundchestfaded);
    conf.getConfig().set("Messages.UnoundChestFaded", unfoundchestfaded);
    conf.getConfig().set("Messages.AlreadyClaimed", alreadyclaimed);
    conf.getConfig().set("Messages.ClosestChest", closestchest);
    conf.getConfig().set("Messages.NoChests", nochests);
    conf.getConfig().set("Messages.OfferItem", offeritem);
    conf.getConfig().set("Messages.CompassChange", compasschange);
    conf.getConfig().set("Messages.CompassNoChange", compassnochange);
    conf.getConfig().set("Messages.Directional", directional);
    conf.getConfig().set("Directions.Forward", forwardtext);
    conf.getConfig().set("Directions.Backward", backwardtext);
    conf.getConfig().set("Directions.Above", abovetext);
    conf.getConfig().set("Directions.Below", belowtext);
    conf.getConfig().set("Directions.Left", lefttext);
    conf.getConfig().set("Directions.Right", righttext);
    conf.saveConfig();
  }

  private void updatePlayerData() {
    Config pd = new Config(this, "players.yml");
    pd.loadConfig();
    Iterator iterator4 = playerdata.keySet().iterator();

    while (iterator4.hasNext()) {
      String s2 = (String)iterator4.next();
      pd.getConfig().set("PlayerData." + s2, ((THPlayer)playerdata.get(s2)).getChestsFound() + ":" + ((THPlayer)playerdata.get(s2)).getValueFound());
    }

    pd.saveConfig();
  }

  public static void addFound(Player p, int value) {
    if (playerdata.containsKey(p.getName()))
      ((THPlayer)playerdata.get(p.getName())).foundChest(value);
    else {
      playerdata.put(p.getName(), new THPlayer(1, value));
    }
    plugin.updatePlayerData();
  }
  public static THHunt getCurrentHunt(Location location) { int x = location.getBlockX();
    int y = location.getBlockY();
    int z = location.getBlockZ();
    Iterator iterator = huntList.iterator();
    THHunt h;
    Location hl;
    do { if (!iterator.hasNext()) {
        return null;
      }

      h = (THHunt)iterator.next();
      hl = h.getLocation(); }
    while ((!hl.getWorld().getName().equalsIgnoreCase(location.getWorld().getName())) || (hl.getBlockX() != x) || ((hl.getBlockY() != y) && (hl.getBlockY() != y + 1)) || (hl.getBlockZ() != z));

    return h; }

  public static int getAmountInInventory(PlayerInventory p, Material m, short d)
  {
    int total = 0;

    for (int i = 0; i < p.getSize(); i++) {
      ItemStack pm = p.getItem(i);
      if ((pm != null) && (pm.getType() == m) && (pm.getDurability() == d)) {
        total += pm.getAmount();
      }
    }

    return total;
  }

  public static void takeItemFromPlayer(PlayerInventory p, Material m, short data, int amt) {
    int amounttotake = amt;
    do
    {
      ItemStack tstack = null;
      ItemStack[] aitemstack;
      int i = (aitemstack = p.getContents()).length;

      for (int pos = 0; pos < i; pos++) {
        ItemStack samt = aitemstack[pos];
        if ((samt != null) && (samt.getType() == m) && (samt.getDurability() == data)) {
          tstack = samt;
        }
      }

      if (tstack != null) {
        int j = tstack.getAmount();
        pos = p.first(tstack);
        if (j <= amounttotake) {
          p.clear(pos);
          amounttotake -= j;
        } else if (j > amounttotake) {
          p.setItem(pos, new ItemStack(m, j - amounttotake, data));
          amounttotake = 0;
        }
      }
    }
    while (amounttotake > 0);
  }

  public static Set getHuntsInWorld(String world)
  {
    HashSet returnlist = new HashSet();
    Iterator iterator = huntList.iterator();

    while (iterator.hasNext()) {
      THHunt h = (THHunt)iterator.next();
      if (h.getWorld().equalsIgnoreCase(world)) {
        returnlist.add(h);
      }
    }

    return returnlist;
  }

  public static THStationaryChest getStationaryChest(Block block) {
    if ((block.getType() != Material.CHEST) && (block.getType() != Material.LOCKED_CHEST)) {
      return null;
    }
    Iterator iterator = stationaryList.iterator();

    while (iterator.hasNext()) {
      THStationaryChest c = (THStationaryChest)iterator.next();
      if (c.chest.equals(block)) {
        return c;
      }
    }

    return null;
  }

  public static String colorize(String s)
  {
    return s == null ? null : s.replaceAll("&([0-9a-f])", "§$1");
  }

  public static String convertTags(String inc, Map values) {
    if (inc == null) {
      return null;
    }
    String message = new String(inc);
    if (values.containsKey("pname")) {
      message = message.replaceAll("<pname>", (String)values.get("pname"));
    }

    if (values.containsKey("worldname")) {
      message = message.replaceAll("<worldname>", (String)values.get("worldname"));
    }

    if (values.containsKey("value")) {
      message = message.replaceAll("<value>", (String)values.get("value"));
    }

    if (values.containsKey("rarity")) {
      message = message.replaceAll("<rarity>", (String)values.get("rarity"));
    }

    if (values.containsKey("item")) {
      message = message.replaceAll("<item>", (String)values.get("item"));
    }

    if (values.containsKey("distance")) {
      message = message.replaceAll("<distance>", (String)values.get("distance"));
    }

    if (values.containsKey("numhunts")) {
      message = message.replaceAll("<numhunts>", (String)values.get("numhunts"));
    }

    if (values.containsKey("location")) {
      message = message.replaceAll("<location>", (String)values.get("location"));
    }

    if (values.containsKey("amount")) {
      message = message.replaceAll("<amount>", (String)values.get("amount"));
    }

    if (values.containsKey("timeleft")) {
      message = message.replaceAll("<timeleft>", (String)values.get("timeleft"));
    }

    if (values.containsKey("direction")) {
      message = message.replaceAll("<direction>", (String)values.get("direction"));
    }

    message = message.replaceAll("<tag>", ptag);
    return message;
  }

  public static THHunt getClosestHunt(Player p, boolean display)
  {
    int currdist = 100000;
    THHunt currhunt = null;
    Set hunts = getHuntsInWorld(p.getWorld().getName());
    Iterator distance = hunts.iterator();

    while (distance.hasNext()) {
      THHunt wname = (THHunt)distance.next();
      int numhunts = threedimensionaldistance ? wname.get3DDistanceFrom(p.getLocation()) : wname.getDistanceFrom(p.getLocation());
      if ((!wname.isLocked()) && (numhunts < currdist)) {
        currdist = numhunts;
        currhunt = wname;
      }
    }

    if (!display) {
      return currhunt;
    }
    if (currhunt == null) {
      HashMap wname1 = new HashMap();
      wname1.put("pname", p.getName());
      wname1.put("worldname", p.getWorld().getName());
      p.sendMessage(colorize(convertTags(nochests, wname1)));
    } else {
      String wname2 = p.getWorld().getName();
      int distance1 = currhunt.getDistanceFrom(p.getLocation());
      int numhunts = getHuntsInWorld(wname2).size();
      HashMap data = new HashMap();
      data.put("pname", p.getName());
      data.put("worldname", wname2);
      data.put("distance", Integer.toString(distance1));
      data.put("numhunts", Integer.toString(numhunts));
      data.put("value", Integer.toString(currhunt.getValue()));
      data.put("rarity", currhunt.getRarityString());
      data.put("location", currhunt.getLocString());
      data.put("timeleft", currhunt.getMinutesLeft() + " minutes");
      p.sendMessage(colorize(convertTags(closestchest, data)));
      if (directionaltext) {
        Vector dir = new Vector(currhunt.getLocation().getBlockX() - p.getLocation().getBlockX(), currhunt.getLocation().getBlockY() - p.getLocation().getBlockY(), currhunt.getLocation().getBlockZ() - p.getLocation().getBlockZ());
        LookDirection direction = null;
        if ((Math.abs(dir.getX()) >= Math.abs(dir.getZ())) && (Math.abs(dir.getX()) >= Math.abs(dir.getY()))) {
          if (dir.getX() >= 0.0D)
            direction = LookDirection.POSX;
          else
            direction = LookDirection.NEGX;
        }
        else if ((Math.abs(dir.getZ()) >= Math.abs(dir.getX())) && (Math.abs(dir.getZ()) >= Math.abs(dir.getY()))) {
          if (dir.getZ() >= 0.0D)
            direction = LookDirection.POSZ;
          else
            direction = LookDirection.NEGZ;
        }
        else if (dir.getY() >= 0.0D)
          direction = LookDirection.field_0;
        else {
          direction = LookDirection.DOWN;
        }

        if ((direction != LookDirection.field_0) && (direction != LookDirection.DOWN)) {
          Vector pdir = p.getLocation().getDirection();
          LookDirection pdirection = null;
          if (Math.abs(pdir.getX()) >= Math.abs(pdir.getZ())) {
            if (pdir.getX() >= 0.0D)
              pdirection = LookDirection.POSX;
            else
              pdirection = LookDirection.NEGX;
          }
          else if (pdir.getZ() >= 0.0D)
            pdirection = LookDirection.POSZ;
          else {
            pdirection = LookDirection.NEGZ;
          }

          if (direction == pdirection) {
            data.put("direction", forwardtext);
            p.sendMessage(colorize(convertTags(directional, data)));
          } else if (((direction != LookDirection.POSX) || (pdirection != LookDirection.NEGX)) && ((direction != LookDirection.NEGX) || (pdirection != LookDirection.POSX)) && ((direction != LookDirection.POSZ) || (pdirection != LookDirection.NEGZ)) && ((direction != LookDirection.NEGZ) || (pdirection != LookDirection.POSZ))) {
            if (((direction != LookDirection.POSX) || (pdirection != LookDirection.POSZ)) && ((direction != LookDirection.NEGZ) || (pdirection != LookDirection.POSX)) && ((direction != LookDirection.NEGX) || (pdirection != LookDirection.NEGZ)) && ((direction != LookDirection.POSZ) || (pdirection != LookDirection.NEGX))) {
              data.put("direction", righttext);
              p.sendMessage(colorize(convertTags(directional, data)));
            } else {
              data.put("direction", lefttext);
              p.sendMessage(colorize(convertTags(directional, data)));
            }
          } else {
            data.put("direction", backwardtext);
            p.sendMessage(colorize(convertTags(directional, data)));
          }
        } else {
          if (direction == LookDirection.field_0)
            data.put("direction", abovetext);
          else {
            data.put("direction", belowtext);
          }

          p.sendMessage(colorize(convertTags(directional, data)));
        }
      }

      currhunt.showClosestPlayer();
    }

    return currhunt;
  }

  public static void broadcast(String s)
  {
    Player[] aplayer;
    int i = (aplayer = server.getOnlinePlayers()).length;

    for (int j = 0; j < i; j++) {
      Player p = aplayer[j];
      if ((!useperms) || ((useperms) && ((permission.has(p, "taien.th.notify.*")) || (permission.has(p, "taien.th.notify." + p.getWorld().getName())))))
        p.sendMessage(s);
    }
  }

  public static String getFirstWorldListName()
  {
    Iterator iterator = worldlists.keySet().iterator();
    if (iterator.hasNext()) {
      String s = (String)iterator.next();
      return s;
    }
    return null;
  }

  public static String getFirstCustomListName()
  {
    Iterator iterator = customlists.keySet().iterator();
    if (iterator.hasNext()) {
      String s = (String)iterator.next();
      return s;
    }
    return null;
  }

  public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
  {
    boolean success = executeCommand(sender, command, label, args);
    updateConfig();
    updateMessages();
    return success;
  }

  public boolean executeCommand(CommandSender sender, Command command, String label, String[] args) {
    boolean isPlayer = sender instanceof Player;
    Player p = isPlayer ? (Player)sender : null;

    if (command.getName().equalsIgnoreCase("top")) {
      if (!isPlayer) {
        return false;
      }

      if ((args.length != 0) && ((args.length != 1) || (!args[0].equalsIgnoreCase("chests")))) {
        if ((args.length == 1) && (args[0].equalsIgnoreCase("values"))) {
          p.sendMessage(ChatColor.DARK_PURPLE + "TOP HUNTERS" + ChatColor.GRAY + "-------------------------");
          LinkedList wn4 = new LinkedList();

          for (Integer o7 = Integer.valueOf(1); o7.intValue() < 11; o7 = Integer.valueOf(o7.intValue() + 1)) {
            int i = 0;
            int oo4 = 0;
            String m2 = "";
            Iterator iterator = playerdata.keySet().iterator();

            while (iterator.hasNext()) {
              String s1 = (String)iterator.next();
              THPlayer tp = (THPlayer)playerdata.get(s1);
              if ((tp.getValueFound() > i) && (!wn4.contains(s1))) {
                i = tp.getValueFound();
                m2 = s1;
                oo4 = tp.getChestsFound();
              }
            }

            if (!m2.equals("")) {
              wn4.add(m2);
              p.sendMessage(ChatColor.GRAY + o7.toString() + ". " + ChatColor.WHITE + m2 + ChatColor.DARK_GRAY + "......" + ChatColor.LIGHT_PURPLE + i + " TV in " + oo4 + " chests");
            }
          }

          p.sendMessage(ChatColor.LIGHT_PURPLE + "/top " + ChatColor.WHITE + "to order by chests");
          return true;
        }if ((args.length == 1) && (args[0].equalsIgnoreCase("reset"))) {
          if (((useperms) || (!p.isOp())) && ((!useperms) || (!permission.has(p, "taien.th.admin")))) {
            return false;
          }
          playerdata.clear();
          p.sendMessage(ChatColor.DARK_PURPLE + "All top hunter rankings have been reset.");
          return true;
        }

        return false;
      }

      p.sendMessage(ChatColor.DARK_PURPLE + "TOP HUNTERS" + ChatColor.GRAY + "-------------------------");
      LinkedList wn4 = new LinkedList();

      for (Integer o7 = Integer.valueOf(1); o7.intValue() < 11; o7 = Integer.valueOf(o7.intValue() + 1)) {
        int i = 0;
        int oo4 = 0;
        String m2 = "";
        Iterator iterator = playerdata.keySet().iterator();

        while (iterator.hasNext()) {
          String s1 = (String)iterator.next();
          THPlayer tp = (THPlayer)playerdata.get(s1);
          if ((tp.getChestsFound() > i) && (!wn4.contains(s1))) {
            i = tp.getChestsFound();
            m2 = s1;
            oo4 = tp.getValueFound();
          }
        }

        if (!m2.equals("")) {
          wn4.add(m2);
          p.sendMessage(ChatColor.GRAY + o7.toString() + ". " + ChatColor.WHITE + m2 + ChatColor.DARK_GRAY + "......" + ChatColor.LIGHT_PURPLE + i + " chests of " + oo4 + " TV");
        }
      }

      p.sendMessage(ChatColor.LIGHT_PURPLE + "/top values " + ChatColor.WHITE + "to order by value");
      return true;
    }

    if (command.getName().equalsIgnoreCase("stattool")) {
      if (!isPlayer) {
        return false;
      }
      if (((!useperms) && (p.isOp())) || ((useperms) && ((permission.has(p, "taien.th.admin")) || (permission.has(p, "taien.th.stattool.*")) || (permission.has(p, "taien.th.stattool." + p.getWorld().getName()))))) {
        if ((args.length == 1) && (args[0].equalsIgnoreCase("off"))) {
          if (selections.containsKey(p)) {
            selections.remove(p);
            p.sendMessage(ChatColor.DARK_PURPLE + "TreasureHunt Stationary Chest tool turned off.");
            return true;
          }

          return false;
        }

        if (args.length < 4) {
          p.sendMessage(ChatColor.DARK_RED + "Incorrect entry.  Correct format(s) for this command:");
          p.sendMessage(ChatColor.RED + "/stattool <value> <minrespawnminutes> <maxrespawnminutes> <itemlist>");
          p.sendMessage(ChatColor.RED + "/stattool off");
          return true;
        }

        if (args.length == 4) {
          int wn2;
          try { wn2 = Integer.parseInt(args[0]);
          } catch (NumberFormatException numberformatexception) {
            p.sendMessage(ChatColor.DARK_RED + "Argument (" + args[0] + ") is not an integer valid for value.");
            return true;
          }
          int o6;
          try
          {
            o6 = Integer.parseInt(args[1]);
          } catch (NumberFormatException numberformatexception1) {
            p.sendMessage(ChatColor.DARK_RED + "Argument (" + args[1] + ") is not an integer valid for minrespawnminutes.");
            return true;
          }
          int i;
          try {
            i = Integer.parseInt(args[2]);
          } catch (NumberFormatException numberformatexception2) {
            p.sendMessage(ChatColor.DARK_RED + "Argument (" + args[2] + ") is not an integer valid for maxrespawnminutes.");
            return true;
          }

          if (wn2 < 1) {
            wn2 = 1;
          }

          if (o6 > i) {
            o6 = i - 1;
          }

          if (o6 < 0) {
            o6 = 0;
          }

          if (i < o6) {
            i = o6;
          }

          if (i < 0) {
            i = 0;
          }

          if ((worldlists.containsKey(args[3])) && (customlists.containsKey(args[3]))) {
            p.sendMessage(ChatColor.DARK_PURPLE + "TreasureHunt Stationary Chest tool set to Value: " + wn2 + " MinMinutes: " + o6 + " MaxMinutes: " + i + " ItemList: " + args[3]);
            selections.put(p, new THToolSettings(o6, i, wn2, args[3]));
            return true;
          }

          p.sendMessage(ChatColor.DARK_RED + "Argument (" + args[3] + ") is not a valid WorldList/CustomList.");
          return true;
        }
      }

      return true;
    }

    if (command.getName().equalsIgnoreCase("starthunt")) {
      if (args.length == 0)
      {
        if (!isPlayer) {
          String[] wn3 = new String[worlds.size()];
          wn3 = (String[])worlds.keySet().toArray(wn3);
          THWorldOpts o = null;
          World o1;
          World o1;
          if (worlds.size() == 1)
            o1 = server.getWorld(wn3[0]);
          else {
            o1 = server.getWorld(wn3[rndGen.nextInt(worlds.size())]);
          }

          THChestGenerator.startHunt(o1, -1, (Block)null, false, (String)null);
          return true;
        }if (((useperms) || (!p.isOp())) && ((!useperms) || ((!permission.has(p, "taien.th.admin")) && (!permission.has(p, "taien.th.starthunt.*")) && (!permission.has(p, "taien.th.starthunt." + p.getWorld().getName()))))) {
          p.sendMessage(ChatColor.DARK_RED + "You are not allowed to do that.");
          return true;
        }
        String[] wn3 = new String[worlds.size()];
        wn3 = (String[])worlds.keySet().toArray(wn3);
        THWorldOpts o = null;
        World o1;
        World o1;
        if (worlds.size() == 1)
          o1 = server.getWorld(wn3[0]);
        else {
          o1 = server.getWorld(wn3[rndGen.nextInt(worlds.size())]);
        }

        THChestGenerator.startHunt(o1, -1, (Block)null, false, (String)null);
        return true;
      }

      if (args.length == 1) { boolean wn1 = false;
        int wn2;
        try {
          wn2 = Integer.parseInt(args[0]);
        } catch (NumberFormatException numberformatexception3) {
          sender.sendMessage(ChatColor.DARK_RED + "You must enter an integer (for value).");
          return true;
        }

        if (!isPlayer) {
          if (worlds.size() == 0) {
            System.out.println("[TreasureHunt] Unable to start hunt!  No worlds set!");
            return true;
          }
          World[] o5 = new World[worlds.size()];
          o5 = (World[])worlds.keySet().toArray(o5);
          THWorldOpts i2 = null;
          World i6;
          World i6;
          if (worlds.size() == 1)
            i6 = o5[0];
          else {
            i6 = o5[rndGen.nextInt(worlds.size())];
          }

          THChestGenerator.startHunt(i6, wn2, (Block)null, false, (String)null);
          return true;
        }
        if (((useperms) || (!p.isOp())) && ((!useperms) || ((!permission.has(p, "taien.th.admin")) && (!permission.has(p, "taien.th.starthunt.*")) && (!permission.has(p, "taien.th.starthunt." + p.getWorld().getName()))))) {
          p.sendMessage(ChatColor.DARK_RED + "You are not allowed to do that.");
          return true;
        }
        World o1 = p.getWorld();
        THWorldOpts i2 = (THWorldOpts)worlds.get(o1.getName());
        if (i2 == null) {
          i2 = new THWorldOpts();
          worlds.put(o1.getName(), i2);
        }

        THChestGenerator.startHunt(o1, wn2, (Block)null, false, (String)null);
        return true;
      }
      if ((args.length == 2) && (args[0].equalsIgnoreCase("here"))) { boolean wn1 = false;
        int wn2;
        try {
          wn2 = Integer.parseInt(args[1]);
        } catch (NumberFormatException numberformatexception4) {
          sender.sendMessage(ChatColor.DARK_RED + "You must enter an integer (for value).");
          return true;
        }

        if (!isPlayer) {
          sender.sendMessage(ChatColor.DARK_RED + "You cannot /starthunt here when you are not ingame.");
          return true;
        }if (((useperms) || (!p.isOp())) && ((!useperms) || ((!permission.has(p, "taien.th.admin")) && (!permission.has(p, "taien.th.starthunt.*")) && (!permission.has(p, "taien.th.starthunt." + p.getWorld().getName()))))) {
          p.sendMessage(ChatColor.DARK_RED + "You are not allowed to do that.");
          return true;
        }
        Block o3 = p.getTargetBlock((HashSet)null, 20);
        THChestGenerator.startHunt(o3.getWorld(), wn2, o3, false, (String)null);
        return true;
      }

      return false;
    }

    if ((!command.getName().equalsIgnoreCase("hunt")) && ((!command.getName().equalsIgnoreCase("th")) || (!(sender instanceof Player))))
      return false;
    if (!isPlayer) {
      sender.sendMessage("Sorry, that command is not yet available from console.");
      return true;
    }
    String wn = p.getWorld().getName();
    if ((isPlayer) && (args.length == 0) && ((!useperms) || ((useperms) && ((permission.has(p, "taien.th.hunt." + p.getWorld().getName())) || (permission.has(p, "taien.th.hunt.*")))))) {
      if ((lastcheck.containsKey(p)) && (((Long)lastcheck.get(p)).longValue() >= System.currentTimeMillis() - 1000 * checksec)) {
        p.sendMessage(ChatColor.DARK_RED + "You can only check for the closest chest once every " + checksec + " seconds.");
        return true;
      }
      getClosestHunt(p, true);
      lastcheck.put(p, Long.valueOf(System.currentTimeMillis()));
      return true;
    }

    if ((args.length == 1) && (((!useperms) && (sender.isOp())) || ((useperms) && (permission.has(p, "taien.th.admin"))))) {
      if ((!args[0].equalsIgnoreCase("help")) && (!args[0].equalsIgnoreCase("?"))) {
        if ((!args[0].equalsIgnoreCase("reload")) && (!args[0].equalsIgnoreCase("load"))) {
          if (args[0].equalsIgnoreCase("save")) {
            saveProcedure();
            p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " Saved items/options.");
            return true;
          }if (args[0].equalsIgnoreCase("list")) {
            p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + "-----------------------------------");
            Iterator i5 = huntList.iterator();

            while (i5.hasNext()) {
              THHunt o4 = (THHunt)i5.next();
              Location oo3 = o4.getLocation();
              if (o4.isLocked())
                p.sendMessage("" + ChatColor.YELLOW + oo3.getBlockX() + "," + oo3.getBlockY() + "," + oo3.getBlockZ() + "(" + oo3.getWorld().getName() + ")" + ChatColor.WHITE + " - " + ChatColor.DARK_RED + "FOUND(" + o4.getMinutesLeft() + " mins to fade)");
              else {
                p.sendMessage("" + ChatColor.YELLOW + oo3.getBlockX() + "," + oo3.getBlockY() + "," + oo3.getBlockZ() + "(" + oo3.getWorld().getName() + ")" + ChatColor.WHITE + " - " + ChatColor.GREEN + "Value " + o4.getValue() + ChatColor.WHITE + " - " + ChatColor.BLUE + o4.getMinutesLeft() + " mins");
              }
            }

            return true;
          }if (!args[0].equalsIgnoreCase("settings")) {
            if (args[0].equalsIgnoreCase("cb")) {
              Block o3 = p.getTargetBlock((HashSet)null, 10);
              boolean i3 = false;
              if (compassblocks.contains(o3)) {
                compassblocks.remove(o3);
              } else {
                compassblocks.add(o3);
                i3 = true;
              }

              if (i3)
                p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " Added " + ChatColor.YELLOW + o3.getX() + ", " + o3.getY() + ", " + o3.getZ() + ChatColor.WHITE + " in " + ChatColor.YELLOW + o3.getWorld().getName() + ChatColor.WHITE + " as a compass block.");
              else {
                p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " Removed " + ChatColor.YELLOW + o3.getX() + ", " + o3.getY() + ", " + o3.getZ() + ChatColor.WHITE + " in " + ChatColor.YELLOW + o3.getWorld().getName() + ChatColor.WHITE + " as a compass block.");
              }

              return true;
            }if (args[0].equalsIgnoreCase("usecb")) {
              usecompass = !usecompass;
              p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " Compass use toggled to " + ChatColor.YELLOW + usecompass + ChatColor.WHITE + ".");
              return true;
            }if (args[0].equalsIgnoreCase("center")) {
              if (!worlds.containsKey(wn)) {
                p.sendMessage(ChatColor.DARK_RED + "Your current world is not set up for chest generation.  Use /hunt addworld.");
                return true;
              }
              THWorldOpts o = (THWorldOpts)worlds.get(wn);
              o.centerx = p.getLocation().getBlockX();
              o.centerz = p.getLocation().getBlockZ();
              p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " Center of treasure generation set to " + ChatColor.YELLOW + o.centerx + ", " + o.centerz + ChatColor.WHITE + ".");
              return true;
            }
            if (args[0].equalsIgnoreCase("tool")) {
              if (!worlds.containsKey(wn)) {
                p.sendMessage(ChatColor.DARK_RED + "Your current world is not set up for chest generation.  Use /hunt addworld.");
                return true;
              }if ((p.getItemInHand() != null) && (p.getItemInHand().getType() != Material.AIR)) {
                THWorldOpts o = (THWorldOpts)worlds.get(wn);
                o.hunttool = p.getItemInHand().getType();
                p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " Hunt tool set to " + ChatColor.YELLOW + o.hunttool.name() + ChatColor.WHITE + ".");
                return true;
              }
              p.sendMessage(ChatColor.DARK_RED + "You are not holding an item!");
              return true;
            }
            if (args[0].equalsIgnoreCase("offeringtool")) {
              if (!worlds.containsKey(wn)) {
                p.sendMessage(ChatColor.DARK_RED + "Your current world is not set up for chest generation.  Use /hunt addworld.");
                return true;
              }if ((p.getItemInHand() != null) && (p.getItemInHand().getType() != Material.AIR)) {
                THWorldOpts o = (THWorldOpts)worlds.get(wn);
                o.offeringtool = p.getItemInHand().getType();
                p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " Offering tool set to " + ChatColor.YELLOW + o.hunttool.name() + ChatColor.WHITE + ".");
                return true;
              }
              p.sendMessage(ChatColor.DARK_RED + "You are not holding an item!");
              return true;
            }
            if (args[0].equalsIgnoreCase("marker")) {
              if (!worlds.containsKey(wn)) {
                p.sendMessage(ChatColor.DARK_RED + "Your current world is not set up for chest generation.  Use /hunt addworld.");
                return true;
              }if ((p.getItemInHand() != null) && (p.getItemInHand().getType() != Material.AIR)) {
                THWorldOpts o = (THWorldOpts)worlds.get(wn);
                o.markerblock = p.getItemInHand().getType();
                p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " Marker block set to " + ChatColor.YELLOW + o.markerblock.name() + ChatColor.WHITE + ".");
                return true;
              }
              p.sendMessage(ChatColor.DARK_RED + "You are not holding an item!");
              return true;
            }
            if (args[0].equalsIgnoreCase("addworld")) {
              if (worlds.containsKey(wn)) {
                p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " This world is already set up.");
              } else {
                worlds.put(wn, new THWorldOpts());
                p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " Treasure world " + ChatColor.YELLOW + wn + ChatColor.WHITE + " added.");
              }

              return true;
            }if (args[0].equalsIgnoreCase("removeworld")) {
              World o1 = p.getWorld();
              if (worlds.containsKey(o1)) {
                worlds.remove(o1);
                p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " Treasure world " + ChatColor.YELLOW + o1.getName() + ChatColor.WHITE + " removed.");
              } else {
                p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " This world is already not a treasure world.");
              }

              return true;
            }
            return false;
          }

          p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "] " + ChatColor.WHITE + "GENERAL OPTIONS--------------------");
          String o2 = "";
          THWorldOpts i2 = null;
          String oo1;
          for (Iterator m = worlds.keySet().iterator(); m.hasNext(); o2 = o2 + oo1 + " ") {
            oo1 = (String)m.next();
          }

          p.sendMessage(ChatColor.DARK_AQUA + "Worlds: " + ChatColor.WHITE + o2);
          p.sendMessage(ChatColor.DARK_AQUA + "Protections: " + ChatColor.AQUA + "Break: " + ChatColor.WHITE + protectbreak + ChatColor.AQUA + " Burn: " + ChatColor.WHITE + protectburn + ChatColor.AQUA + " Explode: " + ChatColor.WHITE + protectexplode + ChatColor.AQUA + " Piston: " + ChatColor.WHITE + protectpiston);
          p.sendMessage(ChatColor.AQUA + "MinPlayers: " + ChatColor.WHITE + minplayers + " online" + ChatColor.AQUA + " | CompassBlocks: " + ChatColor.WHITE + usecompass);
          p.sendMessage(ChatColor.AQUA + "MaxSpawnAttempts: " + ChatColor.WHITE + maxspawnattempts + ChatColor.AQUA + " | DetailLogs: " + ChatColor.WHITE + detaillogs);
          p.sendMessage(ChatColor.AQUA + "FoundChestFadeTime: " + ChatColor.WHITE + foundchestfadetime + " min" + ChatColor.AQUA + " | ThreeDimensionalDistance: " + ChatColor.WHITE + threedimensionaldistance);
          p.sendMessage(ChatColor.AQUA + "CheckSec: " + ChatColor.WHITE + checksec + " sec" + ChatColor.AQUA + " | ItemsLists: " + ChatColor.WHITE + (worldlists.size() + customlists.size()) + ChatColor.AQUA + " | Enchants: " + ChatColor.WHITE + enchanted.size());
          p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "Current World" + ChatColor.YELLOW + "] " + ChatColor.WHITE + p.getWorld().getName().toUpperCase() + "--------------------");
          if (!worlds.containsKey(wn)) {
            p.sendMessage(ChatColor.YELLOW + "Current world is not set up for chest generation.  Use /hunt addworld.");
            return true;
          }
          i2 = (THWorldOpts)worlds.get(wn);
          if (worldlists.get(i2.itemlist) == null) {
            p.sendMessage(ChatColor.DARK_AQUA + " ** ItemList: " + ChatColor.WHITE + i2.itemlist + " (ERROR:  List doesn't exist)");
          } else {
            THWorldList oo2 = (THWorldList)worldlists.get(i2.itemlist);
            p.sendMessage(ChatColor.DARK_AQUA + " ** ItemList: " + ChatColor.WHITE + i2.itemlist + " (" + (oo2.common.size() + oo2.uncommon.size() + oo2.rare.size() + oo2.legendary.size() + oo2.epic.size()) + " items in list)");
          }

          p.sendMessage(ChatColor.DARK_AQUA + " ** Enabled: " + ChatColor.WHITE + i2.enabled + ChatColor.DARK_AQUA + " | Strict Items: " + ChatColor.WHITE + i2.strictitems + ChatColor.DARK_AQUA + " | MinChests: " + ChatColor.WHITE + i2.minchests);
          p.sendMessage(ChatColor.DARK_AQUA + "WEIGHTS: " + ChatColor.AQUA + "Value: " + ChatColor.WHITE + i2.drawweight + " draws" + ChatColor.AQUA + " Items: " + ChatColor.WHITE + i2.gooditemweight + " draws" + ChatColor.AQUA + " Amounts: " + ChatColor.WHITE + i2.amountweight + " draws");
          p.sendMessage(ChatColor.AQUA + "Center: " + ChatColor.WHITE + i2.centerx + "," + i2.centerz + ChatColor.AQUA + " | Distance: " + ChatColor.WHITE + i2.mindistance + "-" + i2.maxdistance + ChatColor.AQUA + " | Duration: " + ChatColor.WHITE + i2.duration + " mins");
          p.sendMessage(ChatColor.AQUA + "Chance: " + ChatColor.WHITE + "1:" + i2.chance + ChatColor.AQUA + " per " + ChatColor.WHITE + i2.interval + " sec" + ChatColor.AQUA + " | Min/Max Light: " + ChatColor.WHITE + i2.minlight + "-" + i2.maxlight);
          p.sendMessage(ChatColor.AQUA + "Min/Max Elevation: " + ChatColor.WHITE + i2.minelevation + "-" + i2.maxelevation + ChatColor.AQUA + "   Max Elevation(Rares): " + ChatColor.WHITE + i2.maxelevationrare);
          p.sendMessage(ChatColor.AQUA + "Min Money: " + ChatColor.WHITE + i2.minmoney + ChatColor.AQUA + " | Money Multiplier: " + ChatColor.WHITE + "x" + this.ratiopercent.format(i2.moneymultiplier) + ChatColor.AQUA + " | Max Compass Dist: " + ChatColor.WHITE + i2.maxcompassdistance);
          String oo1 = "";
          Material m1;
          for (Iterator s = i2.spawnableblocks.iterator(); s.hasNext(); oo1 = oo1 + m1.name() + " ") {
            m1 = (Material)s.next();
          }

          p.sendMessage(ChatColor.AQUA + "Spawns on: " + ChatColor.WHITE + oo1);
          if (i2.fadeblock == null)
            p.sendMessage(ChatColor.AQUA + "Hunt Tool: " + ChatColor.WHITE + i2.hunttool.name() + ChatColor.AQUA + " | Marker Block: " + ChatColor.WHITE + i2.markerblock.name() + ChatColor.AQUA + " | Fade Block: " + ChatColor.WHITE + "RETURN");
          else {
            p.sendMessage(ChatColor.AQUA + "Hunt Tool: " + ChatColor.WHITE + i2.hunttool.name() + ChatColor.AQUA + " | Marker Block: " + ChatColor.WHITE + i2.markerblock.name() + ChatColor.AQUA + " | Fade Block: " + ChatColor.WHITE + i2.fadeblock.name());
          }

          p.sendMessage(ChatColor.AQUA + "Use Marker Block: " + ChatColor.WHITE + i2.usemarker + ChatColor.AQUA + " | Override MinPlayers: " + ChatColor.WHITE + i2.overrideminplayers);
          return true;
        }

        loadProcedure();
        p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " Reloaded items/options.");
        return true;
      }

      p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + "-----------------------------------");
      p.sendMessage(ChatColor.YELLOW + "/hunt reload|load" + ChatColor.GRAY + " - reload from config, overwriting settings.");
      p.sendMessage(ChatColor.YELLOW + "/hunt save" + ChatColor.GRAY + " - save settings to the config file.");
      p.sendMessage(ChatColor.YELLOW + "/hunt settings" + ChatColor.GRAY + " - view current chest settings");
      p.sendMessage(ChatColor.YELLOW + "/hunt list" + ChatColor.GRAY + " - list info on all active hunts");
      p.sendMessage(ChatColor.YELLOW + "/hunt center" + ChatColor.GRAY + " - set current loc as the center of spawning.");
      p.sendMessage(ChatColor.YELLOW + "/hunt addworld" + ChatColor.GRAY + " - add your current world as a chest world.");
      p.sendMessage(ChatColor.YELLOW + "/hunt removeworld" + ChatColor.GRAY + " - remove your current world as a chest world.");
      p.sendMessage(ChatColor.YELLOW + "/hunt maxdist <int>" + ChatColor.GRAY + " - set max dist of chests from center.");
      p.sendMessage(ChatColor.YELLOW + "/hunt mindist <int>" + ChatColor.GRAY + " - set min dist of chests from center.");
      p.sendMessage(ChatColor.YELLOW + "/hunt duration <int>" + ChatColor.GRAY + " - set time in mins until chests fade.");
      p.sendMessage(ChatColor.YELLOW + "/hunt chance <int>" + ChatColor.GRAY + " - set spawn chance to 1 in <int>.");
      p.sendMessage(ChatColor.YELLOW + "/hunt weight <int>" + ChatColor.GRAY + " - set weight of chest draws(more = lower val).");
      p.sendMessage(ChatColor.YELLOW + "/hunt interval <int>" + ChatColor.GRAY + " - set interval in secs between spawn checks.");
      p.sendMessage(ChatColor.GREEN + "/hunt help 2 or /hunt ? 2 for more help");
      return true;
    }
    if ((args.length == 2) && (((!useperms) && (sender.isOp())) || ((useperms) && (permission.has(p, "taien.th.admin"))))) {
      if ((!args[0].equalsIgnoreCase("help")) && (!args[0].equalsIgnoreCase("?"))) {
        if (args[0].equalsIgnoreCase("addenchant")) {
          String o2 = args[1];
          if ((!o2.contains(":")) && (!o2.contains("'")) && (!o2.contains(".")) && (!enchanted.containsKey(args[1]))) {
            ItemStack i4 = p.getItemInHand();
            if ((i4 != null) && (i4.getType() != Material.AIR)) {
              if (i4.getEnchantments().size() == 0) {
                p.sendMessage(ChatColor.DARK_RED + "You are not holding an enchanted item!");
                return true;
              }
              p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " Added enchantment setup '" + ChatColor.YELLOW + o2 + ChatColor.WHITE + "'.");
              enchanted.put(o2, i4);
              return true;
            }

            p.sendMessage(ChatColor.DARK_RED + "You are not holding anything!");
            return true;
          }

          p.sendMessage(ChatColor.DARK_RED + "That is an invalid name for an enchant, or it already exists.");
          return true;
        }
        if (args[0].equalsIgnoreCase("usemarker")) {
          if (!worlds.containsKey(wn)) {
            p.sendMessage(ChatColor.DARK_RED + "Your current world is not set up for chest generation.  Use /hunt addworld.");
            return true;
          }
          THWorldOpts o = (THWorldOpts)worlds.get(wn);
          boolean i3 = Boolean.parseBoolean(args[1]);
          o.usemarker = i3;
          p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " This world's marker use set to " + ChatColor.YELLOW + i3 + ChatColor.WHITE + ".");
          return true;
        }
        if (args[0].equalsIgnoreCase("enable")) {
          if (!worlds.containsKey(wn)) {
            p.sendMessage(ChatColor.DARK_RED + "Your current world is not set up for chest generation.  Use /hunt addworld.");
            return true;
          }
          THWorldOpts o = (THWorldOpts)worlds.get(wn);
          boolean i3 = Boolean.parseBoolean(args[1]);
          o.enabled = i3;
          p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " This world's random treasure generation set to " + ChatColor.YELLOW + i3 + ChatColor.WHITE + ".");
          return true;
        }
        if (args[0].equalsIgnoreCase("strictitems")) {
          if (!worlds.containsKey(wn)) {
            p.sendMessage(ChatColor.DARK_RED + "Your current world is not set up for chest generation.  Use /hunt addworld.");
            return true;
          }
          THWorldOpts o = (THWorldOpts)worlds.get(wn);
          boolean i3 = Boolean.parseBoolean(args[1]);
          o.strictitems = i3;
          p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " This world's item strictness set to " + ChatColor.YELLOW + i3 + ChatColor.WHITE + ".");
          return true;
        }
        if (args[0].equalsIgnoreCase("overrideminplayers")) {
          if (!worlds.containsKey(wn)) {
            p.sendMessage(ChatColor.DARK_RED + "Your current world is not set up for chest generation.  Use /hunt addworld.");
            return true;
          }
          THWorldOpts o = (THWorldOpts)worlds.get(wn);
          boolean i3 = Boolean.parseBoolean(args[1]);
          o.overrideminplayers = i3;
          p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " Override MinPlayers to reach MinChests set to " + ChatColor.YELLOW + i3 + ChatColor.WHITE + ".");
          return true;
        }
        if (args[0].equalsIgnoreCase("fadefoundchests")) {
          if (!worlds.containsKey(wn)) {
            p.sendMessage(ChatColor.DARK_RED + "Your current world is not set up for chest generation.  Use /hunt addworld.");
            return true;
          }
          THWorldOpts o = (THWorldOpts)worlds.get(wn);
          boolean i3 = Boolean.parseBoolean(args[1]);
          o.fadefoundchests = i3;
          p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " Fading of found chests set to " + ChatColor.YELLOW + i3 + ChatColor.WHITE + ".");
          return true;
        }
        if (args[0].equalsIgnoreCase("copyworld")) {
          if (!worlds.containsKey(wn)) {
            p.sendMessage(ChatColor.DARK_RED + "Your current world is not set up for chest generation.  Use /hunt addworld.");
            return true;
          }
          THWorldOpts o = null;
          World o1 = server.getWorld(args[1]);
          if (o1 == null) {
            p.sendMessage(ChatColor.DARK_RED + "That world doesn't appear to exist.");
            return true;
          }if (!worlds.containsKey(o1)) {
            p.sendMessage(ChatColor.DARK_RED + "The world you're trying to copy from is not set up for chest generation.");
            return true;
          }
          THWorldOpts i2 = (THWorldOpts)worlds.get(wn);
          THWorldOpts oo = (THWorldOpts)worlds.get(o1);
          i2.chance = oo.chance;
          i2.consumechance = oo.consumechance;
          i2.drawweight = oo.drawweight;
          i2.gooditemweight = oo.gooditemweight;
          i2.amountweight = oo.amountweight;
          i2.duration = oo.duration;
          i2.enabled = oo.enabled;
          i2.hunttool = oo.hunttool;
          i2.interval = oo.interval;
          i2.lastcheck = oo.lastcheck;
          i2.markerblock = oo.markerblock;
          i2.maxelevation = oo.maxelevation;
          i2.maxlight = oo.maxlight;
          i2.maxelevationrare = oo.maxelevationrare;
          i2.maxvalue = oo.maxvalue;
          i2.minelevation = oo.minelevation;
          i2.minlight = oo.minlight;
          i2.minmoney = oo.minmoney;
          i2.moneymultiplier = oo.moneymultiplier;
          i2.spawnableblocks = new LinkedList(oo.spawnableblocks);
          i2.usemarker = oo.usemarker;
          i2.strictitems = oo.strictitems;
          i2.maxcompassdistance = oo.maxcompassdistance;
          i2.fadefoundchests = oo.fadefoundchests;
          i2.minchests = oo.minchests;
          i2.offeramount = oo.offeramount;
          i2.offeringtool = oo.offeringtool;
          i2.overrideminplayers = oo.overrideminplayers;
          p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " All settings except maxdist, mindist, centerx, and centerz have been copied from world " + args[1] + ".");
          return true;
        }

        if (args[0].equalsIgnoreCase("maxvalue")) {
          if (!worlds.containsKey(wn)) {
            p.sendMessage(ChatColor.DARK_RED + "Your current world is not set up for chest generation.  Use /hunt addworld.");
            return true;
          }
          THWorldOpts o = (THWorldOpts)worlds.get(wn);
          int i = Integer.parseInt(args[1]);
          if (i < 0)
            i = 0;
          else if (i > 5000) {
            i = 5000;
          }

          o.maxvalue = i;
          p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " Max value of treasure set to " + ChatColor.YELLOW + i + ChatColor.WHITE + ".");
          return true;
        }
        if (args[0].equalsIgnoreCase("minlight")) {
          if (!worlds.containsKey(wn)) {
            p.sendMessage(ChatColor.DARK_RED + "Your current world is not set up for chest generation.  Use /hunt addworld.");
            return true;
          }
          THWorldOpts o = (THWorldOpts)worlds.get(wn);
          int i = Integer.parseInt(args[1]);
          if (i < 0) {
            i = 0;
          }

          if (i > 14) {
            i = 14;
          }

          if (i > o.maxlight) {
            i = o.maxlight;
          }

          o.minlight = i;
          p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " Min lightlevel set to " + ChatColor.YELLOW + i + ChatColor.WHITE + ".");
          return true;
        }
        if (args[0].equalsIgnoreCase("maxlight")) {
          if (!worlds.containsKey(wn)) {
            p.sendMessage(ChatColor.DARK_RED + "Your current world is not set up for chest generation.  Use /hunt addworld.");
            return true;
          }
          THWorldOpts o = (THWorldOpts)worlds.get(wn);
          int i = Integer.parseInt(args[1]);
          if (i > 14) {
            i = 14;
          }

          if (i < 0) {
            i = 0;
          }

          if (i < o.minlight) {
            i = o.minlight;
          }

          o.maxlight = i;
          p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " Max lightlevel set to " + ChatColor.YELLOW + i + ChatColor.WHITE + ".");
          return true;
        }
        if (args[0].equalsIgnoreCase("minchests")) {
          if (!worlds.containsKey(wn)) {
            p.sendMessage(ChatColor.DARK_RED + "Your current world is not set up for chest generation.  Use /hunt addworld.");
            return true;
          }
          THWorldOpts o = (THWorldOpts)worlds.get(wn);
          int i = Integer.parseInt(args[1]);
          if (i < 0) {
            i = 0;
          }

          o.minchests = i;
          p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " Minimum amount of chests set to " + ChatColor.YELLOW + i + ChatColor.WHITE + ".");
          return true;
        }
        if (args[0].equalsIgnoreCase("offeramount")) {
          if (!worlds.containsKey(wn)) {
            p.sendMessage(ChatColor.DARK_RED + "Your current world is not set up for chest generation.  Use /hunt addworld.");
            return true;
          }
          THWorldOpts o = (THWorldOpts)worlds.get(wn);
          int i = Integer.parseInt(args[1]);
          if (i < 1) {
            i = 1;
          }

          o.offeramount = i;
          p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " Amount of items needed for offering set to " + ChatColor.YELLOW + i + ChatColor.WHITE + ".");
          return true;
        }
        if (args[0].equalsIgnoreCase("amountweight")) {
          if (!worlds.containsKey(wn)) {
            p.sendMessage(ChatColor.DARK_RED + "Your current world is not set up for chest generation.  Use /hunt addworld.");
            return true;
          }
          THWorldOpts o = (THWorldOpts)worlds.get(wn);
          int i = Integer.parseInt(args[1]);
          if (i < 1)
            i = 1;
          else if (i > 5000) {
            i = 100;
          }

          o.amountweight = i;
          p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " Amount weight of treasure set to " + ChatColor.YELLOW + i + ChatColor.WHITE + ".");
          return true;
        }
        if (args[0].equalsIgnoreCase("maxdist")) {
          if (!worlds.containsKey(wn)) {
            p.sendMessage(ChatColor.DARK_RED + "Your current world is not set up for chest generation.  Use /hunt addworld.");
            return true;
          }
          THWorldOpts o = (THWorldOpts)worlds.get(wn);
          int i = Integer.parseInt(args[1]);
          if (i < 0) {
            i = 0;
          }

          o.maxdistance = i;
          p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " Max distance of treasure from center set to " + ChatColor.YELLOW + i + ChatColor.WHITE + ".");
          return true;
        }
        if (args[0].equalsIgnoreCase("maxcompassdist")) {
          if (!worlds.containsKey(wn)) {
            p.sendMessage(ChatColor.DARK_RED + "Your current world is not set up for chest generation.  Use /hunt addworld.");
            return true;
          }
          THWorldOpts o = (THWorldOpts)worlds.get(wn);
          int i = Integer.parseInt(args[1]);
          if (i < 0) {
            i = 0;
          }

          o.maxcompassdistance = i;
          p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " Max compass block reach distance set to " + ChatColor.YELLOW + i + ChatColor.WHITE + ".");
          return true;
        }
        if (args[0].equalsIgnoreCase("mindist")) {
          if (!worlds.containsKey(wn)) {
            p.sendMessage(ChatColor.DARK_RED + "Your current world is not set up for chest generation.  Use /hunt addworld.");
            return true;
          }
          THWorldOpts o = (THWorldOpts)worlds.get(wn);
          int i = Integer.parseInt(args[1]);
          if (i < 0) {
            i = 0;
          }

          o.mindistance = i;
          p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " Min distance of treasure from center set to " + ChatColor.YELLOW + i + ChatColor.WHITE + ".");
          return true;
        }
        if (args[0].equalsIgnoreCase("minmoney")) {
          if (!worlds.containsKey(wn)) {
            p.sendMessage(ChatColor.DARK_RED + "Your current world is not set up for chest generation.  Use /hunt addworld.");
            return true;
          }
          THWorldOpts o = (THWorldOpts)worlds.get(wn);
          int i = Integer.parseInt(args[1]);
          if (i < 0) {
            i = 0;
          }

          o.minmoney = i;
          p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " Min money in a chest set to " + ChatColor.YELLOW + i + ChatColor.WHITE + ".");
          return true;
        }
        if (args[0].equalsIgnoreCase("consumechance")) {
          if (!worlds.containsKey(wn)) {
            p.sendMessage(ChatColor.DARK_RED + "Your current world is not set up for chest generation.  Use /hunt addworld.");
            return true;
          }
          THWorldOpts o = (THWorldOpts)worlds.get(wn);
          int i = Integer.parseInt(args[1]);
          if (i < 0) {
            i = 0;
          }

          if (i > 100) {
            i = 100;
          }

          o.consumechance = i;
          p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " Chance of hunt tool consumption set to " + ChatColor.YELLOW + i + ChatColor.WHITE + ".");
          return true;
        }
        if (args[0].equalsIgnoreCase("moneymultiplier")) {
          if (!worlds.containsKey(wn)) {
            p.sendMessage(ChatColor.DARK_RED + "Your current world is not set up for chest generation.  Use /hunt addworld.");
            return true;
          }
          THWorldOpts o = (THWorldOpts)worlds.get(wn);
          double i1 = Double.parseDouble(args[1]);
          if (i1 < 0.0D) {
            i1 = 0.0D;
          }

          o.moneymultiplier = i1;
          p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " Max money multiplier set to " + ChatColor.YELLOW + i1 + ChatColor.WHITE + "x chest value.");
          return true;
        }
        if (args[0].equalsIgnoreCase("duration")) {
          if (!worlds.containsKey(wn)) {
            p.sendMessage(ChatColor.DARK_RED + "Your current world is not set up for chest generation.  Use /hunt addworld.");
            return true;
          }
          THWorldOpts o = (THWorldOpts)worlds.get(wn);
          int i = Integer.parseInt(args[1]);
          if (i < 5) {
            i = 5;
          }

          o.duration = i;
          p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " Duration of new hunts set to " + ChatColor.YELLOW + i + ChatColor.WHITE + " minutes.");
          return true;
        }
        if (args[0].equalsIgnoreCase("chance")) {
          if (!worlds.containsKey(wn)) {
            p.sendMessage(ChatColor.DARK_RED + "Your current world is not set up for chest generation.  Use /hunt addworld.");
            return true;
          }
          THWorldOpts o = (THWorldOpts)worlds.get(wn);
          int i = Integer.parseInt(args[1]);
          if (i < 1) {
            i = 1;
          }

          o.chance = i;
          p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " Chance of chests spawning set to 1 in " + ChatColor.YELLOW + i + ChatColor.WHITE + ".");
          return true;
        }
        if (args[0].equalsIgnoreCase("weight")) {
          if (!worlds.containsKey(wn)) {
            p.sendMessage(ChatColor.DARK_RED + "Your current world is not set up for chest generation.  Use /hunt addworld.");
            return true;
          }
          THWorldOpts o = (THWorldOpts)worlds.get(wn);
          int i = Integer.parseInt(args[1]);
          if (i < 1) {
            i = 1;
          }

          o.drawweight = i;
          p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " Draw weight of chests set to " + ChatColor.YELLOW + i + ChatColor.WHITE + " draws.");
          return true;
        }
        if (args[0].equalsIgnoreCase("itemweight")) {
          if (!worlds.containsKey(wn)) {
            p.sendMessage(ChatColor.DARK_RED + "Your current world is not set up for chest generation.  Use /hunt addworld.");
            return true;
          }
          THWorldOpts o = (THWorldOpts)worlds.get(wn);
          int i = Integer.parseInt(args[1]);
          if (i < 1) {
            i = 1;
          }

          o.gooditemweight = i;
          p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " Weight of good items in chests set to " + ChatColor.YELLOW + i + ChatColor.WHITE + " draws.");
          return true;
        }
        if (args[0].equalsIgnoreCase("interval")) {
          if (!worlds.containsKey(wn)) {
            p.sendMessage(ChatColor.DARK_RED + "Your current world is not set up for chest generation.  Use /hunt addworld.");
            return true;
          }
          THWorldOpts o = (THWorldOpts)worlds.get(wn);
          int i = Integer.parseInt(args[1]);
          if (i < 5) {
            i = 5;
          }

          o.interval = i;
          p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + " Interval of chest draws set to " + ChatColor.YELLOW + i + ChatColor.WHITE + " seconds.");
          return true;
        }

        return false;
      }
      if (args[1].equalsIgnoreCase("2")) {
        p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + "-----------------------------------");
        p.sendMessage(ChatColor.YELLOW + "/hunt minmoney <int>" + ChatColor.GRAY + " - set minimum money found in chests.");
        p.sendMessage(ChatColor.YELLOW + "/hunt consumechance <int>" + ChatColor.GRAY + " - set chance of tool being consumed.");
        p.sendMessage(ChatColor.YELLOW + "/hunt maxcompassdist <int>" + ChatColor.GRAY + " - set max distance compass blocks work.");
        p.sendMessage(ChatColor.YELLOW + "/hunt moneymultiplier <decimal>" + ChatColor.GRAY + " - set money multiplier (X value).");
        p.sendMessage(ChatColor.YELLOW + "/hunt itemweight <int>" + ChatColor.GRAY + " - set good item weight.");
        p.sendMessage(ChatColor.YELLOW + "/hunt tool" + ChatColor.GRAY + " - set hunt tool to held item.");
        p.sendMessage(ChatColor.YELLOW + "/hunt cb" + ChatColor.GRAY + " - toggle block you're looking at as compass block.");
        p.sendMessage(ChatColor.YELLOW + "/hunt usecb" + ChatColor.GRAY + " - toggle whether to allow compass use or not.");
        p.sendMessage(ChatColor.YELLOW + "/hunt maxvalue <int>" + ChatColor.GRAY + " - set max value of chests.");
        p.sendMessage(ChatColor.YELLOW + "/hunt marker" + ChatColor.GRAY + " - set marker under chests to held item.");
        p.sendMessage(ChatColor.YELLOW + "/hunt usemarker <true/false>" + ChatColor.GRAY + " - use chest marker.");
        p.sendMessage(ChatColor.YELLOW + "/hunt enable <true/false>" + ChatColor.GRAY + " - enable/disable this world.");
        p.sendMessage(ChatColor.YELLOW + "/hunt copyworld <world>" + ChatColor.GRAY + " - copy <world> settings to this world.");
        p.sendMessage(ChatColor.GREEN + "/hunt help 3 or /hunt ? 3 for more help");
        return true;
      }if (args[1].equalsIgnoreCase("3")) {
        p.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "TreasureHunt" + ChatColor.YELLOW + "]" + ChatColor.WHITE + "-----------------------------------");
        p.sendMessage(ChatColor.YELLOW + "/hunt minchests <int>" + ChatColor.GRAY + " - set minimum chests in world.");
        p.sendMessage(ChatColor.YELLOW + "/hunt overrideminplayers <true/false>" + ChatColor.GRAY + " - override minplayers to reach minchests.");
        p.sendMessage(ChatColor.YELLOW + "/hunt offeramount <int>" + ChatColor.GRAY + " - set amount of offer item needed to set compass.");
        p.sendMessage(ChatColor.YELLOW + "/hunt fadefoundchests <true/false>" + ChatColor.GRAY + " - remove chests that have been found.");
        p.sendMessage(ChatColor.YELLOW + "/hunt strictitems <true/false>" + ChatColor.GRAY + " - set rarity-level strictness.");
        p.sendMessage(ChatColor.YELLOW + "/hunt offeringtool" + ChatColor.GRAY + " - set offering tool to item in hand.");
        p.sendMessage(ChatColor.YELLOW + "/hunt addenchant <string>" + ChatColor.GRAY + " - add an enchantment setup using the held item.");
        p.sendMessage(ChatColor.GREEN + "* TreasureHunt v" + version + " by " + ChatColor.DARK_PURPLE + "Taien");
        p.sendMessage(ChatColor.GREEN + "* taienverdain@gmail.com for paypal donations/suggestions/feedback!");
        return true;
      }
      return false;
    }

    return false;
  }

  public static enum LookDirection
  {
    POSX, 
    POSZ, 
    NEGX, 
    NEGZ, 

    field_0, 
    DOWN;
  }
}