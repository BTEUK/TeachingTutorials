package teachingtutorials.tutorials;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.newlocation.NewLocation;

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
    public Lesson lesson; //Still want component objects to be able to access it. Or do I? yep, and all the children of that
    public boolean bStageFinished;

    public ArrayList<Step> steps = new ArrayList<>();
    private int iCurrentStep; //1 indexed
    private Step currentStep;

    public int iNewLocationID;
    public NewLocation newLocation;

    public final boolean bLocationCreation;

    //Provision for score recording

    //--------------------------------------------------
    //-------------------Constructors-------------------
    //--------------------------------------------------
    public Stage(int iStageID, int iOrder, Player player, TeachingTutorials plugin, Lesson lesson)
    {
        this.plugin = plugin;
        this.lesson = lesson;

        this.iStageID = iStageID;
        this.iOrder = iOrder;
        this.bStageFinished = false;

        this.player = player;

        bLocationCreation = false;
    }

    public Stage(String szName)
    {
        bLocationCreation = false;
        this.szName = szName;
    }

    public Stage(int iStageID, int iOrder, Player player, TeachingTutorials plugin, int iLocationID, NewLocation newLocation)
    {
        this.plugin = plugin;

        this.iStageID = iStageID;
        this.iOrder = iOrder;
        this.bStageFinished = false;

        this.player = player;

        this.iNewLocationID = iLocationID;
        this.newLocation = newLocation;

        bLocationCreation = true;
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
            return this.lesson.location.getLocationID();
    }

    public Player getPlayer()
    {
        return player;
    }

    public int getCurrentStep()
    {
        return iCurrentStep;
    }

    private void fetchAndInitialiseSteps()
    {
        //Gets a list of all of the steps of the specified stage and loads each with the relevant data.
        //List is in order of step 1 1st
        steps = Step.fetchStepsByStageID(player, plugin, this);
    }

    public void startStage(int iStep)
    {
        //Step is 1 indexed
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Stage "+iOrder +" starting, at step "+iStep);

        fetchAndInitialiseSteps();

        //Takes the step back, for it to be increased in the next step
        iCurrentStep = iStep - 1;
        nextStep();
    }

    //Incomplete
    protected void nextStep()
    {
        //1 indexed
        iCurrentStep++;
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] "+lesson.student.player.getName() +" has started step " +iCurrentStep +" of stage " +iOrder);

        if (iCurrentStep <= steps.size())
        {
            currentStep = steps.get(iCurrentStep-1);
            currentStep.startStep();

            //Save the positions of stage and step
            lesson.savePositions();
        }
        else
        {
            bStageFinished = true;
            endStage();
        }
    }

    protected void endStage()
    {
        if (bLocationCreation)
            newLocation.nextStage();
        else
            lesson.nextStage();
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
            sql = "Select * FROM Stages WHERE TutorialID = "+lesson.tutorial.getTutorialID() +" ORDER BY 'Order' ASC";
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            while (resultSet.next())
            {
                Stage stage = new Stage(resultSet.getInt("StageID"), resultSet.getInt("Order"), player, plugin, lesson);
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
            sql = "Select * FROM Stages WHERE TutorialID = "+iTutorialID +" ORDER BY 'Order' ASC";
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            while (resultSet.next())
            {
                Stage stage = new Stage(resultSet.getInt("StageID"), resultSet.getInt("Order"), player, plugin, iLocationID, newLocation);
                stages.add(stage);
                iCount++;
            }
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"" +iCount +" Stages fetched from the database");
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
