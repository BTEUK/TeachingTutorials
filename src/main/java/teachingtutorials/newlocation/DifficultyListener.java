package teachingtutorials.newlocation;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.fundamentalTasks.FundamentalTaskType;
import teachingtutorials.fundamentalTasks.Task;
import teachingtutorials.tutorials.LocationTask;
import teachingtutorials.utils.Display;


public class DifficultyListener implements Listener
{
    TeachingTutorials plugin;
    Player player;
    LocationTask locationTask;
    Task task;
    FundamentalTaskType taskType;
    boolean bReadyForDifficulty;

    public DifficultyListener(TeachingTutorials plugin, Player player, Task task, FundamentalTaskType taskType)
    {
        this.plugin = plugin;
        this.player = player;
        this.task = task;
        this.taskType = taskType;
        bReadyForDifficulty = false;
    }

    public void register()
    {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void setLocationTask(LocationTask locationTask)
    {
        this.locationTask = locationTask;
        bReadyForDifficulty = true;
    }

    public boolean getIsReady()
    {
        return bReadyForDifficulty;
    }

    //Want this /tutorials event to be handled first
    @EventHandler(priority = EventPriority.LOWEST)
    public void commandEvent(PlayerCommandPreprocessEvent event)
    {
        if (event.getPlayer().getUniqueId().equals(player.getUniqueId()))
        {
            String command = event.getMessage();
            if (command.matches("(/tutorials 0)\\.[0-9]+") || command.equals("/tutorials 1"))
            {
                //Cancels the event
                event.setCancelled(true);

                if (!bReadyForDifficulty)
                {
                    Display display = new Display(event.getPlayer(), ChatColor.RED +"Complete the " +taskType.toString() +" task first");
                    display.Message();
                    return;
                }

                //Extracts the difficulty of the task
                command = command.replace("/tutorials ", "");
                float fDifficulty = Float.parseFloat(command);

                //Signals the listener to store the details of the new LocationTask in the DB
                switch (taskType)
                {
                    case tpll:
                        locationTask.setDifficulties(fDifficulty, 0, 0, 0, 0);
                        break;
                    case selection:
                    case command:
                        locationTask.setDifficulties(0, fDifficulty, 0, 0, 0);
                        break;
                    case chat:
                        locationTask.setDifficulties(0, 0, 0, 0, 0);
                        break;
                    case place:
                        locationTask.setDifficulties(0, 0, fDifficulty, 0, 0);
                        break;
                }
                if (locationTask.storeNewData())
                {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"LocationTask stored in database");
                    Display display = new Display(player, ChatColor.GREEN +"Task stored in DB");
                    display.Message();
                }
                else
                {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"LocationTask not stored in database");
                    Display display = new Display(player, ChatColor.RED +"Task could not be stored in DB. Please report this");
                    display.Message();
                }

//                Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Unregistering difficulty listeners");

//                //Unregisters all the difficulty listeners associated with the creator in question
//                ArrayList<RegisteredListener> listeners = HandlerList.getRegisteredListeners(this.plugin);
//                int iListeners = listeners.size();
//                for (int i = 0 ; i < iListeners ; i++)
//                {
//                    RegisteredListener listener = listeners.get(i);
//                    if (listener.getListener() instanceof DifficultyListener)
//                    {
//                        if (((DifficultyListener) listener.getListener()).player.getUniqueId().equals(event.getPlayer().getUniqueId()))
//                        {
//                            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Found a difficulty listener, unregistering");
//                            HandlerList.unregisterAll(listener.getListener());
//                        }
//                    }
//                }

                HandlerList.unregisterAll(this);

                task.newLocationSpotHit();
            }
        }
    }
}