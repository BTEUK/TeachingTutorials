package teachingtutorials.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.guis.Gui;
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

    //Main gui, includes everything that is part of the navigator.
    public Gui mainGui;

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
    public void calculateRatings(DBConnection dbConnection)
    {
        iScoreTpll = calculateRating(Category.tpll, dbConnection);
        iScoreWE = calculateRating(Category.worldedit, dbConnection);
        iScoreTerraforming = calculateRating(Category.terraforming, dbConnection);
        iScoreColouring = calculateRating(Category.colouring, dbConnection);
        iScoreDetailing = calculateRating(Category.detail, dbConnection);
    }

    //Uses scores from previous lessons to calculate a rating for a player in a category
    private int calculateRating(Category category, DBConnection dbConnection)
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
            sql = "SELECT * FROM `Lessons`, `Scores` WHERE `Lessons`.`UUID` = '"+player.getUniqueId() +"' " +
                    "AND `Scores`.`Category` = '" + category.toString() + "' "+
                    "AND `Lessons`.`LessonID` = `Scores`.`LessonID` " +
                    "ORDER BY `Scores`.`LessonID` DESC";
            SQL = dbConnection.getConnection().createStatement();

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

                int i;

                do
                {
                    bAllRemoved = true;

                    for (i = 0 ; i < lessons.size() ; i++)
                    {
                        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Lesson "+i);
                        lesson = lessons.get(i);
                        if (lesson.getCreatorOrStudent().equals(this))
                        {
                            bAllRemoved = false;
                            //Saves the scores, saves the position, removes the listeners, removes the virtual blocks (of the current task)
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

                    for (i = 0 ; i < newLocations.size() ; i++)
                    {
                        newLocation = newLocations.get(i);
                        if (newLocation.getCreatorOrStudent().equals(this))
                        {
                            bAllRemoved = false;
                            //Removes the listeners, removes the virtual blocks (of the current task)
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


//    //Updates the scoreboard with the current ratings stored in the User object
//    public void refreshScoreboard()
//    {
//        //Get scoreboard
//        Scoreboard SB;
//
//        ScoreboardManager SBM = Bukkit.getScoreboardManager();
//        SB = SBM.getNewScoreboard();
//
//        Objective scores = SB.registerNewObjective("Ratings", "dummy", ChatColor.AQUA +"Skills", RenderType.INTEGER);
//
//        Score tpllRating = scores.getScore("1. Tpll");
//        tpllRating.setScore(this.iScoreTpll);
//
//        Score WERating = scores.getScore("2. WorldEdit");
//        WERating.setScore(this.iScoreWE);
//
//        Score TerraRating = scores.getScore("5. Terraforming");
//        TerraRating.setScore(this.iScoreTerraforming);
//
//        Score ColouringRating = scores.getScore("3. Colouring");
//        ColouringRating.setScore(this.iScoreColouring);
//
//        Score DetailingRating = scores.getScore("4. Detailing");
//        DetailingRating.setScore(this.iScoreDetailing);
//
//        scores.setDisplaySlot(DisplaySlot.SIDEBAR);
//
//        this.player.setScoreboard(SB);
//    }

    public static User identifyUser(TeachingTutorials plugin, Player player)
    {
        //Finds the correct user for this player from the plugin's list of users
        boolean bUserFound = false;

        ArrayList<User> users = plugin.players;
        int iLength = users.size();
        int i;
        User user = new User(player);

        //Prevents null exception error if player is null as would happen if the player was offline
        if (player == null)
            return null;

        for (i = 0 ; i < iLength ; i++)
        {
            if (users.get(i).player.getUniqueId().equals(player.getUniqueId()))
            {
                user = users.get(i);
                bUserFound = true;
                break;
            }
        }

        if (!bUserFound)
        {
            Display display = new Display(player, ChatColor.RED +"An error occurred. Please contact a support staff. Error: 1");
            display.Message();
            display = new Display(player, ChatColor.RED +"Try relogging");
            display.Message();
            return null;
        }
        else
        {
            return user;
        }
    }

    public static void teleportPlayerToLobby(Player player, TeachingTutorials plugin, long waitTimeTicks)
    {
        FileConfiguration config = plugin.getConfig();

        World tpWorld = Bukkit.getWorld(config.getString("Lobby_World"));
        if (tpWorld == null)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Cannot tp player to lobby, world null");
            Display display = new Display(player, ChatColor.RED +"Cannot tp you to lobby");
            display.Message();
        }
        else
        {
            org.bukkit.Location location = new org.bukkit.Location(tpWorld, config.getDouble("Lobby_X"), config.getDouble("Lobby_Y"), config.getDouble("Lobby_Z"), config.getInt("Lobby_Yaw"), config.getInt("Lobby_Pitch"));

            //Teleports the player
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
            {
                @Override
                public void run()
                {
                    player.teleport(location);
                }
            }, waitTimeTicks);
        }

    }

    //---------------------------------------------------
    //--------------------SQL Fetches--------------------
    //---------------------------------------------------

    //Fetches all of the information about a user in the DB, and adds them to the DB if they are not in it
    public void fetchDetailsByUUID(DBConnection dbConnection)
    {
        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            //Compiles the command to select all data about the user
            sql = "SELECT * FROM `Players` WHERE `UUID` = '"+player.getUniqueId()+"'";
            System.out.println(sql);
            SQL = dbConnection.getConnection().createStatement();

            //Executes the update and returns the amount of records updated
            resultSet = SQL.executeQuery(sql);
            if (resultSet.next())
            {
                this.bHasCompletedCompulsory = resultSet.getBoolean("CompletedCompulsory");
                this.bInLesson = resultSet.getBoolean("InLesson");
            }

            //If no result is found for the user, insert a new user into the database
            else
            {
                sql = "INSERT INTO `Players` (`UUID`) VALUES ('"+ player.getUniqueId() +"')";
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

    //Fetches all tutorials made by the user
    public void fetchAllTutorials(DBConnection dbConnection)
    {
        this.allTutorials = Tutorial.fetchAllByCreator(this.player.getUniqueId(), dbConnection);
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
            szSql = "UPDATE `Players` SET `CompletedCompulsory` = 1 WHERE `UUID` = '"+ this.player.getUniqueId()+"'";
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
                szSql = "UPDATE `Players` SET `InLesson` = 0 WHERE `UUID` = '"+player.getUniqueId() +"' ";
            else
                szSql = "UPDATE `Players` SET `InLesson` = 1 WHERE `UUID` = '"+player.getUniqueId() +"' ";

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

            szSql = "UPDATE `Players` SET `InLesson` = " +i +" WHERE `UUID` = '"+player.getUniqueId() +"' ";

            SQL.executeUpdate(szSql);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
