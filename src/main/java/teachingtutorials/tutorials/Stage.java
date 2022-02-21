package teachingtutorials.tutorials;

import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.fundamentalTasks.Task;
import teachingtutorials.fundamentalTasks.TpllListener;

import java.util.ArrayList;

public class Stage
{
    private Player player;
    private TeachingTutorials plugin;
    private int iCurrentStep;
    protected Lesson lesson; //Still want component objects to be able to access it. Or do I?
    public boolean bStageFinished;

    // Tasks are divided into separate groups of tasks (steps)
    // Each step must be completed synchronously but tasks in the step can be done in any order

    // Not true Now ^^^^^^^^^

    private ArrayList<Step> steps = new ArrayList<>();

    //Provision for score recording

    public Stage(Player player, TeachingTutorials plugin, Lesson lesson)
    {
        this.plugin = plugin;
        this.player = player;
        this.lesson = lesson;
        this.bStageFinished = false;
    }

    private void initialiseStage()
    {
        Step step = new Step(player, plugin, this);
    }

    public void startStage()
    {
        initialiseStage();

        //loadTasks()

        //Load the steps

        iCurrentStep = 1;

        //Assume that there is a 1st step


        steps.get(0).startStep();

    }

    //Incomplete
    protected void nextStep()
    {
        iCurrentStep++;
        if (iCurrentStep <= steps.size())
        {
            steps.get(iCurrentStep-1).startStep();
        }
        else
        {
            endStage();
        }
    }

    protected void endStage()
    {
        //Calculate final scores for stage?

        lesson.nextStage();
    }

    //Completely redo
    private void loadTasks()
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
                    TpllListener tpll = new TpllListener(plugin, latitude, longitude, this.player, fMaxPoints);
                    group.add(tpll);
                    break;
            }
        }
    }

}
