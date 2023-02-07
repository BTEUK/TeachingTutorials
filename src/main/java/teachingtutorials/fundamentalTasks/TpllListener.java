package teachingtutorials.fundamentalTasks;

import net.buildtheearth.terraminusminus.util.geo.CoordinateParseUtils;
import net.buildtheearth.terraminusminus.util.geo.LatLng;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.newlocation.DifficultyListener;
import teachingtutorials.tutorials.Group;
import teachingtutorials.tutorials.LocationTask;
import teachingtutorials.utils.Display;

//All tpll command config will be the real life lat and long cords
//Having them as the block coords would be more work for the designer of a tutorial

public class TpllListener extends Task implements Listener
{
    //Stores the target coords - the location a player should tpll to
    final double dTargetCoords[] = new double[2];

    public TpllListener(TeachingTutorials plugin, Player player, Group parentGroup, String szAnswers, float fDifficulty)
    {
        super(plugin);
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
        this.player = player;
        this.bNewLocation = true;
        this.parentGroup = parentGroup;
        this.iTaskID = iTaskID;
    }

    @Override
    public void register()
    {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void interactEvent(PlayerCommandPreprocessEvent event)
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
                }
                //Checks whether it is a new location
                else if (bNewLocation)
                {
                    //Set the answers
                    LocationTask locationTask = new LocationTask(this.parentGroup.parentStep.parentStage.getLocationID(), iTaskID);
                    locationTask.setAnswers(latLong.getLat()+","+latLong.getLng());
                    //Data is added to database once difficulty is provided

                    //Listen out for difficulty
                    DifficultyListener difficultyListener = new DifficultyListener(this.plugin, this.player, locationTask, this);
                    difficultyListener.register();

                    //SpotHit is then called from inside the difficulty listener once the difficulty has been established
                    //This is what moves it onto the next task
                }
                else
                {
                    //Tpll accuracy checker
                    double dLatitude1 = latLong.getLat();
                    double dLatitude2 = dTargetCoords[0];

                    double dLongitude1 = latLong.getLng();
                    double dLongitude2 = dTargetCoords[1];

                    int iRadius = 6371000; // metres
                    double φ1 = dLatitude1 * Math.PI/180; // φ, λ in radians
                    double φ2 = dLatitude2 * Math.PI/180;
                    double Δφ = (dLatitude2-dLatitude1) * Math.PI/180;
                    double Δλ = (dLongitude2-dLongitude1) * Math.PI/180;

                    double a = Math.sin(Δφ/2) * Math.sin(Δφ/2) +
                        Math.cos(φ1) * Math.cos(φ2) *
                                Math.sin(Δλ/2) * Math.sin(Δλ/2);
                    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

                    float fDistance = (float) (iRadius * c); // in metres

                    if (fDistance <= 0.25)
                    {
                        Display display = new Display(player, ChatColor.DARK_GREEN+"Perfect! Well done");
                        display.Message();
                        fPerformance = 1;
                        spotHit();
                    }
                    else if (fDistance <= 1.0) //Make the acceptable value configurable
                    {
                        //Pretty decent
                        HandlerList.unregisterAll(this);
                        //Ranked from 0 to 1
                        //iScore = ....
                        Display display = new Display(player, ChatColor.GREEN+"Point hit");
                        display.Message();
                        fPerformance = (4F / 3F) * (1 - (float) fDistance);
                        spotHit();
                    }
                }
            }
        }
    }

    private void spotHit()
    {
        //Unregisters this task
        HandlerList.unregisterAll(this);

        //Marks the task as complete
        taskComplete();
    }

    //A public version is required for when spotHit is called from the difficulty listener
    //This is required as it means that the tutorial can be halted until the difficulty listener completes the creation of the new LocationTask
    public void newLocationSpotHit()
    {
        spotHit();
    }
}