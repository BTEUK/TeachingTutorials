package teachingtutorials.tutorials;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.fundamentalTasks.Task;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class Group
{
    private final int groupID;
    private int taskNo;

    public boolean groupFinished;

    private Player player;
    private TeachingTutorials plugin;
    protected Step parentStep;

    private ArrayList<Task> tasks = new ArrayList<>();

    public Group(int groupID, Player player, TeachingTutorials plugin, Step parentStep)
    {
        this.player = player;
        this.plugin = plugin;
        this.parentStep = parentStep;
        this.groupID = groupID;
        this.groupFinished = false;
    }

    //Where to fetch tasks, where to initialise them etc

    public void fetchAndInitialiseTasks()
    {
        tasks = Task.fetchTasks(plugin,this, parentStep.parentStage.lesson.iLocationID, parentStep.parentStage.lesson.student.player);
    }

    public void initialRegister()
    {
        fetchAndInitialiseTasks();

        if (tasks.size() > 0)
        { //Need to send this group object to each task so it can call task ended or whatever
            tasks.get(0).register();
            taskNo = 1;
            //Tasks unregister themselves once complete
        }
        else
        {
            //Signal that group is complete before it even started
            groupFinished = true;
            parentStep.groupFinished();
        }
    }

    protected void taskFinished()
    {
        //taskNo is that of the previous, so it is the correct index of the next
        if (taskNo >= tasks.size()) //If the task was the last one in the group
        {
            //Signal that group is complete
            groupFinished = true;
            parentStep.groupFinished();
        }
        else //Registers the next task
        {
            tasks.get(taskNo).register();
        }
    }

    public static ArrayList<Group> fetchGroupsByStepID(Player player, TeachingTutorials plugin, Step step)
    {
        ArrayList<Group> groups = new ArrayList<>();

        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            //Compiles the command to fetch groups
            sql = "Select * FROM Groups WHERE StepID = "+step.iStepID;
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            while (resultSet.next())
            {
                Group group = new Group(resultSet.getInt("GroupID"), player, plugin, step);
                groups.add(group);
            }
        }
        catch(SQLException se)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - SQL Error fetching Groups by StepID");
            se.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return groups;
    }
}
