package teachingtutorials.fundamentalTasks;

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
    //Stores the target coords or the location a player should tpll to
    final double fTargetCoords[] = new double[2];
    Player player;
    TeachingTutorials plugin;

    public TpllListener(TeachingTutorials plugin, double lat, double lon, Player player, float maxPoints)
    {
        this.plugin = plugin;
        this.fTargetCoords[0] = lat;
        this.fTargetCoords[1] = lon;
        this.player = player;
    }

    @Override
    public void register()
    {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void interactEvent(PlayerCommandPreprocessEvent event)
    {
        double lat, lon;

        int iScore = 0;
        int iAdjustedScore = 0;

        if (event.getPlayer().getUniqueId().equals(player.getUniqueId()))
        {
            String command = event.getMessage();
            //TODO: Tpll coord extractor here

            //TODO: Tpll accuracy checker here

            //if shit
            HandlerList.unregisterAll(this);

            //Ranked from 0 to 1
            //iScore = ....


            //Then normalised to -1 to 1
            iAdjustedScore = iScore*2 - 1;
        }
    }
}