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
    float fDifficulty;
    float fFinalScore;

    Player player;
    TeachingTutorials plugin;
    protected Group parentGroup;

    public void register()
    {

    }

    private void taskComplete()
    {

    }

    public static ArrayList<Task> fetchTasks(TeachingTutorials plugin, Group parentGroup, int iLocationID, Player player)
    {
        ArrayList<Task> tasks = new ArrayList<Task>();

        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            //Compiles the command to fetch groups
            sql = "Select * FROM Tasks WHERE LocationID = "+iLocationID;
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            while (resultSet.next())
            {
                String szType = resultSet.getString("TaskType");
                String szAnswers = resultSet.getString("Answers");
                String szDifficulty = resultSet.getString("Difficulty");

                switch (szType)
                {
                    case "tpll":
                        String[] cords = szAnswers.split(",");
                        TpllListener tpllListener = new TpllListener(plugin, Double.parseDouble(cords[0]), Double.parseDouble(cords[1]), player, Float.parseFloat(szDifficulty));
                        tasks.add(tpllListener);
                }
            }
        }
        catch(SQLException se)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - SQL Error fetching Groups by StepID");
            se.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return tasks;
    }
}
