package teachingtutorials.fundamentalTasks;

import net.buildtheearth.terraminusminus.generator.EarthGeneratorSettings;
import net.buildtheearth.terraminusminus.projection.GeographicProjection;
import net.buildtheearth.terraminusminus.projection.OutOfProjectionBoundsException;
import net.buildtheearth.terraminusminus.util.geo.LatLng;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.newlocation.DifficultyListener;
import teachingtutorials.tutorials.Group;
import teachingtutorials.tutorials.LocationTask;
import teachingtutorials.utils.Display;

public class Selection extends Task implements Listener
{
    //Stores the target coords - the location a player should select as a point
    final double dTargetCoords1[] = new double[2]; //Lat then long
    final double dTargetCoords2[] = new double[2]; //Lat then long

    double[] dSelectionPoint1 = new double[2]; //Lat then long
    double[] dSelectionPoint2 = new double[2]; //Lat then long

    //Variables used by new location procedures
    boolean bSelection1Made;
    boolean bSelection2Made;

    float fWEDifficulty;

    private DifficultyListener difficultyListener;

    public Selection(TeachingTutorials plugin, Player player, Group parentGroup, String szAnswers, float fWEDifficulty)
    {
        super(plugin);
        this.type = "selection";
        this.player = player;
        this.parentGroup = parentGroup;

        //Extracts the answers
        String[] cords = szAnswers.split(",");
        this.dTargetCoords1[0] = Double.parseDouble(cords[0]);
        this.dTargetCoords1[1] = Double.parseDouble(cords[1]);
        this.dTargetCoords2[0] = Double.parseDouble(cords[2]);
        this.dTargetCoords2[1] = Double.parseDouble(cords[3]);

        this.fWEDifficulty = fWEDifficulty;

        this.bNewLocation = false;

        this.bSelection1Made = false;
        this.bSelection2Made = false;
    }

    public Selection(TeachingTutorials plugin, Player player, Group parentGroup, int iTaskID)
    {
        super(plugin);
        this.type = "selection";
        this.player = player;
        this.bNewLocation = true;
        this.parentGroup = parentGroup;
        this.iTaskID = iTaskID;

        this.bSelection1Made = false;
        this.bSelection2Made = false;

        //Listen out for difficulty - There will only be one difficulty listener per selection to avoid bugs
        difficultyListener = new DifficultyListener(this.plugin, this.player, this, FundamentalTask.selection);
        difficultyListener.register();
    }

    @Override
    public void register()
    {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void interactEvent(PlayerInteractEvent event)
    {
        fPerformance = 0F;

        //Checks that it is the correct player
        if (!event.getPlayer().getUniqueId().equals(player.getUniqueId()))
        {
            return;
        }
        //Checks that it is the correct tool
        if (!event.hasItem())
        {
            return;
        }
        if (!event.getItem().getType().equals(Material.WOODEN_AXE))
        {
            return;
        }
        //Checks that it is a left or right click of a block
        if (!(event.getAction().equals(Action.LEFT_CLICK_BLOCK) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK)))
        {
            return;
        }

        boolean bIsSelection1 = false;

        //Converts block coordinates to lat/long
        double[] longLat;
        float[] fDistance = new float[2];

        final GeographicProjection projection = EarthGeneratorSettings.parse(EarthGeneratorSettings.BTE_DEFAULT_SETTINGS).projection();
        try
        {
            longLat = projection.toGeo(event.getClickedBlock().getX()+0.5d, event.getClickedBlock().getY()+0.5d);
        }
        catch (OutOfProjectionBoundsException e)
        {
            //Player has selected an area outside of the projection
            return;
        }

        //Checks whether it is a new location
        if (bNewLocation)
        {
            //Checks whether it is a left click or right click and stores the coordinates
            if (event.getAction().equals(Action.LEFT_CLICK_BLOCK))
            {
               // Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Designer made left click selection");
                bSelection1Made = true;
                dTargetCoords1[0] = longLat[1];
                dTargetCoords1[1] = longLat[0];
            }
            else if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK))
            {
               // Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Designer made right click selection");
                bSelection2Made = true;
                dTargetCoords2[0] = longLat[1];
                dTargetCoords2[1] = longLat[0];
            }
            else
            {
                //Should never reach this really
                return;
            }

            //Checks whether both selections have been made
            if ((bSelection1Made && bSelection2Made))
            {
                Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Player has now made both points of the selection");
                Display display = new Display(player, ChatColor.DARK_GREEN+"Selection complete");
                display.ActionBar();

                //Set the answers - answers are stored in a format matching that of how they are decoded in the constructor of this class
                LocationTask locationTask = new LocationTask(this.parentGroup.parentStep.parentStage.getLocationID(), iTaskID);
                locationTask.setAnswers(dTargetCoords1[0] +"," +dTargetCoords1[1] +"," +dTargetCoords2[0] +"," +dTargetCoords2[1]);
                difficultyListener.setLocationTask(locationTask);

                //Data is added to database once difficulty is provided

                //Prompt difficulty
                Display difficultyPrompt = new Display(player, ChatColor.AQUA +"Enter the difficulty of that selection from 0 to 1 as a decimal. Use /tutorials [difficulty]");
                difficultyPrompt.Message();

                //SpotHit is then called from inside the difficulty listener once the difficulty has been established
                //This is what moves it onto the next task
            }
        }

        //Checks whether it is a left click or right click and stores the coordinates
        else
        {
            if (event.getAction().equals(Action.LEFT_CLICK_BLOCK))
            {
               // Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Player made left click selection");
                bIsSelection1 = true;
                dSelectionPoint1[0] = longLat[1];
                dSelectionPoint1[1] = longLat[0];
            }
            else if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK))
            {
               // Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Player made right click selection");
                bIsSelection1 = false;
                dSelectionPoint2[0] = longLat[1];
                dSelectionPoint2[1] = longLat[0];
            }

            //Checks whether it is anywhere near one of the block they are supposed to select
            boolean bPointFound = false;
            boolean bWasTarget1 = false;

            LatLng selectionGeoCoords;
            selectionGeoCoords = new LatLng(longLat[1], longLat[0]);
            fDistance[0] = Utils.geometricDistance(selectionGeoCoords, dTargetCoords1);
            fDistance[1] = Utils.geometricDistance(selectionGeoCoords, dTargetCoords2);

            for (int i = 0 ; i < 2 ; i++)
            {
                //Generally, tutorials should have a player tpll to the position first, so any reasonable value here is performance of 1
                if (fDistance[i] <= 1.5)
                {
                    Display display = new Display(player, ChatColor.GREEN+"Correct position selected");
                    display.ActionBar();
                    bPointFound = true;

                    //Records which target was found
                    bWasTarget1 = (i == 0);
                    break; //If they found a point, then good on them, but they can't get both with just one click
                }
            }

            //Now check that the other is also a valid point
            if (bPointFound)
            {
                float fOtherDistance;
                //If user left-clicked earlier and found a point, we want to check the right click point. Visa-versa
                if (bIsSelection1)
                    selectionGeoCoords = new LatLng(dSelectionPoint2[0], dSelectionPoint2[1]);
                else
                    selectionGeoCoords = new LatLng(dSelectionPoint1[0], dSelectionPoint1[1]);

                //If the point the user just found was target 1 then we want to check target 2
                if (bWasTarget1)
                    fOtherDistance = Utils.geometricDistance(selectionGeoCoords, dTargetCoords2);
                else
                    fOtherDistance = Utils.geometricDistance(selectionGeoCoords, dTargetCoords1);

                //Generally, tutorials should have a player tpll to the position first, so any reasonable value here is performance of 1
                if (fOtherDistance <= 1.5)
                {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Player has now made both points of the selection");
                    Display display = new Display(player, ChatColor.DARK_GREEN+"Selection complete");
                    display.ActionBar();

                    //Generally, tutorials should have a player tpll to the position first, so any reasonable value here is performance of 1
                    //Especially since this is quantised into blocks, so it wouldn't be a precise measure of performance anyway
                    fPerformance = 1;

                    bothSelectionsMade();
                }
            }
        }
    }

    private void bothSelectionsMade()
    {
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Unregistering selection listener");

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
        bothSelectionsMade();
    }
}
