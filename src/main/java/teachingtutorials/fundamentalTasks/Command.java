package teachingtutorials.fundamentalTasks;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.platform.permission.ActorSelectorLimits;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.util.BlockVector;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.newlocation.DifficultyListener;
import teachingtutorials.tutorials.Group;
import teachingtutorials.tutorials.LocationTask;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.VirtualBlock;
import teachingtutorials.utils.WorldEdit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

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

    private ArrayList<Task> tasksInGroup;

    /**
     * Used in a lesson
     * @param plugin
     * @param player
     * @param parentGroup
     * @param iTaskID
     * @param iOrder
     * @param szDetails
     * @param szAnswers
     * @param fDifficulty
     * @param tasks
     */
    public Command(TeachingTutorials plugin, Player player, Group parentGroup, int iTaskID, int iOrder, String szDetails, String szAnswers, float fDifficulty, ArrayList<Task> tasks)
    {
        super(plugin, player, parentGroup, iTaskID, iOrder, "command", szDetails, false);
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
            calculateVirtualBlocks(tasks);
        }

        this.fDifficulty = fDifficulty;
    }

    /**
     * Used when creating a new location
     * @param plugin
     * @param player
     * @param parentGroup
     * @param iTaskID
     * @param iOrder
     * @param szDetails
     * @param tasks
     */
    public Command(TeachingTutorials plugin, Player player, Group parentGroup, int iTaskID, int iOrder, String szDetails, ArrayList<Task> tasks)
    {
        super(plugin, player, parentGroup, iTaskID, iOrder, "command", szDetails, true);

        this.commandType = teachingtutorials.fundamentalTasks.commandType.valueOf(szDetails);

        //Listen out for difficulty - There will only be one difficulty listener per command to avoid bugs
        difficultyListener = new DifficultyListener(this.plugin, this.player, this, FundamentalTaskType.command);
        difficultyListener.register();

        tasksInGroup = tasks;
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
        if (bCreatingNewLocation) //Set the answers
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
                    //Calculates the list of virtual blocks
                    calculateVirtualBlocks(tasksInGroup);

                    //Displays the virtual blocks
                    displayVirtualBlocks();

                    event.setCancelled(true);
                    break;
                case none:
                    event.setCancelled(true);
                    break;
            }
        }

        //Not a new location
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
                        //Displays the virtual blocks
                        displayVirtualBlocks();

                        event.setCancelled(true);

                        // This point would be the point where the command is allowed to run,
                        // and then another listener listeners out for the block changes
                        // and converts these changes into virtual blocks.
                        // To deal with the current virtual blocks/overlapping, the virtual blocks could temporarily be
                        // converted to real blocks before the WE changes were applied, and then the world would be
                        // compared to how it was previously and any additional blocks would be all of the virtual blocks,
                        // which would then be stored as the new list.

                        // That would mean that the WE deals with the gmask stuff itself.
                        // The whole world would not need to be checked for changes, just any locations which had block
                        // changes from the command itself.

                        // Unless, I can find a way to have the WE api do this for me.
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
        super.unregister();

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

    private void calculateVirtualBlocks(ArrayList<Task> tasks)
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
            //---------------------------------------------
            //--------Calculate locations of blocks--------
            //---------------------------------------------

            //If it's a virtual blocks command type, assume a selection task was the previous task
            //Gets the associated selection of this virtual blocks type command - we want the actual answers
            Selection selection = (Selection) tasks.get(this.iOrder - 2);

            double[] dTargetCoords1 = selection.dTargetCoords1;
            double[] dTargetCoords2 = selection.dTargetCoords2;

            World world = parentGroup.parentStep.parentStage.tutorialPlaythrough.getLocation().getWorld();
            Location location1 = GeometricUtils.convertToBukkitLocation(world, dTargetCoords1[0], dTargetCoords1[1]);
            Location location2 = GeometricUtils.convertToBukkitLocation(world, dTargetCoords2[0], dTargetCoords2[1]);
            if (location1 == null || location2 == null)
                return;

//            The code for using the player selected points of the previous selection: (we now just use the answers)

//            Selection selection = (Selection) parentGroup.getTasks().get(iOrder - 2);
//            selectionBlocks = WorldEdit.BlocksCalculator(szTargetCommand, selection.iSelectedBlockCoordinates1, selection.iSelectedBlockCoordinates2, szWorldName);

            BlockVector3 selectionPoint1 = BlockVector3.at(location1.getBlockX(), (int) dTargetCoords1[2], location1.getBlockZ());
            BlockVector3 selectionPoint2 = BlockVector3.at(location2.getBlockX(), (int) dTargetCoords2[2], location2.getBlockZ());

//            //Get the player's selection limits
//            ActorSelectorLimits selectionLimits = ActorSelectorLimits.forActor(BukkitAdapter.adapt(player));

            //Creates the 'correct' world edit selection region
            //For now this is only cuboid, but more selection mechanics will be made available
            RegionSelector regionSelector = new CuboidRegionSelector(BukkitAdapter.adapt(world), selectionPoint1, selectionPoint2);

            //Calculates the virtual blocks
            WorldEdit.BlocksCalculator(iTaskID, virtualBlocks, regionSelector, szTargetCommand, szTargetCommandArgs.split(" "), parentGroup.parentStep.parentStage.tutorialPlaythrough);

            //It will create a new calculation object and add this to the queue. The plugin will calculate the blocks when available
        }
    }
}
