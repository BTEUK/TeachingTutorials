package teachingtutorials.tutorials;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import teachingtutorials.TeachingTutorials;
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

    private int iTutorialIndex;
    protected Tutorial tutorial;

    int iTutorialID;
    public int iStage;
    public int iStep;
    public int iLocationID;

    TeachingTutorials plugin;

    ArrayList<Stage> Stages = new ArrayList<>();

    //Scores in each of the categories

    public Lesson(User player, TeachingTutorials plugin, boolean bCompulsory)
    {
        this.plugin = plugin;
        this.student = player;
        this.bCompulsory = bCompulsory;
        this.tutorial = new Tutorial();
    }

    public void resumeLesson()
    {
        setUpLesson();

        fetchCurrentFromUUID();

        continueStage();
    }

    private void setUpLesson()
    {
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Setting up lesson for "+student.player.getName());
        if (bCompulsory)
        { //Finds the compulsory tutorial and sets the ID to that
            boolean bCompulsoryExists = fetchCompulsoryID();
            if (!bCompulsoryExists)
            {
                student.player.sendMessage(ChatColor.AQUA +"No compulsory tutorial was available");
                return;
            }
        }
        else //Find an appropriate tutorial and sets the ID to that
        {
            decideTutorial();
        }

        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Fetching stages");
        fetchStages();
    }

    public void startLesson()
    {
        setUpLesson();

        this.iStage = 0;

        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Signal next stage");
        nextStage();

        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Lesson ended");
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
            sql = "Select * FROM Tutorials WHERE Tutorials.Compulsory = 1";
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            if (resultSet.next())
            {
                iTutorialID = resultSet.getInt("TutorialID");
            }
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

        this.tutorial.iTutorialID = iTutorialID;

        if (iTutorialID == -1)
            return false;
        else
            return true;
    }

    private void decideTutorial()
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
            iSecondLowestRating = student.iScoreDetailing;
        }

        //-----------------------------------------------------------------
        //-------------Selecting all tutorials that are in use-------------
        //-----------------------------------------------------------------
        Tutorial[] tutorials;

        int iCount = 0;

        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            //Compiles the command to fetch tutorials
            sql = "Select * FROM Tutorials WHERE Tutorials.InUse = 1";
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            while (resultSet.next())
            {
                iCount++;
            }

            tutorials = new Tutorial[iCount];

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            for (int i = 0 ; i < iCount ; i++)
            {
                tutorials[i].iTutorialID = resultSet.getInt("TutorialID");
            }



            //Compiles the command to fetch category difficulties
            sql = "Select * FROM Tutorials,CategoryPoints WHERE Tutorials.InUse = 1 AND Tutorials.TutorialID = CategoryPoints.TutorialsID";
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            while (resultSet.next())
            {
                int i;
                //Goes through until it finds the linked tutorial
                for (i = 0 ; i < iCount ; i++)
                {
                    if (tutorials[i].iTutorialID == resultSet.getInt("CategoryPoints.TutorialID"))
                    {
                        switch (resultSet.getString("Category"))
                        {
                            case "tpll":
                                tutorials[i].categoryDifficulties[0] = resultSet.getInt("CategoryPoints.Difficulty");
                                break;
                            case "we":
                                tutorials[i].categoryDifficulties[1] = resultSet.getInt("CategoryPoints.Difficulty");
                                break;
                            case "terraforming":
                                tutorials[i].categoryDifficulties[2] = resultSet.getInt("CategoryPoints.Difficulty");
                                break;
                            case "colouring":
                                tutorials[i].categoryDifficulties[3] = resultSet.getInt("CategoryPoints.Difficulty");
                                break;
                            case "detailing":
                                tutorials[i].categoryDifficulties[4] = resultSet.getInt("CategoryPoints.Difficulty");
                                break;
                            default:

                        }
                    }
                }
            }
        }
        catch(SQLException se)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - SQL Error fetching all in use tutorials");
            se.printStackTrace();
            tutorials = new Tutorial[iCount];
        }
        catch (Exception e)
        {
            e.printStackTrace();
            tutorials = new Tutorial[iCount];
        }

        //----------------------------------------------------------------
        //------------Decide the most relevant tutorial to use------------
        //----------------------------------------------------------------

        //We know the rating of each tutorial and the categories we want
        float fBiggestRelevance = 0;
        int iIndexBiggestRelevance = 0;

        //Go through each tutorial, and score each with relevance, and keep track of the most relevant tutorial
        for (int i = 0 ; i < iCount ; i++)
        {
            float fRelevance = 2 * tutorials[i].categoryDifficulties[iIndexLowestRating - 1];
            fRelevance = fRelevance + tutorials[i].categoryDifficulties[iIndexSecondLowestRating - 1];
            if (fRelevance > fBiggestRelevance) {
                fBiggestRelevance = fRelevance;
                iIndexBiggestRelevance = i;
            }
        }

        iTutorialIndex = iIndexBiggestRelevance;
        iTutorialID = tutorials[iTutorialIndex].iTutorialID;
    }

    private void fetchStages()
    {
        //Gets a list of all of the stages of the specified tutorial and loads each with the relevant data.
        //List is in order of stage 1 1st
        this.Stages = Stage.fetchStagesByTutorialID(student.player, plugin, this);
    }

    protected void nextStage()
    {
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Next stage");

        this.iStage++;
        Stage stage;
        while (this.iStage <= Stages.size())
        {
            stage = Stages.get(this.iStage-1);
            stage.startStage();
        }

        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] All stages complete");

        endLesson();
    }

    protected void continueStage()
    {
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Next stage");

        Stage stage;
        while (this.iStage <= Stages.size())
        {
            stage = Stages.get(this.iStage-1);
            stage.startStage();
        }

        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] All stages complete");

        endLesson();
    }

    private void endLesson()
    {
        //Calculate final scores


        //Then store in DB


        //And trigger the scoreboard to refresh
    }

    public static void main(String[] args)
    {
     //   Lesson lesson = new Lesson()
    }

    public void fetchCurrentFromUUID()
    {
        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            //Compiles the command to fetch the lesson in progress
            sql = "Select * FROM Lessons WHERE UUID = '" +student.player.getUniqueId() +"' AND Finished = 0";
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            if (resultSet.next())
            {
                this.iTutorialID = resultSet.getInt("TutorialID");
                this.iStage = resultSet.getInt("StageAt");
                this.iStep = resultSet.getInt("StepAt");
                this.iLocationID = resultSet.getInt("StepAt");
            }
        }
        catch(SQLException se)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - SQL Error fetching current lesson for "+student.player.getName() +": "+student.player.getUniqueId());
            se.printStackTrace();
            this.iTutorialID = -1;
        }

    }
}
