package teachingtutorials.newlocation;

import net.buildtheearth.terraminusminus.util.geo.CoordinateParseUtils;
import net.buildtheearth.terraminusminus.util.geo.LatLng;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.messaging.ChannelNameTooLongException;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.User;

import java.util.ArrayList;

public class AreaSelectionListener implements Listener
{
    //The listener is associated with the player who is creating the new location
    private User creator;

    private TeachingTutorials plugin;
    private NewLocation callingClass;
    private ArrayList<LatLng> bounds;

    public AreaSelectionListener(User Creator, TeachingTutorials plugin, NewLocation callingClass)
    {
        this.creator = Creator;
        this.plugin = plugin;
        this.callingClass = callingClass;
        this.bounds = new ArrayList<>();
    }

    public ArrayList<LatLng> getBounds()
    {
        return bounds;
    }

    @EventHandler
    public void tpllCommand(PlayerCommandPreprocessEvent event)
    {
        if (event.getPlayer().getUniqueId().equals(this.creator.player.getUniqueId()))
        {
            String command = event.getMessage();
            if (command.startsWith("/tpll "))
            {
                event.setCancelled(true);
                command = command.replace("/tpll ", "");
                LatLng latLong = CoordinateParseUtils.parseVerbatimCoordinates(command.replace(", ", " "));

                if (latLong !=null)
                {
                    bounds.add(latLong);
                    Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Point added");
                    Display display = new Display(event.getPlayer(), ChatColor.AQUA +"Point added");
                    display.Message();
                }
                else
                {
                    Display display = new Display(event.getPlayer(), ChatColor.RED +"Incorrect tpll command format, try again");
                    display.Message();
                }
            }
            else if (command.startsWith("/tutorials endarea"))
            {
                //Checks whether at least 3 points have been specified for the area
                if (this.bounds.size() >= 3)
                {
                    HandlerList.unregisterAll(this);
                    event.setCancelled(true);
                    callingClass.AreaMade();
                }
                else
                {
                    Display display = new Display(event.getPlayer(), ChatColor.DARK_AQUA +"You need at least 3 points to create an area");
                    display.Message();
                }
            }
        }
    }

    public void register()
    {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }
}
