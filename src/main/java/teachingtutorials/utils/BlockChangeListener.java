package teachingtutorials.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;
import teachingtutorials.TeachingTutorials;

public class BlockChangeListener implements Listener
{
    private TeachingTutorials plugin;
    private Object worldEditEventListener;

    public BlockChangeListener(TeachingTutorials plugin, Object worldEditEventListener)
    {
        this.plugin = plugin;
        this.worldEditEventListener = worldEditEventListener;

        //Registers the listener
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    //Lowest will mean that it can be recorded and then immediately cancelled. It yet be uncancelled, but this is highly unlikely
    @EventHandler(priority = EventPriority.LOWEST)
    public void blockChangeEvent(ServerCommandEvent event)
    {
        //Checks that it is the correct command
        String command = event.getCommand();
        if (command.startsWith("/tpll"))
        {
            //Cancels the event
            event.setCancelled(true);

        }
    }
}
