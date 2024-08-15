package teachingtutorials.utils;

import com.google.common.base.Joiner;
import com.sk89q.worldedit.regions.RegionSelector;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import teachingtutorials.TutorialPlaythrough;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class WorldEdit
{
    /**
     * A queue of pending calculations to be performed
     */
    public static ConcurrentLinkedQueue<WorldEditCalculation> pendingCalculations = new ConcurrentLinkedQueue<>();
    private static AtomicBoolean bCurrentCalculationOngoing = new AtomicBoolean(false);

    //Records whether there are virtual blocks on the real world
    private static AtomicBoolean bVirtualBlocksOnRealWorld = new AtomicBoolean(false);

    /**
     * Current calculation refers to whether a calculation is already in the process from listening to the
     * EditSessionEvent to waiting for the first sign that the calculation has been complete and still having blocks on
     * the world which need to be removed after using them for calculation.
     * @return
     */

    public static boolean isCurrentCalculationOngoing()
    {
        return bCurrentCalculationOngoing.get();
    }

    public static void setCalculationInProgress()
    {
        bCurrentCalculationOngoing.set(true);
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"[TeachingTutorials] The calculations queue has been blocked - calculation in progress");
    }

    public static void setCalculationFinished()
    {
        bCurrentCalculationOngoing.set(false);
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN +"[TeachingTutorials] The calculations queue has been unblocked!");
    }

    public static boolean areVirtualBlocksOnRealWorld()
    {
        return bVirtualBlocksOnRealWorld.get();
    }

    public static void setVirtualBlocksOnRealWorld()
    {
        bVirtualBlocksOnRealWorld.set(true);
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN +"[TeachingTutorials] Blocks have been marked as on the world");
    }

    public static void setVirtualBlocksOffRealWorld()
    {
        bVirtualBlocksOnRealWorld.set(false);
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN +"[TeachingTutorials] Blocks have been marked as taken off the world");
    }

    /**
     * Creates a world edit event listener which catches the block changes arising from world edit commands owned by the given player
     * and creates virtual block objects from these. The specific command parsed is then run, triggering the calculation of all blocks
     * changes from this command.
     * @param virtualBlocks The list of virtual blocks for the calling task
     * @param szCommandLabel The command label (first word) of the command
     * @param szCommandArgs The command args
     * @param tutorialPlaythrough The tutorial playthrough which this task belongs to
     * @return
     */
    public static void BlocksCalculator(int iTaskID, final VirtualBlockGroup<Location, BlockData> virtualBlocks, RegionSelector correctSelectionRegion, String szCommandLabel, String[] szCommandArgs, TutorialPlaythrough tutorialPlaythrough)
    {
        //1. Modifies the command
        //This code is taken from WorldEdit - See https://enginehub.org/
        int plSep = szCommandLabel.indexOf(':');
        if (plSep >= 0 && plSep < szCommandLabel.length() +1)
        {
            szCommandLabel = szCommandLabel.substring(plSep + 1);
        }
        StringBuilder sb = new StringBuilder(szCommandLabel);
        String szWorldEditCommand = Joiner.on(" ").appendTo(sb, szCommandArgs).toString();

        //The command is now fully formatted correctly
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Upcoming command being run via the console: "+szWorldEditCommand);


        //Create a new calculation manager for this calculation and add to the list
        WorldEditCalculation newCalculation = new WorldEditCalculation(szWorldEditCommand, correctSelectionRegion, tutorialPlaythrough, iTaskID, virtualBlocks);
        WorldEdit.pendingCalculations.add(newCalculation);
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] This calculation has been added to the queue: "+szWorldEditCommand);
    }
}
