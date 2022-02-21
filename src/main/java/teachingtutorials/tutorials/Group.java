package teachingtutorials.tutorials;

import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.fundamentalTasks.Task;

import java.util.ArrayList;

public class Group
{
    private final int groupNo;
    public boolean groupFinished;

    private Player player;
    private TeachingTutorials plugin;
    private Step parentStep;

    private ArrayList<Task> tasks = new ArrayList<>();

    public Group(Player player, TeachingTutorials plugin, Step parentStep, int groupNo)
    {
        this.player = player;
        this.plugin = plugin;
        this.parentStep = parentStep;
        this.groupNo = groupNo;
        this.groupFinished = false;
    }

    //Where to fetch tasks, where to initialise them etc

    public void initialRegister()
    {
        if (tasks.size() > 0)
        { //Need to send this group object to each task so it can call task ended or whatever
            tasks.get(0).register();
        }
        else
        {
            //TODO: Signal that group is complete
            groupFinished = true;
            parentStep.groupFinished(groupNo);
        }
    }

    protected void taskFinished()
    {

    }
    //NEED to consider where to deregister
}
