package com.samoatesgames.advancedplugincontrol;

import com.samoatesgames.advancedplugincontrol.command.PluginCommandHandler;
import com.samoatesgames.samoatesplugincore.plugin.SamOatesPlugin;
import java.util.Arrays;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * The main plugin class
 * @author Sam Oates <sam@samoatesgames.com>
 */
public final class AdvancedPluginControl extends SamOatesPlugin {
        
    /**
     * The /plugin command handler
     */
    private PluginCommandHandler m_pluginCommandHandler = null;
    
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
        m_pluginCommandHandler = new PluginCommandHandler();
        m_commandManager.registerCommandHandler("plugin", m_pluginCommandHandler);        
    }
    
    /**
     * Register all configuration settings
     */
    public void setupConfigurationSettings() {

    }
    
    /**
     * Called when the plugin is disabled
     */
    @Override
    public void onDisable() {

    }
    
    /**
     * Handle commands.
     * If we have a registered command with that name execute it.
     * @param sender    The sender of the command
     * @param cmd       The command being executed
     * @param label     The commands label
     * @param args      The arguments passed with the command
     * @return          True if handled, false otherwise
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        return m_commandManager.onCommand(sender, cmd, label, args); 
    }
    
    /**
     * Pre-process commands to override internal bukkit commands
     * @param event     The preprocess command event
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
