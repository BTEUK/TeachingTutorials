package teachingtutorials.utils;

import com.google.common.base.Joiner;
import com.sk89q.worldedit.IncompleteRegionException;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

public class WorldEdit
{
    /**
     * A queue of pending calculations to be performed
     */
    public static LinkedList<WorldEditCalculation> pendingCalculations = new LinkedList<>();
    private static boolean bCurrentCalculationOngoing = false;

    public static boolean isCurrentCalculationOngoing()
    {
        return bCurrentCalculationOngoing;
    }

    public static void setCalculationInProgress()
    {
        bCurrentCalculationOngoing = true;
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"[TeachingTutorials] The calculations queue has been blocked - calculation in progress");
    }

    public static void setCalculationFinished()
    {
        bCurrentCalculationOngoing = false;
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN +"[TeachingTutorials] The calculations queue has been unblocked!");
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
    public static void BlocksCalculator(int iTaskID, final HashSet<VirtualBlock> virtualBlocks, RegionSelector correctSelectionRegion, String szCommandLabel, String[] szCommandArgs, TutorialPlaythrough tutorialPlaythrough)
    {
        //1. Modifies the command
        //This code is taken from WorldEdit - See https://enginehub.org/
        int plSep = szCommandLabel.indexOf(':');
        if (plSep >= 0 && plSep < szCommandLabel.length() +1)
        {
            szCommandLabel = szCommandLabel.substring(plSep + 1);
        }
//        StringBuilder sb = new StringBuilder("/").append(szCommandLabel);
        StringBuilder sb = new StringBuilder(szCommandLabel);
//        if (szCommandArgs.length > 0)
//            sb.append(" ");
        String szWorldEditCommand = Joiner.on(" ").appendTo(sb, szCommandArgs).toString();

        //The command is now fully formatted correctly
//        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Command being run via the API: "+szWorldEditCommand);
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Upcoming command being run via the console: "+szWorldEditCommand);


        //Create a new calculation manager for this calculation and add to the list
        WorldEditCalculation newCalculation = new WorldEditCalculation(szWorldEditCommand, correctSelectionRegion, tutorialPlaythrough, iTaskID, virtualBlocks);
        WorldEdit.pendingCalculations.add(newCalculation);
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] This calculation has been added to the queue: "+szWorldEditCommand);
    }
}
