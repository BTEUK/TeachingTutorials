package teachingtutorials.tutorials;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.fundamentalTasks.GeometricUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Holds step data specific to a location and contains some procedures related to utilising this data
 */
public class LocationStep// extend step?
{
    //Data stored in the LocationSteps table in the DB
    private int iLocationID;
    private int iStepID;

    private double dStartLatitude;
    private double dStartLongitude;
    private float fStartYaw;
    private float fStartPitch;

    private String szInstructions;
    private double dHologramLocationX;
    private double dHologramLocationY;
    private double dHologramLocationZ;



    private boolean bLocationSet;

    public LocationStep(int iLocationID, int iStepID)
    {
        this.iLocationID = iLocationID;
        this.iStepID = iStepID;

        bLocationSet = false;
    }

    public boolean isLocationSet()
    {
        return bLocationSet;
    }

    /**
     * //Accesses the DB and fetches the information about the step location
     * @param iStepID The step ID of the step
     * @param iLocationID The location that is being played
     * @return
     */
    public static LocationStep getFromStepAndLocation(int iStepID, int iLocationID)
    {
        LocationStep locationStep = new LocationStep(iLocationID, iStepID);

        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            //Compiles the command to fetch the location step
            sql = "Select * FROM LocationSteps WHERE Step = "+iStepID +" AND Location = " +iLocationID;
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            if (resultSet.next())
            {
                //Extracts and stores the data
                locationStep.dStartLatitude = resultSet.getDouble("Latitude");
                locationStep.dStartLongitude = resultSet.getDouble("Longitude");
                locationStep.fStartYaw = resultSet.getFloat("StartYaw");
                locationStep.fStartPitch = resultSet.getFloat("StartPitch");
//                locationStep.szInstructions = resultSet.getString("Instructions");
//                locationStep.dHologramLocationX = resultSet.getDouble("InstructionsX");
//                locationStep.dHologramLocationY = resultSet.getDouble("InstructionsY");
//                locationStep.dHologramLocationZ = resultSet.getDouble("InstructionsZ");
            }
        }
        catch(SQLException se)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - SQL Error fetching Steps by StageID");
            se.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return locationStep;
    }

    /**
     * Adds the location step to the DB
     */
    public boolean storeDetailsInDB()
    {
        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        int iCount;

        try
        {
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();
            sql = "INSERT INTO LocationSteps (Location, Step, Latitude, Longitude, StartYaw, StartPitch) VALUES ("
                    + iLocationID +", "
                    + iStepID +", "
                    + dStartLatitude +", "
                    + dStartLongitude +", "
                    + fStartYaw +", "
                    + fStartPitch
                    +")";
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +sql);
            iCount = SQL.executeUpdate(sql);

            if (iCount != 1)
            {
                return false;
            }
            return true;
        }
        catch (SQLException se)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - SQL Error adding new location step");
            se.printStackTrace();
            return false;
        }
        catch (Exception e)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - Other error adding new location step");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Teleports the player to the start of the step
     * @param player The player to teleport
     * @param world The world for the relevant location
     * @return The start location of the step
     */
    public Location teleportPlayerToStartOfStep(Player player, World world, TeachingTutorials plugin)
    {
        Location location = getStartLocation(world);
        if (location != null)
        {
            //Teleports the player
            Location finalLocation = location;
            Bukkit.getScheduler().runTask(plugin, () -> player.teleport(finalLocation));
        }
        else
            location = player.getLocation();

        return location;
    }

    /**
     * Sets the step start location
     * @param location The location of the intended step start location
     */
    public void setStartLocation(Location location)
    {
        double[] longLat = GeometricUtils.convertToGeometricCoordinates(location.getX(), location.getZ());

        if (longLat != null)
        {
            this.dStartLongitude = longLat[0];
            this.dStartLatitude = longLat[1];

            this.fStartPitch = location.getPitch();
            this.fStartYaw = location.getYaw();

            bLocationSet = true;
        }
        else
        {

        }
    }

    /**
     *
     * @return The start location for the step as a bukkit object
     */
    public Location getStartLocation(World world)
    {
        Location location = GeometricUtils.convertToBukkitLocation(world, dStartLatitude, dStartLongitude);

        if (location != null)
        {
            location.setY(location.getY() + 1);
            location.setYaw(fStartYaw);
            location.setPitch(fStartPitch);
        }
        return location;
    }
}
