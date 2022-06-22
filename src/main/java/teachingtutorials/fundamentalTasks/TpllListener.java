package teachingtutorials.fundamentalTasks;

import net.buildtheearth.terraminusminus.util.geo.CoordinateParseUtils;
import net.buildtheearth.terraminusminus.util.geo.LatLng;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import teachingtutorials.TeachingTutorials;

//All tpll command config will be the real life lat and long cords
//Having them as the block coords would be more work for the designer of a tutorial

public class TpllListener extends Task implements Listener
{
    //Stores the target coords - the location a player should tpll to
    final double fTargetCoords[] = new double[2];

    public TpllListener(TeachingTutorials plugin, double lat, double lon, Player player, float fDifficulty)
    {
        super(plugin);
        this.fTargetCoords[0] = lat;
        this.fTargetCoords[1] = lon;
        this.player = player;
        this.fDifficulty = fDifficulty;
    }

    @Override
    public void register()
    {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void interactEvent(PlayerCommandPreprocessEvent event)
    {
        fPerformance = 0F;

        if (event.getPlayer().getUniqueId().equals(player.getUniqueId()))
        {
            //Tpll coordinate extractor
            String command = event.getMessage();
            LatLng latLong = CoordinateParseUtils.parseVerbatimCoordinates(command.replace(", ", " "));

            //Tpll accuracy checker
            double fDistance = Math.sqrt( Math.pow((latLong.getLat() - fTargetCoords[0]), 2) + Math.pow((latLong.getLng() - fTargetCoords[1]), 2) );
            fDistance = fDistance * 111139;

            if (fDistance <= 0.25)
            {
                fPerformance = 1;
                spotHit();
            }
            else if (fDistance <= 1.0) //Make the acceptable value configurable
            {
                //Pretty decent
                HandlerList.unregisterAll(this);
                //Ranked from 0 to 1
                //iScore = ....
                fPerformance = (4 / 3) * (1 - (float) fDistance);
                spotHit();
            }
        }
    }

    private void spotHit()
    {
        //Unregisters this task
        HandlerList.unregisterAll(this);

        //Marks the task as complete
        taskComplete();
    }
}