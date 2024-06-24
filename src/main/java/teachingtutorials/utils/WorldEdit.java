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

import java.util.HashSet;

public class WorldEdit
{
    private static Object worldEditEditEvent;

    /**
     * Creates a world edit event listener which catches the block changes arising from world edit commands owned by the given player
     * and creates virtual block objects from these. The specific command parsed is then run, triggering the calculation of all blocks
     * changes from this command.
     * @param virtualBlocks The list of virtual blocks for the calling task
     * @param szCommandLabel The command label (first word) of the command
     * @param szCommandArgs The command args
     * @param bukkitWorld The bukkit world for this set of blocks
     * @param player The player doing the task
     * @param tutorialPlaythrough The tutorial playthrough which this task belongs to
     * @return
     */
    public static void BlocksCalculator(int iTaskID, final HashSet<VirtualBlock> virtualBlocks, RegionSelector correctSelectionRegion, String szCommandLabel, String[] szCommandArgs, World bukkitWorld, Player player, TutorialPlaythrough tutorialPlaythrough)
    {
        //Get instance
        com.sk89q.worldedit.WorldEdit worldEdit = com.sk89q.worldedit.WorldEdit.getInstance();

        //Get the console actor
        Actor consoleActor = BukkitAdapter.adapt(Bukkit.getConsoleSender());

        //Modifies the command
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
//        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorial] Command being run via the API: "+szWorldEditCommand);
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorial] Command being run via the console: "+szWorldEditCommand);

        //Create the new event listener
        worldEditEditEvent = new Object()
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

                //Creates the new extent
                AbstractDelegateExtent blockChangeRecorderExtent = new AbstractDelegateExtent(event.getExtent())
                {
                    @Override
                    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 position, B block) {
                        Bukkit.getConsoleSender().sendMessage("\nA world edit block change has been detected, belonging to the listener for task " +iTaskID +". Recording to the given virtual blocks list");
                        onBlockChange(position, block);
                        //return super.setBlock(position, block);
                        return false; // It's unclear whether this should really be used.
                        // We don't want it to actually set the block so we can just cancel the whole event, but we do also want to set the block or at least try to
                    }


                    protected <B extends BlockStateHolder<B>> void onBlockChange(BlockVector3 pt, B block)
                    {
//                        //This should only ever be for one of the 3 stages anyway right?
//                        if (event.getStage() != EditSession.Stage.BEFORE_CHANGE) {
//                            return;
//                        }
                        //Unregisters the event after 0.1 seconds (2 ticks)
                        Bukkit.getScheduler().runTaskLater(TeachingTutorials.getInstance(), () ->
                        {
                            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Unregistering world edit listener for task "+iTaskID);
                            worldEdit.getEventBus().unregister(worldEditEditEvent);
                            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Unregistered world edit listener for task "+iTaskID);

                            int iSize = virtualBlocks.size();

                            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] There were " +iSize + " block changes detected for task "+iTaskID);
                        }, 2L);


                        //Calculates the old block
                        Location location = BukkitAdapter.adapt(bukkitWorld, pt);
                        Block blockBefore = location.getBlock();
                        BlockData blockDataBefore = blockBefore.getBlockData();

                        //Gets the new block
                        BlockData blockDataNew = BukkitAdapter.adapt(block);

                        //If there is actually a change of block
                        if (!blockDataBefore.equals(blockDataNew))
                        {
                            Bukkit.getConsoleSender().sendMessage("There was a change of block: ");
                            Bukkit.getConsoleSender().sendMessage("New block: " +blockDataNew.getMaterial());
                            Bukkit.getConsoleSender().sendMessage("Location: " +location.toString());

                            //Creates a virtual block
                            VirtualBlock virtualBlock = new VirtualBlock(tutorialPlaythrough, player, location, blockDataNew);
                            //Adds it to the new list
                            virtualBlocks.add(virtualBlock);
                        }
                    }
                };

                //Sets the new extent into the event
                Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Setting the extent");
                event.setExtent(blockChangeRecorderExtent);
            }
        };

        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Sending command: "+szWorldEditCommand);

        Bukkit.getScheduler().runTask(TeachingTutorials.getInstance(), () ->
        {
            try
            {
                //Sets the selection
                Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Adjusting the selection");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "/world "+tutorialPlaythrough.getLocation().getLocationID());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "/pos1 " + ((CuboidRegion) correctSelectionRegion.getRegion()).getPos1().toParserString());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "/pos2 " + ((CuboidRegion) correctSelectionRegion.getRegion()).getPos2().toParserString());

                //Registers the world change event listener
                worldEdit.getEventBus().register(worldEditEditEvent);
                Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] World edit change event listener registered");

                //Runs the command
                Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Sending the command");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), szWorldEditCommand);
                Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Command sent");
            }
            catch (IncompleteRegionException e)
            {

            }
        });
    }
}
