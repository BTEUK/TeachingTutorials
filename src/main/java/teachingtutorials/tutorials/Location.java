package teachingtutorials.tutorials;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import teachingtutorials.TeachingTutorials;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Location
{
    private int iLocationID;
    private int iTutorialID;
    private float fDifficulty;
    private World world;

    //--------------------------------------------------
    //-------------------Constructors-------------------
    //--------------------------------------------------

    //Used for creating a new location
    public Location(int iTutorialID, boolean bNew)
    {
        this.iTutorialID = iTutorialID;
    }

    //Used for fetching a location
    public Location(int iLocationID)
    {
        this.iLocationID = iLocationID;
        fetchDetailsByLocationID();
        this.world = Bukkit.getWorld(this.getLocationID()+"");
    }

    //---------------------------------------------------
    //----------------------Getters----------------------
    //---------------------------------------------------

    /**
     * Care must be taken using this method, as this class may not always be initialised
     *<p>
     * Use Stage.getLocationID() to be safe
     * @return The LocationID
     */
    public int getLocationID()
    {
        return iLocationID;
    }

    public World getWorld()
    {
        if (world == null)
            world = Bukkit.getWorld(iLocationID+"");
        return world;
    }

    public int getTutorialID()
    {
        return iTutorialID;
    }

    //---------------------------------------------------
    //----------------------Setters----------------------
    //---------------------------------------------------

    public void setWorld(World world)
    {
        this.world = world;
    }

    //---------------------------------------------------
    //--------------------SQL Fetches--------------------
    //---------------------------------------------------

    private void fetchDetailsByLocationID()
    {
        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            //Compiles the command to fetch location
            sql = "SELECT * FROM `Locations` WHERE `Locations`.`LocationID` = " +iLocationID;

            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);

            //Move the cursor to the first row
            resultSet.next();

            //Stores the information
            this.fDifficulty = resultSet.getFloat("Difficulty");;
            this.iTutorialID = resultSet.getInt("TutorialID");
        }
        catch (Exception e)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"[TeachingTutorials] Error fetching location information of location ID " +this.iLocationID);
        }
    }

    public static int[] getAllLocationIDsForTutorial(int iTutorialID)
    {
        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;
        int iLocations = 0;
        int iLocationIDs[];
        int i;

        try
        {
            //Compiles the command to fetch all the locations for the tutorial
            sql = "SELECT * FROM `Locations` WHERE `TutorialID` = " +iTutorialID;
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +sql);
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            while (resultSet.next())
            {
                iLocations++;
            }

            iLocationIDs = new int[iLocations];

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            for (i = 0 ; i < iLocationIDs.length ; i++)
            {
                resultSet.next();
                iLocationIDs[i] = resultSet.getInt("LocationID");
            }
        }
        catch (SQLException se)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - SQL Error fetching location IDs for tutorial with ID: "+iTutorialID);
            se.printStackTrace();
            iLocationIDs = new int[0];
        }
        return iLocationIDs;
    }

    //---------------------------------------------------
    //--------------------SQL Updates--------------------
    //---------------------------------------------------
    public boolean insertNewLocation()
    {
        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        int iCount;

        try
        {
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();
            sql = "INSERT INTO `Locations` (`TutorialID`) VALUES (" +iTutorialID+")";
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +sql);
            iCount = SQL.executeUpdate(sql);

            if (iCount != 1)
            {
                return false;
            }

            //Gets the LocationID of the newly inserted location
            sql = "SELECT LAST_INSERT_ID()";
            resultSet = SQL.executeQuery(sql);
            resultSet.next();
            iLocationID = resultSet.getInt(1);
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "LocationID of new location = "+iLocationID);
            return true;
        }
        catch (SQLException se)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - SQL Error adding new location");
            se.printStackTrace();
            return false;
        }
        catch (Exception e)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - Other error adding new location");
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteLocationByID(int iLocationID)
    {
        String sql;
        Statement SQL = null;

        int iCount;

        try
        {
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Removes the answers
            sql = "DELETE FROM `LocationTasks` WHERE `LocationID` = " +iLocationID;
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] " +sql);
            iCount = SQL.executeUpdate(sql);
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] " +iCount +" LocationTasks were deleted");

            //Removes the location specific step details
            sql = "DELETE FROM `LocationSteps` WHERE `LocationID` = " +iLocationID;
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] " +sql);
            iCount = SQL.executeUpdate(sql);
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] " +iCount +" LocationSteps were deleted");

            //Removes the location
            sql = "DELETE FROM `Locations` WHERE `LocationID` = " +iLocationID;
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] " +sql);
            iCount = SQL.executeUpdate(sql);

            if (iCount != 1)
            {
                return false;
            }

            return true;
        }
        catch (SQLException se)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - SQL Error deleting location with LocationID = "+iLocationID);
            se.printStackTrace();
            return false;
        }
        catch (Exception e)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - Non-SQL Error deleting location with LocationID = "+iLocationID);
            e.printStackTrace();
            return false;
        }
    }
}
