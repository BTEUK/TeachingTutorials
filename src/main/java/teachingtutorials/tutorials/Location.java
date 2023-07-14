package teachingtutorials.tutorials;

import net.buildtheearth.terraminusminus.generator.EarthGeneratorSettings;
import net.buildtheearth.terraminusminus.projection.GeographicProjection;
import net.buildtheearth.terraminusminus.util.geo.LatLng;
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
    private LatLng startCoordinates;
    private int iTutorialID;
    private float fDifficulty;
    private World world;

    //--------------------------------------------------
    //-------------------Constructors-------------------
    //--------------------------------------------------

    //Used for creating a new location
    public Location(LatLng latlong, int iTutorialID)
    {
        this.startCoordinates = latlong;
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
        return world;
    }

    public int getTutorialID()
    {
        return iTutorialID;
    }

    public LatLng getStartCoordinates()
    {
        return startCoordinates;
    }

    //---------------------------------------------------
    //----------------------Setters----------------------
    //---------------------------------------------------

    public void setWorld(World world)
    {
        this.world = world;
    }

    //---------------------------------------------------
    //-----------------------Utils-----------------------
    //---------------------------------------------------

    public org.bukkit.Location calculateBukkitStartLocation()
    {
        double[] xz;
        final GeographicProjection projection = EarthGeneratorSettings.parse(EarthGeneratorSettings.BTE_DEFAULT_SETTINGS).projection();

        //Converts the longitude and latitude start coordinates of the location to minecraft coordinates
        try
        {
            xz = projection.fromGeo(this.getStartCoordinates().getLng(), this.getStartCoordinates().getLat());
            //Declares location object
            org.bukkit.Location tpLocation;

            tpLocation = new org.bukkit.Location(world, xz[0], world.getHighestBlockYAt((int) xz[0], (int) xz[1]) + 1, xz[1]);
            return tpLocation;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Unable to convert lat,long coordinates of start location to minecraft coordinates");
            return null;
        }
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
            sql = "Select * FROM Locations WHERE Locations.LocationID = " +iLocationID;

            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);

            //Move the cursor to the first row
            resultSet.next();

            //Stores the information
            this.fDifficulty = resultSet.getFloat("Difficulty");;
            double dLatitude = resultSet.getDouble("Latitude");
            double dLongitude = resultSet.getDouble("Longitude");
            this.iTutorialID = resultSet.getInt("TutorialID");

            //Puts the start coordinates into the LatLng object
            this.startCoordinates = new LatLng(dLatitude, dLongitude);
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
            sql = "Select * FROM Locations WHERE TutorialID = " +iTutorialID;
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
            sql = "INSERT INTO Locations (TutorialID, Latitude, Longitude) VALUES (" +iTutorialID +", " +startCoordinates.getLat() +", " +startCoordinates.getLng() +")";
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +sql);
            iCount = SQL.executeUpdate(sql);

            if (iCount != 1)
            {
                return false;
            }

            //Gets the LocationID of the newly inserted location
            sql = "Select LAST_INSERT_ID()";
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
            sql = "Delete FROM LocationTasks WHERE LocationID = " +iLocationID;
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] " +sql);
            iCount = SQL.executeUpdate(sql);
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] " +iCount +" LocationTasks were deleted");

            //Removes the location
            sql = "Delete FROM Locations WHERE LocationID = " +iLocationID;
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
