package com.samoatesgames.advancedplugincontrol;

import com.samoatesgames.advancedplugincontrol.command.PluginCommandHandler;
import com.samoatesgames.samoatesplugincore.plugin.SamOatesPlugin;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;

/**
 * The main plugin class
 *
 * @author Sam Oates <sam@samoatesgames.com>
 */
public final class AdvancedPluginControl extends SamOatesPlugin {

    /**
     * The /plugin command handler
     */
    private PluginCommandHandler m_pluginCommandHandler = null;
    
    /**
    * Plugin description cache as short urls take time to generate.
    */
    List<String> m_pluginDetailsCache = new ArrayList<String>();

    /**
     * Class constructor
     */
    public AdvancedPluginControl() {
        super("AdvancedPluginControl", "Plugin", ChatColor.GOLD);
    }

    /**
     * Called when the plugin is enabled
     */
    @Override
    public void onEnable() {
        super.onEnable();
        m_pluginCommandHandler = new PluginCommandHandler(this);
        m_commandManager.registerCommandHandler("plugin", m_pluginCommandHandler);
    }

    /**
     * Register all configuration settings
     */
    public void setupConfigurationSettings() {

        PluginManager pluginManager = this.getServer().getPluginManager();
        Plugin[] plugins = pluginManager.getPlugins();
        for (Plugin plugin : plugins) {

            this.logInfo("Add configuration settings for: " + plugin.getName());
            
            String url = null;
            String pluginWebsite = plugin.getDescription().getWebsite();
            if (pluginWebsite != null) {
                url = adflyURL(pluginWebsite);
            }
            if (url == null) {
                url = adflyURL("http://lmgtfy.com/?q=Bukkit+Plugin+" + plugin.getName().replaceAll(" ", "+"));
            }
            if (url == null) {
                this.logInfo("Failed to create Adfly url, creating goo.gl link.");
                url = googleShortenURL("http://lmgtfy.com/?q=Bukkit+Plugin+" + plugin.getName().replaceAll(" ", "+"));
            }
            if (url == null) {
                url = "http://lmgtfy.com/?q=Bukkit+Plugin+" + plugin.getName().replaceAll(" ", "+");
            }
            
            String key = "plugin." + pluginNameToKey(plugin.getName());
            this.registerSetting(key + ".visible", true);
            this.registerSetting(key + ".name", plugin.getName());
            this.registerSetting(key + ".webpage.show", true);
            this.registerSetting(key + ".webpage.url", url);
        }

    }

    /**
     * Adfly wrap a url, then wrap that in a google short url
     *
     * @param url
     * @return
     */
    private String adflyURL(String url) {
        try {
            URL urlRequest = new URL("http://api.adf.ly/api.php?key=0025a7a40ff810788e2e6dc5f74af886&uid=8733703&advert_type=int&domain=adf.ly&url=" + url);
            BufferedReader in = new BufferedReader(new InputStreamReader(urlRequest.openStream()));
            url = in.readLine();
            in.close();
        } catch (IOException ex) {
            this.logException("Failed to Adfly '" + url + "'.", ex);
            return googleShortenURL(url);
        }
        return url;
    }

    /**
     * Shorten of web url using googles url shortening api
     *
     * @param longUrl
     * @return
     */
    private String googleShortenURL(String longUrl) {
        String googUrl = "https://www.googleapis.com/urlshortener/v1/url?shortUrl=http://goo.gl/fbsS&key=AIzaSyA1fQnseCFv6vWOefIcNe7XL8lVOV7YVbU";
        String shortUrl = "";
        try {
            URLConnection conn = new URL(googUrl).openConnection();
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write("{\"longUrl\":\"" + longUrl + "\"}");
            wr.flush();

            // Get the response
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                if (line.contains("id")) {
                    shortUrl = line.substring(8, line.length() - 2);
                    break;
                }
            }
            wr.close();
            rd.close();
        } catch (MalformedURLException ex) {
            this.logException("Failed to goo.gl '" + longUrl + "'.", ex);
            return longUrl;
        } catch (IOException ex) {
            this.logException("Failed to goo.gl '" + longUrl + "'.", ex);
            return longUrl;
        }
        return shortUrl;
    }

    /**
     * 
     * @return 
     */
    public List<String> getPluginDetailsCache() {
        return m_pluginDetailsCache;
    }
    
    /**
     * 
     */
    public void cachePluginDetails() {
        
        m_pluginDetailsCache.clear();        
        PluginManager pluginManager = Bukkit.getServer().getPluginManager();
        Plugin[] plugins = pluginManager.getPlugins();
        for (Plugin plugin : plugins) {
            PluginDescriptionFile discription = plugin.getDescription();
            String shortVersion = discription.getVersion();
            if (shortVersion.length() >= 9) {
                shortVersion = shortVersion.substring(0, 6) + "...";
            }
            
            String key = "plugin." + pluginNameToKey(plugin.getName());
            boolean visible = this.getSetting(key + ".visible", true);
            if (visible) {
                String pluginName = this.getSetting(key + ".name", plugin.getName());
                boolean showUrl = this.getSetting(key + ".webpage.show", true);
                String url = this.getSetting(key + ".webpage.url", "http://www.google.com");
                m_pluginDetailsCache.add(" • " + ChatColor.GOLD + pluginName + " " + ChatColor.DARK_GREEN + "version " + shortVersion + (showUrl ? (ChatColor.BLUE + " " + url) : ""));
            }
        }
        
        Collections.sort(m_pluginDetailsCache, new Comparator<String>() {
            @Override
            public int compare(String t, String t1) {
                return t.compareToIgnoreCase(t1);
            }
        });
    }
    
    /**
     * 
     * @param pluginName
     * @return 
     */
    public String pluginNameToKey(String pluginName) {
        String key = pluginName.toLowerCase();
        key = key.replaceAll(" ", "_");
        return key;
    }

    /**
     * Called when the plugin is disabled
     */
    @Override
    public void onDisable() {

    }

    /**
     * Handle commands. If we have a registered command with that name execute
     * it.
     *
     * @param sender The sender of the command
     * @param cmd The command being executed
     * @param label The commands label
     * @param args The arguments passed with the command
     * @return True if handled, false otherwise
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        return m_commandManager.onCommand(sender, cmd, label, args);
    }

    /**
     * Pre-process commands to override internal bukkit commands
     *
     * @param event The preprocess command event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        // If a player uses /pl, then pass the command on to our plugin handler
        String[] commandParts = event.getMessage().toLowerCase().split(" ");
        if (commandParts.length >= 1) {
            String command = commandParts[0];
            if (command.equals("/pl")) {
                event.setCancelled(true);
                m_pluginCommandHandler.execute(m_commandManager, event.getPlayer(), Arrays.copyOfRange(commandParts, 1, commandParts.length));
            }
        }
    }
}
