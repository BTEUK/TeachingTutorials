package teachingtutorials;

import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import teachingtutorials.listeners.Falling;
import teachingtutorials.listeners.PlaythroughCommandListeners;
import teachingtutorials.tutorials.Location;
import teachingtutorials.tutorials.Stage;
import teachingtutorials.tutorials.Tutorial;
import teachingtutorials.utils.Mode;
import teachingtutorials.utils.User;
import teachingtutorials.utils.VirtualBlockGroup;

import java.util.ArrayList;
import java.util.Stack;

//To be extended by lesson and new location
//They both share a lot of data and processes, and they are also rather similar in the user experience as well
public abstract class TutorialPlaythrough
{
    protected TeachingTutorials plugin;
    protected Tutorial tutorial;
    protected Location location;
    protected User creatorOrStudent;

    //The full list of stages the tutorial has
    protected ArrayList<Stage> stages;

    //Keeps track of the current stage
    protected Stage currentStage;
    protected int iStageIndex;

    //Listens out for player falling below the min Y level
    protected Falling fallListener;

    //Enables tpll, ll and blocks gmask
    protected PlaythroughCommandListeners playthroughCommandListeners;

    //A list of spies also viewing the virtual blocks
    private ArrayList<Player> spies = new ArrayList<>();

    public Tutorial getTutorial()
    {
        return tutorial;
    }

    public Location getLocation()
    {
        return location;
    }

    public User getCreatorOrStudent()
    {
        return creatorOrStudent;
    }

    /**
     * Returns the list of spies on this playthrough
     */
    public ArrayList<Player> getSpies()
    {
        return spies;
    }

    /**
     * Add a spy to the list of spies viewing virtual blocks for this playthrough.
     * <P> </P>
     * If a player is already in a separate tutorial, will remove them from that first and then add them to this.
     */
    public void addSpy(Player player)
    {
        //Check perms to spy
        if (player.hasPermission("teachingtutorials.canspy"))
        {
            //Identify the user instance
            User spyUser = User.identifyUser(plugin, player);
            if (spyUser != null)
            {
                //Ensures they are idle
                if (spyUser.currentMode.equals(Mode.Idle))
                {
                    //Check if they are already spying and if so, remove them from that
                    if (spyUser.isSpying())
                        spyUser.disableSpying();

                    //Add them as a spy to this tutorial playthrough
                    spies.add(player);

                    //Mark the spy user's spy target to this playthrough
                    spyUser.setSpyTarget(this);

                    //Refresh happens frequently so no need to call for an adhoc refresh
                }
            }
        }
    }

    /**
     * Removes a player from the list of spies viewing virtual blocks for this playthrough and resets their view.
     * <P> </P>
     * This method will also set the spy target of the User to null.
     */
    public void removeSpy(Player player)
    {
        //Removes the player from the list of spies
        if (spies.remove(player))
        {
            //Resets all of their blocks
            //Get the list of virtual blocks
            VirtualBlockGroup[] virtualBlockGroups = this.plugin.getVirtualBlockGroups().toArray(VirtualBlockGroup[]::new);

            //Declares the temporary list object
            VirtualBlockGroup<org.bukkit.Location, BlockData> virtualBlockGroup;

            //Goes through all virtual block groups - will do this going from end of tutorial to start
            int iTasksActive = virtualBlockGroups.length;
            for (int j = iTasksActive-1 ; j >=0 ; j--)
            {
                //Extracts the jth virtual block group
                virtualBlockGroup = virtualBlockGroups[j];

                //Resets the virtual blocks to the real blocks of this spy on this tutorial
                if (virtualBlockGroup.isOfPlaythrough(this))
                {
                    //Call for the world blocks to be reset
                    virtualBlockGroup.removeVirtualBlocksForSpy(player);
                }
            }

            //Identify the user instance and set the user's spy target to null
            User spyUser = User.identifyUser(plugin, player);
            if (spyUser != null)
            {
                spyUser.setSpyTarget(null);
            }
        }
    }

    /**
     * Removes all spies and resets their view. Also sets the spytarget of their User instances to null.
     */
    private void removeAllSpies()
    {
        int iNumSpies = getSpies().size();
        while (iNumSpies > 0)
        {
            removeSpy(getSpies().get(0).getPlayer());
            iNumSpies--;
        }
    }

    /**
     * Returns whether this playthrough is being spied on by the given player
     */
    public boolean hasSpy(Player player)
    {
        return this.spies.contains(player);
    }

    public void setFallListenerSafeLocation(org.bukkit.Location location)
    {
        //Raises the safe location by 1 block to ensure players do not tp inside blocks as used to happen sometimes
        fallListener.setSafeLocation(location.add(0, 1, 0));
    }

    // Moves the tutorial on to the next stage
    // Accessed after the end of each stage (Called from Stage.endStage() asynchronously)
    // or at the start of the playthrough
    public void nextStage(int iStepToStartStageOn, boolean bDelayTitle)
    {
        int iNumStages = stages.size();

        iStageIndex++; //1 indexed

        if (iStageIndex <= iNumStages)
        {
            currentStage = stages.get(iStageIndex-1);
            currentStage.startStage(iStepToStartStageOn, bDelayTitle);
            //Save the positions of stage and step after each stage is started
            // savePositions(); - Optional. Not needed since there is a save after each step
        }
        else
        {
            endPlaythrough();
        }
    }

    protected abstract void endPlaythrough();

    protected void commonEndPlaythrough()
    {
        //Change player mode
        creatorOrStudent.currentMode = Mode.Idle;

        //Unregisters the gameplay listeners
        fallListener.unregister();
        playthroughCommandListeners.unregister();

        //Removes virtual blocks
        Stack<VirtualBlockGroup<org.bukkit.Location, BlockData>> virtualBlockGroups = plugin.getVirtualBlockGroups();

        VirtualBlockGroup<org.bukkit.Location, BlockData> virtualBlockGroup;

        //Goes through the list of the plugin's active virtual block groups
        for (int i = 0 ; i < virtualBlockGroups.size() ; i++)
        {
            virtualBlockGroup = virtualBlockGroups.get(i);

            //Checks whether the virtual block group is of this tutorial playthrough
            if (virtualBlockGroup.isOfPlaythrough(this))
            {
                //Removes the list from the plugin's list of lists
                this.plugin.removeVirtualBlocks(virtualBlockGroup);

                //Resets the blocks back to the original state for the players
                virtualBlockGroup.removeBlocks();

                i--;
            }
        }

        //Removes all spies
        removeAllSpies();

        //Teleport the player back to the lobby area
        teleportToLobby();
    }

    protected void teleportToLobby()
    {
        FileConfiguration config = this.plugin.getConfig();

        String szLobbyTPType = "";
        szLobbyTPType = config.getString("Lobby_TP_Type");

        //If a server switch is to occur
        if (szLobbyTPType.equals("Server"))
        {
            String szServerName = config.getString("Server_Name");

            //Switches the player's server after a delay
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> creatorOrStudent.player.performCommand("server " +szServerName), config.getLong("Completion_TP_Wait"));
        }

        //If a player teleport is to occur
        else if (szLobbyTPType.equals("LobbyLocation"))
        {
            //Teleports the player to the lobby after a delay
            User.teleportPlayerToLobby(creatorOrStudent.player, plugin, config.getLong("Completion_TP_Wait"));
        }
    }
}
