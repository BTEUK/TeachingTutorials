package teachingtutorials.fundamentalTasks;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.tutorials.Group;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class Task
{
    public String type;

    public float fDifficulties[] = new float[5];

    float fDifficulty;
    float fPerformance;
    float fFinalScore;

    Player player;
    TeachingTutorials plugin;
    protected Group parentGroup;

    public void register()
    {
    }

    public Task(String type)
    {
        this.type = type;
    }

    public Task(TeachingTutorials plugin)
    {
        this.plugin = plugin;
    }

    protected void taskComplete()
    {
        fFinalScore = fDifficulty*fPerformance;

        //Add to the totals
        parentGroup.parentStep.parentStage.lesson.fTpllScoreTotal = parentGroup.parentStep.parentStage.lesson.fTpllScoreTotal + fFinalScore;
        parentGroup.parentStep.parentStage.lesson. fTpllDifTotal = parentGroup.parentStep.parentStage.lesson.fTpllDifTotal + fDifficulty;

        parentGroup.taskFinished();
    }

    public static ArrayList<Task> fetchTasks(TeachingTutorials plugin, Group parentGroup, int iLocationID, int iGroupID, Player player)
    {
        ArrayList<Task> tasks = new ArrayList<>();

        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            //Compiles the command to fetch groups
            sql = "Select * FROM LocationTasks,Tasks WHERE LocationTasks.LocationID = "+iLocationID +" AND Tasks.GroupID = "+iGroupID +" AND Tasks.TaskID = LocationTasks.TaskID ORDER BY 'Order' ASC";
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            while (resultSet.next())
            {
                String szType = resultSet.getString("Tasks.TaskType");
                String szAnswers = resultSet.getString("LocationTasks.Answers");
                float fTpllDifficulty = Float.parseFloat(resultSet.getString("LocationTasks.TpllDifficulty"));
                float fWEDifficulty = Float.parseFloat(resultSet.getString("LocationTasks.WEDifficulty"));
                float fColouringDifficulty = Float.parseFloat(resultSet.getString("LocationTasks.ColouringDifficulty"));
                float fDetailingDifficulty = Float.parseFloat(resultSet.getString("LocationTasks.DetailingDifficulty"));
                float fTerraDifficulty = Float.parseFloat(resultSet.getString("LocationTasks.TerraDifficulty"));

                switch (szType)
                {
                    case "tpll":
                        String[] cords = szAnswers.split(",");
                        TpllListener tpllListener = new TpllListener(plugin, Double.parseDouble(cords[0]), Double.parseDouble(cords[1]), player, fTpllDifficulty);
                        tasks.add(tpllListener);
                }
            }
        }
        catch(SQLException se)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - SQL Error fetching Tasks by LocationID and GroupID");
            se.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return tasks;
    }
}
