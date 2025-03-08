package teachingtutorials.tutorialplaythrough;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.tutorialobjects.LocationTask;
import teachingtutorials.tutorialplaythrough.fundamentalTasks.Task;
import teachingtutorials.newlocation.DifficultyListener;
import teachingtutorials.utils.Category;
import teachingtutorials.utils.VirtualBlockGroup;

//Possible change: Make a separate NewLocation task and LessonTask.
// Certainly would allow us to separate certain things which are only used in a particular implementation, which could
// be useful in the future if these classes expand further

/**
 * A class representing a playable task in game.
 */
public abstract class PlaythroughTask
{
    /** Represents the LocationTask that this is a play-through of. Holds the information of the task. */
    private final LocationTask locationTask;

    /**
     * A reference to the parent group play-through object
     */
    protected final GroupPlaythrough parentGroupPlaythrough;

    /**
     * A reference to the instance of the TeachingTutorials plugin.
     */
    protected final TeachingTutorials plugin;

    /**
     * A reference to the player that has to perform this task.
     */
    // This could be accessed via parentGroup but it is used so much within the children of the Task class
    // that having a separate reference to it significantly improves readability of those classes.
    protected final Player player;

    /**
     * Stores whether or not the task is currently active and completable
     */
    public boolean bActive;

    /**
     * A list of virtual blocks which are to be displayed as a result of the completion of this task
     */
    protected VirtualBlockGroup<Location, BlockData> virtualBlocks;

    /**
     * Stores whether the task is initialised as part of a new location creation
     */
    // This could be accessed via parentGroup but it is used so much within the children of the Task class
    // that having a separate reference to it significantly improves readability of those classes.
    protected final boolean bCreatingNewLocation;

    //Things in place for the full scoring update
    public float fPerformance;
//    public float[] fFinalScores = new float[5];

    /** The difficulty listener, used for creating new locations. It is used for inputting the difficulty of the task when recording the answers */
    protected DifficultyListener difficultyListener;

    /**
     * Used when initialising a task for a tutorial play-through for an existing location.
     * @param plugin A reference to the TeachingTutorials plugin instance
     * @param player A reference to the Bukkit player for whom the task is for
     * @param locationTask A reference to the Tutorials LocationTask which this task is a play-through of
     * @param groupPlaythrough A reference to the GroupPlaythrough that this PlaythroughTask is a part of
     */
    public PlaythroughTask(TeachingTutorials plugin, Player player, LocationTask locationTask, GroupPlaythrough groupPlaythrough)
    {
        this.plugin = plugin;
        this.player = player;
        this.parentGroupPlaythrough = groupPlaythrough;
        this.locationTask = locationTask;

        this.bCreatingNewLocation = false;

        //Initiates a new virtual blocks group list
        this.virtualBlocks = new VirtualBlockGroup<>(this.parentGroupPlaythrough.parentStepPlaythrough.parentStagePlaythrough.tutorialPlaythrough);
    }

    /**
     * Used when initialising a task for a tutorial play-through for a NewLocation. It will create a new LocationTask.
     * @param plugin A reference to the TeachingTutorials plugin instance
     * @param player A reference to the Bukkit player for whom the task is for
     * @param task A reference to the Tutorials Task which this task is a play-through of
     * @param groupPlaythrough A reference to the GroupPlaythrough that this PlaythroughTask is a part of
     */
    public PlaythroughTask(TeachingTutorials plugin, Player player, Task task, GroupPlaythrough groupPlaythrough)
    {
        this.plugin = plugin;
        this.player = player;
        this.parentGroupPlaythrough = groupPlaythrough;
        this.locationTask = new LocationTask(task, groupPlaythrough.parentStepPlaythrough.parentStagePlaythrough.getLocationID());

        this.bCreatingNewLocation = true;

        //Initiates a new virtual blocks group list
        this.virtualBlocks = new VirtualBlockGroup<>(this.parentGroupPlaythrough.parentStepPlaythrough.parentStagePlaythrough.tutorialPlaythrough);

        //Listens out for difficulty - There will only be one difficulty listener per task to avoid bugs
        difficultyListener = new DifficultyListener(this.plugin, this.player, this);
    }

    /**
     *
     * @return A reference to the LocationTask that this play-through task is of
     */
    public LocationTask getLocationTask()
    {
        return locationTask;
    }

    /**
     * Performs any common registration functionality when the task is ready to be completed. Sets the bActive marker to {@code true}.
     */
    public void register()
    {
        bActive = true;
    }

    /**
     * Calculates some scores and then tells the parent group that its active task has just been finished.
     * Deactivates the task.
     */
    protected void taskComplete()
    {
        //Marks the task as not being completable anymore, since it has been completed
        bActive = false;

        if (!parentGroupPlaythrough.parentStepPlaythrough.parentStagePlaythrough.bLocationCreation)
        {
            //A reference to the parent lesson
            Lesson lesson = (Lesson) parentGroupPlaythrough.parentStepPlaythrough.parentStagePlaythrough.tutorialPlaythrough;
            //Temporary variable used to store the final score calculations in each category
            float fFinalScore, fDifficulty;
            for (int i = 0 ; i < 5 ; i++)
            {
                fDifficulty = locationTask.getDifficulty(Category.values()[i]);
                fFinalScore = fDifficulty*fPerformance;

                //Add scores to the totals
                lesson.fTotalScores[i] = lesson.fTotalScores[i] +fFinalScore;
                lesson.fDifficultyTotals[i] = lesson.fDifficultyTotals[i] + fDifficulty;

            }

            //Add scores to the totals
        }
        //Notifies the parent group that one of its tasks havs been complete
        parentGroupPlaythrough.taskFinished();
    }

    /**
     * Removes virtual blocks and sets task to inactive
     */
    public void deactivate()
    {
        removeVirtualBlocks();
        bActive = false;
    }

    /**
     * Should unregister the plugin listener and deactivate the play-through task
     */
    public abstract void unregister();

    /**
     * Adds the list of virtual blocks of this task to the plugin's virtual block group list
     */
    public void displayVirtualBlocks()
    {
        plugin.addVirtualBlocks(virtualBlocks);
    }

    /**
     * Removes the list of virtual blocks of this task from the plugin's list of virtual block groups
     */
    protected void removeVirtualBlocks()
    {
        plugin.removeVirtualBlocks(virtualBlocks);
    }

    /**
     * To be called from a difficulty listener when the difficulty has been specified. Should unregister the
     * play-through task and move the new location creation on to the next task
     */
    public abstract void newLocationSpotHit();

}
