package teachingtutorials.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.newlocation.NewLocation;
import teachingtutorials.tutorials.Lesson;
import teachingtutorials.tutorials.Tutorial;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class User
{
    public Player player;

    //Stores whether a player has completed the compulsory tutorial or not
    public boolean bHasCompletedCompulsory;

    //Records whether a player has Lesson to finish in the Lesson's table of the database
    public boolean bInLesson;

    //Determines what the player is currently doing on the tutorials server
    public Mode currentMode;

    //Holds the information on the ratings for a user
    public int iScoreTpll;
    public int iScoreWE;
    public int iScoreTerraforming;
    public int iScoreColouring;
    public int iScoreDetailing;

    //Holds a list of all the tutorials a user has created
    private Tutorial[] allTutorials;

    //--------------------------------------------------
    //-------------------Constructors-------------------
    //--------------------------------------------------
    public User(Player player)
    {
        this.player = player;
    }

    //---------------------------------------------------
    //----------------------Getters----------------------
    //---------------------------------------------------

    //Returns the list of tutorials created by the player
    public Tutorial[] getAllTutorials()
    {
        return allTutorials;
    }

    //Recalculates all ratings
    public void calculateRatings()
    {
        iScoreTpll = calculateRating(Category.tpll);
        iScoreWE = calculateRating(Category.worldedit);
        iScoreTerraforming = calculateRating(Category.terraforming);
        iScoreColouring = calculateRating(Category.colouring);
        iScoreDetailing = calculateRating(Category.detail);
    }

    //Uses scores from previous lessons to calculate a rating for a player in a category
    private int calculateRating(Category category)
    {
        //Declare variables
        int iTotalScore = 0;

        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        //Attempts to fetch the data from the database
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

    //Does all the necessary thing when a player leaves the server
    public void playerLeave(TeachingTutorials plugin)
    {
        boolean bAllRemoved;

        //Checks their status
        switch (currentMode)
        {
            case Idle:
                Bukkit.getConsoleSender().sendMessage(ChatColor.LIGHT_PURPLE +"[TeachingTutorials] Player "+player.getName() +" is leaving but was idle");
                break; //Assume the system is keeping an accurate account of the player's status
            case Doing_Tutorial:
                Bukkit.getConsoleSender().sendMessage(ChatColor.LIGHT_PURPLE +"[TeachingTutorials] Player "+player.getName() +" is leaving and was doing a tutorial. Saving and removing listeners...");
                ArrayList<Lesson> lessons = plugin.lessons;
                Lesson lesson;
                Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] There are currently "+lessons.size() +" lessons taking place");

                do
                {
                    bAllRemoved = true;

                    for (int i = 0 ; i < lessons.size() ; i++)
                    {
                        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Lesson "+i);
                        lesson = lessons.get(i);
                        if (lesson.getStudent().equals(this))
                        {
                            bAllRemoved = false;
                            //Saves the scores, saves the position, removes the listeners
                            lesson.terminateEarly();

                            //If a lesson is removed from the list we must start the for-loop again
                            break;
                        }
                    }

                } while (bAllRemoved);
                //If none where found for this user, we can stop looking

                Bukkit.getConsoleSender().sendMessage(ChatColor.LIGHT_PURPLE +"[TeachingTutorials] Paused all lessons for player "+player.getName());
                break;
            case Creating_New_Location:
                Bukkit.getConsoleSender().sendMessage(ChatColor.LIGHT_PURPLE +"[TeachingTutorials] Player "+player.getName() +" is leaving whilst creating a location for a tutorial. Removing listeners...");
                ArrayList<NewLocation> newLocations = plugin.newLocations;
                NewLocation newLocation;

                do
                {
                    bAllRemoved = true;

                    for (int i = 0 ; i < newLocations.size() ; i++)
                    {
                        newLocation = newLocations.get(i);
                        if (newLocation.getCreator().equals(this))
                        {
                            bAllRemoved = false;
                            //Removes the listeners
                            newLocation.terminateEarly();

                            //If a newLocation is removed from the list we must start the for-loop again
                            break;
                        }
                    }
                } while (bAllRemoved);
                //If none where found for this user, we can stop looking

                Bukkit.getConsoleSender().sendMessage(ChatColor.LIGHT_PURPLE +"[TeachingTutorials] Paused all new location creations for player "+player.getName());
                break;
            case Creating_New_Tutorial:
            default:
                break;
        }
    }


    //Updates the scoreboard with the current ratings stored in the User object
    public void refreshScoreboard()
    {
        //Get scoreboard
        Scoreboard SB;

        ScoreboardManager SBM = Bukkit.getScoreboardManager();
        SB = SBM.getNewScoreboard();

        Objective scores = SB.registerNewObjective("Ratings", "dummy", ChatColor.AQUA +"Skills", RenderType.INTEGER);

        Score tpllRating = scores.getScore("1. Tpll");
        tpllRating.setScore(this.iScoreTpll);

        Score WERating = scores.getScore("2. WorldEdit");
        WERating.setScore(this.iScoreWE);

        Score TerraRating = scores.getScore("5. Terraforming");
        TerraRating.setScore(this.iScoreTerraforming);

        Score ColouringRating = scores.getScore("3. Colouring");
        ColouringRating.setScore(this.iScoreColouring);

        Score DetailingRating = scores.getScore("4. Detailing");
        DetailingRating.setScore(this.iScoreDetailing);

        scores.setDisplaySlot(DisplaySlot.SIDEBAR);

        this.player.setScoreboard(SB);
    }

    //---------------------------------------------------
    //--------------------SQL Fetches--------------------
    //---------------------------------------------------

    //Fetches all of the information about a user in the DB, and adds them to the DB if they are not in it
    public void fetchDetailsByUUID()
    {
        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            //Compiles the command to select all data about the user
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

            //If no result is found for the user, insert a new user into the database
            else
            {
                sql = "INSERT INTO Players (UUID) VALUES ('"+ player.getUniqueId() +"')";
                SQL.executeUpdate(sql);
                this.bHasCompletedCompulsory = false;
                this.bInLesson = false;
            }
        }
        catch (SQLException se)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - SQL Error fetching user info by UUID");
            se.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    //Fetches all tutorials made by a user
    public void fetchAllTutorials()
    {
        this.allTutorials = Tutorial.fetchAllForUser(this.player.getUniqueId());
    }

    //---------------------------------------------------
    //--------------------SQL Updates--------------------
    //---------------------------------------------------

    //Updates the database to set completion of the compulsory tutorial to true
    public void triggerCompulsory()
    {
        //Declare variables
        String szSql;
        Statement SQL;

        //Updates the variable
        this.bHasCompletedCompulsory = true;

        //Updates the database
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

    //Changes the boolean value of whether a player is in the lesson
    public void toogleInLesson()
    {
        //Declare variables
        String szSql;
        Statement SQL;

        try
        {
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();
            if (this.bInLesson)
                szSql = "UPDATE Players SET InLesson = 0 WHERE `UUID` = '"+player.getUniqueId() +"' ";
            else
                szSql = "UPDATE Players SET InLesson = 1 WHERE `UUID` = '"+player.getUniqueId() +"' ";

            SQL.executeUpdate(szSql);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    //Sets the value of in lesson to whatever is specified
    public void setInLesson(int i)
    {
        //Declare variables
        String szSql;
        Statement SQL;

        try
        {
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            szSql = "UPDATE Players SET InLesson = " +i +" WHERE `UUID` = '"+player.getUniqueId() +"' ";

            SQL.executeUpdate(szSql);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
