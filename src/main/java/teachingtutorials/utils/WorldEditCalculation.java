package teachingtutorials.utils;

import com.fastasyncworldedit.core.extent.processor.ProcessorScope;
import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.util.ExtentTraverser;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.TutorialPlaythrough;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Stores the details of a world edit block change calculation which is to be made
 */
public class WorldEditCalculation
{
    private Object worldEditEventListener;
    private final String szWorldEditCommand;
    private final RegionSelector regionSelector;
    private final TutorialPlaythrough tutorialPlaythrough;
    private final int iTaskID;

    //Records the default blocks of the world  where the calculation
    public HashMap<Location, BlockData> realBlocks = new HashMap<>();

    //Indicates whether the blocks in the world need resetting
    private AtomicBoolean bBlocksRequireReset = new AtomicBoolean(false);

    /**
     * Attempts to reset the actual blocks of the world back to how they were before the calculation took place.
     * Should only happen once per calculation
     */
    public void tryResettingWorld()
    {
        //Checks whether the blocks have already been reset and if not then mark it as having been reset so that something else doesn't access it
        if (bBlocksRequireReset.getAndSet(false))
        {
            //Extracts the real blocks
            int iSize;
            iSize = realBlocks.size();
            Location[] locations = realBlocks.keySet().toArray(Location[]::new);
            BlockData[] blockData = realBlocks.values().toArray(BlockData[]::new);

            Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN +"[TeachingTutorials] About to run the resetting of the blocks on the world for task: " +iTaskID);

            //Run the resetting
            Bukkit.getScheduler().runTask(TeachingTutorials.getInstance(), new Runnable() {
                @Override
                public void run()
                {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN +"[TeachingTutorials] Running the resetting of the blocks on the world for task: " +iTaskID);

                    //Set the blocks of the world back to the correct blocks
                    World world = getWorld();
                    for (int i = 0 ; i < iSize ; i++)
                    {
                        world.setBlockData(locations[i], blockData[i]);
//                        Bukkit.getConsoleSender().sendMessage(ChatColor.BLUE +"[TeachingTutorials] Setting block at "+locations[i].toString()  +" to the correct block: "+blockData[i].getMaterial().toString());
                    }

                    //Wait a few ticks before saying that the blocks have been set to give it a chance to set the blocks before the next calculation
                    Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_PURPLE +"[TeachingTutorials] In 5 ticks will mark the blocks as having been reset for task: " +iTaskID);

                    //Mark blocks as rest after a time
                    Bukkit.getScheduler().runTaskLater(TeachingTutorials.getInstance(), new Runnable() {
                        @Override
                        public void run() {
                            Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_PURPLE +"[TeachingTutorials] Marking the blocks as having been reset for task: " +iTaskID);

                            //Marks that virtual blocks have been taken off the real world
                            teachingtutorials.utils.WorldEdit.setVirtualBlocksOffRealWorld();

                            //Unblock the calculation queue
                            unblockCalculationQueue();

                            Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_PURPLE +"[TeachingTutorials] Marked the blocks as having been reset for task: " +iTaskID);
                        }
                    }, 5L);
                }
            });
        }
    }

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

    public WorldEditCalculation(String szWorldEditCommand, RegionSelector regionSelector, TutorialPlaythrough tutorialPlaythrough, int iTaskID, ConcurrentHashMap<VirtualBlockLocation, BlockData> virtualBlocks)
    {
        this.szWorldEditCommand = szWorldEditCommand;
        this.regionSelector = regionSelector;
        this.tutorialPlaythrough = tutorialPlaythrough;
        this.iTaskID = iTaskID;

        //2. Create the new event listener
        setUpListener(iTaskID, virtualBlocks);
    }

    public Object getEditSessionListener()
    {
        return this.worldEditEventListener;
    }

    /**
     * Creates a listener to pick up the EditSessionEvent event for the command being run. This listener will replace
     *  the 'extent' of the EditSession to one which records the changes and blocks them from being put onto the world.
     * @param iTaskID The TaskID of the task
     * @param virtualBlocks A reference to the list of virtual blocks to record block changes into
     */
    private void setUpListener(int iTaskID, ConcurrentHashMap<VirtualBlockLocation, BlockData> virtualBlocks)
    {
        //Get the console actor
        Actor consoleActor = BukkitAdapter.adapt(Bukkit.getConsoleSender());

        //Get a reference to this object
        WorldEditCalculation thisCalculation = this;

        //Creates a listener to listen out for world edit events and insert the recorder extent into the correct one
        worldEditEventListener = new Object()
        {
            //Runs after the command has been sent
            @Subscribe
            public void onEditSessionEvent(EditSessionEvent event)
            {
                final Actor actor = event.getActor();

                if (actor == null)
                {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Edit session event detected belonging a null actor (assuming console) - at stage: "+event.getStage().toString() +" for task:" +iTaskID);
                }
                else if (actor.getName().equals(consoleActor.getName()))
                {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Edit session event detected belonging to the actor we are listening for - at stage: "+event.getStage().toString() +" for task:" +iTaskID);
                }
                else
                {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Edit session event detected but doesn't belong to the correct actor, so ignoring");
                    return;
                }

                //Creates the new recorder extent
                AbstractDelegateExtent blockChangeRecorderExtent;

//                if (TeachingTutorials.getInstance().worldEditImplementation.equals(WorldEditImplementation.FAWE))
                blockChangeRecorderExtent = new BlockChangeRecorderExtentFAWE(event.getExtent(), thisCalculation, virtualBlocks, iTaskID);
//                else
//                    blockChangeRecorderExtent = new BlockChangeRecorderExtentWE(event.getExtent(), thisCalculation, virtualBlocks, iTaskID);

                //Updates the extent of the edit session to be that of a block recording extent
                //The block recording extent means that block changes are blocked and recorded in the set block mechanism
                //Sets the new extent into the event
                Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Setting the extent for task:" +iTaskID);
                event.setExtent(blockChangeRecorderExtent);

                //Once the extent has been set we don't need the listener anymore since any world edit changes under that extent will be recorded in the correct list
                unregisterWorldChangeListener();
            }
        };
    }

    /**
     * Runs the block calculation. Ultimately this will send the world edit command through the console,
     *  and catch it with the event. This event has already been defined and contains the list of virtual blocks which
     *  it will add detected block changes to.
     * @return True if the calculation was implemented successfully, False if there was an error.
     */
    public void runCalculation()
    {
        //Console output
        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW +"[TeachingTutorials] Starting a new WE block change calculation on task: "+iTaskID);

        //Runs the world edit command through the console and registers the listener to identify the resulting operation
        Bukkit.getScheduler().runTask(TeachingTutorials.getInstance(), () ->
        {
            //Sets the selection
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Adjusting the selection on task: "+iTaskID);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "/world "+tutorialPlaythrough.getLocation().getLocationID());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "/pos1 " + ((CuboidRegion) regionSelector.getRegion()).getPos1().toParserString());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "/pos2 " + ((CuboidRegion) regionSelector.getRegion()).getPos2().toParserString());

/*
            I think we need to be really careful with this and do another check.  We cannot let this run if the blocks of the last one have not been reset
                i.e We cannot let this run until the calculation queue of the last one is not unblocked.

            Although the resetting of the blocks in calculation A should be done before B is even allowed to start, that may not actually happen if the setting of the blocks in calculation A is done in a bukkit.RunTask()
            Possibly test for if we are ready
            Possibly add some recursion
*/
            //Check for whether a task is ongoing - uses atomic boolean to be thread safe
            if (teachingtutorials.utils.WorldEdit.isCurrentCalculationOngoing())
            {
                //Wait a tick
                Bukkit.getScheduler().runTaskLater(TeachingTutorials.getInstance(), new Runnable() {
                    @Override
                    public void run() {
                        recordRealBlocksAndSetVirtualsAndRunCommand();
                    }
                }, 1L);
            }
            else
                recordRealBlocksAndSetVirtualsAndRunCommand();
        });

        //Console output
        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW +"[TeachingTutorials] All pre-tasks and command have been added to the bukkit scheduler for task: "+iTaskID);
    }

    private void recordRealBlocksAndSetVirtualsAndRunCommand()
    {
        //Check for whether blocks are on the world - uses atomic boolean to be thread safe
        if (teachingtutorials.utils.WorldEdit.areVirtualBlocksOnRealWorld())
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"[TeachingTutorials] Delaying the real world block recording and command run on task: "+iTaskID);
            //Wait a tick and try again
            Bukkit.getScheduler().runTaskLater(TeachingTutorials.getInstance(), new Runnable() {
                @Override
                public void run() {
                    recordRealBlocksAndSetVirtualsAndRunCommand();
                }
            }, 1L);
        }
        //If the virtual blocks are off the world then we can record the default world blocks now
        else
        {
            //Extracts the current list of virtual blocks
            ConcurrentHashMap<VirtualBlockLocation, BlockData> virtualBlocks = TeachingTutorials.getInstance().virtualBlocks;

            final int iSize = virtualBlocks.size();
            VirtualBlockLocation[] virtualBlockLocations = virtualBlocks.keySet().toArray(VirtualBlockLocation[]::new);
            final BlockData[] virtualBlockData = virtualBlocks.values().toArray(BlockData[]::new);

            final World world = getWorld();

            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Recording the world blocks for: "+iTaskID);

            //Marks that virtual blocks have been placed on the real world
            teachingtutorials.utils.WorldEdit.setVirtualBlocksOnRealWorld();

            //Records the real blocks of the world at the virtual blocks location
            for (int i = 0 ; i < iSize ; i++)
            {
                final int iPosition = i;
                if (virtualBlockLocations[iPosition].isFromTutorial(tutorialPlaythrough))
                {
                    //Store the block details in local objects
                    Location location = virtualBlockLocations[iPosition].location;
                    BlockData realBlock = world.getBlockData(location).clone(); //This actually gets run asynchronously and takes over a second sometimes

                    //Adds the real block at this location to the list
                    realBlocks.put(location, realBlock);

//                    Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_BLUE +"Block recorded at ("
//                            +virtualBlockLocations[iPosition].location.getX()+","
//                            +virtualBlockLocations[iPosition].location.getY()+","
//                            +virtualBlockLocations[iPosition].location.getZ()
//                            +") with material: "+realBlock.getMaterial());
                }
            }

            //We set virtual blocks to the world so that it takes them into account in the WE calculation
            //Wait 15 ticks before setting the virtual blocks to the world because the recording of the blocks (see for loop above)
            // often overruns by several ticks and ends up recording the blocks after they are set below
            Bukkit.getScheduler().runTaskLater(TeachingTutorials.getInstance(), new Runnable() {
                @Override
                public void run() {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Setting the virtual blocks to the world on task: "+iTaskID);
                    //Sets the real block to that of the virtual block at this location
                    for (int i = 0 ; i < iSize ; i++)
                    {
                        final int iPosition = i;
                        if (virtualBlockLocations[iPosition].isFromTutorial(tutorialPlaythrough))
                        {
                            Location location = virtualBlockLocations[iPosition].location;

                            //Sets the real block to that of the virtual block at this location
                            world.setBlockData(location, virtualBlockData[iPosition]);
                        }
                    }

                    Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Finished recording the world blocks and setting virtual blocks on task: "+iTaskID);

                    //Once the chunks have started being processed it will put all of the blocks back
                    bBlocksRequireReset.set(true);

                    //Registers the world change event listener
                    try
                    {
                        WorldEdit.getInstance().getEventBus().register(worldEditEventListener);
                        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Registered the EditSessionEvent listener on task: "+iTaskID);
                    }
                    catch (Exception e)
                    {
                        Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"[Teaching Tutorials] Error registering WorldEdit event listener: "+e.getMessage());
                        Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"[Teaching Tutorials] :" +e.getCause());
                        Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"[Teaching Tutorials] :" +e);
                        e.printStackTrace();
                    }

                    //Runs the command
                    Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Sending command for task "+iTaskID +": "+szWorldEditCommand);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), szWorldEditCommand);
                    Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Command sent for task: "+iTaskID);
                }
            }, TeachingTutorials.getInstance().getConfig().getLong("BlockRecordDelay"));
        }
    }

    /**
     * Unregisters the listener
     */
    private void unregisterWorldChangeListener()
    {
        //Unregisters the WorldEdit event listener
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Unregistering world edit listener for task:" +iTaskID);
        com.sk89q.worldedit.WorldEdit.getInstance().getEventBus().unregister(this.getEditSessionListener());
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Unregistered world edit listener for task:" +iTaskID);
    }

    /**
     * Unblocks the calculation queue
     */
    private void unblockCalculationQueue()
    {
        //Updates the queue system, unblocking the queue
        teachingtutorials.utils.WorldEdit.pendingCalculations.remove(this);
        teachingtutorials.utils.WorldEdit.setCalculationFinished();
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Unblocked calculation queue from task:" +iTaskID);
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN +"[TeachingTutorials] There are " +teachingtutorials.utils.WorldEdit.pendingCalculations.size() +" calculations remaining in the queue");
    }
}

/**
 * A new type of extent which records block changes and blocks them from being placed into the world
 */
class BlockChangeRecorderExtentFAWE extends AbstractDelegateExtent implements IBatchProcessor
{
    WorldEditCalculation worldEditCalculation;
    ConcurrentHashMap<VirtualBlockLocation, BlockData> virtualBlocks;

    private final int iTaskID;

    public BlockChangeRecorderExtentFAWE(Extent originalExtent, WorldEditCalculation worldEditCalculation, ConcurrentHashMap<VirtualBlockLocation, BlockData> virtualBlocks, int iTaskID)
    {
        super(originalExtent);
        this.worldEditCalculation = worldEditCalculation;
        this.virtualBlocks = virtualBlocks;
        this.iTaskID = iTaskID;

        super.addProcessor(this);
        super.addPostProcessor(this);
    }

    /**
     * Is called when processing the changes of a given chunk in the operation.
     * This is where blocks are recorded and blocked from being placed on the world.
     * @param chunk An object representing the chunk
     * @param get An object representing the state of the chunk before the operation
     * @param set An object representing the state of the chunk after the operation
     * @return An object representing the required state of the chunk after the operation
     */
    @Override
    public IChunkSet processSet(IChunk chunk, IChunkGet get, IChunkSet set)
    {
        //Now we know the calculation has taken place, we can remove the blocks we placed for the preexisting virtual blocks
        //This will get called for each chunk but after it is called the first time any future calls won't have an effect
        //Because there is a boolean marking whether it has already been called or not
        worldEditCalculation.tryResettingWorld();

        //For some reason this gets called way too much

        //Set:
        // Presumably an implementation of CharSetBlocks
        // Layers are height layers of a chunk
        // A layer is a 16x16x16 piece of a chunk which stack up vertically to make up the chunk, There are 15 above 0 and 8 below 0.
        // The layer index is layer = y >> 4

        //The index of a chunk is calculated by = (y & 15) << 8 | z << 4 | x;
        //This is just an index to a 16*16*16 array with Index = Y*256 + Z*16 + X

        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"\n[TeachingTutorials] Processing chunk: " +chunk.getChunkBlockCoord().toString() +" for task:" +iTaskID);
//        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Y range for chunk: " +chunk.getMinY() +" to " +chunk.getMaxY());

        //Declare the variables for the block coordinate markers
        int Y, dX, dZ;

        //Declare the variables for the new and old block states
        BlockState newBlock;
        BlockState oldBlock;

        //Initialise the chunk vector - a 2d vector to the 0,0 point of the chunk
        BlockVector2 chunkVector;
        chunkVector = BlockVector2.at(chunk.getX()*16, chunk.getZ()*16);

        //Stores the ID of the 'reserved' block state locally
        final int iReservedState = BlockTypesCache.ReservedIDs.__RESERVED__;

        //Check whether the chunk is empty
        if (set.isEmpty())
        {
            //Skip this chunk
//            Bukkit.getConsoleSender().sendMessage("Chunk has no edits");
            return set;
        }

        //Goes through every 'layer' in the new chunk
        final int iMaxSectionPosition = set.getMaxSectionPosition();
        for (int iLayer = set.getMinSectionPosition() ; iLayer <= iMaxSectionPosition ; iLayer++)
        {
            //Get the layer
            char[] blocks = set.loadIfPresent(iLayer);
            if (blocks == null)
            {
                //Layer had no blocks sections
//                Bukkit.getConsoleSender().sendMessage("Layer "+iLayer +" of chunk " +chunk.getChunkBlockCoord().toString() +" had no block sections");
                //Do nothing
            }
            else
            {
                //Layer had block sections
//                Bukkit.getConsoleSender().sendMessage("Layer "+iLayer +" of chunk " +chunk.getChunkBlockCoord().toString() +" had block sections, identifying blocks in this layer now");

                //Goes through every block in the layer
                int iNumBlocksInLayer = blocks.length;
                for (int index = 0 ; index < iNumBlocksInLayer ; index++)
                {
                    //Extracts the ID of the block
                    char block = blocks[index];

                    //Check whether there is a block change
                    if (block == iReservedState)
                    {
                        //No block change recorded
                    }
                    else
                    {
                        //Record this block change

                        //Extract the block coordinates
                        //Index works as if you are indexing a 3D matrix but on a 1D scale

                        //In the index, Y is taken and timesed by 256, add Z by 16 and X is added on at the end

                        //To get X we need to take index % 16
                        //To get Z we need to take index/16  % 16
                        //To get Y we need to take index/256  % 16

                        //The following code is equivalent to using mod and div by uses bitwise operations to extract the appropriate bits instead
                        dX = index & 15;
                        dZ = (index >> 4) & 15;
                        Y = (index >> 8) & 15; //the Y within the layer

                        Y = Y + iLayer * 16; //This should shift the Y back

                        //Extract the block type
                        newBlock = BlockTypesCache.states[block];

                        //Creates a vector to the block
                        BlockVector3 block3Vector = BlockVector3.at(chunkVector.getX() + dX, Y, chunkVector.getBlockZ() +dZ);

                        //Gets the old block
                        oldBlock = get.getBlock(dX, Y, dZ);

                        //Records the block change
                        adaptAndRecordBlockChange(block3Vector, oldBlock, newBlock);

                        //Cancels the world change
                        blocks[index] = iReservedState;
                    }
                }
            }
        }

        return set;
    }

    @Override
    public void postProcess(final IChunk chunk, final IChunkGet get, final IChunkSet set)
    {
        //Print out the number of new virtual blocks
        int iSize = virtualBlocks.size();
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"\n[TeachingTutorials] Post processing of chunk: " +chunk.getChunkBlockCoord().toString()+". There have been " +iSize + " block changes recorded so far for task:" +iTaskID);
    }

    @Override
    public Extent construct(final Extent child) {
        if (getExtent() != child) {
            new ExtentTraverser<Extent>(this).setNext(child);
        }
        return this;
    }

    @Override
    public ProcessorScope getScope() {
        return ProcessorScope.READING_SET_BLOCKS;
    }

    /**
     * Takes a vector, and WorldEdit BlockState objects for the old and new blocks and records the change of block
     * @param vector A vector pointing to the location of the block change
     * @param blockDataOld WorldEdit BlockState object for the old block
     * @param blockDataNew WorldEdit BlockState object for the new block
     */
    private void adaptAndRecordBlockChange(BlockVector3 vector, BlockState blockDataOld, BlockState blockDataNew)
    {
        recordBlockChange(BukkitAdapter.adapt(worldEditCalculation.getWorld(), vector), BukkitAdapter.adapt(blockDataOld), BukkitAdapter.adapt(blockDataNew));
    }

    /**
     * Creates a new virtual block and adds it to the list for this task
     * @param location The location of the virtual block
     * @param blockDataOld The old block's block data
     * @param blockDataNew The new block's block data
     */
    private void recordBlockChange(Location location, BlockData blockDataOld, BlockData blockDataNew)
    {
        //Notify the console of a block change
//        Bukkit.getConsoleSender().sendMessage("[TeachingTutorials] Block change detected | Old block: " +blockDataOld.getMaterial() +", New block: " +blockDataNew.getMaterial());

        //Creates a virtual block
        VirtualBlock virtualBlock = new VirtualBlock(worldEditCalculation.getTutorialPlaythrough(), worldEditCalculation.getPlayer(), location, blockDataNew);

        //Adds it to the new list
        virtualBlocks.put(virtualBlock.blockLocation, virtualBlock.blockData);
    }
}

///**
// * A new type of extent which records block changes and blocks them from being placed into the world
// */
//class BlockChangeRecorderExtentWE extends AbstractDelegateExtent
//{
//    WorldEditCalculation worldEditCalculation;
//    HashSet<VirtualBlock> virtualBlocks;
//    int iTaskID;
//
//    public BlockChangeRecorderExtentWE(Extent originalExtent, WorldEditCalculation worldEditCalculation, HashSet<VirtualBlock> virtualBlocks, int iTaskID)
//    {
//        super(originalExtent);
//        this.worldEditCalculation = worldEditCalculation;
//        this.virtualBlocks = virtualBlocks;
//        this.iTaskID = iTaskID;
//    }
//
//    /**
//     * Creates a new virtual block and adds it to the list for this task
//     * @param location The location of the virtual block
//     * @param blockDataOld The old block's block data
//     * @param blockDataNew The new block's block data
//     */
//    private void recordBlockChange(Location location, BlockData blockDataOld, BlockData blockDataNew)
//    {
//        //Notify the console of a block change
//        Bukkit.getConsoleSender().sendMessage("[TeachingTutorials] Block change detected | Old block: " +blockDataOld.getMaterial() +", New block: " +blockDataNew.getMaterial());
//
//        //Creates a virtual block
//        VirtualBlock virtualBlock = new VirtualBlock(worldEditCalculation.getTutorialPlaythrough(), worldEditCalculation.getPlayer(), location, blockDataNew);
//
//        //Adds it to the new list
//        virtualBlocks.add(virtualBlock);
//    }
//
//    @Override
//    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 position, B block)
//    {
//        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"\n[TeachingTutorials] A world edit block change has been detected, belonging to the listener for task " +iTaskID +". Recording to the given virtual blocks list");
//        if (!block.getBlockType().equals(BlockTypes.__RESERVED__))
//            calculateBlockChange(position.getX(), position.getY(), position.getZ(), block, worldEditCalculation);
//        //return super.setBlock(position, block);
//        return false;
//    }
//
//    @Override
//    public <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B block)
//    {
//        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"\n[TeachingTutorials] A world edit block change has been detected, belonging to the listener for task " +iTaskID +". Recording to the given virtual blocks list");
//        if (!block.getBlockType().equals(BlockTypes.__RESERVED__))
//            calculateBlockChange(x, y, z, block, worldEditCalculation);
//        //return super.setBlock(position, block);
//        return false;
//    }
//
//    @Deprecated
//    private <B extends BlockStateHolder<B>> void calculateBlockChange(int x, int y, int z, B blockDataNew, WorldEditCalculation worldEditCalculation)
//    {
//        //Creates a location object
//        Location location = new Location(worldEditCalculation.getWorld(), x, y, z);
//
//        //Gets the original block
//        BlockData blockDataBefore = location.getBlock().getBlockData();
//
//        //Records the change
//        recordBlockChange(location, blockDataBefore, BukkitAdapter.adapt(blockDataNew));
//    }
//}
//
