package teachingtutorials.tutorialplaythrough.fundamentalTasks;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.tutorialplaythrough.GroupPlaythrough;
import teachingtutorials.tutorialobjects.LocationTask;
import teachingtutorials.tutorialplaythrough.Lesson;
import teachingtutorials.tutorialplaythrough.PlaythroughTask;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.GeometricUtils;
import teachingtutorials.utils.WorldEdit;

import java.util.ArrayList;
import java.util.logging.Level;

/**
 * Represents different types of command task actions
 */
enum CommandActionType
{
    none, virtualBlocks, full
}

/**
 * Represents a type of Task where the user must run a command. Contains the relevant listeners used when the task is active.
 */
public class Command extends PlaythroughTask implements Listener
{
    /** The base command */
    private String szTargetCommand;

    /** Any command arguments */
    private String szTargetCommandArgs;

    /** The action type */
    private CommandActionType actionType;

    /** Stores a list of tasks within the group. This is used when the command has been recorded when creating a location
     * in order to calculate the necessary virtual blocks to be created */
    private ArrayList<PlaythroughTask> tasksInGroup;

    /**
     * Used when initialising a task for a lesson, i.e when the answers are already known
     * @param plugin A reference to the TeachingTutorials plugin instance
     * @param player A reference to the Bukkit player for which the task is for
     * @param locationTask A reference to the location task object of this command
     * @param groupPlaythrough A reference to the parent group play-through object which this task is a member of
     * @param previousTasks A reference to the list of play-through tasks for the group which this task belongs to
     */
    Command(TeachingTutorials plugin, Player player, LocationTask locationTask, GroupPlaythrough groupPlaythrough, ArrayList<PlaythroughTask> previousTasks)
    {
        super(plugin, player, locationTask, groupPlaythrough);

        //Extracts the answers
        String[] commandAnswers = locationTask.getAnswer().split(",");

        //Extracts the root command
        this.szTargetCommand = commandAnswers[0];

        //Builds the string of command args
        this.szTargetCommandArgs = "";
        for (int i = 1 ; i < commandAnswers.length ; i++)
        {
            this.szTargetCommandArgs = this.szTargetCommandArgs + commandAnswers[i];
        }

        //Adds a space before the correct command arguments if there is an argument required
        //This is needed because a command with no argument will have no trailing space, but a command with arguments does have a space after the base command
        if (!this.szTargetCommandArgs.equals(""))
        {
            this.szTargetCommandArgs = " " + szTargetCommandArgs;
        }

        //Uses the details of the command from the DB and determines what action should be taken after completion
        this.actionType = CommandActionType.valueOf(locationTask.szDetails);

        //Makes the calculation of virtual blocks if it is a virtual blocks action command
        if (actionType.equals(CommandActionType.virtualBlocks))
        {
            calculateVirtualBlocks(previousTasks);
        }
    }

    /**
     * Used when initialising a task when creating a new location
     * @param plugin A reference to the TeachingTutorials plugin instance
     * @param player A reference to the Bukkit player for which the task is for
     * @param task A reference to the task
     * @param groupPlaythrough A reference to the parent group play-through object which this task is a member of
     * @param tasks Stores a list of tasks within the parent group
     */
    Command(TeachingTutorials plugin, Player player, Task task, GroupPlaythrough groupPlaythrough, ArrayList<PlaythroughTask> tasks)
    {
        super(plugin, player, task, groupPlaythrough);

        //Uses the details of the command from the DB and determines what action should be taken after completion
        this.actionType = CommandActionType.valueOf(task.szDetails);

        //Loads the tasks into a global list for use later
        tasksInGroup = tasks;
    }

    /**
     * Registers the task listener, activating the task
     */
    @Override
    public void register()
    {
        //Output the required command to assist debugging
        if (!this.parentGroupPlaythrough.getParentStep().getParentStage().bLocationCreation)
            plugin.getLogger().log(Level.INFO, "Lesson: " +((Lesson) this.parentGroupPlaythrough.getParentStep().getParentStage().getTutorialPlaythrough()).getLessonID()
                    +". Task: " +this.getLocationTask().iTaskID);
        else
            plugin.getLogger().log(Level.INFO, "New Location being made by :"+player.getName()
                    +". Command Task: " +this.getLocationTask().iTaskID);

        super.register();

        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Detects player command events, determines if it is from the player of this task, and if so performs necessary logic
     * @param event A reference to a player command preprocess event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void commandEvent(PlayerCommandPreprocessEvent event)
    {
        fPerformance = 0F;

        //Checks that the command is from the relevant player
        if (!event.getPlayer().getUniqueId().equals(player.getUniqueId()))
        {
            return;
        }

        //Extracts the command
        String command = event.getMessage();

        //Checks whether we are creating a new location
        if (bCreatingNewLocation) //Set the answers
        {
            LocationTask locationTask = getLocationTask();

            //Catches the /tutorials command
            //Note that this listener will still actually be active even as the logic for the difficulty is going through
            //If this listener is performed first, the message will get sent that you can't run /tutorials
            //Or it would change the desired command if not caught. It also then cancels the command so the difficulty wouldn't work
            //If this listener is performed second, then the difficulty listener would've already unregistered and an unnecessary message would go through
            if (command.startsWith("/tutorials"))
            {
                if (!difficultyListener.getIsReady())
                {
                    Display.ActionBar(player, Display.colouredText("You cannot set /tutorials an an answer. Did you make a mistake?", NamedTextColor.RED));
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

            //Data is added to database once difficulty is provided

            //Prompt difficulty
            player.sendMessage(player, Display.aquaText("Enter the difficulty of that command from 0 to 1 as a decimal. Use /tutorials [difficulty]"));

            //SpotHit is then called from inside the difficulty listener once the difficulty has been established
            //This is what moves it onto the next task

            //Does the desired command action
            switch (actionType)
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

        //Not a new location - a lesson playthrough
        else if (command.startsWith("/"+szTargetCommand))
        {
            command = command.replace(("/"+szTargetCommand), "");
            if (command.equals(szTargetCommandArgs))
            {
                Display.ActionBar(player, Display.colouredText("Correct command", NamedTextColor.DARK_GREEN));
                commandComplete();
                fPerformance = 1F;

                switch (actionType)
                {
                    case full:
                        break;
                    case virtualBlocks:
                        //Displays the precalculated virtual blocks
                        displayVirtualBlocks();

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
                Display.ActionBar(player, Display.colouredText("Command /"+szTargetCommand +" was correct but the arguments of the command were not", NamedTextColor.GOLD));

                //Cancels the event if the args were wrong but the command was correct
                event.setCancelled(true);
            }
        }
    }

    /**
     * Handles completion of the task - unregisters listeners and moves onto the next task
     */
    private void commandComplete()
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
     * Will unregister the command task and move forwards to the next task
     */
    public void newLocationSpotHit()
    {
        commandComplete();
    }

    //-------------------------------------------------
    //----------------------Utils----------------------
    //-------------------------------------------------

    /**
     * Calculates the list of virtual blocks to be displayed upon completion of this command
     * @param tasks A list of tasks within the parent group, which should contain a selection task
     */
    private void calculateVirtualBlocks(ArrayList<PlaythroughTask> tasks)
    {
        // For now, this only works for cuboid selections, but more selection mechanics will be made available in the future)

        int iOrder = super.getLocationTask().iOrder;

        //Gets the selection task associated with this command (assumes it is the previous task in the group)
        if (iOrder == 1) //Checks if this is the first task in the group
        {
            plugin.getLogger().log(Level.WARNING, ChatColor.RED +"There was no previous selection task before the virtual blocks command task, changing to a no action command task");

            //Sets the action type to none - adds no virtual blocks to the list
            this.actionType = teachingtutorials.tutorialplaythrough.fundamentalTasks.CommandActionType.none;
        }
        else if (!(tasks.get(iOrder - 2) instanceof Selection selection)) //Checks whether the previous task was a selection task
        {
            plugin.getLogger().log(Level.WARNING, ChatColor.RED +"There was no previous selection task before the virtual blocks command task, changing to a no action command task");

            //Sets the action type to none - adds no virtual blocks to the list
            this.actionType = teachingtutorials.tutorialplaythrough.fundamentalTasks.CommandActionType.none;
        }
        else
        {
            //---------------------------------------------------------
            //--------Calculate location and material of blocks--------
            //---------------------------------------------------------

            //If it's a virtual blocks command type, assume a selection task was the previous task - this has already been fetched above

            //Gets the associated selection of this virtual blocks type command - we want the actual answers
            double[] dTargetCoords1 = selection.dTargetCoords1;
            double[] dTargetCoords2 = selection.dTargetCoords2;

            //Gets the world
            World world = parentGroupPlaythrough.getParentStep().getParentStage().getLocation().getWorld();

            //Converts the selection points to bukkit location
            Location location1 = GeometricUtils.convertToBukkitLocation(world, dTargetCoords1[0], dTargetCoords1[1]);
            Location location2 = GeometricUtils.convertToBukkitLocation(world, dTargetCoords2[0], dTargetCoords2[1]);
            if (location1 == null || location2 == null)
                return;

            //Turns the bukkit locations into WorldEdit Block3Vectors
            BlockVector3 selectionPoint1 = BlockVector3.at(location1.getBlockX(), (int) dTargetCoords1[2], location1.getBlockZ());
            BlockVector3 selectionPoint2 = BlockVector3.at(location2.getBlockX(), (int) dTargetCoords2[2], location2.getBlockZ());

            //Creates the 'correct' WorldEdit selection region based on the selection task's selection
            RegionSelector regionSelector = new CuboidRegionSelector(BukkitAdapter.adapt(world), selectionPoint1, selectionPoint2);

            //Calculates the virtual blocks based on the selection and the command
            //The Second argument is a reference to the virtual blocks list for this task, to which the calculation will add any virtual blocks to
            WorldEdit.BlocksCalculator(super.getLocationTask().iTaskID, virtualBlocks, regionSelector, szTargetCommand, szTargetCommandArgs.split(" "), parentGroupPlaythrough.getParentStep().getParentStage().getTutorialPlaythrough());

            //It will create a new calculation object and add this to the queue. The plugin will calculate the blocks when available and add these to the virtual blocks list sent across
        }
    }
}
