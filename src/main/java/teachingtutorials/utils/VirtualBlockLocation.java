package teachingtutorials.utils;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import teachingtutorials.TutorialPlaythrough;

/**
 * Locates a virtual block via its location in the world and the tutorial playthrough it belongs to
 */
public class VirtualBlockLocation
{
    private TutorialPlaythrough tutorialPlaythrough;
    protected Player player;
    protected Location location;

    public VirtualBlockLocation(TutorialPlaythrough tutorialPlaythrough, Player player, Location location)
    {
        this.tutorialPlaythrough = tutorialPlaythrough;
        this.player = player;

        this.location = location;
    }

    /**
     * Sends the virtual block change to the player
     * @param blockData The block data to send to the player for the location
     */
    public void sendUpdate(BlockData blockData)
    {
        this.player.sendBlockChange(this.location, blockData);
    }

    /**
     * Returns the player's view of the block to match the actual world
     */
    public void removeAndReset()
    {
        this.player.sendBlockChange(this.location, this.location.getBlock().getBlockData());
    }

    /**
     * Returns whether the virtual block is one created by the given tutorial
     */
    public boolean isFromTutorial(TutorialPlaythrough tutorialPlaythrough)
    {
        return this.tutorialPlaythrough.equals(tutorialPlaythrough);
    }
}
