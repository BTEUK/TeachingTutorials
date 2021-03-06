package teachingtutorials.tutorials;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;

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
    private int iCurrentStep;
    public Lesson lesson; //Still want component objects to be able to access it. Or do I? yep, and all the children of that
    public boolean bStageFinished;

    public ArrayList<Step> steps = new ArrayList<>();

    //Provision for score recording

    public Stage(int iStageID, int iOrder, Player player, TeachingTutorials plugin, Lesson lesson)
    {
        this.plugin = plugin;
        this.lesson = lesson;

        this.iStageID = iStageID;
        this.iOrder = iOrder;
        this.bStageFinished = false;

        this.player = player;
    }

    public Stage(String szName)
    {
        this.szName = szName;
    }

    //Getters
    public String getName()
    {
        return szName;
    }

    private void fetchAndInitialiseSteps()
    {
        //Gets a list of all of the steps of the specified stage and loads each with the relevant data.
        //List is in order of step 1 1st
        steps = Step.fetchStepsByStageID(player, plugin, this);
    }

    public void startStage()
    {
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Stage "+iOrder +" starting");

        fetchAndInitialiseSteps();
        iCurrentStep = 0;
        nextStep();
    }

    //Incomplete
    protected void nextStep()
    {
        iCurrentStep++;
        Step step;

        if (iCurrentStep <= steps.size())
        {
            step = steps.get(iCurrentStep-1);
            step.startStep();
        }
        else
        {
            bStageFinished = true;
            return;
            //   endStage();
        }
        while(!step.bStepFinished)
        {
        }
    }

    protected void endStage()
    {
        lesson.nextStage();
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
            sql = "Select * FROM Stages WHERE TutorialID = "+lesson.iTutorialID +" ORDER BY 'Order' ASC";
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
}
