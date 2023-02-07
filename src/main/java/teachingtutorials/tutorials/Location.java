package teachingtutorials.tutorials;

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

    public Location(LatLng latlong, int iTutorialID)
    {
        this.startCoordinates = latlong;
        this.iTutorialID = iTutorialID;
    }

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
}
