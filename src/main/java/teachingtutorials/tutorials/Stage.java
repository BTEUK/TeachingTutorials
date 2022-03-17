package teachingtutorials.tutorials;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.fundamentalTasks.Task;
import teachingtutorials.fundamentalTasks.TpllListener;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class Stage
{
    protected int iStageID;
    private int iOrder;

    private Player player;
    private TeachingTutorials plugin;
    private int iCurrentStep;
    protected Lesson lesson; //Still want component objects to be able to access it. Or do I?
    public boolean bStageFinished;

    // Tasks are divided into separate groups of tasks (steps)
    // Each step must be completed synchronously but tasks in the step can be done in any order

    // Not true Now ^^^^^^^^^

    private ArrayList<Step> steps = new ArrayList<>();

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

    private void fetchAndInitialiseSteps()
    {
        //Gets a list of all of the steps of the specified stage and loads each with the relevant data.
        //List is in order of step 1 1st
        steps = Step.fetchStepsByStageID(player, plugin, this);
    }

    public void startStage()
    {
        iCurrentStep = 0;
        nextStep();
    }

    //Incomplete
    protected void nextStep()
    {
        iCurrentStep++;
        if (iCurrentStep <= steps.size())
        {
            steps.get(iCurrentStep-1).startStep();
        }
        else
        {
            endStage();
        }
    }

    protected void endStage()
    {
        //Calculate final scores for stage?

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
