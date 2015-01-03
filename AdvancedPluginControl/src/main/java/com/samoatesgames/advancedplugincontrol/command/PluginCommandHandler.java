package com.samoatesgames.advancedplugincontrol.command;

import com.samoatesgames.samoatesplugincore.commands.BasicCommandHandler;
import com.samoatesgames.samoatesplugincore.commands.PluginCommandManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.UnknownDependencyException;

/**
 *
 * @author Sam Oates <sam@samoatesgames.com>
 */
public class PluginCommandHandler extends BasicCommandHandler {

    /**
    * Plugin description cache as short urls take time to generate.
    */
    List<String> m_pluginDetailsCache = new ArrayList<String>();

    /**
     * Class constructor
     */
    public PluginCommandHandler() {
        super("advancedpluginmanager.command.plugin.admin");
        cachePluginDetails();
    }
    
    /**
     * Handle the /plugin command
     *
     * @param sender
     * @param args
     * @return
     */
    public boolean execute(PluginCommandManager manager, CommandSender sender, String[] args) {

        if (args.length == 0) {
            outputAllPluginsOverview(manager, sender);
            return true;
        }

        if (!manager.hasPermission(sender, this.getPermission())) {
            manager.sendMessage(sender, "You do not have permission to access these commands.");
            return true;
        }

        if (args.length != 2) {
            // invalid usage
            manager.sendMessage(sender, "The following plugin command uses exist:");
            manager.sendMessage(sender, " - plugin reload <plugin>");
            manager.sendMessage(sender, " - plugin disable <plugin>");
            manager.sendMessage(sender, " - plugin enable <plugin>");
            manager.sendMessage(sender, " - plugin load <plugin path>");
            manager.sendMessage(sender, " - plugin unload <plugin>");
            manager.sendMessage(sender, " - plugin info <plugin>");
            return true;
        }

        final String command = args[0];
        final String pluginName = args[1];
        if (command.equalsIgnoreCase("load")) {
            return loadPlugin(manager, sender, pluginName);
        }
        if (command.equalsIgnoreCase("unload")) {
            return unloadPlugin(manager, sender, pluginName);
        }
        if (command.equalsIgnoreCase("reload")) {
            return reloadPlugin(manager, sender, pluginName);
        }
        if (command.equalsIgnoreCase("disable")) {
            return disablePlugin(manager, sender, pluginName);
        }
        if (command.equalsIgnoreCase("enable")) {
            return enablePlugin(manager, sender, pluginName);
        }
        if (command.equalsIgnoreCase("info")) {
            return pluginInformation(manager, sender, pluginName);
        }

        return true;
    }

    /**
     * Finds a plugin by name ignoring case.
     *
     * @param pluginName
     * @return
     */
    private Plugin getPlugin(String pluginName) {
        PluginManager pluginManager = Bukkit.getServer().getPluginManager();
        for (Plugin plugin : pluginManager.getPlugins()) {
            if (plugin.getName().toLowerCase().equals(pluginName.toLowerCase())) {
                return plugin;
            }
        }
        return null;
    }

    /**
     * Shorten of web url using googles url shortening api
     * @param longUrl
     * @return
     */
    private String shortenURL(String longUrl) {
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
                    // I'm sure there's a more elegant way of parsing
                    // the JSON response, but this is quick/dirty =)
                    shortUrl = line.substring(8, line.length() - 2);
                    break;
                }
            }
            wr.close();
            rd.close();
        } catch (MalformedURLException ex) {
            return null;
        } catch (IOException ex) {
            return null;
        }
        return shortUrl;
    }

    private void cachePluginDetails() {
        
        m_pluginDetailsCache.clear();        
        PluginManager pluginManager = Bukkit.getServer().getPluginManager();
        Plugin[] plugins = pluginManager.getPlugins();
        for (Plugin plugin : plugins) {
            PluginDescriptionFile discription = plugin.getDescription();
            String url = null;
            String pluginWebsite = discription.getWebsite();
            if (pluginWebsite != null) {
                url = shortenURL(pluginWebsite);
            }
            if (url == null) {
                url = shortenURL("http://lmgtfy.com/?q=Bukkit+Plugin+" + plugin.getName().replaceAll(" ", "+"));
            }
            String shortVersion = discription.getVersion();
            if (shortVersion.length() >= 9) {
                shortVersion = shortVersion.substring(0, 6) + "...";
            }
            m_pluginDetailsCache.add(" • " + ChatColor.GOLD + plugin.getName() + " " + ChatColor.DARK_GREEN + "version " + shortVersion + ChatColor.BLUE + " " + url);
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
     * @param sender
     */
    private void outputAllPluginsOverview(PluginCommandManager manager, CommandSender sender) {
        if (m_pluginDetailsCache.isEmpty()) {
            cachePluginDetails();
        }
        for (String message : m_pluginDetailsCache) {
            sender.sendMessage(message);
        }
    }

    /**
     * Output information about a given plugin.
     */
    private boolean pluginInformation(PluginCommandManager manager, CommandSender player, String pluginName) {
        Plugin plugin = getPlugin(pluginName);
        if (plugin == null) {
            manager.sendMessage(player, "A plugin with the name '" + pluginName + "' could not be found.");
            return true;
        }
        PluginDescriptionFile discription = plugin.getDescription();
        String authors = "";
        List<String> authorsList = discription.getAuthors();
        for (int authorIndex = 0; authorIndex < authorsList.size(); ++authorIndex) {
            String author = authorsList.get(authorIndex);
            authors += author + ", ";
        }
        if (authorsList.size() > 0) {
            authors = authors.substring(0, authors.length() - 2);
        } else {
            authors = "Unknown";
        }
        manager.sendMessage(player, ChatColor.GOLD + "||======================================||");
        manager.sendMessage(player, ChatColor.DARK_GREEN + "Name: " + ChatColor.WHITE + plugin.getName());
        manager.sendMessage(player, ChatColor.DARK_GREEN + "Version: " + ChatColor.WHITE + discription.getVersion());
        manager.sendMessage(player, ChatColor.DARK_GREEN + "Authors: " + ChatColor.WHITE + authors);
        manager.sendMessage(player, ChatColor.DARK_GREEN + "Website: " + ChatColor.BLUE + shortenURL(discription.getWebsite()));
        manager.sendMessage(player, ChatColor.DARK_GREEN + "Enabled: " + ChatColor.WHITE + (plugin.isEnabled() ? "True" : "False"));
        manager.sendMessage(player, ChatColor.GOLD + "||======================================||");
        return true;
    }

    /**
     * Disable a given plugin.
     */
    private boolean disablePlugin(PluginCommandManager manager, CommandSender player, String pluginName) {
        PluginManager pluginManager = Bukkit.getServer().getPluginManager();
        Plugin plugin = getPlugin(pluginName);
        if (plugin == null) {
            manager.sendMessage(player, "A plugin with the name '" + pluginName + "' could not be found.");
            return true;
        }
        pluginManager.disablePlugin(plugin);
        manager.sendMessage(player, "The plugin '" + pluginName + "' was successfully disabled.");
        cachePluginDetails();
        return true;
    }

    /**
     * Unload a given plugin.
     */
    @SuppressWarnings("unchecked")
    private boolean unloadPlugin(PluginCommandManager manager, CommandSender player, String pluginName) {
        PluginManager pluginManager = Bukkit.getServer().getPluginManager();
        Plugin plugin = getPlugin(pluginName);
        if (plugin == null) {
            manager.sendMessage(player, "A plugin with the name '" + pluginName + "' could not be found.");
            return true;
        }
        SimplePluginManager simplePluginManager = (SimplePluginManager) pluginManager;
        try {
            Field pluginsField = simplePluginManager.getClass().getDeclaredField("plugins");
            pluginsField.setAccessible(true);
            List<Plugin> plugins = (List<Plugin>) pluginsField.get(simplePluginManager);
            Field lookupNamesField = simplePluginManager.getClass().getDeclaredField("lookupNames");
            lookupNamesField.setAccessible(true);
            Map<String, Plugin> lookupNames = (Map<String, Plugin>) lookupNamesField.get(simplePluginManager);
            Field commandMapField = simplePluginManager.getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            SimpleCommandMap commandMap = (SimpleCommandMap) commandMapField.get(simplePluginManager);
            Field knownCommandsField;
            Map<String, Command> knownCommands = null;
            if (commandMap != null) {
                knownCommandsField = commandMap.getClass().getDeclaredField("knownCommands");
                knownCommandsField.setAccessible(true);
                knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);
            }
            pluginManager.disablePlugin(plugin);
            if (plugins != null && plugins.contains(plugin)) {
                plugins.remove(plugin);
            }
            if (lookupNames != null && lookupNames.containsKey(pluginName)) {
                lookupNames.remove(pluginName);
            }
            if (commandMap != null && knownCommands != null) {
                for (Iterator<Map.Entry<String, Command>> it = knownCommands.entrySet().iterator(); it.hasNext();) {
                    Map.Entry<String, Command> entry = it.next();
                    if (entry.getValue() instanceof PluginCommand) {
                        PluginCommand command = (PluginCommand) entry.getValue();
                        if (command.getPlugin() == plugin) {
                            command.unregister(commandMap);
                            it.remove();
                        }
                    }
                }
            }
        } catch (NoSuchFieldException ex) {
            manager.sendMessage(player, "Failed to query plugin manager, could not unload plugin.");
            return true;
        } catch (SecurityException ex) {
            manager.sendMessage(player, "Failed to query plugin manager, could not unload plugin.");
            return true;
        } catch (IllegalArgumentException ex) {
            manager.sendMessage(player, "Failed to query plugin manager, could not unload plugin.");
            return true;
        } catch (IllegalAccessException ex) {
            manager.sendMessage(player, "Failed to query plugin manager, could not unload plugin.");
            return true;
        }
        
        manager.sendMessage(player, "The plugin '" + pluginName + "' was successfully unloaded.");
        cachePluginDetails();
        return true;
    }

    /**
     * Enable a given plugin.
     */
    private boolean enablePlugin(PluginCommandManager manager, CommandSender player, String pluginName) {
        PluginManager pluginManager = Bukkit.getServer().getPluginManager();
        Plugin plugin = getPlugin(pluginName);
        if (plugin == null) {
            manager.sendMessage(player, "A plugin with the name '" + pluginName + "' could not be found.");
            return true;
        }
        pluginManager.enablePlugin(plugin);
        manager.sendMessage(player, "The plugin '" + pluginName + "' was successfully enabled.");
        cachePluginDetails();
        return true;
    }

    /**
     * Reload a given plugin.
     */
    private boolean reloadPlugin(PluginCommandManager manager, CommandSender player, String pluginName) {
        PluginManager pluginManager = Bukkit.getServer().getPluginManager();
        Plugin plugin = getPlugin(pluginName);
        if (plugin == null) {
            manager.sendMessage(player, "A plugin with the name '" + pluginName + "' could not be found.");
            return true;
        }
        pluginManager.disablePlugin(plugin);
        pluginManager.enablePlugin(plugin);
        manager.sendMessage(player, "The plugin '" + pluginName + "' was successfully reloaded.");
        cachePluginDetails();
        return true;
    }

    /**
     * Load a given plugin.
     */
    private boolean loadPlugin(PluginCommandManager manager, CommandSender player, String pluginName) {
        PluginManager pluginManager = Bukkit.getServer().getPluginManager();
        // load and enable the given plugin
        File pluginFolder = manager.getPlugin().getDataFolder().getParentFile();
        File pluginFile = new File(pluginFolder + File.separator + pluginName);
        if (!pluginFile.exists()) {
            // plugin does not exist
            manager.sendMessage(player, "A plugin with the name '" + pluginName + "' could not be found at location:");
            manager.sendMessage(player, pluginFile.getAbsolutePath());
            return true;
        }
        // Try and load the plugin
        Plugin plugin = null;
        try {
            plugin = pluginManager.loadPlugin(pluginFile);
        } catch (InvalidPluginException e) {
            plugin = null;
        } catch (InvalidDescriptionException e) {
            // Something went wrong so set the plugin to null
            plugin = null;
        } catch (UnknownDependencyException e) {
            // Something went wrong so set the plugin to null
            plugin = null;
        }
        if (plugin == null) {
            // The plugin failed to load correctly
            manager.sendMessage(player, "The plugin '" + pluginName + "' failed to load correctly.");
            return true;
        }
        // plugin loaded and enabled successfully
        pluginManager.enablePlugin(plugin);
        manager.sendMessage(player, "The plugin '" + pluginName + "' has been succesfully loaded and enabled.");
        cachePluginDetails();
        return true;
    }

}
