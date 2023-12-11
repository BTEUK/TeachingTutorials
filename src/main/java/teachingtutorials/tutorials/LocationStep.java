package teachingtutorials.tutorials;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.fundamentalTasks.GeometricUtils;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.Hologram;

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
    private Hologram instructions;
    private String szVideoWalkthroughLink;

    private boolean bLocationSet;
    private boolean bInstructionsSet;
    private boolean bHologramLocationSet;

    public LocationStep(int iLocationID, int iStepID, boolean bHologramNeeded)
    {
        this.iLocationID = iLocationID;
        this.iStepID = iStepID;
        this.szInstructions = "";
        this.szVideoWalkthroughLink = "";

        bLocationSet = false;
        bInstructionsSet = false;
        bHologramLocationSet = !bHologramNeeded;

        if (bHologramLocationSet)
            Bukkit.getConsoleSender().sendMessage("Hologram location is set because no hologram is needed");
        //If hologram needed then we set this to false as the location is not set yet
        //If a hologram is not needed then we set this to true, indicating that this does not need to be done
    }

    public boolean isOtherInformationSet()
    {
        if (bLocationSet)
            Bukkit.getConsoleSender().sendMessage("Location is set");
        if (bInstructionsSet)
            Bukkit.getConsoleSender().sendMessage("Instruction is set");
        if (bHologramLocationSet)
            Bukkit.getConsoleSender().sendMessage("Hologram location is set");

        boolean bAllExtraInformationIsSet = (bLocationSet && bInstructionsSet) && bHologramLocationSet;
        if (bAllExtraInformationIsSet)
            Bukkit.getConsoleSender().sendMessage("All extra information is set");

        return bAllExtraInformationIsSet;
    }

    //------------------------------------------------
    //--------------------Database--------------------
    //------------------------------------------------

    /**
     * //Accesses the DB and fetches the information about the step location
     * @param iStepID The step ID of the step
     * @param iLocationID The location that is being played
     * @return
     */
    public static LocationStep getFromStepAndLocation(int iStepID, int iLocationID, boolean bHologramNeeded)
    {
        LocationStep locationStep = new LocationStep(iLocationID, iStepID, bHologramNeeded);

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
                locationStep.szInstructions = resultSet.getString("Instructions");
                locationStep.dHologramLocationX = resultSet.getDouble("InstructionsX");
                locationStep.dHologramLocationY = resultSet.getDouble("InstructionsY");
                locationStep.dHologramLocationZ = resultSet.getDouble("InstructionsZ");
                locationStep.szVideoWalkthroughLink = resultSet.getString("VideoWalkthroughLink");
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
            sql = "INSERT INTO LocationSteps (Location, Step, Latitude, Longitude, StartYaw, StartPitch, Instructions, InstructionsX, InstructionsY, InstructionsZ, VideoWalkthroughLink) VALUES ("
                    + iLocationID +", "
                    + iStepID +", "
                    + dStartLatitude +", "
                    + dStartLongitude +", "
                    + fStartYaw +", "
                    + fStartPitch +", '"
                    + szInstructions +"', "
                    + dHologramLocationX +", "
                    + dHologramLocationY +", "
                    + dHologramLocationZ +", '"
                    + szVideoWalkthroughLink +"'"
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

    //-----------------------------------------------
    //---------------------Utils---------------------
    //-----------------------------------------------

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
        {
            location = player.getLocation();
            player.sendMessage(ChatColor.RED +"No start location for this step has been set yet");
        }

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
    private Location getStartLocation(World world)
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

    /**
     * Displays the instructions to the player
     * @param displayType The way the instruction should be displayed
     * @param player The player to which the instruction should be displayed
     */
    public void displayInstructions(Display.DisplayType displayType, Player player, String szStepName, World world)
    {
        Display display;

        switch (displayType)
        {
            case hologram:
                display = new Display(player, this.szInstructions);
                instructions = display.Hologram(ChatColor.AQUA +"" +ChatColor.UNDERLINE +ChatColor.BOLD +szStepName, getHologramLocation(world));
                break;
            default:
                display = new Display(player, this.szInstructions);
                display.Message();
                break;
        }
    }

    /**
     * Removes the hologram from view if it is displayed
     */
    public void removeInstructionsHologram()
    {
        if (instructions != null)
            instructions.removeHologram();
    }

    /**
     * Sets the location of the instructions hologram and displays the instructions to the creator
     * @param player The player to which the location must be set to
     * @param szStepName The name of the step, so that the instructions can then be displayed
     */
    public void setHologramLocationToThatOfPlayer(Player player, String szStepName)
    {
        //Sets the location
        Location playerLocation = player.getLocation();
        this.dHologramLocationX = playerLocation.getX();
        this.dHologramLocationY = playerLocation.getY();
        this.dHologramLocationZ = playerLocation.getZ();

        //Displays the instructions
        removeInstructionsHologram();
        displayInstructions(Display.DisplayType.hologram, player, szStepName, player.getWorld());

        this.bHologramLocationSet = true;
    }

    /**
     *
     * @return The instructions' hologram location for the step as a bukkit object
     */
    private Location getHologramLocation(World world)
    {
        Location hologramLocation = new Location(world, dHologramLocationX, dHologramLocationY, dHologramLocationZ);
        return hologramLocation;
    }

    /**
     * Sets the instructions of the step and displays the instructions
     * @param szInstructions The desired instructions
     * @param displayType The display type, so the instructions can be displayed
     * @param player The player, so the instructions can be displayed
     * @param szStepName The player, so the instructions can be displayed
     */
    public void setInstruction(String szInstructions, Display.DisplayType displayType, Player player, String szStepName)
    {
        //Sets the instructions
        this.szInstructions = szInstructions;

        //Displays the instructions
        removeInstructionsHologram();
        displayInstructions(displayType, player, szStepName, player.getWorld());

        //Instructions may be blank at this point, but this is fine and is displayed blank on the hologram

        this.bInstructionsSet = true;
    }

    public void setVideoLink(String szLink)
    {
        this.szVideoWalkthroughLink = szLink;
    }

    public void displayVideoLink(Player player)
    {
        TextComponent linkMessage;

        if (szVideoWalkthroughLink.equals(""))
            linkMessage = Component.text("There is no video available for this step, sorry", NamedTextColor.GREEN);
        else
        {
            linkMessage = Component.text("Click here to access a video walk-through for this step !", NamedTextColor.GREEN);
            ClickEvent openLinkEvent = ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, this.szVideoWalkthroughLink);
            linkMessage = linkMessage.clickEvent(openLinkEvent);
        }
        player.sendMessage(linkMessage);
    }
}
