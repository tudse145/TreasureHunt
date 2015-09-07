package me.taien;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.YamlConfigurationOptions;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.scheduler.BukkitScheduler;

public class Metrics
{
  private static final int REVISION = 5;
  private static final String BASE_URL = "http://mcstats.org";
  private static final String REPORT_URL = "/report/%s";
  private static final String CUSTOM_DATA_SEPARATOR = "~~";
  private static final int PING_INTERVAL = 10;
  private final Plugin plugin;
  private final Set graphs = Collections.synchronizedSet(new HashSet());
  private final Graph defaultGraph = new Graph("Default", (Graph)null);
  private final YamlConfiguration configuration;
  private final File configurationFile;
  private final String guid;
  private final Object optOutLock = new Object();
  private volatile int taskId = -1;

  public Metrics(Plugin plugin) throws IOException {
    if (plugin == null) {
      throw new IllegalArgumentException("Plugin cannot be null");
    }
    this.plugin = plugin;
    this.configurationFile = getConfigFile();
    this.configuration = YamlConfiguration.loadConfiguration(this.configurationFile);
    this.configuration.addDefault("opt-out", Boolean.valueOf(false));
    this.configuration.addDefault("guid", UUID.randomUUID().toString());
    if (this.configuration.get("guid", (Object)null) == null) {
      this.configuration.options().header("http://mcstats.org").copyDefaults(true);
      this.configuration.save(this.configurationFile);
    }

    this.guid = this.configuration.getString("guid");
  }

  public Graph createGraph(String name)
  {
    if (name == null) {
      throw new IllegalArgumentException("Graph name cannot be null");
    }
    Graph graph = new Graph(name, (Graph)null);
    this.graphs.add(graph);
    return graph;
  }

  public void addGraph(Graph graph)
  {
    if (graph == null) {
      throw new IllegalArgumentException("Graph cannot be null");
    }
    this.graphs.add(graph);
  }

  public void addCustomData(Plotter plotter)
  {
    if (plotter == null) {
      throw new IllegalArgumentException("Plotter cannot be null");
    }
    this.defaultGraph.addPlotter(plotter);
    this.graphs.add(this.defaultGraph);
  }

  public boolean start()
  {
    Object object = this.optOutLock;
    synchronized (this.optOutLock) {
      if (isOptOut())
        return false;
      if (this.taskId >= 0) {
        return true;
      }
      this.taskId = this.plugin.getServer().getScheduler().scheduleAsyncRepeatingTask(this.plugin, new Object() {
        private boolean firstPost = true;

        public void run() {
          try {
            synchronized (Metrics.this.optOutLock) {
              if ((Metrics.this.isOptOut()) && (Metrics.this.taskId > 0)) {
                Metrics.this.plugin.getServer().getScheduler().cancelTask(Metrics.this.taskId);
                Metrics.this.taskId = -1;
                Iterator iterator = Metrics.this.graphs.iterator();

                while (iterator.hasNext()) {
                  Metrics.Graph graph = (Metrics.Graph)iterator.next();
                  graph.onOptOut();
                }
              }
            }

            Metrics.this.postPlugin(!this.firstPost);
            this.firstPost = false;
          } catch (IOException ioexception) {
            Bukkit.getLogger().log(Level.INFO, "[Metrics] " + ioexception.getMessage());
          }
        }
      }
      , 0L, 12000L);

      return true;
    }
  }

  public boolean isOptOut()
  {
    Object object = this.optOutLock;
    synchronized (this.optOutLock) {
      try {
        this.configuration.load(getConfigFile());
      } catch (IOException ioexception) {
        Bukkit.getLogger().log(Level.INFO, new StringBuilder().append("[Metrics] ").append(ioexception.getMessage()).toString());
        return true;
      } catch (InvalidConfigurationException invalidconfigurationexception) {
        Bukkit.getLogger().log(Level.INFO, new StringBuilder().append("[Metrics] ").append(invalidconfigurationexception.getMessage()).toString());
        return true;
      }

      return this.configuration.getBoolean("opt-out", false);
    }
  }

  public void enable() throws IOException {
    Object object = this.optOutLock;
    synchronized (this.optOutLock) {
      if (isOptOut()) {
        this.configuration.set("opt-out", Boolean.valueOf(false));
        this.configuration.save(this.configurationFile);
      }

      if (this.taskId < 0)
        start();
    }
  }

  public void disable()
    throws IOException
  {
    Object object = this.optOutLock;
    synchronized (this.optOutLock) {
      if (!isOptOut()) {
        this.configuration.set("opt-out", Boolean.valueOf(true));
        this.configuration.save(this.configurationFile);
      }

      if (this.taskId > 0) {
        this.plugin.getServer().getScheduler().cancelTask(this.taskId);
        this.taskId = -1;
      }
    }
  }

  public File getConfigFile()
  {
    File pluginsFolder = this.plugin.getDataFolder().getParentFile();
    return new File(new File(pluginsFolder, "PluginMetrics"), "config.yml");
  }

  private void postPlugin(boolean isPing) throws IOException {
    PluginDescriptionFile description = this.plugin.getDescription();
    StringBuilder data = new StringBuilder();
    data.append(encode("guid")).append('=').append(encode(this.guid));
    encodeDataPair(data, "version", description.getVersion());
    encodeDataPair(data, "server", Bukkit.getVersion());
    encodeDataPair(data, "players", Integer.toString(Bukkit.getServer().getOnlinePlayers().length));
    encodeDataPair(data, "revision", String.valueOf(5));
    if (isPing) {
      encodeDataPair(data, "ping", "true");
    }

    Set url = this.graphs;
    synchronized (this.graphs) {
      Iterator connection = this.graphs.iterator();

      while (connection.hasNext())
      {
        Graph writer = (Graph)connection.next();
        Iterator response = writer.getPlotters().iterator();

        while (response.hasNext()) {
          Plotter reader = (Plotter)response.next();
          String key = String.format("C%s%s%s%s", new Object[] { "~~", writer.getName(), "~~", reader.getColumnName() });
          String iter = Integer.toString(reader.getValue());
          encodeDataPair(data, key, iter);
        }
      }
    }

    URL url1 = new URL(new StringBuilder().append("http://mcstats.org").append(String.format("/report/%s", new Object[] { encode(this.plugin.getDescription().getName()) })).toString());
    URLConnection connection1;
    URLConnection connection1;
    if (isMineshafterPresent())
      connection1 = url1.openConnection(Proxy.NO_PROXY);
    else {
      connection1 = url1.openConnection();
    }

    connection1.setDoOutput(true);
    OutputStreamWriter writer1 = new OutputStreamWriter(connection1.getOutputStream());
    writer1.write(data.toString());
    writer1.flush();
    BufferedReader reader1 = new BufferedReader(new InputStreamReader(connection1.getInputStream()));
    String response1 = reader1.readLine();
    writer1.close();
    reader1.close();
    if ((response1 != null) && (!response1.startsWith("ERR"))) {
      if (response1.contains("OK This is your first update this hour")) {
        Set key1 = this.graphs;
        synchronized (this.graphs) {
          Iterator iter1 = this.graphs.iterator();

          while (iter1.hasNext()) {
            Graph graph = (Graph)iter1.next();
            Iterator iterator = graph.getPlotters().iterator();

            while (iterator.hasNext()) {
              Plotter plotter = (Plotter)iterator.next();
              plotter.reset();
            }
          }
        }
      }
    }
    else
      throw new IOException(response1);
  }

  private boolean isMineshafterPresent()
  {
    try {
      Class.forName("mineshafter.MineServer");
      return true; } catch (Exception exception) {
    }
    return false;
  }

  private static void encodeDataPair(StringBuilder buffer, String key, String value) throws UnsupportedEncodingException
  {
    buffer.append('&').append(encode(key)).append('=').append(encode(value));
  }

  private static String encode(String text) throws UnsupportedEncodingException {
    return URLEncoder.encode(text, "UTF-8");
  }

  public static abstract class Plotter
  {
    private final String name;

    public Plotter()
    {
      this("Default");
    }

    public Plotter(String name) {
      this.name = name;
    }

    public abstract int getValue();

    public String getColumnName() {
      return this.name;
    }

    public void reset() {
    }

    public int hashCode() {
      return getColumnName().hashCode();
    }

    public boolean equals(Object object) {
      if (!(object instanceof Plotter)) {
        return false;
      }
      Plotter plotter = (Plotter)object;
      return (plotter.name.equals(this.name)) && (plotter.getValue() == getValue());
    }
  }

  public static class Graph
  {
    private final String name;
    private final Set plotters;

    private Graph(String name)
    {
      this.plotters = new LinkedHashSet();
      this.name = name;
    }

    public String getName() {
      return this.name;
    }

    public void addPlotter(Metrics.Plotter plotter) {
      this.plotters.add(plotter);
    }

    public void removePlotter(Metrics.Plotter plotter) {
      this.plotters.remove(plotter);
    }

    public Set getPlotters() {
      return Collections.unmodifiableSet(this.plotters);
    }

    public int hashCode() {
      return this.name.hashCode();
    }

    public boolean equals(Object object) {
      if (!(object instanceof Graph)) {
        return false;
      }
      Graph graph = (Graph)object;
      return graph.name.equals(this.name);
    }

    protected void onOptOut()
    {
    }

    Graph(String s, Graph graph) {
      this(s);
    }
  }
}