package teachingtutorials.utils;

//import com.sk89q.worldedit.EditSession;
//import com.sk89q.worldedit.LocalSession;
//import com.sk89q.worldedit.bukkit.BukkitWorld;
//import com.sk89q.worldedit.entity.Player;
//import com.sk89q.worldedit.extension.platform.Actor;
//import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
//import com.sk89q.worldedit.function.operation.Operation;
//import com.sk89q.worldedit.math.BlockVector3;
//import com.sk89q.worldedit.regions.CuboidRegion;
//import com.sk89q.worldedit.session.ClipboardHolder;
//import com.sk89q.worldedit.session.SessionManager;
//import com.sk89q.worldedit.session.SessionOwner;
//import org.apache.commons.lang.WordUtils;
import com.google.common.base.Joiner;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitCommandSender;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.command.RegionCommands;
import com.sk89q.worldedit.event.Event;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.AbstractNonPlayerActor;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.PlatformCommandManager;
import com.sk89q.worldedit.extension.platform.permission.ActorSelectorLimits;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.history.UndoContext;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.regions.selector.RegionSelectorType;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.session.SessionKey;
import com.sk89q.worldedit.session.SessionManager;
import com.sk89q.worldedit.session.SessionOwner;
import com.sk89q.worldedit.util.auth.AuthorizationException;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.world.AbstractWorld;
import com.sk89q.worldedit.world.NullWorld;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.weather.WeatherTypes;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockVector;
import org.enginehub.piston.CommandManager;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.TutorialPlaythrough;
import teachingtutorials.fundamentalTasks.Command;
import teachingtutorials.utils.plugins.WorldGuard;

import javax.print.DocFlavor;
import java.util.*;

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

//        //Get the session manager
//        SessionManager sessionManager = worldEdit.getSessionManager();

        //Get the console actor
        Actor consoleActor = BukkitAdapter.adapt(Bukkit.getConsoleSender());

//        //Create an edit session for the console
//        EditSession consoleEditSession = worldEdit.newEditSessionBuilder().world(correctSelectionRegion.getWorld()).actor(consoleActor).build();
//
//        //Get the local session of the console and set the edit session there
//        LocalSession tempLocalSession = sessionManager.get(consoleActor);
////        tempLocalSession.remember(consoleEditSession);
//
////        //Get the player's edit session
////        LocalSession localSession = sessionManager.get(WEPlayer);
//
////        //Get the player's selection limits
////        ActorSelectorLimits selectionLimits = ActorSelectorLimits.forActor(WEPlayer);
//
////        //Injects the new selection - adjusts the player's selection
////        playersRegionSelector.selectPrimary(correctSelectionRegion.getMinimumPoint(), selectionLimits);
////        playersRegionSelector.selectSecondary(correctSelectionRegion.getMaximumPoint(), selectionLimits);
//
////        //Gets the player
////        com.sk89q.worldedit.entity.Player WEPlayer = ;
//        //Get the player's selection limits
//        ActorSelectorLimits selectionLimits = ActorSelectorLimits.forActor(BukkitAdapter.adapt(player));
//
//        //Gets the temp actor's selection and sets the selection points
//        RegionSelector actorRegionSelector = tempLocalSession.getRegionSelector(correctSelectionRegion.getWorld());
//        actorRegionSelector.selectPrimary(correctSelectionRegion.getMinimumPoint(), selectionLimits);
//        actorRegionSelector.selectSecondary(correctSelectionRegion.getMaximumPoint(), selectionLimits);
//
        //Modifies the command
        //This code is taken from WorldEdit
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

//                event.getExtent().getMinimumPoint() = ev

                if (actor==null)// || actor.getUniqueId().equals(tempActor.getUniqueId()))
                {
                    System.out.println("Edit session event detected belonging to a null actor - assuming console - at stage: "+event.getStage().toString());
                    //Cancel if at a certain stage?
                }
                else if (actor.getSessionKey().equals(consoleActor.getSessionKey()))
                {
                    System.out.println("Edit session event detected belonging to the actor we are listening for - at stage: "+event.getStage().toString());
                }
                else
                {
                    System.out.println("Edit session event detected but doesn't belong to the correct actor, so ignoring");
                    return;
                }

                AbstractDelegateExtent blockChangeRecorderExtent = new AbstractDelegateExtent(event.getExtent())
                {
                    @Override // Is this not working? It seems to not run this modified setBlock
                    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 position, B block) {
                        System.out.println("A world edit block change has been detected, recording to the given virtual blocks list");
                        onBlockChange(position, block);
                        //return super.setBlock(position, block);
                        return false; // It's unclear whether this should really be used.
                        // We don't want it to actually set the block so we can just cancel the whole event, but we do also want to set the block or at least try to
                    }


                    protected <B extends BlockStateHolder<B>> void onBlockChange(BlockVector3 pt, B block)
                    {
//                        //This should only ever be a specific stage anyway
//                        if (event.getStage() != EditSession.Stage.BEFORE_CHANGE) {
//                            return;
//                        }
                        //Unregisters the event and deletes the world guard region after 0.5 seconds (10 ticks)
                        Bukkit.getScheduler().runTaskLater(TeachingTutorials.getInstance(), () ->
                        {
                            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Unregistering world edit listener");
                            worldEdit.getEventBus().unregister(worldEditEditEvent);
                            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Unregistered world edit listener");
                        }, 10L);


                        //Calculates the old block
                        Location location = BukkitAdapter.adapt(bukkitWorld, pt);
                        Block blockBefore = location.getBlock();
                        BlockData blockDataBefore = blockBefore.getBlockData();

                        //Gets the new block
                        BlockData blockDataNew = BukkitAdapter.adapt(block);

                        //If there is actually a change of block
                        if (!blockDataBefore.equals(blockDataNew))
                        {
                            //Creates a virtual block
                            VirtualBlock virtualBlock = new VirtualBlock(tutorialPlaythrough, player, location, blockDataNew);
                            //Adds it to the new list
                            virtualBlocks.add(virtualBlock);
                        }
                    }
                };

                event.getExtent().setBlock() // Try this maybe

                Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Setting the extent");
                event.setExtent(blockChangeRecorderExtent);

                event.getExtent().setBlock() // Try this maybe
            }
        };

        //Temporarily block player's world edit output
//        To do;
        //If we create a new actor then it should be fine right?
        //As long as we extract the selection correctly

        //Temporarily allow worldguard access
        //Tbh just make it the whole world
//        WorldGuard.addToGlobalRegion(bukkitWorld, player);
//        WorldGuard.createNewRegion(iTaskID+"", BukkitAdapter.adapt(bukkitWorld), player, correctSelectionRegion.getMinimumPoint(), correctSelectionRegion.getMaximumPoint());

//        //Performs the WE command via the API
//        CommandEvent commandEvent = new CommandEvent(consoleActor, szWorldEditCommand);
//        PlatformCommandManager platformCommandManager = worldEdit.getPlatformManager().getPlatformCommandManager();
//
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Sending command: "+szWorldEditCommand);
//        platformCommandManager.handleCommand(commandEvent);
        Bukkit.getScheduler().runTask(TeachingTutorials.getInstance(), () ->
        {
            try
            {
                Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Adjusting the selection");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "/world "+tutorialPlaythrough.getLocation().getLocationID());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "/pos1 " + ((CuboidRegion) correctSelectionRegion.getRegion()).getPos1().toParserString());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "/pos2 " + ((CuboidRegion) correctSelectionRegion.getRegion()).getPos2().toParserString());

                //Register the event
                worldEdit.getEventBus().register(worldEditEditEvent);
                Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] World edit change event listener registered");

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
