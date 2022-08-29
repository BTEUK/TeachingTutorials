package teachingtutorials.tutorials;

import net.buildtheearth.terraminusminus.util.geo.LatLng;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
    }

    //---------------------------------------------------
    //----------------------Getters----------------------
    //---------------------------------------------------
    public int getLocationID()
    {
        return iLocationID;
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
            this.fDifficulty = resultSet.getInt("Difficulty");;
            double dLatitude = resultSet.getInt("Latitude");
            double dLongitude = resultSet.getInt("Longitude");
            this.iTutorialID = resultSet.getInt("TutorialID");

            //Puts the start coordinates into the LatLng object
            this.startCoordinates = new LatLng(dLatitude, dLongitude);
        }
        catch (Exception e)
        {

        }
    }

    //---------------------------------------------------
    //--------------------SQL Updates--------------------
    //---------------------------------------------------
    public void insertNewLocation()
    {
        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();
            sql = "INSERT INTO Locations (TutorialID, Latitude, Longitude) VALUES (" +iTutorialID +", " +startCoordinates.getLat() +", " +startCoordinates.getLng() +")";
            SQL.executeUpdate(sql);

            sql = "Select LAST_INSERT_ID()";
            resultSet = SQL.executeQuery(sql);
            resultSet.next();
            iLocationID = resultSet.getInt(1);
        }
        catch (SQLException se)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - SQL Error adding new location");
            se.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}