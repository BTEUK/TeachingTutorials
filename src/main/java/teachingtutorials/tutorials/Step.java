package teachingtutorials.tutorials;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.fundamentalTasks.TpllListener;
import teachingtutorials.guis.locationcreatemenus.StepEditorMenu;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class Step
{
    private String szName;
    private Display.DisplayType instructionDisplayType;
    private Player player;
    private TeachingTutorials plugin;
    public Stage parentStage;
    private VideoLinkCommandListener videoLinkListener;

    /**
     * Notes whether all tasks have been completed/set or not
     */
    public boolean bStepFinished;
    protected int iStepID;
    protected int iStepInStage;

    //Stores the location specific step data
    private LocationStep locationStep;

    private int iGroupInStepLocationCreation;
    private Group currentGroup;

    //Handle multiple tasks being registered and the way they depend on each other
    private boolean selectionCompleteHold;
    public ArrayList<TpllListener> handledTpllListeners = new ArrayList<>();
    public boolean bTpllDistanceMessageQueued;
    public boolean bPointWasHit = false;

    //Groups are completed asynchronously.
    //Tasks in groups are completed synchronously
    public ArrayList<Group> groups = new ArrayList<>();

    private StepEditorMenu menu;

    //Used for creating a step for a lesson
    public Step(int iStepID, int iStepInStage, String szStepName, Player player, TeachingTutorials plugin, Stage parentStage, String szInstructionDisplayType)
    {
        this.player = player;
        this.plugin = plugin;
        this.parentStage = parentStage;
        this.bStepFinished = false;
        this.iStepID = iStepID;
        this.iStepInStage = iStepInStage;
        this.szName = szStepName;
        setInstructionDisplayType(szInstructionDisplayType);
        this.selectionCompleteHold = false;

        if (parentStage.bLocationCreation)
            //Initialises location step
            this.locationStep = new LocationStep(parentStage.getLocationID(), iStepID, getInstructionDisplayType().equals(Display.DisplayType.hologram));
        else
            //Gets the location specific data
            this.locationStep = LocationStep.getFromStepAndLocation(this.iStepID, this.parentStage.tutorialPlaythrough.getLocation().getLocationID(), getInstructionDisplayType().equals(Display.DisplayType.hologram));

        //Initialises the video link listener
        videoLinkListener = new VideoLinkCommandListener(this.plugin, this.player, this.locationStep);
    }

    //Used for adding a step to the DB
    public Step(String szName, String szInstructionDisplayType)
    {
        this.szName = szName;
        setInstructionDisplayType(szInstructionDisplayType);
        this.selectionCompleteHold = false;
    }

    //Getters
    public String getName()
    {
        return szName;
    }

    public void setInstructionDisplayType(String szInstructionDisplayType)
    {
        Display.DisplayType displayType;
        try {
            displayType = Display.DisplayType.valueOf(szInstructionDisplayType);
        }
        catch (IllegalArgumentException e) {
            Bukkit.getConsoleSender().sendMessage("The step instruction display type was not properly specified ("+szInstructionDisplayType +"), reverting to chat");
            displayType = Display.DisplayType.chat;
        }
        this.instructionDisplayType = displayType;
    }

    public Display.DisplayType getInstructionDisplayType()
    {
        return this.instructionDisplayType;
    }

    public boolean getSelectionCompleteHold()
    {
        return selectionCompleteHold;
    }

    public void displayAllVirtualBlocks()
    {
        //Gets the groups from the DB
        fetchAndInitialiseGroups();

        int iNumGroups = groups.size();
        for (int i = 0 ; i < iNumGroups ; i++)
        {
            groups.get(i).displayAllVirtualBlocks();
        }
    }

    private void fetchAndInitialiseGroups()
    {
        groups = Group.fetchGroupsByStepID(player, plugin, this);
    }

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

    public void calculateNearestTpllPointAfterWait()
    {
        bTpllDistanceMessageQueued = true;

        //Calculates the tpll point closest to where they tplled to
        this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
        {
            public void run()
            {
                if (bPointWasHit)
                {
                    bPointWasHit = false;
                }
                else
                {
                    bTpllDistanceMessageQueued = false;

                    float fShortestGeometricDistance;

                    if (handledTpllListeners.size() > 0)
                    {
                        fShortestGeometricDistance = handledTpllListeners.get(0).fGeometricDistance;
                        handledTpllListeners.remove(0);
                    }
                    else
                    {
                        //Should never be reached in reality because this function is only ever called from a tpll lister
                        //immediately after it adds itself to this ArrayList
                        return;
                    }

                    while (handledTpllListeners.size() > 0)
                    {
                        TpllListener handledTpllListener = handledTpllListeners.get(0);
                        float fGeometricDistance =handledTpllListener.fGeometricDistance;
                        if (fGeometricDistance < fShortestGeometricDistance)
                            fShortestGeometricDistance = fGeometricDistance;
                        handledTpllListeners.remove(0);
                    }
                    Display display = new Display(player, ChatColor.GOLD +"You were "+fShortestGeometricDistance +" metres away from a tpll target point");
                    display.ActionBar();
                }
            }
        }, 2L);
    }

    /**
     * Starts the player on this step. Sends them the title of the step, registers the fall listener,
     * teleports them to the start, displays the instructions and initialises the groups of tasks
     */
    public void startStep()
    {
        //Display step title
        Display display = new Display(player, " ");

        //Wait a second before sending it if it is the first step in a stage. We don't want to override the stage title
        if (iStepInStage == 1)
        {
            final Display finalDisplay = display;
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    finalDisplay.Title(ChatColor.AQUA +"Step " +iStepInStage +" - " +szName, 10, 60, 12);
                }
            }, 76L);
        }
        else
        {
            display.Title(ChatColor.AQUA +"Step " +iStepInStage +" - " +szName, 10, 60, 12);
        }

        //Fetches the details of groups and stores them in memory
        fetchAndInitialiseGroups();
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] " +groups.size() +" groups fetched");

        //Player is a student doing a tutorial
        if (!parentStage.bLocationCreation)
        {
            //Registers the video link listener
            videoLinkListener.register();

            //Register the start of all groups
            int i;
            int iGroups = groups.size();

            for (i = 0; i < iGroups; i++)
            {
                final int I = i;
                //Registers each group 0.2 seconds apart from each other - this is to allow all world edit block calculations to complete
                //WE block calculations involves running the WE command over the console and detecting and recording changes, before
                // then removing those changes from the actual world. This involves delicate use of listeners, hence this 0.1 second control
                Bukkit.getScheduler().runTaskLater(TeachingTutorials.getInstance(), () ->
                {
                    groups.get(I).initialRegister();
                    Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Registered group "+(I+1));
                }, i*8L);
            }

            //TP to start location, and store this location for later use
            Location startLocation = locationStep.teleportPlayerToStartOfStep(player, parentStage.tutorialPlaythrough.getLocation().getWorld(), plugin);

            //Updates the fall listener
            parentStage.tutorialPlaythrough.setFallListenerSafeLocation(startLocation);

            //Displays the step instructions
            this.locationStep.displayInstructions(getInstructionDisplayType(), player, szName, parentStage.tutorialPlaythrough.getLocation().getWorld());
        }

        //Player is a creator creating a new location for a tutorial
        else
        {
            //Register the start of the first group
            //If a location is being created, groups are made synchronous rather than asynchronous
            currentGroup = groups.get(0);
            iGroupInStepLocationCreation = 1;
            currentGroup.initialRegister();
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Registered group "+iGroupInStepLocationCreation +" of step");

            //Creates the menu, assigns it to the user
            User user = parentStage.tutorialPlaythrough.getCreatorOrStudent();
            menu = new StepEditorMenu(plugin, user, this, this.locationStep);
            if (user.mainGui != null)
                user.mainGui.delete();
            user.mainGui = menu;
        }
    }

    protected void groupFinished()
    {
        int i;
        int iGroups = groups.size();

        boolean bAllGroupsFinished = true;

        //Different logic needed as location creation groups are performed in sync
        if (parentStage.bLocationCreation)
        {
            //iGroupInStepLocationCreation is 1 indexed
            if (iGroupInStepLocationCreation == groups.size()) //If the current group is the last group
            {
                bAllGroupsFinished = true;
            }
            else
            {
                bAllGroupsFinished = false;
                Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"  [TeachingTutorials] Registering group "+iGroupInStepLocationCreation);
                iGroupInStepLocationCreation++;
                currentGroup = groups.get(iGroupInStepLocationCreation-1);
                currentGroup.initialRegister();
                Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"  [TeachingTutorials] Registered group "+iGroupInStepLocationCreation);
            }
        }
        else
        {
            //Goes through all groups and checks if one of them is not finished yet
            for (i = 0 ; i < iGroups ; i++)
            {
                if (!groups.get(i).groupFinished)
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
            if (parentStage.bLocationCreation)
            {
                //Checks whether the additional information is set - start location and instructions etc
                if (locationStep.isOtherInformationSet())
                    tryNextStep();
                else
                {
                    Display display = new Display(player, Component.text("You must now set the step's start location and instructions. Use the learning menu", NamedTextColor.RED));
                    display.Message();

                    //Sets the player's menu as the step editor menu
                    this.parentStage.tutorialPlaythrough.getCreatorOrStudent().mainGui = menu;

                    //Opens the step editor menu
                    menu.open(this.parentStage.tutorialPlaythrough.getCreatorOrStudent());

                    //We wait and then perform the code in the if statement above once the location has been set, via tryNextStep()
                }
            }
            else
            {
                //Unregisters the video link listener
                videoLinkListener.unregister();

                //Remove hologram
                if (getInstructionDisplayType().equals(Display.DisplayType.hologram))
                    locationStep.removeInstructionsHologram();

                //Calls stage to start the next step
                parentStage.nextStep();
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
        if (!parentStage.bLocationCreation)
            return;

        if (bStepFinished)
        {
            if (locationStep.isOtherInformationSet())
            {
                //Remove hologram
                if (getInstructionDisplayType().equals(Display.DisplayType.hologram))
                    locationStep.removeInstructionsHologram();

                //Deletes menu
                menu.delete();
                menu = null;

                locationStep.storeDetailsInDB();
                parentStage.nextStep();
            }
            else
            {
                Display display = new Display(player, ChatColor.GREEN +"Continue to set the additional information, use the learning menu");
                display.Message();
            }
        }
        else
        {
            Display display = new Display(player, ChatColor.GREEN +"Continue to set the answers");
            display.Message();
        }
    }

    /**
     * Use this if a manual termination of the tutorial must occur, for example a player leaves the server. This will terminate the step, unregister listeners and unregister the tasks.
     */
    public void terminateEarly()
    {
        //Unregisters the video link listener
        videoLinkListener.unregister();

        //Unregisters the current task listener
        if (parentStage.bLocationCreation)
        {
            currentGroup.terminateEarly();
            menu.delete();
            menu = null;

            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"  [TeachingTutorials] Unregistered group "+iGroupInStepLocationCreation);
        }

        //Unregisters the task listeners
        else
        {
            int i;
            int iGroups = groups.size();
            for (i = 0; i < iGroups; i++)
            {
                Group group = groups.get(i);
                group.terminateEarly();
                Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"  [TeachingTutorials] Unregistered group "+(i+1));
            }
        }

        //Remove holograms
        if (getInstructionDisplayType().equals(Display.DisplayType.hologram))
            this.locationStep.removeInstructionsHologram();
    }

    /**
     * Retrieves from the database the list of steps for the specified stage
     * @param player The player playing through the tutorial
     * @param plugin The instance of the plugin
     * @param stage The stage for which all steps must be retrieved
     * @return A list of steps for this stage
     */
    public static ArrayList<Step> fetchStepsByStageID(Player player, TeachingTutorials plugin, Stage stage)
    {
        ArrayList<Step> steps = new ArrayList<>();

        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            //Compiles the command to fetch steps
            sql = "SELECT * FROM `Steps` WHERE `StageID` = "+stage.iStageID +" ORDER BY 'StepInStage' ASC";
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            while (resultSet.next())
            {
                Step step = new Step(resultSet.getInt("StepID"), resultSet.getInt("StepInStage"), resultSet.getString("StepName") ,player, plugin, stage, resultSet.getString("InstructionDisplay"));
                steps.add(step);
            }
        }
        catch(SQLException se)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - SQL Error fetching Steps by StageID");
            se.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return steps;
    }
}