package teachingtutorials;

import org.bukkit.entity.Player;
import teachingtutorials.fundamentalTasks.Task;
import teachingtutorials.fundamentalTasks.TpllListener;

import java.util.ArrayList;

//Lesson stores all of the information needed for a lesson and handles importing the tasks from
public class Lesson
{
    Player student;
    int iStage;

    // Tasks are divided into separate groups of tasks
    // Each group of tasks must be completed synchronously but tasks in the group can be done in any order
    ArrayList<ArrayList<Task>> synchronousTasks = new ArrayList<ArrayList<Task>>();

    public Lesson(Player player)
    {
        this.student = player;
    }

    public void loadTasks()
    {
        int i;
        int iGroups = 10;

        for (i = 0 ; i < iGroups ; i++)
        {
            ArrayList<Task> group = new ArrayList<Task>();
            String[] data = {"0.2","51.3","f","2"};

            switch (data[0].toLowerCase())
            {
                case "tpll":
                    //TODO: Get the tutorial ID and search for examples in the DB for this tutorial
                    //Should always start of with like a 0.3 difficulty then if they do crap find a 0.1 etc


                    double latitude = 0;
                    double longitude = 0;
                    float fMaxPoints = 0;
                    //Array: Lat, long, max points
                    TpllListener tpll = new TpllListener(new TeachingTutorials(), latitude, longitude, this.student, fMaxPoints);
                    group.add(tpll);
                    break;
            }
        }
    }

    public void startLesson()
    {
        iStage = 1;
        ArrayList<Task> initialTasks = synchronousTasks.get(iStage-1);
        int iSteps1 = initialTasks.size();
        for (int i = 0 ; i < iSteps1 ; i++)
        {
            initialTasks.get(i).register();
        }
    }
    //Need some way of knowing when an ssyncrnous group / stage is finished
    //Maybe create a Stage class and have registering in there?

    public void nextStage()
    {
        iStage++;
        if (iStage <= synchronousTasks.size())
        {
            ArrayList<Task> initialTasks = synchronousTasks.get(iStage-1);
            int iSteps1 = initialTasks.size();
            for (int i = 0 ; i < iSteps1 ; i++)
            {
                initialTasks.get(i).register();
            }
        }
        else
        {
            endLesson();
        }
    }

    public void endLesson()
    {

    }

    public static void main(String[] args)
    {
     //   Lesson lesson = new Lesson()
    }
}
