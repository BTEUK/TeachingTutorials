package teachingtutorials.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import teachingtutorials.TutorialPlaythrough;

//Represents a virtual block
public class VirtualBlock
{
    //Stores where the virtual block was created from. Used when needing to delete all virtual blocks at completion of the tutorial
    //Technically the TutorialPlaythrough could iterate through all stages, steps, groups and tasks to disable the virtual blocks but
    //this would be quite resource intensive (involves iterating through the virtual blocks list for every task and ultimately
    //the exact same code could be run just using the tutorial playthrough check but just with one iteration through the list
    private TutorialPlaythrough tutorialPlaythrough;

    public Player player;
    private int iBlockX, iBlockY, iBlockZ;
    public Material material;

    public Location location;

    public VirtualBlock(TutorialPlaythrough tutorialPlaythrough, Player player, World world, int iBlockX, int iBlockY, int iBlockZ, Material material)
    {
        this.tutorialPlaythrough = tutorialPlaythrough;
        this.player = player;

        this.iBlockX = iBlockX;
        this.iBlockY = iBlockY;
        this.iBlockZ = iBlockZ;
        this.material = material;

        this.location = new Location(world, iBlockX, iBlockY, iBlockZ);
    }

    public void sendUpdate()
    {
        //Sends the virtual block change to the player
        player.sendBlockChange(location, material.createBlockData());
    }

    public void removeAndReset()
    {
        //Returns the block to match the actual world
        player.sendBlockChange(location, location.getBlock().getBlockData());
    }

    public boolean isFromTutorial(TutorialPlaythrough tutorialPlaythrough)
    {
        return tutorialPlaythrough.equals(this.tutorialPlaythrough);
    }
}
