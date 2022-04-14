package teachingtutorials.tutorials;

import teachingtutorials.TeachingTutorials;
import teachingtutorials.fundamentalTasks.Task;
import teachingtutorials.utils.User;

import java.util.ArrayList;

//Lesson stores all the information needed for a lesson and handles importing the tasks from
public class Lesson
{
    User student;
    protected int iTutorialID;
    protected int iLocationID;

    protected int iCurrentStage;
    TeachingTutorials plugin;

    ArrayList<Stage> Stages = new ArrayList<>();

    //Scores in each of the categories

    public Lesson(User player, TeachingTutorials plugin)
    {
        this.plugin = plugin;
        this.student = player;
    }

    public void decideTutorial()
    {
        this.iTutorialID = 1;//Something
    }

    public void fetchStages()
    {
        //Gets a list of all of the stages of the specified tutorial and loads each with the relevant data.
        //List is in order of stage 1 1st
        this.Stages = Stage.fetchStagesByTutorialID(student.player, plugin, this);

        //Stages.add() etc.
        //Get IDs maybe? and other data
    }

    public void startLesson()
    {
        fetchStages();
        iCurrentStage = 0;
        nextStage();
    }

    //Incomplete
    public void nextStage()
    {
        iCurrentStage++;
        if (iCurrentStage <= Stages.size())
        {
            Stages.get(iCurrentStage-1).startStage();
        }
        else
        {
            endLesson();
        }
    }

    public void endLesson()
    {
        //Calculate final scores


        //Then store in DB
    }

    public static void main(String[] args)
    {
     //   Lesson lesson = new Lesson()
    }
}
