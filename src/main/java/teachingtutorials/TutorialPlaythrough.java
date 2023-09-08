package teachingtutorials;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import teachingtutorials.listeners.Falling;
import teachingtutorials.tutorials.Location;
import teachingtutorials.tutorials.Stage;
import teachingtutorials.tutorials.Tutorial;
import teachingtutorials.utils.Mode;
import teachingtutorials.utils.User;
import teachingtutorials.utils.VirtualBlock;

import java.util.ArrayList;

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

    public void setFallListenerSafeLocation(org.bukkit.Location location)
    {
        fallListener.setSafeLocation(location);
    }

    // Moves the tutorial on to the next stage
    // Accessed after the end of each stage (Called from Stage.endStage() asynchronously)
    // or at the start of the playthrough
    public void nextStage(int iStepToStartStageOn)
    {
        int iNumStages = stages.size();

        iStageIndex++; //1 indexed

        if (iStageIndex <= iNumStages)
        {
            currentStage = stages.get(iStageIndex-1);
            currentStage.startStage(iStepToStartStageOn);
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
        //Remove tracker scoreboard
        Bukkit.getScheduler().runTask(plugin, () -> creatorOrStudent.player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard()));

        //Change player mode
        creatorOrStudent.currentMode = Mode.Idle;

        //Unregisters the fall listener
        fallListener.unregister();

        //Removes virtual blocks
        int i;
        ArrayList<VirtualBlock> virtualBlocks = plugin.virtualBlocks;
        int iVirtualBlocks = virtualBlocks.size();
        VirtualBlock virtualBlock;
        for (i = 0 ; i < iVirtualBlocks ; i++)
        {
            virtualBlock = virtualBlocks.get(i);
            if (virtualBlock.isFromTutorial(this))
            {
                virtualBlocks.remove(i);
                virtualBlock.removeAndReset();
                iVirtualBlocks--;
                i--;
            }
        }

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

            //Switches the player's server after 40 seconds
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> creatorOrStudent.player.performCommand("server " +szServerName), 40L);
        }

        //If a player teleport is to occur
        else if (szLobbyTPType.equals("LobbyLocation"))
        {
            User.teleportPlayerToLobby(creatorOrStudent.player, plugin, 40L);
        }
    }
}
