package teachingtutorials.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.TutorialPlaythrough;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An extension of a concurrent hashmap for lists of virtual block
 * @param <K> Key
 * @param <V> Value
 */
public class VirtualBlockGroup<K, V> extends ConcurrentHashMap<K,V>
{
    private final TutorialPlaythrough tutorialPlaythrough;

    //A list of real world blocks. This list runs parallel to the virtual blocks group list
    //WARNING: This may not hold the true value of the block. It is important to reset all virtual blocks groups in the reverse order to which they were created
    private ConcurrentHashMap<Location, BlockData> realWorldBlocks = new ConcurrentHashMap<>();

    //A list of spies also viewing the virtual blocks
    private ArrayList<Player> spies = new ArrayList<>();

    public VirtualBlockGroup(TutorialPlaythrough tutorialPlaythrough)
    {
        this.tutorialPlaythrough = tutorialPlaythrough;
    }

    /**
     * Returns whether this VirtualBlockGroup belongs to the given tutorial playthrough
     * @param tutorialPlaythrough
     * @return True if the given tutorial playthrough and the tutorialPlaythrough of this VirtualBlockGroup are equal. False if not.
     */
    public boolean isOfPlaythrough(TutorialPlaythrough tutorialPlaythrough)
    {
        return tutorialPlaythrough.equals(this.tutorialPlaythrough);
    }

    @Override
    public V put(K key, V value)
    {
        //Find the real value for this block and record this
        if (key instanceof Location)
        {
            //Store a reference to the world locally
            World worldToRead = tutorialPlaythrough.getLocation().getWorld();

            BlockData realBlock = worldToRead.getBlockData((Location) key).clone(); //This actually gets run asynchronously and takes over 2 seconds sometimes

            Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_BLUE +"Adding block to list and block's original material/: "+realBlock.getMaterial());

            realWorldBlocks.put((Location) key, realBlock);
        }
        return super.put(key, value);
    }

    /**
     * Displays the virtual blocks to the player and all spies
     */
    public void displayBlocks()
    {
        //Sends the changes to the player
        tutorialPlaythrough.getCreatorOrStudent().player.sendMultiBlockChange((Map<Location, BlockData>) this);

        //Sends the changes to the spies
        int iNumSpies = spies.size();
        int i;
        for (i = 0 ; i < iNumSpies ; i++)
        {
            spies.get(i).sendMultiBlockChange((Map<Location, BlockData>) this);
        }
    }

    /**
     * Removes the virtual blocks from the player and all spies, sets their views back to the real world.
     */
    public void removeBlocks()
    {
        //Creates a map holding the world's real data at the locations of the virtual blocks
        Map<Location, BlockData> realWorldBlocks = getRealBlocks();

        //Sends the changes to the player
        tutorialPlaythrough.getCreatorOrStudent().player.sendMultiBlockChange(realWorldBlocks);

        //Sends the changes to the spies
        int iNumSpies = spies.size();
        int i;
        for (i = 0 ; i < iNumSpies ; i++)
        {
            spies.get(i).sendMultiBlockChange(realWorldBlocks);
        }
    }

    /**
     * Adds all of the virtual blocks in this list to the actual world.
     * <p> </p>
     * USE WITH EXTREME CAUTION!
     * <p> </p>
     */
    public void addBlocksToWorld()
    {
        //Store a reference to the world locally
        World worldToSet = tutorialPlaythrough.getLocation().getWorld();

        //Gets the list of locations and block data for this list
        final Location[] locations = this.keySet().stream().toArray(Location[]::new);
        final BlockData[] virtualBlockData = this.values().toArray(BlockData[]::new);
        int iLocations = locations.length;

        //Get the blocks in the world
        for (int i = 0 ; i < iLocations ; i++)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Setting a " +virtualBlockData[i].getMaterial() +" block to the actual world");
            worldToSet.setBlockData(locations[i], virtualBlockData[i]);
        }
    }

    /**
     * Resets the world back to its original state.
     * <P> </P>
     * When using this method with lots of virtual block groups it is important to call the resets from later virtual block groups first. As in FILO.
     */
    public void resetWorld()
    {
        //Store a reference to the world locally
        World worldToSet = tutorialPlaythrough.getLocation().getWorld();

        //Gets the list of locations and block data for this list
        final Location[] locations = realWorldBlocks.keySet().stream().toArray(Location[]::new);
        final BlockData[] virtualBlockData = realWorldBlocks.values().toArray(BlockData[]::new);
        int iLocations = locations.length;

//        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Resetting the blocks to the world");

        Bukkit.getScheduler().runTask(TeachingTutorials.getInstance(), () -> {
            //Set the blocks in the world
            for (int i = 0 ; i < iLocations ; i++)
            {
                worldToSet.setBlockData(locations[i], virtualBlockData[i]);
            }
        });
    }

    /**
     * Returns the map of location to real blocks at the locations of the virtual blocks of this list
     * @return A list of the real world blocks at locations of the virtual blocks of this list
     */
    public Map<Location, BlockData> getRealBlocks()
    {
        return realWorldBlocks;
    }


}
