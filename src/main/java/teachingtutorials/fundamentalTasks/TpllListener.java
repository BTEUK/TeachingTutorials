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

    //Stores the required accuracies. Perfect distance first then limit distance
    private float fAccuracies[];

    //Stores the distance from the tpll command coordinates to the target coordinates once a tpll command is run during the active time of the listener
    public float fGeometricDistance;

    private DifficultyListener difficultyListener;

    //Used in a lesson
    public TpllListener(TeachingTutorials plugin, Player player, Group parentGroup, int iOrder, String szDetails, String szAnswers, float fDifficulty)
    {
        super(plugin);
        this.type = "tpll";
        this.player = player;
        this.parentGroup = parentGroup;

        //Extracts the answers
        String[] cords = szAnswers.split(",");
        this.dTargetCoords[0] = Double.parseDouble(cords[0]);
        this.dTargetCoords[1] = Double.parseDouble(cords[1]);

        //Extracts the details - required accuracies
        if (szDetails.equals("")) // Deals with pre 1.1.0 tutorials
        {
            this.fAccuracies = new float[]{0.25f, 1};
        }
        else
        {
            String[] szAccuracies = szDetails.split(";");
            this.fAccuracies = new float[2];
            this.fAccuracies[0] = Float.parseFloat(szAccuracies[0]);
            this.fAccuracies[1] = Float.parseFloat(szAccuracies[1]);
        }

        this.iOrder = iOrder;
        this.szDetails = szDetails;

        this.fDifficulty = fDifficulty;

        this.bNewLocation = false;
    }

    //Used when creating a new location
    public TpllListener(TeachingTutorials plugin, Player player, Group parentGroup, int iTaskID, int iOrder, String szDetails)
    {
        super(plugin);
        this.type = "tpll";
        this.player = player;
        this.bNewLocation = true;
        this.parentGroup = parentGroup;
        this.iTaskID = iTaskID;
        this.iOrder = iOrder;
        this.szDetails = szDetails;

        //Listen out for difficulty - There will only be one difficulty listener per tpll command to avoid bugs
        difficultyListener = new DifficultyListener(this.plugin, this.player, this, FundamentalTask.tpll);
        difficultyListener.register();
    }

    @Override
    public void register()
    {
        super.register();

        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);

        //Displays the marker on the world
        World world;
        if (this.parentGroup.parentStep.parentStage.bLocationCreation)
            world = this.parentGroup.parentStep.parentStage.newLocation.getLocation().getWorld();
        else
            world = this.parentGroup.parentStep.parentStage.lesson.location.getWorld();

        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run()
            {
                if (bActive)
                    player.spawnParticle(Particle.REDSTONE, Utils.convertToBukkitLocation(world, dTargetCoords[0], dTargetCoords[1]).add(0, 1, 0), 10, new Particle.DustOptions(Color.GREEN, 3));
                else
                {
                    return;
                }
            }
        }, 0L, 15L);
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
                    fGeometricDistance = fDistance;

                    float fPerfect = fAccuracies[0];
                    float fLimit = fAccuracies[1];

                    if (fDistance <= fPerfect)
                    {
                        //Very accurate
                        Display display = new Display(player, ChatColor.DARK_GREEN+"Perfect! Well done");
                        display.ActionBar();
                        fPerformance = 1;
                        spotHit();
                        parentGroup.parentStep.bPointWasHit = true;
                    }
                    else if (fDistance <= fLimit)
                    {
                        //Pretty decent
                        Display display = new Display(player, ChatColor.GREEN+"Point hit");
                        display.ActionBar();
                        fPerformance = (1F / fLimit-fPerfect) * (fLimit - fDistance);
                        spotHit();
                        parentGroup.parentStep.bPointWasHit = true;
                    }

                    //Provide the step with a value for how far away it was and a reference to this
                    this.parentGroup.parentStep.handledTpllListeners.add(this);

                    //Once all tpll tasks have finished it will then look through them all and if any were completed
                    //Then it will not bother outputting a distance message
                    //If none are complete then it will output a distance message with how far they were from the nearest point

                    //Only initiates the calculateNearestTpllPoint method if there is not already one queued
                    if (!this.parentGroup.parentStep.bTpllDistanceMessageQueued)
                    {
                        this.parentGroup.parentStep.calculateNearestTpllPointAfterWait();
                    }
                }
            }
        }
    }

    private void spotHit()
    {
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Unregistering tpll listener");

        //Unregisters this task
        unregister();

        //Marks the task as complete
        taskComplete();
    }

    @Override
    public void unregister()
    {
        super.unregister();

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