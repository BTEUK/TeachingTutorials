package teachingtutorials.fundamentalTasks;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.newlocation.DifficultyListener;
import teachingtutorials.tutorials.Group;
import teachingtutorials.tutorials.LocationTask;
import teachingtutorials.utils.Display;

public class Chat extends Task implements Listener
{
    private String szTargetAnswer;

    private DifficultyListener difficultyListener;

    //Used in a lesson
    public Chat(TeachingTutorials plugin, Player player, Group parentGroup, int iOrder, String szDetails, String szAnswers, float fDifficulty)
    {
        super(plugin);
        this.type = "chat";
        this.player = player;
        this.parentGroup = parentGroup;
        this.szTargetAnswer = szAnswers;
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Target answer = "+szAnswers);
        this.iOrder = iOrder;
        this.szDetails = szDetails;
        this.fDifficulty = fDifficulty;
        this.bNewLocation = false;
    }

    public Chat(TeachingTutorials plugin, Player player, Group parentGroup, int iTaskID, int iOrder, String szDetails)
    {
        super(plugin);
        this.type = "chat";
        this.player = player;
        this.bNewLocation = true;
        this.parentGroup = parentGroup;
        this.iTaskID = iTaskID;
        this.iOrder = iOrder;
        this.szDetails = szDetails;

        //Listen out for difficulty - There will only be one difficulty listener per chat to avoid bugs
        difficultyListener = new DifficultyListener(this.plugin, this.player, this, FundamentalTask.chat);
        difficultyListener.register();
    }

    @Override
    public void register()
    {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void commandEvent(AsyncPlayerChatEvent event)
    {
        fPerformance = 0F;

        //Checks that it is the correct player
        if (!event.getPlayer().getUniqueId().equals(player.getUniqueId()))
        {
            return;
        }

        //The message that the user has sent
        String szChat = event.getMessage();

        //Checks whether it is a new location
        if (bNewLocation) //Set the answers
        {
            //Creates a new locationTask object to hold the data that is to be stored in the LocationTasks table of the DB
            LocationTask locationTask = new LocationTask(this.parentGroup.parentStep.parentStage.getLocationID(), iTaskID);

            //Sets the answers into the locationTask object
            locationTask.setAnswers(szChat);
            difficultyListener.setLocationTask(locationTask);

            //Data is added to database once difficulty is provided
            //Prompt difficulty
            Display difficultyPrompt = new Display(player, ChatColor.AQUA +"Enter the difficulty of that chat message from 0 to 1 as a decimal. Use /tutorials [difficulty]");
            difficultyPrompt.Message();

            //messageCorrect is then called from inside the difficulty listener once the difficulty has been established
            //This is what moves it onto the next task

            event.setCancelled(true);

            return;
        }

        if (szChat.matches(szTargetAnswer))
        {
            Display display = new Display(player, ChatColor.DARK_GREEN+"Correct answer !");
            display.ActionBar();
            messageCorrect();
            fPerformance = 1F;
            event.setCancelled(true);
        }
        else
        {
            Display display = new Display(player, ChatColor.GOLD+"Incorrect, try again");
            display.ActionBar();

            //Make wrong answers private. May be made configurable in a later update
            event.setCancelled(true);
        }
    }

    private void messageCorrect()
    {
        //Unregisters this task
        HandlerList.unregisterAll(this);

        //Marks the task as complete, move on to next task
        taskComplete();
    }

    @Override
    public void unregister()
    {
        //Unregisters this task
        HandlerList.unregisterAll(this);
    }

    //A public version is required for when spotHit is called from the difficulty listener
    //This is required as it means that the tutorial can be halted until the difficulty listener completes the creation of the new LocationTask
    @Override
    public void newLocationSpotHit()
    {
        messageCorrect();
    }
}
