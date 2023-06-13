package teachingtutorials.fundamentalTasks;

import net.buildtheearth.terraminusminus.generator.EarthGeneratorSettings;
import net.buildtheearth.terraminusminus.projection.GeographicProjection;
import net.buildtheearth.terraminusminus.projection.OutOfProjectionBoundsException;
import net.buildtheearth.terraminusminus.util.geo.CoordinateParseUtils;
import net.buildtheearth.terraminusminus.util.geo.LatLng;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.newlocation.DifficultyListener;
import teachingtutorials.tutorials.Group;
import teachingtutorials.tutorials.Location;
import teachingtutorials.tutorials.LocationTask;
import teachingtutorials.utils.Display;

//All tpll command config will be the real life lat and long cords
//Having them as the block coords would be more work for the designer of a tutorial

public class TpllListener extends Task implements Listener
{
    //Stores the target coords - the location a player should tpll to
    final double dTargetCoords[] = new double[2];

    private DifficultyListener difficultyListener;

    public TpllListener(TeachingTutorials plugin, Player player, Group parentGroup, String szAnswers, float fDifficulty)
    {
        super(plugin);
        this.type = "tpll";
        this.player = player;
        this.parentGroup = parentGroup;

        //Extracts the answers
        String[] cords = szAnswers.split(",");
        this.dTargetCoords[0] = Double.parseDouble(cords[0]);
        this.dTargetCoords[1] = Double.parseDouble(cords[1]);

        this.fDifficulty = fDifficulty;

        this.bNewLocation = false;
    }

    public TpllListener(TeachingTutorials plugin, Player player, Group parentGroup, int iTaskID)
    {
        super(plugin);
        this.type = "tpll";
        this.player = player;
        this.bNewLocation = true;
        this.parentGroup = parentGroup;
        this.iTaskID = iTaskID;

        //Listen out for difficulty - There will only be one difficulty listener per tpll command to avoid bugs
        difficultyListener = new DifficultyListener(this.plugin, this.player, this, FundamentalTask.tpll);
        difficultyListener.register();
    }

    @Override
    public void register()
    {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    //Want the tutorials tpll process to occur first
    @EventHandler(priority = EventPriority.LOWEST)
    public void commandEvent(PlayerCommandPreprocessEvent event)
    {
        fPerformance = 0F;

        //Checks that it is the correct player
        if (event.getPlayer().getUniqueId().equals(player.getUniqueId()))
        {
            //Checks that it is the correct command
            String command = event.getMessage();
            if (command.startsWith("/tpll"))
            {
                //Cancels the event
                event.setCancelled(true);

                command = command.replace("/tpll ", "");
                LatLng latLong = CoordinateParseUtils.parseVerbatimCoordinates(command.replace(", ", " "));

                //Checks that the coordinates were established
                if (latLong == null)
                {
                    event.setCancelled(true);
                    Display display = new Display(player, ChatColor.RED +"Incorrect tpll format");
                    display.Message();
                    return;
                }

                //Teleports the player to where they tplled to
                World world;
                if (bNewLocation)
                    world = player.getWorld();
                else
                {
                    Location location = parentGroup.parentStep.parentStage.lesson.location;
                    world = location.getWorld();
                }
                final GeographicProjection projection = EarthGeneratorSettings.parse(EarthGeneratorSettings.BTE_DEFAULT_SETTINGS).projection();
                try
                {
                    double[] xz = projection.fromGeo(latLong.getLng(), latLong.getLat());
                    org.bukkit.Location tpLocation;
                    tpLocation = new org.bukkit.Location(world, xz[0], world.getHighestBlockYAt((int) xz[0], (int) xz[1]) + 1, xz[1]);
                    player.teleport(tpLocation);
                }
                catch (OutOfProjectionBoundsException e)
                {
                    Display display = new Display(player, ChatColor.RED +"Coordinates not on the earth");
                    display.Message();
                    return;
                }

                //Checks whether it is a new location
                if (bNewLocation)
                {
                    //Set the answers
                    LocationTask locationTask = new LocationTask(this.parentGroup.parentStep.parentStage.getLocationID(), iTaskID);
                    locationTask.setAnswers(latLong.getLat()+","+latLong.getLng());
                    difficultyListener.setLocationTask(locationTask);

                    //Data is added to database once difficulty is provided

                    //Prompt difficulty
                    Display difficultyPrompt = new Display(player, ChatColor.AQUA +"Enter the difficulty of that tpll from 0 to 1 as a decimal. Use /tutorials [difficulty]");
                    difficultyPrompt.Message();

                    //SpotHit is then called from inside the difficulty listener once the difficulty has been established
                    //This is what moves it onto the next task
                }
                else
                {
                    //Tpll accuracy checker
                    float fDistance = Utils.geometricDistance(latLong, dTargetCoords);

                    if (fDistance <= 0.25)
                    {
                        //Very accurate
                        Display display = new Display(player, ChatColor.DARK_GREEN+"Perfect! Well done");
                        display.ActionBar();
                        fPerformance = 1;
                        spotHit();
                    }
                    else if (fDistance <= 1.0) //Make the acceptable value configurable
                    {
                        //Pretty decent
                        Display display = new Display(player, ChatColor.GREEN+"Point hit");
                        display.ActionBar();
                        fPerformance = (4F / 3F) * (1 - fDistance);
                        spotHit();
                    }
                }
            }
        }
    }

    private void spotHit()
    {
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Unregistering tpll listener");

        //Unregisters this task
        HandlerList.unregisterAll(this);

        //Marks the task as complete
        taskComplete();
    }

    @Override
    public void unregister()
    {
        //Unregisters this task
        HandlerList.unregisterAll(this);
    }

    //A public version is required for when spotHit is called from the difficulty listener
    //This is required as it means that the tutorial can be halted until the difficulty listener completes the creation of the new LocationTask
    @Override
    public void newLocationSpotHit()
    {
        spotHit();
    }
}