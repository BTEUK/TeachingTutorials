package teachingtutorials.tutorials;

import teachingtutorials.TeachingTutorials;
import teachingtutorials.fundamentalTasks.Task;
import teachingtutorials.utils.User;

import java.util.ArrayList;

//Lesson stores all the information needed for a lesson and handles importing the tasks from
public class Lesson
{
    User student;
    int iCurrentStage;
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

    }

    public void fetchStages()
    {
        Stage stage = new Stage(student.player, plugin, this);
        //Stages.add() etc.
        //Get IDs maybe? and other data
    }

    public void startLesson()
    {
        iCurrentStage = 1;

        //Assume that there is a 1st step

        Stages.get(0).startStage(); //Or initialise?
    }

    //Incomplete
    public void nextStage()
    {
        iCurrentStage++;
        if (iCurrentStage <= Stages.size())
        {
            Stages.get(iCurrentStage-1).startStage(); //Or initialise?
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
