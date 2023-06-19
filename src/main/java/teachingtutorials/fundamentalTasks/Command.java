package teachingtutorials.fundamentalTasks;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.newlocation.DifficultyListener;
import teachingtutorials.tutorials.Group;
import teachingtutorials.tutorials.LocationTask;
import teachingtutorials.utils.Display;

public class Command extends Task implements Listener
{
    private String szTargetCommand;
    private String szTargetCommandArgs;

    private DifficultyListener difficultyListener;

    public Command(TeachingTutorials plugin, Player player, Group parentGroup, String szAnswers, float fDifficulty)
    {
        super(plugin);
        this.type = "command";
        this.player = player;
        this.parentGroup = parentGroup;

        //Extracts the answers
        String[] commandDetails = szAnswers.split(",");

        this.szTargetCommand = commandDetails[0];
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Target command = "+this.szTargetCommand);

        this.szTargetCommandArgs = "";

        for (int i = 1 ; i < commandDetails.length ; i++)
        {
            this.szTargetCommandArgs = this.szTargetCommandArgs + commandDetails[i];
        }

        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Target command args = "+this.szTargetCommandArgs);

        //Adds a space before the correct command arguments if there is an argument required
        //This is needed because a command with no argument will have no trailing space, but a command with arguments does have a space after the base command
        if(!this.szTargetCommandArgs.equals(""))
        {
            this.szTargetCommandArgs = " " + szTargetCommandArgs;
        }

        this.fDifficulty = fDifficulty;

        this.bNewLocation = false;
    }

    public Command(TeachingTutorials plugin, Player player, Group parentGroup, int iTaskID)
    {
        super(plugin);
        this.type = "command";
        this.player = player;
        this.bNewLocation = true;
        this.parentGroup = parentGroup;
        this.iTaskID = iTaskID;

        //Listen out for difficulty - There will only be one difficulty listener per command to avoid bugs
        difficultyListener = new DifficultyListener(this.plugin, this.player, this, FundamentalTask.command);
        difficultyListener.register();
    }

    @Override
    public void register()
    {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void commandEvent(PlayerCommandPreprocessEvent event)
    {
        fPerformance = 0F;

        //Checks that it is the correct player
        if (!event.getPlayer().getUniqueId().equals(player.getUniqueId()))
        {
            return;
        }
        String command = event.getMessage();

        //Checks whether it is a new location
        if (bNewLocation) //Set the answers
        {
            LocationTask locationTask = new LocationTask(this.parentGroup.parentStep.parentStage.getLocationID(), iTaskID);

            //Catches the /tutorials command
            //Note that this listener will still actually be active even as the logic for the difficulty is going through
            //If this listener is performed first, the message will get sent that you can't run /tutorials
            //Or it would change the desired command if not caught. It also then cancels the command so the difficulty wouldn't work
            //If this listener is performed second, then the difficulty listener would've already unregistered and an unnecessary message would go through
            if (command.startsWith("/tutorials"))
            {
                if (!difficultyListener.getIsReady())
                {
                    Display display = new Display(player, ChatColor.RED+"You cannot set /tutorials an an answer. Did you make a mistake?");
                    display.ActionBar();
                }
                return;
            }

            String szTargetCommand;
            String szArgs;
            //Checks whether there are parameters or just the bare command
            if (command.contains(" "))
            {
                szTargetCommand = command.substring(1, event.getMessage().indexOf(" "));
                szArgs = command.replace("/" +szTargetCommand+" ", "");
            }
            else
            {
                szTargetCommand = command.substring(1);
                szArgs = "";
            }

            String szAnswers = szTargetCommand+","+szArgs;
            locationTask.setAnswers(szAnswers);
            difficultyListener.setLocationTask(locationTask);

            //Data is added to database once difficulty is provided

            //Prompt difficulty
            Display difficultyPrompt = new Display(player, ChatColor.AQUA +"Enter the difficulty of that command from 0 to 1 as a decimal. Use /tutorials [difficulty]");
            difficultyPrompt.Message();

            //SpotHit is then called from inside the difficulty listener once the difficulty has been established
            //This is what moves it onto the next task

            event.setCancelled(true);

            return;
        }

        if (command.startsWith("/"+szTargetCommand))
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Correct base command");
            command = command.replace(("/"+szTargetCommand), "");
            if (command.equals(szTargetCommandArgs))
            {
                Display display = new Display(player, ChatColor.DARK_GREEN+"Correct command");
                display.ActionBar();
                commandComplete();
                fPerformance = 1F;
            }
            else
            {
                Display display = new Display(player, ChatColor.GOLD+"Command /"+szTargetCommand +" was correct but the arguments of the command were not");
                display.ActionBar();
            }
        }
    }

    private void commandComplete()
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
        commandComplete();
    }
}
