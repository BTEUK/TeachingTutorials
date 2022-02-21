package teachingtutorials.tutorials;

import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.fundamentalTasks.Task;

import java.util.ArrayList;

public class Step
{
    private Player player;
    private TeachingTutorials plugin;
    private Stage parentStage;
    public boolean bStepFinished;

    //Groups are completed asynchronously.
    //Tasks in groups are completed synchronously
    private ArrayList<Group> groups = new ArrayList<>();

    public Step(Player player, TeachingTutorials plugin, Stage parentStage)
    {
        this.player = player;
        this.plugin = plugin;
        this.parentStage = parentStage;
        this.bStepFinished = false;
    }

    private void initialiseStep()
    {
        Group group = new Group(player, plugin, this, 3);
    }

    public void startStep()
    {
        initialiseStep();

        int i;
        int iGroups = groups.size();

        for (i = 0 ; i < iGroups ; i++)
        {
            groups.get(i).initialRegister();
        }
    }

    protected void groupFinished(int iGroupNo)
    {
        int i;
        int iGroups = groups.size();

        boolean bAllGroupsFinished = true;

        for (i = 0 ; i < iGroups ; i++)
        {
            if (!groups.get(i).groupFinished)
            {
                bAllGroupsFinished = false;
                break;
            }
        }

        if (bAllGroupsFinished == true)
        {
            this.bStepFinished = true;
            parentStage.nextStep();
        }
    }
}
