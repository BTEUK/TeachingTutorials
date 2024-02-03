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

    //What task of the group the player is at
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

    public Task getCurrentTask()
    {
       return this.currentTask;
    }

    public int getTaskNo()
    {
        return this.taskNo;
    }

    public void displayAllVirtualBlocks()
    {
        //Gets the tasks from the DB
        fetchAndInitialiseTasks();

        int iNumTasks = tasks.size();

        for (int i = 0 ; i < iNumTasks ; i++)
        {
            tasks.get(i).displayVirtualBlocks();
        }
    }

    //Fetches the tasks from the database and initialises them, then adds them to a list which is sent here
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
            tasks = Task.fetchTasks(plugin, parentStep.parentStage.getLocationID(), this, parentStep.parentStage.tutorialPlaythrough.getCreatorOrStudent().player);
        }
    }

    public void initialRegister()
    {
        fetchAndInitialiseTasks();

        if (tasks.size() > 0)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] This group has "+tasks.size() +" tasks");

            currentTask = tasks.get(0);
            //Tasks unregister themselves once complete
            currentTask.register();
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] First task registered: "+currentTask.type);
        }
        else
        {
            //Signal that group is complete before it even started
            groupFinished = true;
            parentStep.groupFinished();
        }
        //Sets the current task number to the first task
        //1 indexed
        taskNo = 1;
    }

    public void taskFinished()
    {
        //taskNo is that of the previous, so it is the correct index of the next
        //taskNo is 1 indexed
        if (taskNo >= tasks.size()) //If the task was the last one in the group
        {
            //Signal that group is complete
            groupFinished = true;
            parentStep.groupFinished();
        }
        else //Registers the next task
        {
            currentTask = tasks.get(taskNo);
            //Tasks unregister themselves once complete
            currentTask.register();
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Next task registered: "+currentTask.type);
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
            sql = "Select * FROM `Groups` WHERE `StepID` = "+step.iStepID;
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
