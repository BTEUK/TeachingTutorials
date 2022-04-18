package teachingtutorials.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class User
{
    public Player player;

    public boolean bHasCompletedCompulsory;
    public boolean bInLesson;

    //Holds the information on the rating for a user
    public int iScoreTpll;
    public int iScoreWE;
    public int iScoreTerraforming;
    public int iScoreColouring;
    public int iScoreDetailing;

    public User(Player player)
    {
        this.player = player;
    }


    public User()
    {

    }

    public void fetchDetailsByUUID()
    {
        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            //Compiles the command to add the new user
            sql = "Select * FROM Players WHERE UUID = "+player.getUniqueId();
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the update and returns the amount of records updated
            resultSet = SQL.executeQuery(sql);
            if (resultSet.next())
            {
                this.bHasCompletedCompulsory = resultSet.getBoolean("CompletedCompulsory");
                this.bInLesson = resultSet.getBoolean("InLesson");
            }
            else
            {
                sql = "INSERT INTO Players (UUID) VALUES ("+ player.getUniqueId() +")";
                SQL.executeUpdate(sql);
            }

        }
        catch(SQLException se)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - SQL Error fetching user info by UUID");
            se.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void calculateScores()
    {
        iScoreTpll = calculateScore(Category.tpll);
        iScoreWE = calculateScore(Category.worldedit);
        iScoreTerraforming = calculateScore(Category.terraforming);
        iScoreColouring = calculateScore(Category.colouring);
        iScoreDetailing = calculateScore(Category.detail);
    }

    private int calculateScore(Category category)
    {
        int iTotalScore = 0;

        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            //Compiles the command to add the new user
            sql = "Select Scores.Score FROM Players,Lessons,Scores WHERE UUID = "+player.getUniqueId() +" " +
                    "AND Scores.Category = " + category.toString() + " "+
                    "AND Lessons.LessonID = Scores.LessonID " +
                    "ORDER BY Lessons.LessonID DESC";
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the update and returns the amount of records updated
            resultSet = SQL.executeQuery(sql);

            //Iterate over last 5 and add up score
            for (int iCount = 5 ; resultSet.next() && iCount >= 1 ; iCount--)
            {
                iTotalScore = iTotalScore + iCount*resultSet.getInt("Score");
            }
        }
        catch(SQLException se)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - SQL Error fetching lessons scores by UUID for category: "+category.toString());
            se.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return iTotalScore;
    }
}
