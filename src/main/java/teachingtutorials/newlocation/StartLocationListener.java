package teachingtutorials.newlocation;

import net.buildtheearth.terraminusminus.projection.GeographicProjection;
import net.buildtheearth.terraminusminus.projection.OutOfProjectionBoundsException;
import net.buildtheearth.terraminusminus.util.geo.CoordinateParseUtils;
import net.buildtheearth.terraminusminus.util.geo.LatLng;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.User;


public class StartLocationListener implements Listener
{
    //The listener is associated with the player who is creating the new location
    private User creator;

    private TeachingTutorials plugin;
    private NewLocation callingClass;
    private int ixMin, ixMax, izMin, izMax;
    private GeographicProjection projection;

    public StartLocationListener(User Creator, TeachingTutorials plugin, NewLocation callingClass, int ixMin, int ixMax, int izMin, int izMax, GeographicProjection projection)
    {
        this.creator = Creator;
        this.plugin = plugin;
        this.callingClass = callingClass;

        //Passes the bounds of the generated area into the start location listener
        this.ixMin = ixMin;
        this.ixMax = ixMax;
        this.izMin = izMin;
        this.izMax = izMax;

        this.projection = projection;
    }

    //Want the tutorials tpll process to occur first
    @EventHandler(priority = EventPriority.LOWEST)
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
                if (latLong == null)
                {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_AQUA +"Latitude, longitude is null");
                    return;
                }

                double xz[];

                //Converts the tpll coordinates to minecraft coordinates
                try
                {
                    xz = projection.fromGeo(latLong.getLng(), latLong.getLat());
                }
                catch (OutOfProjectionBoundsException e)
                {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Unable to convert lat,long coordinates of start location to minecraft coordinates");
                    return;
                }

                //Make sure that it is within the generated area
                if ((int) xz[0] < ixMin || (int) xz[0] > ixMax || (int) xz[1] < izMin || (int) xz[1] > izMax)
                {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Start location coordinates were not within the generated area");
                    Display display = new Display(creator.player, ChatColor.RED +"The start location must be inside of the generated area. Try again.");
                    display.Message();
                    return;
                }

                //Only unregisters and moves on if all the criteria for the start location are met. These means that they get another chance.
                HandlerList.unregisterAll(this);

                this.callingClass.lessonStart(latLong);
            }
        }
    }

    public void register()
    {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void deregister()
    {
        HandlerList.unregisterAll(this);
    }
}
