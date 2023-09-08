package teachingtutorials.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import teachingtutorials.TeachingTutorials;

public class Falling implements Listener
{
    private Player player;
    private Location safeLocation;
    private TeachingTutorials plugin;

    public Falling (Player player, Location safeLocation, TeachingTutorials plugin)
    {
        this.player = player;
        this.safeLocation = safeLocation;
        this.plugin = plugin;
    }

    public Falling (Player player, TeachingTutorials plugin)
    {
        this.player = player;
        this.plugin = plugin;
    }

    public void setSafeLocation(Location location)
    {
        this.safeLocation = location;
    }

    @EventHandler
    public void onClick(PlayerMoveEvent event)
    {
        if (!event.getPlayer().equals(player))
        {
            //If it's not the correct player then do nothing. Must be the correct player because
            // the safeLocation depends on the player
        }
        else if (event.getTo().getY() < plugin.getConfig().getInt("Min_Y"))
        {
            player.teleport(safeLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
        }
    }

    public void register()
    {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void unregister()
    {
        HandlerList.unregisterAll(this);
    }
}