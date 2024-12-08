package teachingtutorials.tutorialplaythrough;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.newlocation.NewLocation;
import teachingtutorials.tutorialobjects.Location;
import teachingtutorials.tutorialobjects.Stage;
import teachingtutorials.utils.Display;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;

public class StagePlaythrough
{
    /** A reference to the stage which this stage playthrough is a play-through of */
    private final Stage stage;

    /** A reference to the instance of the TeachingTutorials plugin */
    private final TeachingTutorials plugin;

    /** A reference to the instance of the player who is doing this stage play-through */
    private final Player player;

    /** A reference to the tutorials playthrough which this stage is a part of */
    final TutorialPlaythrough tutorialPlaythrough;

    /** Marks whether all of the steps within the stage have been completed */
    private boolean bStageFinished;

    /** A list of all of the steps which much be completed as part of this stage */
    private ArrayList<StepPlaythrough> stepPlaythroughs = new ArrayList<>();

    /** The index (0 indexed) of the step to start next. Therefore also equals the step currently on if 1 indexed */
    private int iCurrentStep;

    /** A reference to the current step playthrough which the player is on */
    private StepPlaythrough currentStepPlaythrough;

    /** Records whether this stage playthrough is part of a location creation */
    public final boolean bLocationCreation;



    //--------------------------------------------------
    //-------------------Constructors-------------------
    //--------------------------------------------------

    /**
     * Used for initiating a stage playthrough, either as part of a Lesson or as part of a creation of a new Location
     * @param player A reference to the instance of the player who is doing this stage play-through
     * @param plugin A reference to the instance of the TeachingTutorials plugin
     * @param stage A reference to the stage which this stage playthrough is a play-through of
     * @param tutorialPlaythrough A reference to the playthrough which this stage is a part of
     */
    StagePlaythrough(Player player, TeachingTutorials plugin, Stage stage, TutorialPlaythrough tutorialPlaythrough)
    {
        //Inherited properties from the lesson
        this.plugin = plugin;
        this.player = player;
        this.tutorialPlaythrough = tutorialPlaythrough;

        this.stage = stage;

        //Other
        this.bLocationCreation = (tutorialPlaythrough instanceof NewLocation);
        this.bStageFinished = false;
    }


    //---------------------------------------------------
    //----------------------Getters----------------------
    //---------------------------------------------------
    public Stage getStage()
    {
        return stage;
    }

    public int getLocationID()
    {
        return this.tutorialPlaythrough.getLocation().getLocationID();
    }

    public Location getLocation()
    {
        return tutorialPlaythrough.getLocation();
    }

    public TutorialPlaythrough getTutorialPlaythrough()
    {
        return tutorialPlaythrough;
    }

    public Player getPlayer()
    {
        return player;
    }

    public int getCurrentStep()
    {
        return iCurrentStep;
    }

    public boolean isFirstStage()
    {
        return (stage.getOrder() == 1);
    }

    /**
     *
     * @return Whether all of the steps within this stage have been completed
     */
    public boolean isFinished()
    {
        return bStageFinished;
    }

    /**
     * Displays the virtual blocks of all tasks in all steps of this stage up to and including the iStepTH step
     * @param iStep The step up to and including which to display virtual blocks for. (This is 1 indexed. The first step has iStep = 1).
     *                     <p> </p>
     *                     If iStep = -1 then all blocks will be displayed
     *                     <P> If iStep = 0 then no blocks will be displayed</P>
     *                     <P> If iStep = 1 then the blocks from step 1 will be displayed</P>
     */
    public void displayAllVirtualBlocks(int iStep)
    {
        //Gets the steps of this stage from the DB
        fetchAndInitialiseSteps();

        //The number of steps to display the virtual blocks for
        int iNumStepsToDisplay;

        if (iStep == -1)
            iNumStepsToDisplay = stepPlaythroughs.size();
        else
            iNumStepsToDisplay = iStep;

        //Calls each step to display all relevant virtual blocks
        for (int i = 0 ; i < iNumStepsToDisplay ; i++)
        {
            stepPlaythroughs.get(i).displayAllVirtualBlocks();
        }
    }

    /**
     * Fetches the list of steps of this stage from the database and stores this in {@link #stepPlaythroughs}. The list is ordered.
     */
    private void fetchAndInitialiseSteps()
    {
        //Gets a list of all of the steps of the specified stage and loads each with the relevant data.
        //List is in order of step 1 1st
        stepPlaythroughs = StepPlaythrough.fetchStepsByStageID(player, plugin, this);
    }

    /**
     * Begins the playthrough of this stage: Displays the stage title, fetches and initialises each step, and begins the
     * first step
     * @param iStepToStartStageOn The step to start the stage on (1 indexed). For example, to start the stage from the
     *                            start (step 1), this value should be 1.
     * @param bDelayTitle Whether to delay the display of the title of the stage and title of the first step
     */
    public void startStage(int iStepToStartStageOn, boolean bDelayTitle)
    {
        //Starting new stage message
        if (this.getTutorialPlaythrough() instanceof Lesson lesson)
        {
            plugin.getLogger().log(Level.INFO, "Lesson: "+lesson.getLessonID() +". Stage " +this.stage.getOrder()
                    +" (" +this.stage.getName() +") of tutorial with ID "+this.getTutorialPlaythrough().getTutorial().getTutorialID() +" starting.");
        }
        else
        {
            plugin.getLogger().log(Level.INFO, "New location of " +this.getTutorialPlaythrough().creatorOrStudent.player.getName() +". Stage " +this.stage.getOrder()
                    +" (" +this.stage.getName() +") of tutorial with ID "+this.getTutorialPlaythrough().getTutorial().getTutorialID() +" starting.");
        }

        //Display the Stage title
        if (bDelayTitle)
        {
            Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
                @Override
                public void run() {
                    Display.Title(player, " ", ChatColor.AQUA +"Stage " +stage.getOrder() +" - " +stage.getName(), 10, 60, 12);
                }
            }, this.plugin.getConfig().getLong("Stage_Title_Delay_On_Start"));
        }
        else
            Display.Title(player, " ", ChatColor.AQUA +"Stage " +stage.getOrder() +" - " +stage.getName(), 10, 60, 12);

        //Get the steps
        fetchAndInitialiseSteps();

        //Takes the step back, for it to be increased in the next step - as in, we are currently on step 0 to make the
        // next step be step 1
        iCurrentStep = iStepToStartStageOn - 1;
        nextStep(bDelayTitle);
    }

    /**
     * Gets the next step and launches it, or if all steps have now been completed, will call for the next stage to be
     * started.
     * @param bDelayTitle Whether to delay the display of the title of the step
     */
    protected void nextStep(boolean bDelayTitle)
    {
        //1 indexed
        iCurrentStep++;

        if (iCurrentStep <= stepPlaythroughs.size())
        {
            //Uses -1 because iCurrentStep is 1 indexed, so need it in computer terms
            currentStepPlaythrough = stepPlaythroughs.get(iCurrentStep-1);
            currentStepPlaythrough.startStep(bDelayTitle);

            if (bLocationCreation == false)
            {
                //Save the positions of stage and step
                ((Lesson) tutorialPlaythrough).savePositions();
            }
        }
        else
        {
            bStageFinished = true;
            tutorialPlaythrough.nextStage(1, false);
        }
    }

    /**
     * Safely terminates the current step
     */
    public void terminateEarly()
    {
        currentStepPlaythrough.terminateEarly();
    }

    /**
     * Fetches a list of stages from the DB, creates a set of StagePlaythroughs from these, knowing the location, and
     * returns this list. The list is ordered, such that the first stage to play through is at index 0.
     * @param player A reference to the player for whom the lesson will be for
     * @param plugin A reference to the instance of the TeachingTutorials plugin
     * @param tutorialPlaythrough A reference to the tutorialPlaythrough for which these stage playtrhoughs will be a part of
     * @return An array list of StagePlaythroughs for the relevant lesson, tutorial and location
     */
    public static ArrayList<StagePlaythrough> fetchStagesByTutorialIDForPlaythrough(Player player, TeachingTutorials plugin, TutorialPlaythrough tutorialPlaythrough)
    {
        ArrayList<StagePlaythrough> stagePlaythroughs = new ArrayList<>();

        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            //Compiles the command to fetch stages
            sql = "SELECT * FROM `Stages` WHERE `TutorialID` = "+tutorialPlaythrough.getTutorial().getTutorialID() +" ORDER BY 'Order' ASC";
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            while (resultSet.next())
            {
                Stage stage = new teachingtutorials.tutorialobjects.Stage(resultSet.getInt("StageID"), resultSet.getInt("Order"), resultSet.getString("StageName"));
                StagePlaythrough stagePlaythrough = new StagePlaythrough(player, plugin, stage, tutorialPlaythrough);
                stagePlaythroughs.add(stagePlaythrough);
            }
        }
        catch (SQLException se)
        {
            plugin.getLogger().log(Level.SEVERE, "SQL - SQL Error fetching Stages by TutorialID", se);
        }
        catch (Exception e)
        {
            plugin.getLogger().log(Level.SEVERE, "SQL - Non-SQL Error fetching Stages by TutorialID", e);
        }
        return stagePlaythroughs;
    }
}
