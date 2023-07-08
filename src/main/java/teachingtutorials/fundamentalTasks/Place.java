package teachingtutorials.fundamentalTasks;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.newlocation.DifficultyListener;
import teachingtutorials.tutorials.Group;
import teachingtutorials.tutorials.LocationTask;
import teachingtutorials.utils.Display;


public class Place extends Task implements Listener
{
    //The minecraft coordinates of the intended block in x, y, z form
    private final int iTargetCoords[] = new int[3];
    private Material mTargetMaterial;

    boolean bDisplayVirtualBlocks = false;

    private DifficultyListener difficultyListener;

    //Used in a lesson
    public Place(TeachingTutorials plugin, Player player, Group parentGroup, int iOrder, String szDetails, String szAnswers, float fDifficulty)
    {
        super(plugin);

        this.bNewLocation = false;

        this.type = "place";
        this.player = player;
        this.parentGroup = parentGroup;

        //Extracts the coordinates
        String[] szCoordinates3AndMaterial = szAnswers.split(",");
        iTargetCoords[0] = Integer.parseInt(szCoordinates3AndMaterial[0]);
        iTargetCoords[1] = Integer.parseInt(szCoordinates3AndMaterial[1]);
        iTargetCoords[2] = Integer.parseInt(szCoordinates3AndMaterial[2]);

        //Extracts the material
        mTargetMaterial = Material.getMaterial(szCoordinates3AndMaterial[3]);

        this.iOrder = iOrder;
        this.szDetails = szDetails;

        this.fDifficulty = fDifficulty;

        scheduleVirtualBlocks();
    }

    //Used in location creation
    public Place(TeachingTutorials plugin, Player player, Group parentGroup, int iTaskID, int iOrder, String szDetails)
    {
        super(plugin);
        this.type = "place";
        this.player = player;
        this.bNewLocation = true;
        this.parentGroup = parentGroup;
        this.iTaskID = iTaskID;
        this.iOrder = iOrder;
        this.szDetails = szDetails;

        scheduleVirtualBlocks();

        //Listen out for difficulty - There will only be one difficulty listener per place task to avoid bugs
        difficultyListener = new DifficultyListener(this.plugin, this.player, this, FundamentalTask.tpll);
        difficultyListener.register();
    }

    @Override
    public void register()
    {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    //Want the tutorials place process to occur first
    @EventHandler(priority = EventPriority.LOWEST)
    public void commandEvent(BlockPlaceEvent event)
    {
        fPerformance = 0F;
        //Checks that it is the correct player
        if (!event.getPlayer().getUniqueId().equals(player.getUniqueId()))
        {
            return;
        }
        else
        {
            event.setCancelled(true);

            //Get the material placed
            Block blockPlaced = event.getBlockPlaced();
            mTargetMaterial = blockPlaced.getType();

            //Get the location of the placed block
            int iBlockX = blockPlaced.getX();
            int iBlockY = blockPlaced.getY();
            int iBlockZ = blockPlaced.getZ();

            //Checks whether it is a new location
            if (bNewLocation)
            {
                //Store the location
                iTargetCoords[0] = iBlockX;
                iTargetCoords[1] = iBlockY;
                iTargetCoords[2] = iBlockZ;

                //Set the answers
                LocationTask locationTask = new LocationTask(this.parentGroup.parentStep.parentStage.getLocationID(), iTaskID);
                locationTask.setAnswers(iBlockX +"," +iBlockY +"," +iBlockZ +"," +blockPlaced.getType());
                difficultyListener.setLocationTask(locationTask);

                //Data is added to database once difficulty is provided

                //Prompt difficulty
                Display difficultyPrompt = new Display(player, ChatColor.AQUA +"Enter the difficulty of that place from 0 to 1 as a decimal. Use /tutorials [difficulty]");
                difficultyPrompt.Message();

                //SpotHit is then called from inside the difficulty listener once the difficulty has been established
                //This is what moves it onto the next task

                //Sets the displaying of the virtual blocks to true
                bDisplayVirtualBlocks = true;
            }
            else
            {   //--Accuracy checker--

                boolean bCorrectPosition;
                boolean bCorrectMaterial;

                //Check material
                bCorrectMaterial = event.getBlockPlaced().getType().equals(mTargetMaterial);

                //Check position
                bCorrectPosition = (iBlockX == iTargetCoords[0] && iBlockY == iTargetCoords[1] && iBlockZ == iTargetCoords[2]);

                if (bCorrectMaterial && bCorrectPosition)
                {
                    //Correct everything
                    Display display = new Display(player, ChatColor.GREEN +"Correct");
                    display.ActionBar();
                    fPerformance = 1; // Will be more comprehensive in future updates

                    //Sets the displaying of the virtual blocks to true
                    bDisplayVirtualBlocks = true;

                    spotHit();
                }
                else if (bCorrectMaterial)
                {
                    //Material correct, position wrong
                    Display display = new Display(player, ChatColor.GOLD +"Correct material, wrong position");
                    display.ActionBar();
                }
                else if (bCorrectPosition)
                {
                    //Position correct, material wrong
                    Display display = new Display(player, ChatColor.GOLD +"Correct position, wrong material");
                    display.ActionBar();
                }
                else
                {
                    //Nothing correct
                    Display display = new Display(player, ChatColor.GOLD +"Incorrect position and material");
                    display.ActionBar();
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

    private void scheduleVirtualBlocks()
    {
        //Gets the selection task associated with this command (assumes it is the previous task in the group)
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable()
        {
            boolean bLessonActive;
            @Override
            public void run()
            {
                //Fetches the status of the lesson or new location creation
                if (bNewLocation)
                    bLessonActive = !parentGroup.parentStep.parentStage.newLocation.bCompleteOrFinished;
                else
                    bLessonActive = !parentGroup.parentStep.parentStage.lesson.bCompleteOrFinished;

                //Have a command complete checker because this just repeats every second
                //Also check whether the lesson/new location creation is still active. If the user is finished with it then we need to stop displaying virtual blocks
                if (bDisplayVirtualBlocks)
                {
                    Location location = new Location(player.getWorld(), iTargetCoords[0], iTargetCoords[1], iTargetCoords[2]);

                    if (bLessonActive)
                    {
                        player.sendBlockChange(location, Bukkit.createBlockData(mTargetMaterial));
                    }
                    else
                    {
                        player.sendBlockChange(location, location.getBlock().getBlockData());
                        return;
                    }
                }
            }
        }, 20, 10);
    }
}
