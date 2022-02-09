package teachingtutorials.teachingtutorials;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import teachingtutorials.teachingtutorials.fundamentalTasks.Task;
import teachingtutorials.teachingtutorials.fundamentalTasks.TpllListener;

import java.util.ArrayList;

//Lesson stores all of the information needed for a lesson and handles importing the tasks from
public class Lesson
{
    Player student;

    // Tasks are divided into separate groups of tasks
    // Each group of tasks must be completed synchronously but tasks in the group can be done in any order
    ArrayList synchronousTasks = new ArrayList<ArrayList<Task>>();

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
                    //TODO: Get the tutorial ID and search for examples in the DB for this tutoria


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



    public static void main(String[] args)
    {
     //   Lesson lesson = new Lesson()
    }
}
