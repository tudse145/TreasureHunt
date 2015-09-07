package me.taien.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Config
{
  private final String fileName;
  private final JavaPlugin plugin;
  private File configFile;
  private FileConfiguration fileConfiguration;

  public Config(JavaPlugin plugin, String fileName)
  {
    if (plugin == null)
      throw new IllegalArgumentException("plugin cannot be null");
    if (!plugin.isInitialized()) {
      throw new IllegalArgumentException("plugin must be initialized");
    }
    this.plugin = plugin;
    this.fileName = fileName;
  }

  public void loadConfig()
  {
    if (this.configFile == null) {
      File defConfigStream = this.plugin.getDataFolder();
      if (defConfigStream == null) {
        throw new IllegalStateException();
      }

      this.configFile = new File(defConfigStream, this.fileName);
    }

    if (this.configFile.exists()) {
      this.fileConfiguration = YamlConfiguration.loadConfiguration(this.configFile);
    } else {
      InputStream defConfigStream1 = this.plugin.getResource(this.fileName);
      this.fileConfiguration = YamlConfiguration.loadConfiguration(defConfigStream1);
    }
  }

  public FileConfiguration getConfig()
  {
    if (this.fileConfiguration == null) {
      loadConfig();
    }

    return this.fileConfiguration;
  }

  public void saveConfig() {
    if ((this.fileConfiguration != null) && (this.configFile != null))
      try {
        getConfig().save(this.configFile);
      } catch (IOException ioexception) {
        this.plugin.getLogger().log(Level.SEVERE, "Could not save config to " + this.configFile, ioexception);
      }
  }

  public void saveDefaultConfig()
  {
    if (!this.configFile.exists())
      this.plugin.saveResource(this.fileName, false);
  }
}