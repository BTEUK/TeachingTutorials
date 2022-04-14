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

    public boolean bHasCompletedOnce;
    public boolean bInLesson;

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
                this.bHasCompletedOnce = resultSet.getBoolean("CompletedCompulsory");
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
    }

    private void calculateScore(Category category)
    {
        //Get all lessons
        //Get Scores.score from Players,Lessons,Scores,Tutorials,Category
        //Where Players.UUID = Lessons.UUID
        //And Lessons.LessonID = Scores.LessonID
        //And Lessons.TutorialID = Tutorials.TutorialID
        //And CategoryPoints.TutorialID = Tutorials.TutorialID
        //And Category = CATEGORY
        //ORDER BY Lessons.LessonID DESC

        //Get first 5

    }
}