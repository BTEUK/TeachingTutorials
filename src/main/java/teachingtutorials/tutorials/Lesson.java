package teachingtutorials.tutorials;

import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.listeners.Falling;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.Mode;
import teachingtutorials.utils.User;
import teachingtutorials.utils.plugins.Luckperms;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

//Lesson stores all the information needed for a lesson and handles the gameplay of said lesson
public class Lesson
{
    TeachingTutorials plugin;

    private int iLessonID;
    protected User student;
    private final boolean bCompulsory;

    private int iTutorialIndex;
    protected Tutorial tutorial;
    public Location location;

    public int iStage;
    //The step that a user is currently at (1 indexed), used for "resuming" tutorials
    private int iStep;
    private Stage currentStage;
    ArrayList<Stage> Stages = new ArrayList<>();

    //Accessed by virtual blocks displays
    public boolean bCompleteOrFinished = false;

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

    //Listens out for player falling below the min Y level
    private Falling fallListener;

    public Lesson(User player, TeachingTutorials plugin, boolean bCompulsory)
    {
        this.plugin = plugin;
        this.student = player;
        this.bCompulsory = bCompulsory;
        this.tutorial = new Tutorial();
        this.iStep = 1;
    }

    public User getStudent()
    {
        return student;
    }

    //Used for kicking the lesson off, determines whether it needs to create a new lesson or resume a previous lesson
    public void startLesson()
    {
        //Checks to see whether a user is actually free to start a new lesson
        //(Not already doing a tutorial, creating a tutorial, creating a location etc.)
        if (!student.currentMode.equals(Mode.Idle))
        {
            Display display = new Display(student.player, ChatColor.DARK_AQUA +"Complete or pause your current tutorial first");
            display.Message();
        }

        //Student is ready to go into a lesson
        else
        {
            //Checks to see whether a student has a lesson to finish as indicated by the database
            if (student.bInLesson)
            {
                //Attempts to resume the lesson if the student has a lesson that they need to complete
                if (resumeLesson())
                { //If the lesson resumed successfully
                    //Registers the fall listener
                    fallListener = new Falling(getStudent().player, location.calculateBukkitStartLocation(), plugin);
                    fallListener.register();

                    student.currentMode = Mode.Doing_Tutorial; //Updates the user's current mode
                    student.bInLesson = true; //Updates the user's "In Lesson" status in RAM
                    student.setInLesson(1); //Updates the DB

                    //Adds this lesson to the list of lessons ongoing on the server
                    this.plugin.lessons.add(this);
                }
                else
                { //If the lesson failed to resume
                    Display display = new Display(student.player, ChatColor.RED +"Could not resume lesson, speak to staff");
                    display.Message();
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Could not resume lesson for player: "+student.player.getName());
                }
            }

//        //Checks to see whether it is in the "new location" mode
//        else if (bNewLocation)
//        {
//            //Initiates a new location lesson
//            createNewLocation();
//
//            //Updates the user's mode
//            student.currentMode = Mode.Creating_New_Location;
//        }

            //If the user is ready to start a new tutorial and isn't creating a new location
            else
            {
                //Attempts to create a new lesson, will select a tutorial and start it
                if (createNewLesson())
                { //If the lesson was created successfully

                    //Creates a new lesson in the DB and fetches it's LessonID
                    addLessonToDB();
                    //There is currently no check to determine whether the DB creation worked

                    //Registers the fall listener
                    fallListener = new Falling(getStudent().player, location.calculateBukkitStartLocation(), plugin);
                    fallListener.register();

                    //Updates the user's mode, "In Lesson" status in RAM, and "In Lesson" status in the DB
                    student.currentMode = Mode.Doing_Tutorial;
                    student.bInLesson = true;
                    student.setInLesson(1);

                    //Adds this lesson to the list of lessons ongoing on the server
                    this.plugin.lessons.add(this);
                }
                else
                { //If the lesson failed to be created
                    Display display = new Display(student.player, ChatColor.RED +"Could not create lesson, speak to staff");
                    display.Message();
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Could not create lesson for player: "+student.player.getName());
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
                +student.player.getName()+" with LessonID = " +this.iLessonID
                +", Tutorial ID = " +this.tutorial.getTutorialID()
                +" and LocationID = "+this.location.getLocationID());

        //Teleports the player to the location's world
        org.bukkit.Location tpLocation = location.calculateBukkitStartLocation();
        if (tpLocation == null)
        {
            student.player.sendMessage(ChatColor.RED +"Could not teleport you to the start location");
            return false;
        }
        else
            student.player.teleport(tpLocation);

        //Takes the stage position back for it to then be set forward again at the start of nextStage()
        iStage = iStage - 1;

        //Continues the current stage
        nextStage();

        return true;
    }

//    //Initiates a new location lesson, where a creator will play through a tutorial and record the answers for the new location
//    private void createNewLocation()
//    {
//        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Setting up lesson, creating a new location, for "+student.player.getName());
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
                student.player.closeInventory();
                Display display = new Display(student.player, ChatColor.DARK_AQUA +"No compulsory tutorial was available");
                display.Message();
                return false;
            }
        }
        else //Find an appropriate tutorial and sets the ID to that
        {
            if (!decideTutorial())
            {
                student.player.closeInventory();
                Display display = new Display(student.player, ChatColor.DARK_AQUA +"No tutorial could be found");
                display.Message();
                return false;
            }
        }

        //Import the stages of the tutorial selected
        fetchStages();

        if (selectLocation())
        {
            //Inform console of lesson start
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Lesson starting for "
                    +student.player.getName()+" with LessonID = " +this.iLessonID
                    +", Tutorial ID = " +this.tutorial.getTutorialID()
                    +" and LocationID = "+this.location.getLocationID());

            //Teleports the student to the start location of the location
            org.bukkit.Location tpLocation = location.calculateBukkitStartLocation();
            if (tpLocation == null)
            {
                student.player.sendMessage(ChatColor.RED +"Could not teleport you to the start location");
                return false;
            }
            else
                student.player.teleport(tpLocation);

            //Set the current stage to the first stage
            this.iStage = 0;

            //Signals for the next stage to begin
            nextStage();
        }
        else
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] No location found for " +student.player.getName()+"'s lesson");
            student.player.sendMessage(ChatColor.AQUA +"No location has been created for this tutorial yet :(");
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
        //    iLowestRating = student.iScoreDetailing;
        }

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
        Stages = Stage.fetchStagesByTutorialID(student.player, plugin, this);
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

    //Moves the tutorial on to the next stage
    protected void nextStage()
    {
        this.iStage++;
        if (this.iStage <= Stages.size())
        {
            currentStage = Stages.get(this.iStage-1);
            currentStage.startStage(iStep);

            //Save the positions of stage and step after each stage is started
            // savePositions(); - Optional. Not needed since there is a save after each step
        }
        else
        {
            endLesson();
        }
    }

    //Ends the lesson. Called if the student has finished the tutorial
    private void endLesson()
    {
        //Sets the lesson as complete in the database
        savePositions();
        setLessonCompleteInDB();

        //Declare variables
        int i;

        //Announce to console
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] All stages complete");

        //Marks the lesson complete or finished
        this.bCompleteOrFinished = true;

        //Remove tracker scoreboard
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                student.player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            }
        });

        //Display a tutorial complete message to the student
        Display display = new Display(student.player, " ");
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
        student.calculateRatings();

        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                student.refreshScoreboard();
            }
        });

        //Change player mode
        student.currentMode = Mode.Idle;

        //Change player lesson status
        student.bInLesson = false;
        student.setInLesson(0);

        //Updates the DB and user's boolean variable, notifies the player of completion
        if (bCompulsory)
        {
            student.triggerCompulsory();

            if (!student.bHasCompletedCompulsory)
            {
                //Informs the user that they have completed the tutorial
                display = new Display(student.player, ChatColor.DARK_GREEN + "You have successfully completed the compulsory tutorial. You may now start building.");
                display.Message();
            }
            else
            {
                //Informs the user that they have completed the tutorial
                display = new Display(student.player, ChatColor.DARK_GREEN + "You have successfully completed the compulsory tutorial");
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
                net.luckperms.api.model.user.User lpUser = Luckperms.getUser(student.player.getUniqueId());

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
                        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] User not on track: lp user " +student.player.getName() +" promote " +szTrack);
                        Bukkit.getScheduler().runTask(plugin, new Runnable() {
                            @Override
                            public void run() {
                                Bukkit.dispatchCommand(console, "lp user " +student.player.getName() +" promote "+szTrack);
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
                            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"lp user " +student.player.getName() +" promote " +szTrack);
                            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                                @Override
                                public void run() {
                                    Bukkit.dispatchCommand(console, "lp user " +student.player.getName() +" promote "+szTrack);
                                }
                            });
                        }
                        Bukkit.broadcast(ChatColor.AQUA +student.player.getName() +" is now a " +szRankNew +" !", "");
                    }
                }
            }
            //Deals with a promotion on a rank
            else if (szCompulsoryTutorialPromotionType.equalsIgnoreCase("rank"))
            {
                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        Bukkit.dispatchCommand(console, "lp user " +student.player.getName() +" parent add "+szRankNew);
                    }
                });
                Bukkit.broadcast(ChatColor.AQUA +student.player.getName() +" is now a " +szRankNew +" !", "");
            }

            //Deals with a manual exchange of ranks
            else if (szCompulsoryTutorialPromotionType.equalsIgnoreCase("manualpromote"))
            {
                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        Bukkit.dispatchCommand(console, "lp user " +student.player.getName() +" parent remove " +szRankOld);
                        Bukkit.dispatchCommand(console, "lp user " +student.player.getName() +" parent add " +szRankNew);
                    }
                });
                Bukkit.broadcast(ChatColor.AQUA +student.player.getName() +" is now a " +szRankNew +" !", "");
            }
        }
        else
        {
            //Informs the user that they have completed the tutorial
            display = new Display(student.player, ChatColor.DARK_GREEN + "You have successfully completed the tutorial");
            display.ActionBar();
        }

        //Informs the console that the tutorial is now completed
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"" +student.player.getName() +" has completed a tutorial");

        //Unregisters the fall listener
        fallListener.unregister();

        //Teleport the player back to the lobby area
        teleportToLobby();

        //Removes the lesson from the lessons list
        this.plugin.lessons.remove(this);
    }

    //Saves the scores (in future versions), saves the position of the player and removes the listeners in the event that a lesson is terminated before completion
    public void terminateEarly()
    {
        //Informs console of an early termination
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Terminating lesson (LessonID = " +this.iLessonID +") early for "+student.player.getName());

        //Save the positions of stage and step
        savePositions();

        //Save the scores
        //TODO: To be added when scores are added

        //Remove the listeners; accesses the stage, step and groups to do this
        currentStage.terminateEarly();

        //Remove tracker scoreboard
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                student.player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            }
        });

        //Unregisters the fall listener
        fallListener.unregister();

        //Teleports the player back to lobby
        teleportToLobby();

        //Marks the lesson complete or finished
        this.bCompleteOrFinished = true;

        //Removes the lesson from the lessons list
        this.plugin.lessons.remove(this);

    }

    private void teleportToLobby()
    {
        FileConfiguration config = this.plugin.getConfig();

        String szLobbyTPType = config.getString("Lobby_TP_Type");

        //If a server switch is to occur
        if (szLobbyTPType.equals("Server"))
        {
            String szServerName = config.getString("Server_Name");

            //Switches the player's server after 40 seconds
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
            {
                @Override
                public void run()
                {
                    student.player.performCommand("server " +szServerName);
                }
            }, 40L);
        }

        //If a player teleport is to occur
        else if (szLobbyTPType.equals("LobbyLocation"))
        {
            User.teleportPlayerToLobby(student.player, plugin, 60L);
        }
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
            sql = "Select * FROM Lessons WHERE UUID = '" +student.player.getUniqueId() +"' AND Finished = 0";
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            if (resultSet.next())
            {
                this.iLessonID = resultSet.getInt("LessonID");
                this.tutorial.setTutorialID(resultSet.getInt("TutorialID"));
                this.tutorial.fetchByTutorialID();
                this.iStage = resultSet.getInt("StageAt");
                this.iStep = resultSet.getInt("StepAt");

                //Fetches the location details during construction
                this.location = new Location(resultSet.getInt("LocationID"));
                return true;
            }
            else
                return false;
        }
        catch(SQLException se)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - SQL Error fetching current lesson for "+student.player.getName() +": "+student.player.getUniqueId());
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
                    +"'"+this.student.player.getUniqueId()+"', "
                    +this.tutorial.getTutorialID()+", "
                    +"0, "
                    +this.iStage+", "
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

            szSql = "UPDATE Lessons SET StageAt = " +iStage +" WHERE LessonID = "+ this.iLessonID;
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
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - Error whilst updating a lesson to Finished = 1 for: " +student.player.getName() +": "+student.player.getUniqueId());
            e.printStackTrace();
        }
    }
}
