package teachingtutorials.fundamentalTasks;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
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
import teachingtutorials.utils.WorldEdit;

import java.util.ArrayList;

enum commandType
{
    none, virtualBlocks, full
}


public class Command extends Task implements Listener
{
    private String szTargetCommand;
    private String szTargetCommandArgs;
    private commandType commandType;

    private DifficultyListener difficultyListener;

    //Used for virtual blocks command type
    private boolean bDisplayVirtualBlocks = false;
    //Blocks of the selection
    private ArrayList<Location> selectionBlocks = null;

    //Used in a lesson
    public Command(TeachingTutorials plugin, Player player, Group parentGroup, int iOrder, String szDetails, String szAnswers, float fDifficulty, ArrayList<Task> tasks)
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

        this.iOrder = iOrder;

        //Gets the details of the command from the DB and determines which of the 3 actions should be taken
        this.szDetails = szDetails;
        this.commandType = teachingtutorials.fundamentalTasks.commandType.valueOf(szDetails);

        //Sets up the necessary logic for if it is a virtual blocks command type
        if (commandType.equals(teachingtutorials.fundamentalTasks.commandType.virtualBlocks))
        {
            scheduleVirtualBlocks(tasks);
        }

        this.fDifficulty = fDifficulty;

        this.bNewLocation = false;
    }

    //Used when creating a new location
    public Command(TeachingTutorials plugin, Player player, Group parentGroup, int iTaskID, int iOrder, String szDetails, ArrayList<Task> tasks)
    {
        super(plugin);
        this.type = "command";
        this.player = player;
        this.bNewLocation = true;
        this.parentGroup = parentGroup;
        this.iTaskID = iTaskID;
        this.iOrder = iOrder;
        this.szDetails = szDetails;
        this.commandType = teachingtutorials.fundamentalTasks.commandType.valueOf(szDetails);

        //Listen out for difficulty - There will only be one difficulty listener per command to avoid bugs
        difficultyListener = new DifficultyListener(this.plugin, this.player, this, FundamentalTask.command);
        difficultyListener.register();

        //Sets up the necessary logic for if it is a virtual blocks command type
        if (commandType.equals(teachingtutorials.fundamentalTasks.commandType.virtualBlocks))
        {
            scheduleVirtualBlocks(tasks);
        }
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

            //Checks whether there are parameters or just the bare command
            if (command.contains(" "))
            {
                szTargetCommand = command.substring(1, event.getMessage().indexOf(" "));
                szTargetCommandArgs = command.replace("/" +szTargetCommand+" ", "");
            }
            else
            {
                szTargetCommand = command.substring(1);
                szTargetCommandArgs = "";
            }

            String szAnswers = szTargetCommand+","+szTargetCommandArgs;
            locationTask.setAnswers(szAnswers);
            difficultyListener.setLocationTask(locationTask);

            //Data is added to database once difficulty is provided

            //Prompt difficulty
            Display difficultyPrompt = new Display(player, ChatColor.AQUA +"Enter the difficulty of that command from 0 to 1 as a decimal. Use /tutorials [difficulty]");
            difficultyPrompt.Message();

            //SpotHit is then called from inside the difficulty listener once the difficulty has been established
            //This is what moves it onto the next task

            //Does the desired command action
            switch (commandType)
            {
                case full:
                    break;
                case virtualBlocks:
                    //There is a schedule which every second checks to see if the command is done, and if so, will display the virtual blocks
                    bDisplayVirtualBlocks = true;

                    String szWorldName;
                    if (this.bNewLocation)
                        szWorldName = this.parentGroup.parentStep.parentStage.newLocation.getTutorialID() +"";
                    else
                        szWorldName = this.parentGroup.parentStep.parentStage.lesson.location.getWorld().getName();

                    Selection selection = (Selection) parentGroup.getTasks().get(iOrder - 2);
                    selectionBlocks = WorldEdit.BlocksCalculator(szTargetCommand, selection.iSelectedBlockCoordinates1, selection.iSelectedBlockCoordinates2, szWorldName);
                    event.setCancelled(true);
                    break;
                case none:
                    event.setCancelled(true);
                    break;
            }
        }

        else if (command.startsWith("/"+szTargetCommand))
        {
            command = command.replace(("/"+szTargetCommand), "");
            if (command.equals(szTargetCommandArgs))
            {
                Display display = new Display(player, ChatColor.DARK_GREEN+"Correct command");
                display.ActionBar();
                commandComplete();
                fPerformance = 1F;

                switch (commandType)
                {
                    case full:
                        break;
                    case virtualBlocks:
                        //There is a schedule which every second checks to see if the command is done, and if so, will display the virtual blocks
                        bDisplayVirtualBlocks = true;

                        String szWorldName;
                        if (this.bNewLocation)
                            szWorldName = this.parentGroup.parentStep.parentStage.newLocation.getTutorialID() +"";
                        else
                            szWorldName = this.parentGroup.parentStep.parentStage.lesson.location.getWorld().getName();

                        Selection selection = (Selection) parentGroup.getTasks().get(iOrder - 2);
                        selectionBlocks = WorldEdit.BlocksCalculator(szTargetCommand, selection.iSelectedBlockCoordinates1, selection.iSelectedBlockCoordinates2, szWorldName);
                        event.setCancelled(true);
                        break;
                    case none:
                        event.setCancelled(true);
                        break;
                }
            }
            else
            {
                //Tells the student that the command was correct but the args were wrong
                Display display = new Display(player, ChatColor.GOLD+"Command /"+szTargetCommand +" was correct but the arguments of the command were not");
                display.ActionBar();

                //Cancels the event if the args were wrong but the command was correct
                event.setCancelled(true);
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

    //-------------------------------------------------
    //----------------------Utils----------------------
    //-------------------------------------------------

    private void scheduleVirtualBlocks(ArrayList<Task> tasks)
    {
        //Gets the selection task associated with this command (assumes it is the previous task in the group)
        if (this.iOrder == 1)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"[TeachingTutorials] There was no previous selection task before the virtual blocks command task, changing to a no action command task");
            this.commandType = teachingtutorials.fundamentalTasks.commandType.none;
        }
        else if (!(tasks.get(this.iOrder - 2) instanceof Selection))
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"[TeachingTutorials] There was no previous selection task before the virtual blocks command task, changing to a no action command task");
            this.commandType = teachingtutorials.fundamentalTasks.commandType.none;
        }
        else
        {
            //If it's a virtual blocks command type, assume a selection task was the previous task
//            //Gets the associated selection of this virtual blocks type command
//            Selection selection = (Selection) tasks.get(this.iOrder - 2);
//
//            double[] dTargetCoords1 = selection.dTargetCoords1;
//            double[] dTargetCoords2 = selection.dTargetCoords2;
//
//            //Converts block coordinates to lat/long
//            double[] xz1;
//            double[] xz2;
//
//            final GeographicProjection projection = EarthGeneratorSettings.parse(EarthGeneratorSettings.BTE_DEFAULT_SETTINGS).projection();
//            try
//            {
//                xz1 = projection.fromGeo(dTargetCoords1[1], dTargetCoords1[0]);
//                xz2 = projection.fromGeo(dTargetCoords2[1], dTargetCoords2[0]);
//            }
//            catch (OutOfProjectionBoundsException e)
//            {
//                //Player has selected an area outside of the projection
//                return;
//            }
//
//            finalXZ1 = xz1;
//            finalXZ2 = xz2;

            plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable()
            {
                boolean bLessonActive;
                @Override
                public void run()
                {
                    //Fetches the status of the lesson or new location creation
                    if (bNewLocation)
                        bLessonActive = !parentGroup.parentStep.parentStage.newLocation.bCompleteOrFinished;
                    else
                        bLessonActive = !parentGroup.parentStep.parentStage.lesson.bCompleteOrFinished;

                    //Have a command complete checker because this just repeats every second
                    //Also check whether the lesson/new location creation is still active. If the user is finished with it then we need to stop displaying virtual blocks
                    if (bDisplayVirtualBlocks)
                    {
                        if (bLessonActive)
                        {
                            if (selectionBlocks != null)
                            {
                                BlockData material = WorldEdit.BlockTypeCalculator(szTargetCommandArgs.replace(" ", ""));
                                for (Location location : selectionBlocks)
                                {
                                    player.sendBlockChange(location, material);
                                }
                            }
                        }
                        else
                        {
                            for (Location location : selectionBlocks)
                            {
                                player.sendBlockChange(location, location.getBlock().getBlockData());
                            }
                            return;
                        }
                    }
                }
            }, 20, 10);
        }
    }
}
