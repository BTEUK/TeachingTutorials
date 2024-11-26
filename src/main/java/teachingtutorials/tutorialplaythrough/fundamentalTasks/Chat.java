package teachingtutorials.tutorialplaythrough.fundamentalTasks;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.tutorialplaythrough.GroupPlaythrough;
import teachingtutorials.tutorialobjects.LocationTask;
import teachingtutorials.tutorialplaythrough.PlaythroughTask;
import teachingtutorials.utils.Display;

/**
 * Represents a type of Task where the user must enter a message into chat. Contains the relevant listeners used when the task is active.
 */
public class Chat extends PlaythroughTask implements Listener
{
    /** Stores the target answer */
    private String szTargetAnswer;

    /**
     * Used when initialising a task for a lesson, i.e when the answers are already known
     * @param plugin A reference to the TeachingTutorials plugin instance
     * @param player A reference to the Bukkit player for which the task is for
     * @param locationTask A reference to the location task object of this chat
     * @param groupPlaythrough A reference to the parent group play-through object which this task is a member of
     */
    public Chat(TeachingTutorials plugin, Player player, LocationTask locationTask, GroupPlaythrough groupPlaythrough)
    {
        super(plugin, player, locationTask, groupPlaythrough);
        this.szTargetAnswer = locationTask.getAnswer();
    }

    /**
     * Used when initialising a task when creating a new location
     * @param plugin A reference to the TeachingTutorials plugin instance
     * @param player A reference to the Bukkit player for which the task is for
     * @param task A reference to the task
     * @param groupPlaythrough A reference to the parent group play-through object which this task is a member of
     */
    public Chat(TeachingTutorials plugin, Player player, Task task, GroupPlaythrough groupPlaythrough)
    {
        super(plugin, player, task, groupPlaythrough);
    }

    /**
     * Registers the task listener, activating the task
     */
    @Override
    public void register()
    {
        super.register();

        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Detects player chat events, determines if it is from the player of this task, and if so performs necessary logic
     * @param event A reference to a player chat event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void chatEvent(AsyncPlayerChatEvent event)
    {
        fPerformance = 0F;

        //Checks that this message is from the relevant player, if not, ignores the event
        if (!event.getPlayer().getUniqueId().equals(player.getUniqueId()))
        {
            return;
        }

        //Extracts the message that the user has sent
        String szChat = event.getMessage();

        //Checks whether it is a new location
        if (bCreatingNewLocation) //Set the answers
        {
            //Gets a local reference to the locationTask object to hold the data that is to be stored in the LocationTasks table of the DB
            LocationTask locationTask = getLocationTask();

            //Sets the answers into the locationTask object
            locationTask.setAnswers(szChat);

            //Data is added to database once difficulty is provided
            //Prompt difficulty
            player.sendMessage(player, Display.aquaText("Enter the difficulty of that chat message from 0 to 1 as a decimal. Use /tutorials [difficulty]"));

            //messageCorrect is then called from inside the difficulty listener once the difficulty has been established
            //This is what moves it onto the next task

            event.setCancelled(true);
        }

        //Performs steps for if it is a lesson
        else if (szChat.equalsIgnoreCase(szTargetAnswer))
        {
            //Answer correct
            Display.ActionBar(player, Display.colouredText("Correct answer !", NamedTextColor.DARK_GREEN));
            messageCorrect();
            fPerformance = 1F;
            event.setCancelled(true);
        }
        else
        {
            //Answer incorrect
            Display.ActionBar(player, Display.colouredText("Incorrect, try again", NamedTextColor.GOLD));

            //Make wrong answers private. May be made configurable in a later update
            event.setCancelled(true);
        }
    }

    /**
     * Handles completion of the task - unregisters listeners and moves onto the next task
     */
    private void messageCorrect()
    {
        //Unregisters this task
        HandlerList.unregisterAll(this);

        //Marks the task as complete, move on to next task
        taskComplete();
    }

    /**
     * Unregisters the listener, marks the task as inactive and removes the virtual blocks of this task
     */
    public void unregister()
    {
        //Marks the task as inactive and removes the virtual blocks of this task
        super.deactivate();

        //Unregisters this task
        HandlerList.unregisterAll(this);
    }

    //A public version is required for when spotHit is called from the difficulty listener
    //This is required as it means that the tutorial can be halted until the difficulty listener completes the creation of the new LocationTask
    /**
     * To be called from a difficulty listener when the difficulty has been specified.
     * <p> </p>
     * Will unregister the chat task and move forwards to the next task
     */
    public void newLocationSpotHit()
    {
        messageCorrect();
    }
}
