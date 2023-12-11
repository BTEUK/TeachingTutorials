package teachingtutorials.tutorials;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import teachingtutorials.TeachingTutorials;

/**
    Listens out for /link when a tutorial is being played
 */
public class VideoLinkCommandListener implements Listener
{
    private TeachingTutorials plugin;
    private Player player;
    private LocationStep locationStep;

    public VideoLinkCommandListener(TeachingTutorials plugin, Player player, LocationStep locationStep)
    {
        this.plugin = plugin;
        this.player = player;
        this.locationStep = locationStep;
    }

    //On command
    @EventHandler(priority = EventPriority.LOWEST)
    public void commandEvent(PlayerCommandPreprocessEvent event)
    {
        if (event.isCancelled())
            return;

        if (this.player.getUniqueId().equals(event.getPlayer().getUniqueId()))
        {
            if (event.getMessage().startsWith("/link") || event.getMessage().startsWith("/video"))
            {
                locationStep.displayVideoLink(event.getPlayer());
                event.setCancelled(true);
            }
        }
    }

    public void register()
    {
        //Registers the listener
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void unregister()
    {
        //Unregisters the listener
        HandlerList.unregisterAll(this);
    }
}
