package teachingtutorials.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
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

    public void fetchDetailsByUUID()
    {
        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            //Compiles the command to add the new user
            sql = "SELECT * FROM Players WHERE `UUID` = '"+player.getUniqueId()+"'";
            System.out.println(sql);
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the update and returns the amount of records updated
            resultSet = SQL.executeQuery(sql);
            if (resultSet.next())
            {
                this.bHasCompletedCompulsory = resultSet.getBoolean("CompletedCompulsory");
                this.bInLesson = resultSet.getBoolean("InLesson");
                player.sendMessage(this.bInLesson +"");
            }
            else
            {
                sql = "INSERT INTO Players (UUID) VALUES ('"+ player.getUniqueId() +"')";
                SQL.executeUpdate(sql);
                this.bHasCompletedCompulsory = false;
                this.bInLesson = false;
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

    public void calculateRatings()
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
            sql = "SELECT * FROM Lessons, Scores WHERE `Lessons`.`UUID` = '"+player.getUniqueId() +"' " +
                    "AND `Scores`.`Category` = '" + category.toString() + "' "+
                    "AND `Lessons`.`LessonID` = `Scores`.`LessonID` " +
                    "ORDER BY `Scores`.`LessonID` DESC";
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the update and returns the amount of records updated
            resultSet = SQL.executeQuery(sql);

            //Iterate over last 5 and add up score
            for (int iCount = 5 ; resultSet.next() && iCount >= 1 ; iCount--)
            {
                iTotalScore = iTotalScore + iCount*resultSet.getInt("Score");
            }
            iTotalScore = iTotalScore/15;
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

    public void refreshScoreboard()
    {
        //Get scoreboard
        Scoreboard SB;

        ScoreboardManager SBM = Bukkit.getScoreboardManager();
        SB = SBM.getNewScoreboard();

        Objective scores = SB.registerNewObjective("Ratings", "dummy", ChatColor.AQUA +"Skills", RenderType.INTEGER);

        Score tpllRating = scores.getScore("Tpll");
        tpllRating.setScore(this.iScoreTpll);

        Score WERating = scores.getScore("WorldEdit");
        WERating.setScore(this.iScoreWE);

        Score TerraRating = scores.getScore("Terraforming");
        TerraRating.setScore(this.iScoreTerraforming);

        Score ColouringRating = scores.getScore("Texturing");
        ColouringRating.setScore(this.iScoreColouring);

        Score DetailingRating = scores.getScore("Detailing");
        DetailingRating.setScore(this.iScoreDetailing);

        scores.setDisplaySlot(DisplaySlot.SIDEBAR);

        this.player.setScoreboard(SB);
    }

    public void triggerCompulsory()
    {
        this.bHasCompletedCompulsory = true;

        String szSql;
        Statement SQL;
        ResultSet resultSet;

        try
        {
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();
            szSql = "UPDATE Players SET CompletedCompulsory = 1 WHERE UUID = '"+ this.player.getUniqueId()+"'";
            SQL.executeUpdate(szSql);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
