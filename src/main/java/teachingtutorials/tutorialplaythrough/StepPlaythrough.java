package teachingtutorials.tutorialplaythrough;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.tutorialobjects.LocationStep;
import teachingtutorials.tutorialobjects.Step;
import teachingtutorials.tutorialplaythrough.fundamentalTasks.Tpll;
import teachingtutorials.guis.locationcreatemenus.StepEditorMenu;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.Hologram;
import teachingtutorials.utils.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;

public class StepPlaythrough
{
    /** A reference to the step of which this is a play-through */
    private final Step step;

    /** A reference to the parent stage which this step playthrough is a member of */
    final StagePlaythrough parentStagePlaythrough;

    /** A reference to the instance of the TeachingTutorials plugin */
    private final TeachingTutorials plugin;

    /** A reference to the instance of the player who is doing this step play-through */
    private final Player player;

    /** Notes whether all tasks have been completed/set or not */
    public boolean bStepFinished;

    /** Stores the location specific step data */
    private LocationStep locationStep;

    /** The list of group playthroughs which must be completed as part of this step */
    public ArrayList<GroupPlaythrough> groupPlaythroughs = new ArrayList<>();


    /** Marks when a selection has been complete and we don't want any other selection tasks to override the hotbar message */
    private boolean selectionCompleteHold;

    /** A list of tpll listeners under this step recently triggered */
    public ArrayList<Tpll> handledTpllListeners = new ArrayList<>();

    /** Whether a tpll listeners under this step has recently been triggered */
    public boolean bTpllDistanceMessageQueued; // This should be atomic really

    /** Whether a tpll listener recently triggered was completed successfully */
    public boolean bPointWasHit;


    /** A listener used to listen for the video link command */
    private VideoLinkCommandListener videoLinkListener;

    /** A Hologram object for the hologram displaying the instructions of this LocationStep */
    private Hologram instructions;

    //--- New Locations---

    /** The index of the group within the group list which the creator is currently on - when creating a new location */
    private int iGroupInStepLocationCreation;

    /** The current group - used when creating a new location - when groups are synchronous */
    private GroupPlaythrough currentGroupPlaythrough;

    /** The step editor menu - used when creating new locations*/
    private StepEditorMenu menu;

    /**
     *
     * @param player A reference to the instance of the player who is doing this step play-through
     * @param plugin A reference to the instance of the TeachingTutorials plugin
     * @param parentStagePlaythrough A reference to the parent stage which this step playthrough is a member of
     * @param step A reference to the step of which this is a play-through
     */
    StepPlaythrough(Player player, TeachingTutorials plugin, StagePlaythrough parentStagePlaythrough, Step step)
    {
        this.player = player;
        this.plugin = plugin;
        this.step = step;
        this.parentStagePlaythrough = parentStagePlaythrough;

        if (parentStagePlaythrough.bLocationCreation)
            //Initialises location step
            this.locationStep = new LocationStep(parentStagePlaythrough.tutorialPlaythrough.location, step);
        else
            //Gets the location specific data
            this.locationStep = LocationStep.getFromStepAndLocation(parentStagePlaythrough.tutorialPlaythrough.location, step);

        //Initialises the video link listener
        videoLinkListener = new VideoLinkCommandListener(this.plugin, this.player, this.locationStep);

        this.bStepFinished = false;
        this.selectionCompleteHold = false;
        this.bPointWasHit = false;
    }

    //Getters

    /**
     *
     * @return A reference to the step of which this is a playthrough
     */
    public Step getStep()
    {
        return this.step;
    }

    /**
     *
     * @return A reference to the parent stage which this step playthrough is a member of
     */
    public StagePlaythrough getParentStage()
    {
        return this.parentStagePlaythrough;
    }

    /** Returns whether a selection has been completed within the past 0.5 seconds */
    public boolean getSelectionCompleteHold()
    {
        return selectionCompleteHold;
    }

    /**
     * Displays the virtual blocks of all tasks is this step
     */
    public void displayAllVirtualBlocks()
    {
        //Gets the groups from the DB
        fetchAndInitialiseGroups();

        int iNumGroups = groupPlaythroughs.size();
        for (int i = 0 ; i < iNumGroups ; i++)
        {
            groupPlaythroughs.get(i).displayAllVirtualBlocks();
        }
    }

    /**
     * Displays the instructions to the player
     * @param displayType The way the instruction should be displayed
     * @param player The player to which the instruction should be displayed
     */
    public void displayInstructions(Display.DisplayType displayType, Player player, String szStepName, World world)
    {
        switch (displayType)
        {
            case hologram:
                instructions = new Hologram(this.locationStep.getHologramLocation(world), this.parentStagePlaythrough.tutorialPlaythrough, ChatColor.AQUA +"" +ChatColor.UNDERLINE +ChatColor.BOLD +szStepName, this.locationStep.getInstructions(), this.step.getStepID());
                instructions.showHologram();
                break;
            default:
                player.sendMessage(this.locationStep.getInstructions());
                break;
        }
    }

    /**
     * Removes the hologram from view if it is displayed
     */
    public void removeInstructionsHologram()
    {
        if (instructions != null)
            instructions.removeHologram();
    }

    /**
     * Removes the given player from being able to view the hologram
     */
    void removePlayerFromHologram(Player player)
    {
        this.instructions.removePlayerVisibility(player);
    }

    /**
     * Refreshes the hologram view list based on the spy list of the master playthrough
     */
    void refreshHologramViewers()
    {
        this.instructions.showHologram();
    }

    /**
     * Fetches the list of groups of this step from the database and stores this in {@link #groupPlaythroughs}.
     * <p> </p>
     * The order of groups does not matter and as such the order that they are fetched and stored in doesn't matter.
     */
    private void fetchAndInitialiseGroups()
    {
        groupPlaythroughs = GroupPlaythrough.fetchGroupsByStepID(plugin, this);
    }

    /**
     * Marks that a selection has been completed, and therefore that no "point selected" should override the hotbar
     */
    public void holdSelectionComplete()
    {
        selectionCompleteHold = true;

        //Changes the hold back to false in 0.5 seconds
        this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
        {
            public void run()
            {
                selectionCompleteHold = false;
            }
        }, 10L);
    }

    /**
     * After 2 ticks, goes through the list of recently triggered tpll listeners and work out which tpll task was
     * closest to the tpll command, and output the distance. If a tpll command was completed successfully nothing is
     * outputted
     * <p> </p>
     * This would be called whenever a tpll command task is run whilst tpll tasks are active, but isn't because the
     * first one which calls it triggers the bTpllDistanceMessageQueued boolean to true, which means it is only run once
     * per tpll command.
     * <p> </p>
     * The two tick pause allows all tpll tasks to add themselves to the list to be compared
     */
    public void calculateNearestTpllPointAfterWait()
    {
        bTpllDistanceMessageQueued = true;

        //Calculates the tpll point closest to where they tplled to
        this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
        {
            public void run()
            {
                plugin.getLogger().log(Level.INFO, "Running a distance checker now");

                bTpllDistanceMessageQueued = false;

                //If a point was hit, don't display any message
                if (bPointWasHit)
                {
                    plugin.getLogger().log(Level.INFO, "A point was hit, cancelling");
                    bPointWasHit = false;
                    //And remove all of them
                    while (handledTpllListeners.size() > 0)
                    {
                        handledTpllListeners.remove(0);
                    }
                }
                else
                {
                    bTpllDistanceMessageQueued = false;

                    float fShortestGeometricDistance;

                    if (handledTpllListeners.size() > 0)
                    {
                        fShortestGeometricDistance = handledTpllListeners.get(0).getGeometricDistance();
                        handledTpllListeners.remove(0);
                    }
                    else
                    {
                        //Should never be reached in reality because this function is only ever called from a
                        // tpll listener immediately after it adds itself to this ArrayList
                        return;
                    }

                    while (handledTpllListeners.size() > 0)
                    {
                        Tpll handledTpllListener = handledTpllListeners.get(0);
                        float fGeometricDistance = handledTpllListener.getGeometricDistance();
                        if (fGeometricDistance < fShortestGeometricDistance)
                            fShortestGeometricDistance = fGeometricDistance;
                        handledTpllListeners.remove(0);
                    }
                    Display.ActionBar(player, Display.colouredText("You were "+fShortestGeometricDistance +" metres away from a tpll target point", NamedTextColor.GOLD));
                }
            }
        }, 2L);
    }

    /**
     * Starts the player on this step. Sends them the title of the step, registers the fall listener,
     * teleports them to the start, displays the instructions and initialises the groups of tasks
     */
    public void startStep(boolean bDelayTitle)
    {
        //Starting new step message
        if (this.parentStagePlaythrough.getTutorialPlaythrough() instanceof Lesson lesson)
        {
            plugin.getLogger().log(Level.INFO, "Lesson: "+lesson.getLessonID() +". Player: "+this.player.getName() +". Step " +this.step.getStepInStage()
                    +" (" +this.step.getName() +") of stage "+this.parentStagePlaythrough.getStage().getOrder() +" starting.");
        }
        else
        {
            plugin.getLogger().log(Level.INFO, "New location of " +this.player.getName() +". Step " +this.step.getStepInStage()
                    +" (" +this.step.getName() +") of stage "+this.parentStagePlaythrough.getStage().getOrder() +" starting.");
        }

        //Wait time
        long lWaitTime = 0L;

        //Wait 72 ticks before sending it if it is the first step in a stage. We don't want to override the stage title
        if (step.getStepInStage() == 1)
            lWaitTime = lWaitTime + 76L;
        //Add on the time it took for the stage title to be displayed
        if (bDelayTitle)
            lWaitTime = lWaitTime + this.plugin.getConfig().getLong("Stage_Title_Delay_On_Start");

        //Display the title
        if (lWaitTime == 0L)
            Display.Title(player, " ", ChatColor.AQUA +"Step " +step.getStepInStage() +" - " +step.getName(), 10, 60, 12);
        else
        {
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    Display.Title(player, " ", ChatColor.AQUA +"Step " +step.getStepInStage() +" - " +step.getName(), 10, 60, 12);
                }
            }, lWaitTime);
        }

        //Fetches the details of groups and stores them in memory
        fetchAndInitialiseGroups();
        plugin.getLogger().log(Level.INFO, groupPlaythroughs.size() +" groups fetched");

        //Player is a student doing a tutorial
        if (!parentStagePlaythrough.bLocationCreation)
        {
            //Registers the video link listener
            videoLinkListener.register();

            //Register the start of all groups
            int i;
            int iGroups = groupPlaythroughs.size();

            for (i = 0; i < iGroups; i++)
            {
                final int I = i;
                groupPlaythroughs.get(I).startGroupPlaythrough();
                plugin.getLogger().log(Level.FINE, "Registered group "+(I+1));
            }

            //TP to start location, and store this location for later use
            Location startLocation = locationStep.teleportPlayerToStartOfStep(player, parentStagePlaythrough.tutorialPlaythrough.getLocation().getWorld(), plugin);

            //Updates the fall listener
            parentStagePlaythrough.tutorialPlaythrough.setFallListenerSafeLocation(startLocation);

            //Displays the step instructions
            displayInstructions(step.getInstructionDisplayType(), player, step.getName(), parentStagePlaythrough.tutorialPlaythrough.getLocation().getWorld());
        }

        //Player is a creator creating a new location for a tutorial
        else
        {
            //Register the start of the first group
            //If a location is being created, groups are made synchronous rather than asynchronous
            currentGroupPlaythrough = groupPlaythroughs.get(0);
            iGroupInStepLocationCreation = 1;
            currentGroupPlaythrough.startGroupPlaythrough();
            plugin.getLogger().log(Level.FINE, "Registered group "+iGroupInStepLocationCreation +" of step");

            //Creates the menu, assigns it to the user
            User user = parentStagePlaythrough.tutorialPlaythrough.getCreatorOrStudent();
            menu = new StepEditorMenu(plugin, user, this, this.locationStep);
            if (user.mainGui != null)
                user.mainGui.delete();
            user.mainGui = menu;
        }
    }

    /**
     * Called when a group is finished, checks the status of all groups and calls stepFinished() if all groups are now
     * complete.
     * <p> </p>
     * If in a Location creation, moves the step onto the next group
     */
    protected void groupFinished()
    {
        int i;
        int iGroups = groupPlaythroughs.size();

        boolean bAllGroupsFinished = true;

        //Different logic needed as location creation groups are performed in sync
        if (parentStagePlaythrough.bLocationCreation)
        {
            //iGroupInStepLocationCreation is 1 indexed
            if (iGroupInStepLocationCreation == groupPlaythroughs.size()) //If the current group is the last group
            {
                bAllGroupsFinished = true;
            }
            else
            {
                bAllGroupsFinished = false;
                plugin.getLogger().log(Level.FINE, "Registered group "+iGroupInStepLocationCreation +" of step");
                iGroupInStepLocationCreation++;
                currentGroupPlaythrough = groupPlaythroughs.get(iGroupInStepLocationCreation-1);
                currentGroupPlaythrough.startGroupPlaythrough();
                plugin.getLogger().log(Level.FINE, "Registered group "+iGroupInStepLocationCreation +" of step");
            }
        }
        else
        {
            //Goes through all groups and checks if one of them is not finished yet
            for (i = 0 ; i < iGroups ; i++)
            {
                if (!groupPlaythroughs.get(i).getFinished())
                {
                    bAllGroupsFinished = false;
                    break;
                }
            }
        }

        if (bAllGroupsFinished == true)
        {
            //Marks the step's tasks as all finished
            this.bStepFinished = true;

            //Player has just finished setting the answers for this step
            if (parentStagePlaythrough.bLocationCreation)
            {
                //Checks whether the additional information is set - start location and instructions etc
                if (locationStep.isOtherInformationSet(plugin.getLogger()))
                    tryNextStep();
                else
                {
                    player.sendMessage(Display.colouredText("You must now set the step's start location and instructions. Use the learning menu", NamedTextColor.GOLD));

                    //Sets the player's menu as the step editor menu
                    parentStagePlaythrough.tutorialPlaythrough.getCreatorOrStudent().mainGui = menu;

                    //Opens the step editor menu
                    menu.open(parentStagePlaythrough.tutorialPlaythrough.getCreatorOrStudent());

                    //We wait and then perform the code in the if statement above once the location has been set, via tryNextStep()
                }
            }
            else
            {
                //Unregisters the video link listener
                videoLinkListener.unregister();

                //Remove hologram
                if (step.getInstructionDisplayType().equals(Display.DisplayType.hologram))
                    removeInstructionsHologram();

                //Calls stage to start the next step
                parentStagePlaythrough.nextStep(false);
            }
        }
    }

    /**
     * Will move the player on to the next step if the current step is finished - answers AND additional information set
     * <p> </p>
     * <p> This method has two uses: it is called directly after any additional step information is set.
     * It is called when the answers have just finished being set.
     * </p>
     */
    public void tryNextStep()
    {
        //Blocks any processes occurring if the method has wrongly been called from outside of location creation
        if (!parentStagePlaythrough.bLocationCreation)
            return;

        if (bStepFinished)
        {
            if (locationStep.isOtherInformationSet(plugin.getLogger()))
            {
                //Remove hologram
                if (step.getInstructionDisplayType().equals(Display.DisplayType.hologram))
                    removeInstructionsHologram();

                //Deletes menu
                menu.delete();
                menu = null;

                locationStep.storeDetailsInDB(plugin);
                parentStagePlaythrough.nextStep(false);
            }
            else
            {
                player.sendMessage(Display.colouredText("Continue to set the additional information, use the learning menu", NamedTextColor.GREEN));
            }
        }
        else
        {
            player.sendMessage(Display.colouredText("Continue to set the answers", NamedTextColor.GREEN));
        }
    }

    /**
     * Use this if a manual termination of the tutorial must occur, for example a player leaves the server. This will safely terminate the step, unregister listeners and unregister the tasks.
     */
    public void terminateEarly()
    {
        //Unregisters the video link listener
        videoLinkListener.unregister();

        //Unregisters the current task listener
        if (parentStagePlaythrough.bLocationCreation)
        {
            currentGroupPlaythrough.terminateEarly();
            menu.delete();
            menu = null;

            plugin.getLogger().log(Level.FINE, "Unregistered group "+iGroupInStepLocationCreation);
        }

        //Unregisters the task listeners
        else
        {
            int i;
            int iGroups = groupPlaythroughs.size();
            for (i = 0; i < iGroups; i++)
            {
                GroupPlaythrough groupPlaythrough = groupPlaythroughs.get(i);
                groupPlaythrough.terminateEarly();
                plugin.getLogger().log(Level.FINE, "Unregistered group "+(i+1));
            }
        }

        //Remove holograms
        if (step.getInstructionDisplayType().equals(Display.DisplayType.hologram))
            removeInstructionsHologram();
    }

    /**
     * Retrieves from the database the list of steps for the specified stage
     * @param player The player playing through the tutorial
     * @param plugin The instance of the plugin
     * @param stagePlaythrough The stage for which all steps must be retrieved
     * @return A list of steps for this stage
     */
    public static ArrayList<StepPlaythrough> fetchStepsByStageID(Player player, TeachingTutorials plugin, StagePlaythrough stagePlaythrough)
    {
        ArrayList<StepPlaythrough> stepPlaythroughs = new ArrayList<>();

        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            //Compiles the command to fetch steps
            sql = "SELECT * FROM `Steps` WHERE `StageID` = "+ stagePlaythrough.getStage().getStageID() +" ORDER BY 'StepInStage' ASC";
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            while (resultSet.next())
            {
                Step step = new Step(resultSet.getInt("StepID"), resultSet.getInt("StepInStage"), resultSet.getString("StepName"), resultSet.getString("InstructionDisplay"));
                StepPlaythrough stepPlaythrough = new StepPlaythrough(player, plugin, stagePlaythrough, step);
                stepPlaythroughs.add(stepPlaythrough);
            }
        }
        catch (SQLException se)
        {
            plugin.getLogger().log(Level.SEVERE, "SQL - SQL Error fetching Steps by StageID", se);
        }
        catch (Exception e)
        {
            plugin.getLogger().log(Level.SEVERE, "SQL - Non-SQL Error fetching Steps by StageID", e);
        }
        return stepPlaythroughs;
    }
}