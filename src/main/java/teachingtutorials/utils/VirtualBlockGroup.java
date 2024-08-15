package teachingtutorials.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import teachingtutorials.TutorialPlaythrough;

import java.util.ArrayList;
import java.util.HashMap;
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
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Setting a virtual block to the actual world");
            worldToSet.setBlockData(locations[i], virtualBlockData[i]);
        }
    }

    /**
     * Creates a map of location to real blocks at the locations of the virtual blocks of this list
     * @return A list of the real world blocks at locations of the virtual blocks of this list
     */
    public Map<Location, BlockData> getRealBlocks()
    {
        //Store a reference to the world locally
        World worldToRead = tutorialPlaythrough.getLocation().getWorld();

        //Gets the list of locations for this list
        Location[] virtualBlockLocations = this.keySet().stream().toArray(Location[]::new);
        int iLocations = virtualBlockLocations.length;

        //Creates a map holding the world's real data at the locations of the virtual blocks
        Map<Location, BlockData> realWorldBlocks = new HashMap<>(iLocations);
        for (int i = 0 ; i < iLocations ; i++)
        {
            BlockData realBlock = worldToRead.getBlockData(virtualBlockLocations[i]).clone(); //This actually gets run asynchronously and takes over a second sometimes

//            Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_BLUE +"Block recorded at ("
//                    +virtualBlockLocations[i].getX()+","
//                    +virtualBlockLocations[i].getY()+","
//                    +virtualBlockLocations[i].getZ()
//                    +") with material: "+realBlock.getMaterial());

            realWorldBlocks.put(virtualBlockLocations[i], realBlock);

        }

        return realWorldBlocks;
    }


}
