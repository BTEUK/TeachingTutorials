package teachingtutorials.tutorials;

import net.buildtheearth.terraminusminus.generator.EarthGeneratorSettings;
import net.buildtheearth.terraminusminus.projection.GeographicProjection;
import net.buildtheearth.terraminusminus.projection.OutOfProjectionBoundsException;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.checkerframework.checker.units.qual.C;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.Mode;
import teachingtutorials.utils.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

//Lesson stores all the information needed for a lesson and handles the play of a tutorial
public class Lesson
{
    protected User student;
    private boolean bCompulsory;
    private boolean bNewLocation;

    private int iTutorialIndex;
    protected Tutorial tutorial;

    public int iStage;
    public int iStep;
    public Location location;

    TeachingTutorials plugin;

    ArrayList<Stage> Stages = new ArrayList<>();

    //Stores the total scores for each of the categories
    public float fTpllScoreTotal;
    public float fWEScoreTotal;
    public float fColourScoreTotal;
    public float fDetailingScoreTotal;
    public float fTerraScoreTotal;

    //Stores the sum of the difficulties for each if the categories
    public float fTpllDifTotal;
    public float fWEDifTotal;
    public float fColourDifTotal;
    public float fDetailDifTotal;
    public float fTerraDifTotal;

    public Lesson(User player, TeachingTutorials plugin, boolean bCompulsory)
    {
        this.plugin = plugin;
        this.student = player;
        this.bCompulsory = bCompulsory;
        this.tutorial = new Tutorial();
        this.bNewLocation = false;
        this.iStep = 1;
    }

    //Used for kicking the lesson off, determines whether it needs to create a new lesson or resume a previous lesson
    public void startLesson()
    {
        if (student.bInLesson)
        {
            if (!resumeLesson())
            {
                Display display = new Display(student.player, ChatColor.DARK_AQUA +"Could not resume lesson");
                display.Message();
                return;
            }
        }

        //Checks whether it is in the new location mode
        else if (bNewLocation)
            createNewLocation();

        else
        {
            if (!createNewLesson())
            {
                Display display = new Display(student.player, ChatColor.DARK_AQUA +"Could not create lesson");
                display.Message();
                return;
            }
        }

        student.currentMode = Mode.Doing_Tutorial;
        student.bInLesson = true;
        student.setInLesson(1);
    }

    //Resumes a previous lesson
    private boolean resumeLesson()
    {
        //Fetches the tutorial ID, the location, the stage and the step the player is at in their current tutorial
        if (!fetchCurrentFromUUID())
            return false;

        //Gets the data for all of the stages
        fetchStages();

        //Takes the stage position back for it to then be set forward again at the start of nextStage()
        iStage = iStage - 1;

        //Continues the current stage
        nextStage();

        return true;
    }

    private void createNewLocation()
    {
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Setting up lesson, creating a new location, for "+student.player.getName());
        //We know the tutorial ID, so we can fetch the stages straight away
        fetchStages();

        this.iStage = 0;
        nextStage();
    }

    //Creates a new lesson
    private boolean createNewLesson()
    {
        //Decide on the Tutorial ID
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Setting up lesson for "+student.player.getName());
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
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Fetching stages");
        fetchStages();

        if (selectLocation())
        {
            //Set the current stage to the first stage
            this.iStage = 0;

            //Signals for the next stage to begin
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Signal next stage");
            nextStage();

            double[] xz;
            World world = location.getWorld();
            final GeographicProjection projection = EarthGeneratorSettings.parse(EarthGeneratorSettings.BTE_DEFAULT_SETTINGS).projection();

            //Converts the longitude and latitude start coordinates of the location to minecraft coordinates
            try
            {
                xz = projection.fromGeo(location.getStartCoordinates().getLng(), location.getStartCoordinates().getLat());
                Bukkit.getConsoleSender().sendMessage(location.getStartCoordinates().getLng() +", " +location.getStartCoordinates().getLat());
                //Declares location object
                org.bukkit.Location tpLocation;

                tpLocation = new org.bukkit.Location(world, xz[0], world.getHighestBlockYAt((int) xz[0], (int) xz[1]) + 1, xz[1]);

                //Teleports the student to the start location of the location
                student.player.teleport(tpLocation);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Unable to convert lat,long coordinates of start location to minecraft coordinates");
                student.player.sendMessage(ChatColor.AQUA +"Could not teleport you to the start location");
                return false;
            }
        }
        else
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] No location found");
            student.player.sendMessage(ChatColor.AQUA +"No location has been created for this tutorial yet :(");
            return false;
        }
        return true;
    }

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
            }
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "Compulsory Tutorial ID found as "+iTutorialID);
        }
        catch(SQLException se)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - SQL Error fetching all in use tutorials");
            se.printStackTrace();
            iTutorialID = -1;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            iTutorialID = -1;
        }

        this.tutorial.setTutorialID(iTutorialID);

        if (iTutorialID == -1)
            return false;
        else
            return true;
    }

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
            iLowestRating = student.iScoreDetailing;
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
            this.tutorial.setTutorialID(tutorials[iTutorialIndex].getTutorialID());
            return true;
        }
        else
        {
            return false;
        }
    }

    private void fetchStages()
    {
        //Gets a list of all of the stages of the specified tutorial and loads each with the relevant data.
        //List is in order of stage 1 1st
        this.Stages = Stage.fetchStagesByTutorialID(student.player, plugin, this);
    }

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

    protected void nextStage()
    {
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Next stage");

        this.iStage++;
        Stage stage;
        if (this.iStage <= Stages.size())
        {
            stage = Stages.get(this.iStage-1);
            stage.startStage(iStep);
        }
        else
        {
            endLesson();
        }
    }

    private void endLesson()
    {
        //Declare variables
        int i;

        //Initialise arrays - used for for loop
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

        //Announce to console
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] All stages complete");
        
        //Calculate final scores
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
        student.refreshScoreboard();

        //Change player mode
        student.currentMode = Mode.Idle;

        //Change player lesson status
        student.bInLesson = false;
        student.setInLesson(0);

        //Updates the DB and user's boolean variable
        student.triggerCompulsory();

        Display display;
        if (bCompulsory)
        {
            display = new Display(student.player, ChatColor.AQUA + "You have successfully completed the tutorial");
            display.Message();

            //Promotes the player

            Bukkit.broadcast(ChatColor.AQUA +student.player.getName() +" is now an applicant", "");
        }
        else
        {
            display = new Display(student.player, ChatColor.AQUA + "You have successfully completed the compulsory tutorial. You may now start building.");
            display.Message();
        }
    }

    public static void main(String[] args)
    {
     //   Lesson lesson = new Lesson()
    }

    public boolean fetchCurrentFromUUID()
    {
        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            //Compiles the command to fetch the lesson in progress - assumes only one lesson can be going on at a time
            sql = "Select * FROM Lessons WHERE UUID = '" +student.player.getUniqueId() +"' AND Finished = 0";
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            if (resultSet.next())
            {
                this.tutorial.setTutorialID(resultSet.getInt("TutorialID"));
                this.iStage = resultSet.getInt("StageAt");
                this.iStep = resultSet.getInt("StepAt");

                //Fetches the location details during construction
                this.location = new Location(resultSet.getInt("LocationID"));
            }
            return true;
        }
        catch(SQLException se)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - SQL Error fetching current lesson for "+student.player.getName() +": "+student.player.getUniqueId());
            se.printStackTrace();
            this.tutorial.setTutorialID(-1);
            return false;
        }
    }
}
