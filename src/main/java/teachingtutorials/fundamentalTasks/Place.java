package teachingtutorials.fundamentalTasks;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
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

    private DifficultyListener difficultyListener;

    /**
     * Used in a lesson
     * @param plugin
     * @param player
     * @param parentGroup
     * @param iTaskID
     * @param iOrder
     * @param szDetails
     * @param szAnswers
     * @param fDifficulty
     */
    public Place(TeachingTutorials plugin, Player player, Group parentGroup, int iTaskID, int iOrder, String szDetails, String szAnswers, float fDifficulty)
    {
        super(plugin, player, parentGroup, iTaskID, iOrder, "place", szDetails, false);

        //Extracts the coordinates
        String[] szCoordinates3AndMaterial = szAnswers.split(",");
        iTargetCoords[0] = Integer.parseInt(szCoordinates3AndMaterial[0]);
        iTargetCoords[1] = Integer.parseInt(szCoordinates3AndMaterial[1]);
        iTargetCoords[2] = Integer.parseInt(szCoordinates3AndMaterial[2]);

        //Extracts the material
        mTargetMaterial = Material.getMaterial(szCoordinates3AndMaterial[3]);

        this.fDifficulty = fDifficulty;

        //Calculates the virtual block
        addVirtualBlock();
    }

    /**
     * Used when creating a new location
     * @param plugin
     * @param player
     * @param parentGroup
     * @param iTaskID
     * @param iOrder
     * @param szDetails
     */
    public Place(TeachingTutorials plugin, Player player, Group parentGroup, int iTaskID, int iOrder, String szDetails)
    {
        super(plugin, player, parentGroup, iTaskID, iOrder, "place", szDetails, true);

        //Listen out for difficulty - There will only be one difficulty listener per place task to avoid bugs
        difficultyListener = new DifficultyListener(this.plugin, this.player, this, FundamentalTaskType.tpll);
        difficultyListener.register();
    }

    @Override
    public void register()
    {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    //Want the tutorials place process to occur first
    @EventHandler(priority = EventPriority.LOWEST)
    public void interactEvent(PlayerInteractEvent event)
    {
        //Checks that it is the correct player
        if (!event.getPlayer().getUniqueId().equals(player.getUniqueId()))
            return;
        else if (!event.hasBlock())
            return;
        else if (!event.isBlockInHand())
            return;
        else if (!event.getAction().isRightClick())
            return;

        //It should now be a left click with a block against another block

        //Gets the location of the new virtual block to be placed
        Location newBlockLocation = event.getClickedBlock().getLocation().add(event.getBlockFace().getDirection());
        Material newBlockMaterial = event.getItem().getType();

        event.setCancelled(true);

        blockPlaced(newBlockLocation, newBlockMaterial);
    }

    //Want the tutorials place process to occur first
//    @EventHandler(priority = EventPriority.LOW)
//    public void placeEvent(BlockPlaceEvent event)
//    {
//
//    }

    private void blockPlaced(Location newBlockLocation, Material newBlockMaterial)
    {
        fPerformance = 0F;

        //Get the location of the placed block
        int iBlockX = newBlockLocation.getBlockX();
        int iBlockY = newBlockLocation.getBlockY();
        int iBlockZ = newBlockLocation.getBlockZ();

        //Checks whether it is a new location
        if (bCreatingNewLocation)
        {
            //Store the material
            mTargetMaterial = newBlockMaterial;

            //Store the location
            iTargetCoords[0] = iBlockX;
            iTargetCoords[1] = iBlockY;
            iTargetCoords[2] = iBlockZ;

            //Set the answers in the LocationTask
            LocationTask locationTask = new LocationTask(this.parentGroup.parentStep.parentStage.getLocationID(), iTaskID);
            locationTask.setAnswers(iBlockX +"," +iBlockY +"," +iBlockZ +"," +mTargetMaterial);
            difficultyListener.setLocationTask(locationTask);

            //Data is added to database once difficulty is provided

            //Prompt difficulty
            Display difficultyPrompt = new Display(player, ChatColor.AQUA +"Enter the difficulty of that place from 0 to 1 as a decimal. Use /tutorials [difficulty]");
            difficultyPrompt.Message();

            //SpotHit is then called from inside the difficulty listener once the difficulty has been established
            //This is what moves it onto the next task

            //Adds the virtual block
            addVirtualBlock();

            //Displays the virtual blocks
            displayVirtualBlocks();
        }
        else
        {   //--Accuracy checker--

            boolean bCorrectPosition;
            boolean bCorrectMaterial;

            //Check material
            bCorrectMaterial = newBlockMaterial.equals(mTargetMaterial);

            //Check position
            bCorrectPosition = (iBlockX == iTargetCoords[0] && iBlockY == iTargetCoords[1] && iBlockZ == iTargetCoords[2]);

            if (bCorrectMaterial && bCorrectPosition)
            {
                //Correct everything
                Display display = new Display(player, ChatColor.GREEN +"Correct");
                display.ActionBar();
                fPerformance = 1; // Will be more comprehensive in future updates

                //Displays the virtual block
                displayVirtualBlocks();

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

    private void spotHit()
    {
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Unregistering place listener");

        //Unregisters this task
        HandlerList.unregisterAll(this);

        //Marks the task as complete
        taskComplete();
    }

    //Called when terminating early
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

    /**
     * Uses the target coords and target material to calculate the virtual block
     */
    public void addVirtualBlock()
    {
        Location location = new Location(this.parentGroup.parentStep.parentStage.tutorialPlaythrough.getLocation().getWorld(), iTargetCoords[0], iTargetCoords[1], iTargetCoords[2]);
        this.virtualBlocks.put(location, mTargetMaterial.createBlockData());
    }
}
