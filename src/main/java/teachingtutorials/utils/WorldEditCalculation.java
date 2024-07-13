package teachingtutorials.utils;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.TutorialPlaythrough;

import java.util.HashSet;

/**
 * Stores the details of a world edit block change calculation which is to be made
 */
public class WorldEditCalculation
{
    private Object worldEditEventListener;
    private final String szWorldEditCommand;
    private final RegionSelector regionSelector;
    private final TutorialPlaythrough tutorialPlaythrough;
    private boolean bCalculationCurrentlyWaitingForFirstBlockChange;

    public Player getPlayer()
    {
        return tutorialPlaythrough.getCreatorOrStudent().player;
    }

    public World getWorld()
    {
        return tutorialPlaythrough.getLocation().getWorld();
    }

    public TutorialPlaythrough getTutorialPlaythrough()
    {
        return tutorialPlaythrough;
    }

    public WorldEditCalculation(String szWorldEditCommand, RegionSelector regionSelector, TutorialPlaythrough tutorialPlaythrough, int iTaskID, HashSet<VirtualBlock> virtualBlocks)
    {
        this.szWorldEditCommand = szWorldEditCommand;
        this.regionSelector = regionSelector;
        this.tutorialPlaythrough = tutorialPlaythrough;

        //2. Create the new event listener
        setUpListener(iTaskID, virtualBlocks);
    }

    public Object getEditSessionListener()
    {
        return this.worldEditEventListener;
    }

    public boolean getWaitingForFirstBlockChange()
    {
        return bCalculationCurrentlyWaitingForFirstBlockChange;
    }

    private void setUpListener(int iTaskID, HashSet<VirtualBlock> virtualBlocks)
    {
        //Get the console actor
        Actor consoleActor = BukkitAdapter.adapt(Bukkit.getConsoleSender());

        //Get a reference to this object
        WorldEditCalculation thisCalculation = this;

        worldEditEventListener = new Object()
        {
            // The following code is extracted from LogBlock under creative commons.
            // http://creativecommons.org/licenses/by-nc-sa/3.0/

            @Subscribe
            public void onEditSessionEvent(EditSessionEvent event)
            {
                final Actor actor = event.getActor();

                if (actor == null)
                {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Edit session event detected belonging a null actor (assuming console) - at stage: "+event.getStage().toString());
                }
                else if (actor.getName().equals(consoleActor.getName()))
                {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Edit session event detected belonging to the actor we are listening for - at stage: "+event.getStage().toString());
                }
                else
                {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Edit session event detected but doesn't belong to the correct actor, so ignoring");
                    return;
                }

                //Creates the new recorder extent
                AbstractDelegateExtent blockChangeRecorderExtent = new BlockChangeRecorderExtent(event, thisCalculation, virtualBlocks, iTaskID);

                //Updates the extent of the edit session to be that of a block recording extent
                //The block recording extent means that block changes are blocked and recorded in the set block mechanism

                //Sets the new extent into the event
                Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Setting the extent");
                event.setExtent(blockChangeRecorderExtent);
            }
        };
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN +"[TeachingTutorials] WorldEdit event listener has been initialised");
    }

    /**
     * Runs the block calculation. Ultimately this will send the world edit command through the console,
     *  and catch it with the event. This event has already been defined and contains the list of virtual blocks which
     *  it will add detected block changes to.
     */
    public void runCalculation()
    {
        //Updates the activity waiting indicator
        bCalculationCurrentlyWaitingForFirstBlockChange = true;
        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW +"[TeachingTutorials] Starting a new block change calculation");

        Bukkit.getScheduler().runTask(TeachingTutorials.getInstance(), () ->
        {
            //Sets the selection
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Adjusting the selection");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "/world "+tutorialPlaythrough.getLocation().getLocationID());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "/pos1 " + ((CuboidRegion) regionSelector.getRegion()).getPos1().toParserString());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "/pos2 " + ((CuboidRegion) regionSelector.getRegion()).getPos2().toParserString());

            //Registers the world change event listener
            try
            {
                WorldEdit.getInstance().getEventBus().register(worldEditEventListener);
            }
            catch (Exception e)
            {
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"[Teaching Tutorials] Error registering WorldEdit event listener: "+e.getMessage());
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"[Teaching Tutorials] :" +e.getCause());
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"[Teaching Tutorials] :" +e);
                e.printStackTrace();
            }

            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] World edit change event listener registered");

            //Runs the command
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Sending command: "+szWorldEditCommand);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), szWorldEditCommand);
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Command sent");
        });
    }

    /**
     * Mark the block changes in the world as having started
     */
    public void markFired()
    {
        bCalculationCurrentlyWaitingForFirstBlockChange = false;
    }
}

class BlockChangeRecorderExtent extends AbstractDelegateExtent
{
    EditSessionEvent editSessionEvent;
    WorldEditCalculation worldEditCalculation;
    HashSet<VirtualBlock> virtualBlocks;
    int iTaskID;

    public BlockChangeRecorderExtent(EditSessionEvent editSessionEvent, WorldEditCalculation worldEditCalculation, HashSet<VirtualBlock> virtualBlocks, int iTaskID)
    {
        super(editSessionEvent.getExtent());
        this.editSessionEvent = editSessionEvent;
        this.worldEditCalculation = worldEditCalculation;
        this.virtualBlocks = virtualBlocks;
        this.iTaskID = iTaskID;
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 position, B block)
    {
        Bukkit.getConsoleSender().sendMessage("\nA world edit block change has been detected, belonging to the listener for task " +iTaskID +". Recording to the given virtual blocks list");
        unregisterWorldChangeListener();
        calculateBlockChange(position, block, worldEditCalculation);
        //return super.setBlock(position, block);
        return false;
    }

    /**
     * Unregisters the listener and unblocks the calculation queue 0.1 seconds (2 ticks) after the first block change was detected
     */
    private void unregisterWorldChangeListener()
    {
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Block change detected, waiting for first block change of this calculation:" +worldEditCalculation.getWaitingForFirstBlockChange());
     //   The next thing to test is whether this above line ever gets sent, and if not then why not
     //   I don't think it will because it doesn't print the other line in the method which calls this so it won't print this one'
        if (worldEditCalculation.getWaitingForFirstBlockChange())
        {
            //Mark the block changes as having started
            worldEditCalculation.markFired();

            Bukkit.getScheduler().runTaskLater(TeachingTutorials.getInstance(), () ->
            {
                //Unregisters the WorldEdit event listener
                Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Unregistering world edit listener for task "+iTaskID);
                com.sk89q.worldedit.WorldEdit.getInstance().getEventBus().unregister(worldEditCalculation.getEditSessionListener());

                //Updates the queue system, unblocking the queue
                teachingtutorials.utils.WorldEdit.pendingCalculations.remove(worldEditCalculation);
                teachingtutorials.utils.WorldEdit.setCalculationFinished();
                Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN +"[TeachingTutorials] There are " +teachingtutorials.utils.WorldEdit.pendingCalculations.size() +" calculations remaining in the queue");

                Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Unregistered world edit listener for task "+iTaskID);

                int iSize = virtualBlocks.size();

                Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] There were " +iSize + " block changes recorded for task "+iTaskID);
            }, 2L);
        }
    }

    private  <B extends BlockStateHolder<B>> void calculateBlockChange(BlockVector3 pt, B block, WorldEditCalculation worldEditCalculation)
    {
//                        //This should only ever be for one of the 3 stages anyway right?
//                        if (event.getStage() != EditSession.Stage.BEFORE_CHANGE) {
//                            return;
//                        }

        //Calculates the old block
        Location location = BukkitAdapter.adapt(worldEditCalculation.getWorld(), pt);
        Block blockBefore = location.getBlock();
        BlockData blockDataBefore = blockBefore.getBlockData();

        //Gets the new block
        BlockData blockDataNew = BukkitAdapter.adapt(block);

        //If there is actually a change of block
        if (!blockDataBefore.equals(blockDataNew))
        {
            Bukkit.getConsoleSender().sendMessage("[TeachingTutorials] There was a change of block: ");
            Bukkit.getConsoleSender().sendMessage("[TeachingTutorials]  New block: " +blockDataNew.getMaterial());
            Bukkit.getConsoleSender().sendMessage("[TeachingTutorials]  Location: " +location.toString());

            //Creates a virtual block
            VirtualBlock virtualBlock = new VirtualBlock(worldEditCalculation.getTutorialPlaythrough(), worldEditCalculation.getPlayer(), location, blockDataNew);
            //Adds it to the new list
            virtualBlocks.add(virtualBlock);
        }
    }
}

