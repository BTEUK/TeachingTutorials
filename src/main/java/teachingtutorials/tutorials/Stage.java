package teachingtutorials.tutorials;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.TutorialPlaythrough;
import teachingtutorials.newlocation.NewLocation;
import teachingtutorials.utils.Display;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class Stage
{
    protected int iStageID;
    private int iOrder;
    private String szName;

    private Player player;
    private TeachingTutorials plugin;
    public TutorialPlaythrough tutorialPlaythrough;
    public boolean bStageFinished;

    public ArrayList<Step> steps = new ArrayList<>();
    private int iCurrentStep; //1 indexed
    private Step currentStep;

    public int iNewLocationID;

    public final boolean bLocationCreation;

    //Provision for score recording

    //--------------------------------------------------
    //-------------------Constructors-------------------
    //--------------------------------------------------

    //Used when fetching the stages for a lesson
    public Stage(int iStageID, int iOrder, String szName, Player player, TeachingTutorials plugin, Lesson lesson)
    {
        //Inherited properties from the lesson
        this.plugin = plugin;
        this.tutorialPlaythrough = lesson;
        this.player = player;

        //Fixed stage properties
        this.iStageID = iStageID;
        this.iOrder = iOrder;
        this.szName = szName;

        //Other
        this.bStageFinished = false;
        bLocationCreation = false;
    }

    //Used for inserting a new stage into the DB
    public Stage(String szName)
    {
        bLocationCreation = false;
        this.szName = szName;
    }

    //Used when creating a new location
    public Stage(int iStageID, int iOrder, String szName, Player player, TeachingTutorials plugin, int iLocationID, NewLocation newLocation)
    {
        this.bLocationCreation = true;
        this.bStageFinished = false;
        this.plugin = plugin;

        //Stage specific properties
        this.iStageID = iStageID;
        this.iOrder = iOrder;
        this.szName = szName;

        //New location specific
        this.player = player;
        this.iNewLocationID = iLocationID;
        this.tutorialPlaythrough = newLocation;
    }

    //---------------------------------------------------
    //----------------------Getters----------------------
    //---------------------------------------------------
    public String getName()
    {
        return szName;
    }
    public int getLocationID()
    {
        if (bLocationCreation)
            return iNewLocationID;
        else
            return this.tutorialPlaythrough.getLocation().getLocationID();
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
        return (iOrder == 1);
    }

    /**
     * Displays the virtual blocks of all tasks in all steps of this stage up to but not including the iStepToStartTH step
     * @param iStepToStart The step at which to start the lesson at (this is not zero indexed. The first step has iStepToStart = 1).
     *                     <p> </p>
     *                     For clarity, if iStepToStart is 1, no virtual blocks from will be displayed from this stage.
     *                     <p>If iStepToStart = 2, only the virtual blocks from the first step (and all previous stages) will be displayed at the start of the lesson.</p>
     */
    public void displayAllVirtualBlocks(int iStepToStart)
    {
        //Gets the steps of this stage from the DB
        fetchAndInitialiseSteps();

        //The number of steps to display the virtual blocks for
        int iNumStepsToDisplay;

        if (iStepToStart == 0)
            iNumStepsToDisplay = steps.size();
        else
            iNumStepsToDisplay = iStepToStart - 1;

        //Calls each step to display all relevant virtual blocks
        for (int i = 0 ; i < iNumStepsToDisplay ; i++)
        {
            steps.get(i).displayAllVirtualBlocks();
        }
    }

    /**
     * Fetches the list of steps of this stage from the database and stores this in {@link #steps}. The list is ordered.
     */
    private void fetchAndInitialiseSteps()
    {
        //Gets a list of all of the steps of the specified stage and loads each with the relevant data.
        //List is in order of step 1 1st
        steps = Step.fetchStepsByStageID(player, plugin, this);
    }

    public void startStage(int iStep)
    {
        //Display the Stage title
        Display display = new Display(player, " ");
        display.Title(ChatColor.AQUA +"Stage " +iOrder +" - " +szName, 10, 60, 12);

        //Get the steps
        fetchAndInitialiseSteps();

        //Takes the step back, for it to be increased in the next step
        iCurrentStep = iStep - 1;
        nextStep();
    }

    protected void nextStep()
    {
        //1 indexed
        iCurrentStep++;

        if (iCurrentStep <= steps.size())
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] "+tutorialPlaythrough.getCreatorOrStudent().player.getName() +" has started step " +iCurrentStep +" of stage " +iOrder);

            //Uses -1 because iCurrentStep is 1 indexed, so need it in computer terms
            currentStep = steps.get(iCurrentStep-1);
            currentStep.startStep();

            if (bLocationCreation == false)
            {
                //Save the positions of stage and step
                ((Lesson) tutorialPlaythrough).savePositions();
            }
        }
        else
        {
            bStageFinished = true;
            tutorialPlaythrough.nextStage(1);
        }
    }

    //Unregister all listeners under this stage
    public void terminateEarly()
    {
        currentStep.terminateEarly();
    }

    public static ArrayList<Stage> fetchStagesByTutorialID(Player player, TeachingTutorials plugin, Lesson lesson)
    {
        ArrayList<Stage> stages = new ArrayList<>();

        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            //Compiles the command to fetch stages
            sql = "SELECT * FROM `Stages` WHERE `TutorialID` = "+lesson.getTutorial().getTutorialID() +" ORDER BY 'Order' ASC";
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            while (resultSet.next())
            {
                Stage stage = new Stage(resultSet.getInt("StageID"), resultSet.getInt("Order"), resultSet.getString("StageName"), player, plugin, lesson);
                stages.add(stage);
            }
        }
        catch(SQLException se)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - SQL Error fetching Stages by TutorialID");
            se.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return stages;
    }

    public static ArrayList<Stage> fetchStagesByTutorialIDWithoutLocationInformation(Player player, TeachingTutorials plugin, int iTutorialID, int iLocationID, NewLocation newLocation)
    {
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Attempting to fetch stages without answers");

        ArrayList<Stage> stages = new ArrayList<>();

        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        int iCount = 0;

        try
        {
            //Compiles the command to fetch stages
            sql = "SELECT * FROM `Stages` WHERE `TutorialID` = "+iTutorialID +" ORDER BY 'Order' ASC";
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            while (resultSet.next())
            {
                Stage stage = new Stage(resultSet.getInt("StageID"), resultSet.getInt("Order"), resultSet.getString("StageName"), player, plugin, iLocationID, newLocation);
                stages.add(stage);
                iCount++;
            }
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials]" +iCount +" Stages fetched from the database for "+player.getName()+"'s lesson");
        }
        catch(SQLException se)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - SQL Error fetching Stages by TutorialID");
            se.printStackTrace();
        }
        catch (Exception e)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - Error fetching Stages by TutorialID");
            e.printStackTrace();
        }
        return stages;
    }
}
