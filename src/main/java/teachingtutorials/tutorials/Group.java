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
    //What exactly is this szName variable for?
    private String szName;

    private int groupID;

    //What stage of the group the player is at
    private int taskNo;
    private Task currentTask;

    public boolean groupFinished;

    private Player player;
    private TeachingTutorials plugin;
    public Step parentStep;

    private ArrayList<Task> tasks = new ArrayList<>();

    public Group(int groupID, Player player, TeachingTutorials plugin, Step parentStep)
    {
        this.player = player;
        this.plugin = plugin;
        this.parentStep = parentStep;
        this.groupID = groupID;
        this.groupFinished = false;
    }

    public Group(String szName)
    {
        this.szName = szName;
    }

    public void addTaskCreation(Task task)
    {
        tasks.add(task);
    }

    public ArrayList<Task> getTasks()
    {
        return tasks;
    }
    public int getGroupID()
    {
        return groupID;
    }
    public String getName()
    {
        return this.szName;
    }

    //Where to fetch tasks, where to initialise them etc

    public void fetchAndInitialiseTasks()
    {
        if (this.parentStep.parentStage.bLocationCreation)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"    Fetching tasks without answers");
            tasks = Task.fetchTasksWithoutAnswers(plugin, this, parentStep.parentStage.getPlayer());
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"    Group.FetchAndInitialiseTasks: "+tasks.size() +" tasks found");
        }
        else
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"    Fetching tasks with answers for location with locationID "+parentStep.parentStage.getLocationID() +" and groupID " +this.groupID);
            tasks = Task.fetchTasks(plugin, parentStep.parentStage.getLocationID(), this, parentStep.parentStage.lesson.student.player);
        }
    }

    public void initialRegister()
    {
        fetchAndInitialiseTasks();

        if (tasks.size() > 0)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"    [TeachingTutorials] This group has "+tasks.size() +" tasks");

            currentTask = tasks.get(0);
            //Tasks unregister themselves once complete
            currentTask.register();
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"    [TeachingTutorials] First task registered: "+currentTask.type);

            //Sets the current task number to the first task
            //1 indexed
            taskNo = 1;
        }
        else
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"    [TeachingTutorials] This group had no tasks, group finished");
            //Signal that group is complete before it even started
            groupFinished = true;
            parentStep.groupFinished();
        }
    }

    public void taskFinished()
    {
        //taskNo is that of the previous, so it is the correct index of the next
        //taskNo is 1 indexed
        if (taskNo >= tasks.size()) //If the task was the last one in the group
        {
            //Signal that group is complete
            groupFinished = true;
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"    [TeachingTutorials] GroupID "+groupID +" finished");
            parentStep.groupFinished();
        }
        else //Registers the next task
        {
            currentTask = tasks.get(taskNo);
            //Tasks unregister themselves once complete
            currentTask.register();
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"    [TeachingTutorials] Next task registered: "+currentTask.type);
            taskNo++;
        }
    }

    public void terminateEarly()
    {
        currentTask.unregister();
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
            Bukkit.getConsoleSender().sendMessage(sql);
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            while (resultSet.next())
            {
                Group group = new Group(resultSet.getInt("GroupID"), player, plugin, step);
                groups.add(group);
                Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"    Added group with group ID " +resultSet.getInt("GroupID") +" to step");
            }
        }
        catch(SQLException se)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "    [TeachingTutorials] - SQL - SQL Error fetching Groups by StepID");
            se.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return groups;
    }
}
