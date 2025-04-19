package teachingtutorials.tutorialplaythrough;

import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.listeners.Falling;
import teachingtutorials.listeners.PlaythroughCommandListeners;
import teachingtutorials.tutorialobjects.Location;
import teachingtutorials.tutorialobjects.Tutorial;
import teachingtutorials.utils.DBConnection;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.Mode;
import teachingtutorials.utils.User;
import teachingtutorials.utils.plugins.Luckperms;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A class representing a Lesson type playthrough of a tutorial. This is what most Tutorial Playthroughs will be.
 * <p> </p>
 * Handles all processes related to the lesson and stores Lesson-wide data.
 */
public class Lesson extends TutorialPlaythrough
{
    /** Holds the LessonID of the lesson as stored in the DB */
    private int iLessonID;

    /** The step that a user is currently at (1 indexed), used for "resuming" tutorials */
    private int iStepToStart;

    /** Stores the total scores for each of the categories - Ready for when scoring is added properly.
     * <p>Order follows that of the order of categories in the Category enum</p>
     */
    public float[] fTotalScores = new float[5];

    /** Store the sum of the difficulties that could've been achieved so far for each of the categories - Ready for when scoring is added properly
     * <p>Order follows that of the order of categories in the Category enum</p>
     */
    public float[] fDifficultyTotals = new float[5];

    /**
     * The only constructor available to initiate a lesson object
     * @param player The User for which this Lesson is being initiated
     * @param plugin A reference to the instance of the TeachingTutorials plugin
     * @param tutorial The tutorial which will be played in this lesson
     */
    public Lesson(User player, TeachingTutorials plugin, Tutorial tutorial)
    {
        super(plugin, player, tutorial, PlaythroughMode.PlayingLesson);
    }

    public int getLessonID()
    {
        return iLessonID;
    }

    /**
     * Used for starting the Lesson. Will determine a location to do the Tutorial in, determine what step and stage to start the tutorial at, and then launch the lesson
     * @param bResetProgress Determines whether or not to reset the player's progress to stage 1 step 1.
     * @return Whether or not the lesson started successfully
     */
    public boolean startLesson(boolean bResetProgress)
    {
        //Checks to see whether a user is actually free to start a new lesson
        //(Not already doing a tutorial, creating a tutorial or creating a location etc)
        if (!creatorOrStudent.getCurrentMode().equals(Mode.Idle))
        {
            creatorOrStudent.player.sendMessage(Display.colouredText("Complete your current tutorial first", NamedTextColor.DARK_AQUA));
            return false;
        }

        //Checks to see whether a student has a lesson to finish - if so, will replace the tutorial with that one and resume it
        if (creatorOrStudent.hasIncompleteLessons(plugin.getDBConnection(), plugin.getLogger()))
        {
            //Attempts to resume the lesson if the student has a lesson that they need to complete
            //Mind: This will overwrite the Tutorial object parsed to the Lesson initially
            if (!resumeLesson(bResetProgress))
            {
                //If the lesson failed to resume display messages
                creatorOrStudent.player.sendMessage(Display.colouredText("Could not resume lesson, speak to staff", NamedTextColor.RED));
                plugin.getLogger().log(Level.WARNING, ChatColor.GOLD +"Could not resume lesson for player: "+creatorOrStudent.player.getName());
                return false;
            }
        }

        //If the user has no ongoing lessons to complete
        else
        {
            //Attempts to create a new lesson will select a location and start it
            if (!createAndSetUpNewLesson())
            {
                //If the lesson failed to be created display messages
                creatorOrStudent.player.sendMessage(Display.colouredText("Could not create lesson, speak to staff", NamedTextColor.RED));
                plugin.getLogger().log(Level.WARNING, ChatColor.GOLD +"Could not resume lesson for player: "+creatorOrStudent.player.getName());

                return false;
            }
        }

        //Removes the player's current blockspy
        creatorOrStudent.disableSpying();

        //Registers the fall listener - this teleports players back to the start point of the step they are on should they fall off the world
        fallListener = new Falling(creatorOrStudent.player, plugin);
        fallListener.register();

        //Register the tpll and ll command and the gmask blocker
        playthroughCommandListeners = new PlaythroughCommandListeners(plugin);
        playthroughCommandListeners.register();

        //Signals the next stage to start, at the required step, as previously determined
        nextStage(iStepToStart, true);

        //Updates the user's "In Completed Lesson" status in RAM
        creatorOrStudent.setHasIncompleteLesson(true);

        //Update the user's current Playthrough and the mode of the user
        creatorOrStudent.setCurrentPlaythrough(this);

        //Adds this lesson to the list of tutorial playthroughs ongoing on the server
        this.plugin.activePlaythroughs.add(this);

        //Gives the player the navigation menu
        this.navigationMenu.refresh();
        this.creatorOrStudent.mainGui = this.navigationMenu;
        return true;
    }

    /**
     * Loads the data for a previously started lesson, fetches the Stages from the DB and displays all relevant virtual blocks.
     * <p> </p>
     * Will replace the Tutorial currently held in this Lesson with the one found in the Lesson DB table for this player.
     * @param bResetProgress Whether to reset the progress to stage 1 step 1
     * @return Whether or not the lesson resumed successfully
     */
    private boolean resumeLesson(boolean bResetProgress)
    {
        //Fetches the Tutorial ID, the location, the stage and the step the player is at in their current lesson
        if (!fetchCurrentLessonOfPlayer(bResetProgress))
            return false;

        //Inform console of lesson start
        plugin.getLogger().log(Level.INFO,  ChatColor.AQUA + "Lesson resuming for "
            +creatorOrStudent.player.getName()+" with LessonID = " +this.iLessonID
            +", Tutorial ID = " +this.tutorial.getTutorialID()
            +" and LocationID = "+this.location.getLocationID());

        //Redisplays virtual blocks of steps already complete
        displayVirtualBlocks(iStageIndex, iStepToStart-1);

        //Takes the stage position back for it to then be set forward again at the start of nextStage()
        iStageIndex = iStageIndex - 1;

        return true;
    }

    /**
     * Creates a new lesson to be played by a student and adds the Lesson to the DB
     * @return Whether the tutorial was created and added to the DB successfully
     */
    private boolean createAndSetUpNewLesson()
    {
        //Selects a location
        if (selectLocation())
        {
            //Inform console of lesson start
            plugin.getLogger().log(Level.INFO,  ChatColor.AQUA + "Lesson starting for "
                    +creatorOrStudent.player.getName()+" with LessonID = " +this.iLessonID
                    +", Tutorial ID = " +this.tutorial.getTutorialID()
                    +" and LocationID = "+this.location.getLocationID());


            //Set the 'next' stage to the first stage
            this.iStageIndex = 0;

            //Sets the Step-to-start as the first step
            this.iStepToStart = 1;
        }
        else
        {
            //Log issue
            plugin.getLogger().log(Level.WARNING, ChatColor.GOLD + "No location found for " +creatorOrStudent.player.getName()+"'s lesson");

            //Notify player of issue - note it may be caused by a different issue to the one displayed, but it will say this for simplicity
            creatorOrStudent.player.sendMessage(Display.errorText("No location has been created for this tutorial yet :("));

            //Return false - lesson could not be created properly due to no location being found
            return false;
        }

        //Creates a new lesson in the DB and fetches it's LessonID
        if (!addLessonToDB())
        {
            //Log issue
            plugin.getLogger().log(Level.SEVERE, "Could not add new Lesson to the DB");

            //Notify player of issue
            creatorOrStudent.player.sendMessage(Display.errorText("An error occurred, speak to staff"));

            //Return false - lesson could not be created properly
            return false;
        }
        iHighestStageCompleted = 0;
        iHighestStepCompleted = 0;
        return true;
    }

    /**
     * Fetches all tutorials that are marked as "In Use" and using the student's ratings, will decide on the best tutorial to make them do
     * @param student The User instance for the player we need to choose a Tutorial for
     * @param dbConnection A connection to the database
     * @return A Tutorial object holding the information of the tutorial they should complete next, or null if there were no tutorials
     */
    public static Tutorial decideTutorial(User student, DBConnection dbConnection, Logger logger)
    {
        //-----------------------------------------------------------------
        //-------Calculate what two skills a player needs to work on-------
        //-----------------------------------------------------------------

        //1 = tpll
        //2 = WE
        //3 = Terraforming
        //4 = Colouring
        //5 = Detailing

        //Find's the first area that the player needs to work on
        int iIndexLowestRating = 1;
        int iLowestRating = 100;

        if (student.iScoreTpll < iLowestRating)
        {
            iIndexLowestRating = 1;
            iLowestRating = student.iScoreTpll;
        }
        if (student.iScoreWE < iLowestRating)
        {
            iIndexLowestRating = 2;
            iLowestRating = student.iScoreWE;
        }
        if (student.iScoreTerraforming < iLowestRating)
        {
            iIndexLowestRating = 3;
            iLowestRating = student.iScoreTerraforming;
        }
        if (student.iScoreColouring < iLowestRating)
        {
            iIndexLowestRating = 4;
            iLowestRating = student.iScoreColouring;
        }
        if (student.iScoreDetailing < iLowestRating)
        {
            iIndexLowestRating = 5;
        //    iLowestRating = creatorOrStudent.iScoreDetailing;
        }

        //Finds the second area that they need to work on
        int iSecondLowestRating = 100;
        int iIndexSecondLowestRating = 2;

        if (student.iScoreTpll < iSecondLowestRating && iIndexLowestRating !=1)
        {
            iIndexSecondLowestRating = 1;
            iSecondLowestRating = student.iScoreTpll;
        }
        if (student.iScoreWE < iSecondLowestRating && iIndexLowestRating !=2)
        {
            iIndexSecondLowestRating = 2;
            iSecondLowestRating = student.iScoreWE;
        }
        if (student.iScoreTerraforming < iSecondLowestRating && iIndexLowestRating !=3)
        {
            iIndexSecondLowestRating = 3;
            iSecondLowestRating = student.iScoreTerraforming;
        }
        if (student.iScoreColouring < iSecondLowestRating && iIndexLowestRating !=4)
        {
            iIndexSecondLowestRating = 4;
            iSecondLowestRating = student.iScoreColouring;
        }
        if (student.iScoreDetailing < iSecondLowestRating && iIndexLowestRating !=5)
        {
            iIndexSecondLowestRating = 5;
        }


        //----------------------------------------------------------------
        //------------Decide the most relevant tutorial to use------------
        //----------------------------------------------------------------

        //-------------Selecting all tutorials that are in use-------------
        Tutorial[] tutorials;

        tutorials = Tutorial.fetchAll(true, false, null, dbConnection, logger);


        //-------------Calculate the relevance of each tutorial-------------

        //We know the rating of each tutorial and the categories we want
        float fBiggestRelevance = 0;
        int iIndexBiggestRelevance = 0;

        int iCount = tutorials.length;

        if (iCount > 0)
        {
            //Go through each tutorial, and score each with relevance, and keep track of the most relevant tutorial
            for (int i = 0 ; i < iCount ; i++)
            {
                float fRelevance = 2 * tutorials[i].getCategoryUsage(iIndexLowestRating - 1);
                fRelevance = fRelevance + tutorials[i].getCategoryUsage(iIndexSecondLowestRating - 1);
                if (fRelevance > fBiggestRelevance) {
                    fBiggestRelevance = fRelevance;
                    iIndexBiggestRelevance = i;
                }
            }

            //Returns a Tutorial object for the most relevant tutorial
            return tutorials[iIndexBiggestRelevance];
        }
        else
        {
            //Returns null if no tutorials were found
            return null;
        }
    }

    /**
     * Selects a location randomly for the Tutorial specified in this Lesson
     * @return Whether or not a location could found
     */
    private boolean selectLocation()
    {
        //Ensure that a tutorial has been selected and load properly
        if (tutorial == null)
        {
            plugin.getLogger().log(Level.SEVERE, "Tried to select a location for a null tutorial. The Lesson object's Tutorial is null");
            return false;
        }
        if (tutorial.getTutorialID() == -1)
        {
            plugin.getLogger().log(Level.SEVERE, "Tried to select a location for a tutorial with a TutorialID of -1. The Lesson object's Tutorial is not load properly");
            return false;
        }

        //Get a list of all LocationID for the tutorial of this Lesson
        int[] iLocationIDs;
        iLocationIDs = Location.getAllLocationIDsForTutorial(this.tutorial.getTutorialID(), TeachingTutorials.getInstance().getDBConnection(), TeachingTutorials.getInstance().getLogger());

        //Checks to see if any locations were found
        if (iLocationIDs.length == 0)
        {
            plugin.getLogger().log(Level.WARNING, "No locations could be found for the Tutorial with ID: "+tutorial.getTutorialID() +". ("+tutorial.getTutorialName() +").");
            return false;
        }
        else
        {
            //Selects a random index
            int iRandomIndex = (int) Math.random()*(iLocationIDs.length-1);

            //Notifies console of Location selected
            plugin.getLogger().log(Level.INFO, "LocationID selected as " +iLocationIDs[iRandomIndex] +" for LessonID " +this.iLessonID);

            creatorOrStudent.player.sendMessage(Display.aquaText("Loading the world for you"));

            //Initialises the Lesson's location object with this lesson
            this.location = new Location(iLocationIDs[iRandomIndex], tutorial.getTutorialID());

            return true;
        }
    }

    /**
     * Ends the lesson if the player has finished the tutorial.
     * <p>Marks the lesson in the DB as complete, displays the "completion" title to the player. </p>
     * <p> </p>
     * Performs any processes for if it was a compulsory tutorial: Updates the 'completed compulsory' field in the DB,
     * sends a specific completion message, promotes the player, sends a message to the whole server</p>
     *
     * <p>Sets the User's InLesson status to false in the plugin and the database, removes this Lesson from the plugin's
     * list of active lessons, then runs the general Playthrough termination code</p>
     */
    @Override
    protected void endPlaythrough()
    {
        //Declare variables
        int i;

        //Notify console of Lesson completion
        plugin.getLogger().log(Level.INFO, "Lesson " +iLessonID +" ending as all stages have been completed! (" +creatorOrStudent.player.getName() +")");

        //Sets the lesson as complete in the database
        savePositions();
        setLessonCompleteInDB();

        //Display a tutorial complete message to the student
        //Gets the time period we have to display the title
        int iTicks = plugin.getConfig().getInt("Completion_TP_Wait");
        //Display the title
        Display.Title(creatorOrStudent.player, " ", ChatColor.GREEN +"Tutorial Complete!", (int) (iTicks*0.125), (int) (iTicks*0.75), (int) (iTicks*0.125));

        //----- Calculate final scores -----

        //Initialise arrays - used for the for loop
        float[] fFinalScores = new float[5];

        for (i = 0 ; i < 5 ; i++)
        {
            if (fDifficultyTotals[i] == 0)
            {
                fFinalScores[i] = -1;
            }
            else
                fFinalScores[i] = fTotalScores[i]/fDifficultyTotals[i];
        }

        /* Todo: Then store in DB
            Scoring not to be included in first release
        */

        //Recalculate the ratings
        creatorOrStudent.calculateRatings(TeachingTutorials.getInstance().getDBConnection());

        //Updates the DB and user's boolean variable, sends player a completion message
        if (tutorial.isCompulsory())
        {
            //Updates the User information within the plugin and the database
            creatorOrStudent.triggerCompulsory();

            //Displays a message in chat

            //Checks whether they have already completed the tutorial previously
            if (!creatorOrStudent.bHasCompletedCompulsory)
            {
                //Informs the user that they have completed the starter tutorial for the first time
                creatorOrStudent.player.sendMessage(Display.colouredText("You have successfully completed the starter tutorial. You may now start building!", NamedTextColor.DARK_GREEN));
            }
            else
            {
                //Informs the user that they have completed the starter tutorial again
                creatorOrStudent.player.sendMessage(Display.colouredText("You have successfully completed the starter tutorial", NamedTextColor.DARK_GREEN));
            }

            //---------------------------------------------------
            //----------------Promotes the player----------------
            //---------------------------------------------------
            //Promotes the player - this is always performed in case there was an issue with the DB, or if the player was demoted and is starting again

            //Notifies console
            plugin.getLogger().log(Level.INFO, "Promoting player " +creatorOrStudent.player.getName() +" after Lesson " +this.iLessonID +" was completed");

            //Promote the player using the promotion service.
            plugin.getPromotionService().promote(creatorOrStudent.player);
        }

        //Update player lesson status in the plugin
        creatorOrStudent.reassessHasIncompleteLesson(plugin.getDBConnection(), plugin.getLogger());

        //Performs common playthrough completion processes
        super.commonEndPlaythrough();
    }

    /**
     * Handles pausing of tutorials/when a lesson is terminated before completion.
     * <p></p>
     * Saves the position of the player and removes the task listeners, then runs the general Playthrough termination code
     */
    public void terminateEarly()
    {
        //Informs console of an early termination
        plugin.getLogger().log(Level.INFO, "Terminating lesson (LessonID = " +this.iLessonID +") early for "+creatorOrStudent.player.getName());

        //Save the positions of stage and step
        savePositions();

        //Save the scores
        //TODO: To be added when scores are added

        //Remove the listeners, and removes virtual blocks (of the current task); accesses the stage, step and groups to do this
        currentStagePlaythrough.terminateEarly();

        //Performs common playthrough termination processes
        super.commonEndPlaythrough();
    }

    //---------------------------------------------------
    //--------------------SQL Fetches--------------------
    //---------------------------------------------------

    /**
     * Fetches the information for a lesson that a user has not yet finished.
     * <p> </p>
     * Includes the LessonID, the StepAt and StageAt and loads all information on the Tutorial into the Tutorial object of this Lesson.
     * <p> </p>
     * If multiple lessons are somehow found in the database this will return the one at the top of the list of Lessons retrieved.
     * @param bResetProgress Whether to replace the current StepAt and StageAt values with 1 and 1.
     * @return Whether or not a lesson was found in the DB and if so whether the data was retrieved and stored properly.
     */
    private boolean fetchCurrentLessonOfPlayer(boolean bResetProgress)
    {
        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            //Compiles the command to fetch the lesson in progress - assumes a player can only have one lesson ongoing at a time
            sql = "SELECT * FROM `Lessons` WHERE `UUID` = '" +creatorOrStudent.player.getUniqueId() +"' AND `Finished` = 0";
            SQL = plugin.getConnection().createStatement();

            //Executes the query and extracts the information into this Lesson and this Lesson's Tutorial object
            resultSet = SQL.executeQuery(sql);
            if (resultSet.next())
            {
                this.iLessonID = resultSet.getInt("LessonID");

                //Used when resetting the progress
                if (bResetProgress)
                {
                    this.iStageIndex = 1;
                    this.iStepToStart = 1;
                }
                //Used when the position is to be fetched from the DB
                else
                {
                    this.iStageIndex = resultSet.getInt("StageAt");
                    this.iStepToStart = resultSet.getInt("StepAt");
                    this.iHighestStepCompleted = resultSet.getInt("HighestStepCompleted");
                    this.iHighestStageCompleted = resultSet.getInt("HighestStageCompleted");
                }

                creatorOrStudent.player.sendMessage(Display.aquaText("Loading the world for you"));

                //Initialises the location - The Location constructor will fetch the location details as well
                this.location = new Location(resultSet.getInt("LocationID"), resultSet.getInt("TutorialID"));
                return true;
            }
            else
            {
                //Reports issues to the console, returns false
                plugin.getLogger().log(Level.WARNING, "An unfinished Lesson could not be found in the DB for user " +creatorOrStudent.player.getUniqueId() +" (" +creatorOrStudent.player.getName()+")");
                return false;
            }
        }
        catch (SQLException se)
        {
            //Reports issues to the console, returns false
            plugin.getLogger().log(Level.SEVERE, "SQL Error fetching current lesson for "+creatorOrStudent.player.getName() +": "+creatorOrStudent.player.getUniqueId(), se);
            return false;
        }
    }

    /**
     * Fetches the tutorial ID of the current in progress lesson for a player
     * @param playerUUID The UUID of the player to get the Tutorial of the current lesson for
     * @param dbConnection A connection to the database
     * @param logger A reference to a plugin logger
     * @return The id of the tutorial of the current lesson a player is in, or -1 if no lesson was found
     */
    public static int getTutorialOfCurrentLessonOfPlayer(UUID playerUUID, DBConnection dbConnection, Logger logger)
    {
        int iTutorialID;

        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            //Sets up the statement
            SQL = dbConnection.getConnection().createStatement();
            sql = "SELECT `TutorialID` FROM `Lessons` WHERE `UUID` = '" +playerUUID +"' AND `Finished` = 0";

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            if (resultSet.next())
            {
                iTutorialID = resultSet.getInt("TutorialID");
            }
            else
                iTutorialID = -1;
        }
        catch(SQLException se)
        {
            logger.log(Level.SEVERE, "Tutorials - SQL Error fetching current lesson for " +playerUUID, se);
            iTutorialID = -1;
        }
        return iTutorialID;
    }

    //---------------------------------------------------
    //--------------------SQL Updates--------------------
    //---------------------------------------------------

    /**
     * Creates a new lesson in the DB for this Lesson and fetches back the LessonID of the newly inserted lesson, storing that ID in this lesson
     * @return Whether the Lesson was added to the DB successfully
     */
    private boolean addLessonToDB()
    {
        //Declare variables
        String szSql;
        Statement SQL;
        ResultSet resultSet;

        //Updates the database
        try
        {
            //Sets up the statement
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();
            szSql = "INSERT INTO `Lessons` (`UUID`, `TutorialID`, `Finished`, `StageAt`, `StepAt`, `LocationID`)" +
                    " VALUES ("
                    +"'"+creatorOrStudent.player.getUniqueId()+"', "
                    +this.tutorial.getTutorialID()+", "
                    +"0, "
                    +"1, "
                    +"1, "
                    +this.location.getLocationID() +")";

            //Executes the update
            SQL.executeUpdate(szSql);

            //Fetches the LessonID that was assigned to this Lesson
            szSql = "SELECT LAST_INSERT_ID()";
            resultSet = SQL.executeQuery(szSql);
            resultSet.next();

            //Stores the LessonID back into this Lesson
            this.iLessonID = resultSet.getInt(1);

            return true;
        }
        catch (SQLException se)
        {
            //Reports to the console
            plugin.getLogger().log(Level.SEVERE, "SQL Error: "+se.getMessage(), se);
            return false;
        }
        catch (Exception e)
        {
            //Reports to the console
            plugin.getLogger().log(Level.SEVERE, "Error: "+e.getMessage(), e);
            return false;
        }
    }

    /**
     * Updates the database to save the position that a player is at in this lesson
     */
    protected void savePositions()
    {
        //Notify console
        plugin.getLogger().log(Level.INFO, "Saving positions for LessonID "+this.iLessonID +". ("+this.creatorOrStudent.player.getName() +")");

        //Declare variables
        String szSql;
        Statement SQL;

        //Updates the database
        try
        {
            //Sets up the statement
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //At this point StageIndex actually refers to what stage they are on and is 1 indexed
            szSql = "UPDATE `Lessons` SET `StageAt` = " +iStageIndex +" WHERE `LessonID` = "+ this.iLessonID;
            SQL.executeUpdate(szSql);

            szSql = "UPDATE `Lessons` SET `StepAt` = " + currentStagePlaythrough.getCurrentStep() +" WHERE `LessonID` = "+ this.iLessonID;
            SQL.executeUpdate(szSql);

            szSql = "UPDATE `Lessons` SET `HighestStepCompleted` = " +iHighestStepCompleted +" WHERE `LessonID` = "+ this.iLessonID;
            SQL.executeUpdate(szSql);

            szSql = "UPDATE `Lessons` SET `HighestStageCompleted` = " +iHighestStageCompleted +" WHERE `LessonID` = "+ this.iLessonID;
            SQL.executeUpdate(szSql);

        }
        catch (SQLException se)
        {
            //Reports error to the console
            plugin.getLogger().log(Level.SEVERE, "SQL Error: "+se.getMessage(), se);
        }
        catch (Exception e)
        {
            //Reports error to the console
            plugin.getLogger().log(Level.SEVERE, "Error: "+e.getMessage(), e);
        }
    }

    /**
     * Marks the lesson as complete/finished in the database
     */
    private void setLessonCompleteInDB()
    {
        //Declare variables
        String szSql;
        Statement SQL;

        //Updates the database
        try
        {
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();
            szSql = "UPDATE `Lessons` SET `Finished` = 1 WHERE `LessonID` = "+ this.iLessonID;
            SQL.executeUpdate(szSql);
        }
        catch (Exception e)
        {
            //Reports error to the console
            plugin.getLogger().log(Level.SEVERE, "Error whilst updating a lesson to Finished = 1 for: " +creatorOrStudent.player.getName() +": "+creatorOrStudent.player.getUniqueId(), e);
        }
    }
}
