package teachingtutorials.utils;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import teachingtutorials.TutorialPlaythrough;

//Represents a virtual block
public class VirtualBlock
{
    //Stores where the virtual block was created from. Used when needing to delete all virtual blocks at completion of the tutorial
    //Technically the TutorialPlaythrough could iterate through all stages, steps, groups and tasks to disable the virtual blocks but
    //this would be quite resource intensive (involves iterating through the virtual blocks list for every task and ultimately
    //the exact same code could be run just using the tutorial playthrough check but just with one iteration through the list
    public VirtualBlockLocation blockLocation;
    public BlockData blockData;


    /**
     * Constructs the virtual block. Use this if the bukkit location object for this block has not yet been created
     * @param tutorialPlaythrough
     * @param player
     * @param world
     * @param iBlockX
     * @param iBlockY
     * @param iBlockZ
     * @param blockData
     */
    public VirtualBlock(TutorialPlaythrough tutorialPlaythrough, Player player, World world, int iBlockX, int iBlockY, int iBlockZ, BlockData blockData)
    {
        //Calculates the location
        Location location = new Location(world, iBlockX, iBlockY, iBlockZ);

        this.blockLocation = new VirtualBlockLocation(tutorialPlaythrough, player, location);

        this.blockData = blockData;
    }

    /**
     * Constructs the virtual block. Use this if the bukkit location object for this block has already been created and initialised
     * @param tutorialPlaythrough The tutorial playthrough instance that this virtual block belongs to
     * @param player The player to display the virtual block for
     * @param location The location of the virtual block
     * @param blockData The block data for the virtual block
     */
    public VirtualBlock(TutorialPlaythrough tutorialPlaythrough, Player player, Location location, BlockData blockData)
    {
        this.blockLocation = new VirtualBlockLocation(tutorialPlaythrough, player, location);
        this.blockData = blockData;
    }

    /**
     * Sends the virtual block change to the player
     */
    public void sendUpdate()
    {
        this.blockLocation.player.sendBlockChange(this.blockLocation.location, blockData);
    }

    /**
     * Returns the player's view of the block to match the actual world
     */
    public void removeAndReset()
    {
        this.blockLocation.player.sendBlockChange(this.blockLocation.location, this.blockLocation.location.getBlock().getBlockData());
    }

    /**
     * Returns whether the virtual block is one created by the given tutorial
     */
    public boolean isFromTutorial(TutorialPlaythrough tutorialPlaythrough)
    {
        return this.blockLocation.isFromTutorial(tutorialPlaythrough);
    }

    //A util class for converting a list of them to one thing?
}
