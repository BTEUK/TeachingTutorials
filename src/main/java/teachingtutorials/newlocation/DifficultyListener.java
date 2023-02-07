package teachingtutorials.newlocation;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.checkerframework.checker.units.qual.C;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.fundamentalTasks.TpllListener;
import teachingtutorials.tutorials.LocationTask;
import teachingtutorials.utils.Display;

public class DifficultyListener implements Listener
{
    TeachingTutorials plugin;
    Player player;
    LocationTask locationTask;
    TpllListener tpllListener;

    public DifficultyListener(TeachingTutorials plugin, Player player, LocationTask locationTask, TpllListener tpllListener)
    {
        this.plugin = plugin;
        this.player = player;
        this.locationTask = locationTask;
        this.tpllListener = tpllListener;
    }

    public void register()
    {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
        player.sendMessage(ChatColor.AQUA +"Enter the difficulty from 0 to 1 as a decimal. Use command /tutorials [difficulty]");
    }

    @EventHandler
    public void commandEvent(PlayerCommandPreprocessEvent event)
    {
        if (event.getPlayer().getUniqueId().equals(player.getUniqueId()))
        {
            String command = event.getMessage();
            if (command.matches("(/tutorials 0).[0-9]+") || command.equals("/tutorials 1"))
            {
                //Cancels the event
                event.setCancelled(true);

                //Extracts the difficulty of the task
                command = command.replace("/tutorials ", "");
                float fDifficulty = Float.parseFloat(command);

                //Signals the tpll listener to store the details of the new LocationTask in the DB
                locationTask.setDifficulties(fDifficulty, 0, 0, 0, 0);
                if (locationTask.storeNewData())
                {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"LocationTask stored in database");
                    Display display = new Display(player, ChatColor.AQUA +"Task stored in DB");
                    display.Message();
                }
                else
                {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"LocationTask not stored in database");
                    Display display = new Display(player, ChatColor.AQUA +"Task could not be stored in DB. Please report this");
                    display.Message();
                }

                //Unregisters this listener
                HandlerList.unregisterAll(this);

                tpllListener.newLocationSpotHit();
            }
        }
    }
}