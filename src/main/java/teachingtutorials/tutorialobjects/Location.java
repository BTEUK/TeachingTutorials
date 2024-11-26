package teachingtutorials.tutorialobjects;

import org.bukkit.Bukkit;
import org.bukkit.World;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.utils.DBConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a Tutorials Location
 */
public class Location
{
    /** The ID of this location in the DB */
    private int iLocationID;

    /** The ID of the tutorial for which this is a location of */
    private final int iTutorialID;

    /** A reference to the bukkit world for this location */
    private World world;

    //--------------------------------------------------
    //-------------------Constructors-------------------
    //--------------------------------------------------

    //Used for creating a new location

    /**
     * Used for creating a new location
     * @param iTutorialID The ID of the tutorial for which this is a location of
     * @param bNew Whether it is new or not. It always is in this case, this gets round the overloading issue, see
     *             constructor below this one would have the same parameters.
     */
    public Location(int iTutorialID, boolean bNew)
    {
        this.iTutorialID = iTutorialID;
    }

    /**
     * Used for loading a location from the DB
     * @param iLocationID The locationID of the location to load from the DB
     * @param iTutorialID The tutorialID of the location's tutorial
     */
    public Location(int iLocationID, int iTutorialID)
    {
        this.iLocationID = iLocationID;
        this.iTutorialID = iTutorialID;

        //Loads the bukkit world for this location
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

    /**
     * Gets the bukkit world for this location if it is not already loaded
     * @return The bukkit world for this location
     */
    public World getWorld()
    {
        if (world == null)
            world = Bukkit.getWorld(iLocationID+"");
        return world;
    }

    //---------------------------------------------------
    //----------------------Setters----------------------
    //---------------------------------------------------
    /**
     * Sets the bukkit world object for this location
     * @param world
     */
    public void setWorld(World world)
    {
        this.world = world;
    }

    //---------------------------------------------------
    //--------------------SQL Fetches--------------------
    //---------------------------------------------------

    /**
     * Fetches a list of locationIDs for all of the Locations of a given Tutorial
     * @param iTutorialID The tutorial ID of the tutorial for which to fetch the locations for
     * @param dbConnection A DB connection object
     * @return A list of locationIDs
     */
    public static int[] getAllLocationIDsForTutorial(int iTutorialID, DBConnection dbConnection, Logger logger)
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
            SQL = dbConnection.getConnection().createStatement();

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
            logger.log(Level.SEVERE, "SQL - SQL Error fetching location IDs for tutorial with ID: "+iTutorialID, se);
            iLocationIDs = new int[0];
        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "SQL - Non-SQL Error fetching location IDs for tutorial with ID: "+iTutorialID, e);
            iLocationIDs = new int[0];
        }
        return iLocationIDs;
    }

    //---------------------------------------------------
    //--------------------SQL Updates--------------------
    //---------------------------------------------------

    /**
     * Inserts a new location into the database based on the data in this object
     * @return Whether the new location was successfully added to the database
     */
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
            return true;
        }
        catch (SQLException se)
        {
            TeachingTutorials.getInstance().getLogger().log(Level.SEVERE, "SQL - SQL Error adding new location", se);
            return false;
        }
        catch (Exception e)
        {
            TeachingTutorials.getInstance().getLogger().log(Level.SEVERE, "SQL - Non-SQL Error adding new location", e);
            return false;
        }
    }

    /**
     * Deletes a location from the DB
     * @param iLocationID The location ID of the location to delete
     * @return Whether the location was successfully deleted
     */
    public static boolean deleteLocationByID(int iLocationID)
    {
        TeachingTutorials.getInstance().getLogger().log(Level.INFO, "Removing location " +iLocationID +" from the DB");
        String sql;
        Statement SQL = null;

        int iCount;

        try
        {
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Removes the answers
            sql = "DELETE FROM `LocationTasks` WHERE `LocationID` = " +iLocationID;
            iCount = SQL.executeUpdate(sql);
            TeachingTutorials.getInstance().getLogger().log(Level.INFO, iCount +" LocationTasks were deleted");

            //Removes the location specific step details
            sql = "DELETE FROM `LocationSteps` WHERE `LocationID` = " +iLocationID;
            iCount = SQL.executeUpdate(sql);
            TeachingTutorials.getInstance().getLogger().log(Level.INFO, iCount +" LocationSteps were deleted");

            //Removes the location
            sql = "DELETE FROM `Locations` WHERE `LocationID` = " +iLocationID;
            iCount = SQL.executeUpdate(sql);

            if (iCount != 1)
            {
                return false;
            }

            return true;
        }
        catch (SQLException se)
        {
            TeachingTutorials.getInstance().getLogger().log(Level.SEVERE, "SQL Error deleting location with LocationID = "+iLocationID, se);
            return false;
        }
        catch (Exception e)
        {
            TeachingTutorials.getInstance().getLogger().log(Level.SEVERE, "Non-SQL Error deleting location with LocationID = "+iLocationID, e);
            return false;
        }
    }
}
