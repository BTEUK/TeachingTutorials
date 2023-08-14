package teachingtutorials.tutorials;

import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.TutorialPlaythrough;
import teachingtutorials.listeners.Falling;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.Mode;
import teachingtutorials.utils.User;
import teachingtutorials.utils.plugins.Luckperms;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

//Lesson stores all the information needed for a lesson and handles the gameplay of said lesson
public class Lesson extends TutorialPlaythrough
{
    private int iLessonID;
    private final boolean bCompulsory;

    private int iTutorialIndex;

    //Records whether the tutorial details were determined on lesson initialisation
    private final boolean bTutorialDetailsAlreadyEntered;

    //The step that a user is currently at (1 indexed), used for "resuming" tutorials
    private int iStepToStart;

    //Stores the total scores for each of the categories
    public float fTpllScoreTotal;
    public float fWEScoreTotal;
    public float fColourScoreTotal;
    public float fDetailingScoreTotal;
    public float fTerraScoreTotal;

    //Stores the sum of the difficulties for each of the categories
    public float fTpllDifTotal;
    public float fWEDifTotal;
    public float fColourDifTotal;
    public float fDetailDifTotal;
    public float fTerraDifTotal;

    public Lesson(User player, TeachingTutorials plugin, boolean bCompulsory)
    {
        this.plugin = plugin;
        this.creatorOrStudent = player;
        this.bCompulsory = bCompulsory;
        this.tutorial = new Tutorial();
        this.iStepToStart = 1;
        this.bTutorialDetailsAlreadyEntered = false;
    }

    public Lesson(User player, TeachingTutorials plugin, Tutorial tutorial)
    {
        this.plugin = plugin;
        this.creatorOrStudent = player;

        // We assume this is false and that the compulsory tutorial wouldn't be accessible from outside the main menu if it wasn't already done
        this.bCompulsory = false;

        this.tutorial = tutorial;
        this.iStepToStart = 1;
        this.bTutorialDetailsAlreadyEntered = true;
    }

    //Used for kicking the lesson off, determines whether it needs to create a new lesson or resume a previous lesson
    public void startLesson()
    {
        //Checks to see whether a user is actually free to start a new lesson
        //(Not already doing a tutorial, creating a tutorial, creating a location etc.)
        if (!creatorOrStudent.currentMode.equals(Mode.Idle))
        {
            Display display = new Display(creatorOrStudent.player, ChatColor.DARK_AQUA +"Complete your current tutorial first");
            display.Message();
        }

        //Student is ready to go into a lesson and the tutorial must now be determined
        else
        {
            //Checks to see whether a student has a lesson to finish as indicated by the database
            if (creatorOrStudent.bInLesson)
            {
                //Attempts to resume the lesson if the student has a lesson that they need to complete
                if (resumeLesson())
                { //If the lesson resumed successfully
                    //Registers the fall listener
                    fallListener = new Falling(creatorOrStudent.player, location.calculateBukkitStartLocation(), plugin);
                    fallListener.register();

                    creatorOrStudent.currentMode = Mode.Doing_Tutorial; //Updates the user's current mode
                    creatorOrStudent.bInLesson = true; //Updates the user's "In Lesson" status in RAM
                    creatorOrStudent.setInLesson(1); //Updates the DB

                    //Adds this lesson to the list of lessons ongoing on the server
                    this.plugin.lessons.add(this);
                }
                else
                { //If the lesson failed to resume
                    Display display = new Display(creatorOrStudent.player, ChatColor.RED +"Could not resume lesson, speak to staff");
                    display.Message();
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Could not resume lesson for player: "+creatorOrStudent.player.getName());
                }
            }

            //If the user is ready to start a new tutorial and the tutorial is in no way already specified
            else
            {
                //Attempts to create a new lesson, will select a tutorial (if not already known) and start it
                if (createNewLesson())
                { //If the lesson was created successfully

                    //Creates a new lesson in the DB and fetches it's LessonID
                    addLessonToDB();
                    //There is currently no check to determine whether the DB creation worked

                    //Registers the fall listener
                    fallListener = new Falling(creatorOrStudent.player, location.calculateBukkitStartLocation(), plugin);
                    fallListener.register();

                    //Updates the user's mode, "In Lesson" status in RAM, and "In Lesson" status in the DB
                    creatorOrStudent.currentMode = Mode.Doing_Tutorial;
                    creatorOrStudent.bInLesson = true;
                    creatorOrStudent.setInLesson(1);

                    //Adds this lesson to the list of lessons ongoing on the server
                    this.plugin.lessons.add(this);
                }
                else
                { //If the lesson failed to be created
                    Display display = new Display(creatorOrStudent.player, ChatColor.RED +"Could not create lesson, speak to staff");
                    display.Message();
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Could not create lesson for player: "+creatorOrStudent.player.getName());
                }
            }
        }
    }

    //Resumes a previously started lesson
    private boolean resumeLesson()
    {
        //Fetches the tutorial ID, the location, the stage and the step the player is at in their current tutorial
        if (!fetchCurrentFromUUID())
            return false;

        //Gets the data for all of the stages
        fetchStages();

        //Inform console of lesson start
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Lesson resuming for "
                +creatorOrStudent.player.getName()+" with LessonID = " +this.iLessonID
                +", Tutorial ID = " +this.tutorial.getTutorialID()
                +" and LocationID = "+this.location.getLocationID());

        //Teleports the player to the location's world
        org.bukkit.Location tpLocation = location.calculateBukkitStartLocation();
        if (tpLocation == null)
        {
            creatorOrStudent.player.sendMessage(ChatColor.RED +"Could not teleport you to the start location");
            return false;
        }
        else
            creatorOrStudent.player.teleport(tpLocation);

        //Takes the stage position back for it to then be set forward again at the start of nextStage()
        iStageIndex = iStageIndex - 1;

        //Continues the current stage
        nextStage(iStepToStart);

        return true;
    }

//    //Initiates a new location lesson, where a creator will play through a tutorial and record the answers for the new location
//    private void createNewLocation()
//    {
//        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Setting up lesson, creating a new location, for "+creatorOrStudent.player.getName());
//        //We know the tutorial ID, so we can fetch the stages straight away
//        fetchStages();
//
//        this.iStage = 0;
//        nextStage();
//    }

    //Creates a new lesson to be played by a student
    private boolean createNewLesson()
    {
        //Decide on the Tutorial ID
        if (bCompulsory)
        { //Finds the compulsory tutorial and sets the ID to that
            if (!fetchCompulsoryID())
            {
                creatorOrStudent.player.closeInventory();
                Display display = new Display(creatorOrStudent.player, ChatColor.DARK_AQUA +"No compulsory tutorial was available");
                display.Message();
                return false;
            }
        }
        else if (bTutorialDetailsAlreadyEntered)
        {
            //No action needed, tutorial already
        }
        else //Find an appropriate tutorial and sets the ID to that
        {
            if (!decideTutorial())
            {
                creatorOrStudent.player.closeInventory();
                Display display = new Display(creatorOrStudent.player, ChatColor.DARK_AQUA +"No tutorial could be found");
                display.Message();
                return false;
            }
        }

        //Import the stages of the tutorial selected
        fetchStages();

        //Selects a location
        if (selectLocation())
        {
            //Inform console of lesson start
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Lesson starting for "
                    +creatorOrStudent.player.getName()+" with LessonID = " +this.iLessonID
                    +", Tutorial ID = " +this.tutorial.getTutorialID()
                    +" and LocationID = "+this.location.getLocationID());

            //Teleports the student to the start location of the location
            org.bukkit.Location tpLocation = location.calculateBukkitStartLocation();
            if (tpLocation == null)
            {
                creatorOrStudent.player.sendMessage(ChatColor.RED +"Could not teleport you to the start location");
                return false;
            }
            else
                creatorOrStudent.player.teleport(tpLocation);

            //Set the current stage to the first stage
            this.iStageIndex = 0;

            //Signals for the next stage (the first stage) to begin
            nextStage(1);
        }
        else
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] No location found for " +creatorOrStudent.player.getName()+"'s lesson");
            creatorOrStudent.player.sendMessage(ChatColor.AQUA +"No location has been created for this tutorial yet :(");
            return false;
        }
        return true;
    }

    //Fetches the TutorialID of the compulsory tutorial from the DB
    //and updates the tutorial object in this Lesson, storing the ID there
    private boolean fetchCompulsoryID()
    {
        int iTutorialID = -1;

        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            //Compiles the command to fetch tutorials
            sql = "Select * FROM Tutorials WHERE `Compulsory` = 1";
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            if (resultSet.next())
            {
                iTutorialID = resultSet.getInt("TutorialID");
                tutorial.szTutorialName = resultSet.getString("TutorialName");
                tutorial.uuidAuthor = UUID.fromString(resultSet.getString("Author"));
            }

            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "Compulsory Tutorial ID found as "+iTutorialID +". Tutorial = "+tutorial.szTutorialName);

            //Stores the TutorialID into the tutorial object of this Lesson
            this.tutorial.setTutorialID(iTutorialID);

            return true;
        }
        catch (SQLException se)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - SQL Error fetching all compulsory tutorials");
            se.printStackTrace();
            return false;
        }
        catch (Exception e)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - Non-SQL Error fetching all compulsory tutorials");
            e.printStackTrace();
            return false;
        }
    }

    //Fetches all tutorials that are marked as "In Use" and using the student's ratings, will decide on the best tutorial to make them do
    private boolean decideTutorial()
    {
        //Prevalence of each category in each tutorial needed

        //-----------------------------------------------------------------
        //-------Calculate what two skills a player needs to work on-------
        //-----------------------------------------------------------------

        //1 = tpll
        //2 = WE
        //3 = Terraforming
        //4 = Colouring
        //5 = Detailing
        int iIndexLowestRating = 1;

        int iLowestRating = 100;

        if (creatorOrStudent.iScoreTpll < iLowestRating)
        {
            iIndexLowestRating = 1;
            iLowestRating = creatorOrStudent.iScoreTpll;
        }
        if (creatorOrStudent.iScoreWE < iLowestRating)
        {
            iIndexLowestRating = 2;
            iLowestRating = creatorOrStudent.iScoreWE;
        }
        if (creatorOrStudent.iScoreTerraforming < iLowestRating)
        {
            iIndexLowestRating = 3;
            iLowestRating = creatorOrStudent.iScoreTerraforming;
        }
        if (creatorOrStudent.iScoreColouring < iLowestRating)
        {
            iIndexLowestRating = 4;
            iLowestRating = creatorOrStudent.iScoreColouring;
        }
        if (creatorOrStudent.iScoreDetailing < iLowestRating)
        {
            iIndexLowestRating = 5;
        //    iLowestRating = creatorOrStudent.iScoreDetailing;
        }

        int iSecondLowestRating = 100;
        int iIndexSecondLowestRating = 2;

        if (creatorOrStudent.iScoreTpll < iSecondLowestRating && iIndexLowestRating !=1)
        {
            iIndexSecondLowestRating = 1;
            iSecondLowestRating = creatorOrStudent.iScoreTpll;
        }
        if (creatorOrStudent.iScoreWE < iSecondLowestRating && iIndexLowestRating !=2)
        {
            iIndexSecondLowestRating = 2;
            iSecondLowestRating = creatorOrStudent.iScoreWE;
        }
        if (creatorOrStudent.iScoreTerraforming < iSecondLowestRating && iIndexLowestRating !=3)
        {
            iIndexSecondLowestRating = 3;
            iSecondLowestRating = creatorOrStudent.iScoreTerraforming;
        }
        if (creatorOrStudent.iScoreColouring < iSecondLowestRating && iIndexLowestRating !=4)
        {
            iIndexSecondLowestRating = 4;
            iSecondLowestRating = creatorOrStudent.iScoreColouring;
        }
        if (creatorOrStudent.iScoreDetailing < iSecondLowestRating && iIndexLowestRating !=5)
        {
            iIndexSecondLowestRating = 5;
        }

        //-----------------------------------------------------------------
        //-------------Selecting all tutorials that are in use-------------
        //-----------------------------------------------------------------
        Tutorial[] tutorials;

        tutorials = Tutorial.fetchAll(true);


        //----------------------------------------------------------------
        //------------Decide the most relevant tutorial to use------------
        //----------------------------------------------------------------

        //We know the rating of each tutorial and the categories we want
        float fBiggestRelevance = 0;
        int iIndexBiggestRelevance = 0;

        int iCount = tutorials.length;

        if (iCount > 0)
        {
            //Go through each tutorial, and score each with relevance, and keep track of the most relevant tutorial
            for (int i = 0 ; i < iCount ; i++)
            {
                float fRelevance = 2 * tutorials[i].categoryUsage[iIndexLowestRating - 1];
                fRelevance = fRelevance + tutorials[i].categoryUsage[iIndexSecondLowestRating - 1];
                if (fRelevance > fBiggestRelevance) {
                    fBiggestRelevance = fRelevance;
                    iIndexBiggestRelevance = i;
                }
            }
            iTutorialIndex = iIndexBiggestRelevance;

            //Sets this lessons tutorial as the tutorial decided upon
            this.tutorial = tutorials[iTutorialIndex];

            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "Tutorial ID found as "+tutorial.getTutorialID() +". Tutorial = "+tutorial.szTutorialName);
            return true;
        }
        else
        {
            return false;
        }
    }

    //Gets a list of all of the stages of the specified tutorial and loads each with the relevant data
    private void fetchStages()
    {
        //List is in order of stage 1 1st
        stages = Stage.fetchStagesByTutorialID(creatorOrStudent.player, plugin, this);
    }

    //Selects the location of a specific tutorial randomly
    private boolean selectLocation()
    {
        int[] iLocationIDs;
        iLocationIDs = Location.getAllLocationIDsForTutorial(this.tutorial.getTutorialID());

        //Checks to see if any locations were found
        if (iLocationIDs.length == 0)
        {
            return false;
        }
        else
        {
            int iRandomIndex = (int) Math.random()*(iLocationIDs.length-1);

            //Initialises location
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] LocationID selected: " +iLocationIDs[iRandomIndex]);
            this.location = new Location(iLocationIDs[iRandomIndex]);

            return true;
        }
    }

    //Ends the lesson. Called if the student has finished the tutorial
    protected void endPlaythrough()
    {
        //Declare variables
        int i;

        //Announce to console
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] All stages complete");

        //Sets the lesson as complete in the database
        savePositions();
        setLessonCompleteInDB();

        //Display a tutorial complete message to the student
        Display display = new Display(creatorOrStudent.player, " ");
        display.Title(ChatColor.AQUA +"Tutorial Complete", 10, 60, 12);

        //Calculate final scores

        //Initialise arrays - used for the for loop
        float[] fFinalScores = new float[5];

        float[] fScoreTotals = new float[5];
        fScoreTotals[0] = fTpllScoreTotal;
        fScoreTotals[1] = fWEScoreTotal;
        fScoreTotals[2] = fColourScoreTotal;
        fScoreTotals[3] = fDetailingScoreTotal;
        fScoreTotals[4] = fTerraScoreTotal;

        float[] fDifficultyTotals = new float[5];
        fDifficultyTotals[0] = fTpllDifTotal;
        fDifficultyTotals[1] = fWEDifTotal;
        fDifficultyTotals[2] = fColourDifTotal;
        fDifficultyTotals[3] = fDetailDifTotal;
        fDifficultyTotals[4] = fTerraDifTotal;

        for (i = 0 ; i < 5 ; i++)
        {
            if (fDifficultyTotals[i] == 0)
            {
                fFinalScores[i] = -1;
            }
            else
                fFinalScores[i] = fScoreTotals[i]/fDifficultyTotals[i];
        }

        //Then store in DB

        //Scoring not to be included in first release.

        //And trigger the scoreboard to refresh
        creatorOrStudent.calculateRatings();

        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                creatorOrStudent.refreshScoreboard();
            }
        });

        //Updates the DB and user's boolean variable, notifies the player of completion
        if (bCompulsory)
        {
            creatorOrStudent.triggerCompulsory();

            if (!creatorOrStudent.bHasCompletedCompulsory)
            {
                //Informs the user that they have completed the tutorial
                display = new Display(creatorOrStudent.player, ChatColor.DARK_GREEN + "You have successfully completed the compulsory tutorial. You may now start building.");
                display.Message();
            }
            else
            {
                //Informs the user that they have completed the tutorial
                display = new Display(creatorOrStudent.player, ChatColor.DARK_GREEN + "You have successfully completed the compulsory tutorial");
                display.ActionBar();
            }

            //Promotes the player
            FileConfiguration config = plugin.getConfig();
            String szCompulsoryTutorialPromotionType = config.getString("Compulsory_Tutorial_Promotion_Type");
            String szRankOld = config.getString("Compulsory_Tutorial_RankOld");
            String szRankNew = config.getString("Compulsory_Tutorial_RankNew");
            String szTrack = config.getString("Compulsory_Tutorial_Track");
            String[] szTracks = config.getString("Compulsory_Tutorial_TrackOutline").split(",");

            ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();

            if (szCompulsoryTutorialPromotionType.equalsIgnoreCase("none"))
            {
                //Do nothing
            }
            //Deals with a promotion on a track
            else if (szCompulsoryTutorialPromotionType.equalsIgnoreCase("track"))
            {
                net.luckperms.api.model.user.User lpUser = Luckperms.getUser(creatorOrStudent.player.getUniqueId());

                //Go through all of a user's groups and see if they have a group with a weight above that of the target promotion
                List<String> groups = lpUser.getNodes().stream()
                        .filter(NodeType.INHERITANCE::matches)
                        .map(NodeType.INHERITANCE::cast)
                        .map(InheritanceNode::getGroupName)
                        .collect(Collectors.toList());
                int iNumGroups = groups.size();

                int iNumGroupsInTrack = szTracks.length;

                //We have a list of tracks in order, we have a target and an old
                //Let's just use indexes
                //Find the index of the new and old rank in the track
                int j, k;
                int iIndexOldRank = -1;
                int iIndexNewRank = -1;
                int iHighestIndexOnTrack = -1;

                //Find the index of the old and new rank in the track
                for (j = 0 ; j < iNumGroupsInTrack ; j++)
                {
                    if (szRankOld.equals(szTracks[j]))
                    {
                        iIndexOldRank = j;
                    }
                    if (szRankNew.equals(szTracks[j]))
                    {
                        iIndexNewRank = j;
                    }
                }

                if (iIndexNewRank == -1 || iIndexOldRank == -1)
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Either new rank or old rank is not on the specified track");
                else if (iIndexNewRank <= iIndexOldRank)
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"The position of the new rank in the track is less than or equal to the position of the old rank");

                else
                {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Let's find the index of the highest rank the player has in the track");
                    //Let's find the index of the highest rank the player has in the track

                    //Compares each of a user's groups to the groups in the relevant track
                    for (j = 0 ; j < iNumGroups ; j++)
                    {
                        //Cycles through the ranks in the track
                        for (k = 0 ; k < iNumGroupsInTrack ; k++)
                        {
                            //The current group of the user is on the track
                            if (groups.get(j).equals(szTracks[k]))
                            {
                                //Let's find the index
                                iHighestIndexOnTrack = k;
                            }
                        }
                    }

                    //User is not currently on the track
                    if (iHighestIndexOnTrack == -1)
                    {
                        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] User not on track: lp user " +creatorOrStudent.player.getName() +" promote " +szTrack);
                        Bukkit.getScheduler().runTask(plugin, new Runnable() {
                            @Override
                            public void run() {
                                Bukkit.dispatchCommand(console, "lp user " +creatorOrStudent.player.getName() +" promote "+szTrack);
                            }
                        });
                        iHighestIndexOnTrack = 0;
                    }
                    if (iHighestIndexOnTrack >= iIndexNewRank)
                    {
                        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Highest index on track is greater than the index of the new rank");
                        //No action should be taken, user is already at the new rank or higher
                    }
                    else
                    {
                        int iDifference = iIndexNewRank - iHighestIndexOnTrack;
                        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Index difference between new rank and highest current rank = " +iDifference);
                        for (int l = 0 ; l < iDifference ; l++)
                        {
                            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"lp user " +creatorOrStudent.player.getName() +" promote " +szTrack);
                            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                                @Override
                                public void run() {
                                    Bukkit.dispatchCommand(console, "lp user " +creatorOrStudent.player.getName() +" promote "+szTrack);
                                }
                            });
                        }
                        Bukkit.broadcast(ChatColor.AQUA +creatorOrStudent.player.getName() +" is now a " +szRankNew +" !", "");
                    }
                }
            }
            //Deals with a promotion on a rank
            else if (szCompulsoryTutorialPromotionType.equalsIgnoreCase("rank"))
            {
                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        Bukkit.dispatchCommand(console, "lp user " +creatorOrStudent.player.getName() +" parent add "+szRankNew);
                    }
                });
                Bukkit.broadcast(ChatColor.AQUA +creatorOrStudent.player.getName() +" is now a " +szRankNew +" !", "");
            }

            //Deals with a manual exchange of ranks
            else if (szCompulsoryTutorialPromotionType.equalsIgnoreCase("manualpromote"))
            {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Bukkit.dispatchCommand(console, "lp user " +creatorOrStudent.player.getName() +" parent remove " +szRankOld);
                    Bukkit.dispatchCommand(console, "lp user " +creatorOrStudent.player.getName() +" parent add " +szRankNew);
                });
                Bukkit.broadcast(ChatColor.AQUA +creatorOrStudent.player.getName() +" is now a " +szRankNew +" !", "");
            }
        }
        else
        {
            //Informs the user that they have completed the tutorial
            display = new Display(creatorOrStudent.player, ChatColor.DARK_GREEN + "You have successfully completed the tutorial");
            display.ActionBar();
        }

        //Informs the console that the tutorial is now completed
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"" +creatorOrStudent.player.getName() +" has completed a tutorial");

        //Change player lesson status
        creatorOrStudent.bInLesson = false;
        creatorOrStudent.setInLesson(0);

        //Removes the lesson from the lessons list
        this.plugin.lessons.remove(this);

        //Performs common playthrough completion processes
        super.commonEndPlaythrough();
    }

    //Saves the scores (in future versions), saves the position of the player and removes the listeners in the event that a lesson is terminated before completion
    public void terminateEarly()
    {
        //Informs console of an early termination
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Terminating lesson (LessonID = " +this.iLessonID +") early for "+creatorOrStudent.player.getName());

        //Save the positions of stage and step
        savePositions();

        //Save the scores
        //TODO: To be added when scores are added

        //Remove the listeners, and removes virtual blocks (of the current task); accesses the stage, step and groups to do this
        currentStage.terminateEarly();

        //Removes the lesson from the lessons list
        this.plugin.lessons.remove(this);

        //Performs common playthrough termination processes
        super.commonEndPlaythrough();
    }

    public static void main(String[] args)
    {
     //   Lesson lesson = new Lesson()
    }

    //---------------------------------------------------
    //--------------------SQL Fetches--------------------
    //---------------------------------------------------

    //Fetches the information for a lesson that a user has not yet finished
    public boolean fetchCurrentFromUUID()
    {
        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            //Compiles the command to fetch the lesson in progress - assumes a player can only have one lesson ongoing at a time
            sql = "Select * FROM Lessons WHERE UUID = '" +creatorOrStudent.player.getUniqueId() +"' AND Finished = 0";
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            if (resultSet.next())
            {
                this.iLessonID = resultSet.getInt("LessonID");
                this.tutorial.setTutorialID(resultSet.getInt("TutorialID"));
                this.tutorial.fetchByTutorialID();
                this.iStageIndex = resultSet.getInt("StageAt");
                this.iStepToStart = resultSet.getInt("StepAt");

                //Fetches the location details during construction
                this.location = new Location(resultSet.getInt("LocationID"));
                return true;
            }
            else
                return false;
        }
        catch(SQLException se)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - SQL Error fetching current lesson for "+creatorOrStudent.player.getName() +": "+creatorOrStudent.player.getUniqueId());
            se.printStackTrace();
            this.tutorial.setTutorialID(-1);
            return false;
        }
    }

    //---------------------------------------------------
    //--------------------SQL Updates--------------------
    //---------------------------------------------------

    //Creates a new lesson in the DB and fetches back the LessonID of the newly inserted lesson
    private void addLessonToDB()
    {
        //Declare variables
        String szSql;
        Statement SQL;
        ResultSet resultSet;

        //Updates the database
        try
        {
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();
            szSql = "INSERT INTO Lessons (UUID, TutorialID, Finished, StageAt, StepAt, LocationID)" +
                    " VALUES ("
                    +"'"+creatorOrStudent.player.getUniqueId()+"', "
                    +this.tutorial.getTutorialID()+", "
                    +"0, "
                    +this.iStageIndex+", "
                    +this.currentStage.getCurrentStep()+", "
                    +this.location.getLocationID() +")";
            SQL.executeUpdate(szSql);

            szSql = "Select LAST_INSERT_ID()";
            resultSet = SQL.executeQuery(szSql);
            resultSet.next();
            this.iLessonID = resultSet.getInt(1);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    //Updates the database to save the position that a player is at in a lesson
    protected void savePositions()
    {
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Saving positions for LessonID "+this.iLessonID);
        //Declare variables
        String szSql;
        Statement SQL;

        //Updates the database
        try
        {
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //At this point StageIndex actually refers to what stage they are on and is 1 indexed
            szSql = "UPDATE Lessons SET StageAt = " +iStageIndex +" WHERE LessonID = "+ this.iLessonID;
            SQL.executeUpdate(szSql);

            szSql = "UPDATE Lessons SET StepAt = " +currentStage.getCurrentStep() +" WHERE LessonID = "+ this.iLessonID;
            SQL.executeUpdate(szSql);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    //Marks the lesson as complete/finished in the database
    private void setLessonCompleteInDB()
    {
        //Declare variables
        String szSql;
        Statement SQL;

        //Updates the database
        try
        {
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();
            szSql = "UPDATE Lessons SET Finished = 1 WHERE LessonID = "+ this.iLessonID;
            SQL.executeUpdate(szSql);
        }
        catch (Exception e)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - Error whilst updating a lesson to Finished = 1 for: " +creatorOrStudent.player.getName() +": "+creatorOrStudent.player.getUniqueId());
            e.printStackTrace();
        }
    }
}
