package teachingtutorials.tutorialplaythrough.fundamentalTasks;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.tutorialobjects.Group;
import teachingtutorials.tutorialplaythrough.GroupPlaythrough;
import teachingtutorials.tutorialobjects.LocationTask;
import teachingtutorials.tutorialplaythrough.PlaythroughTask;
import teachingtutorials.utils.DBConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;

/**
 * The Task class represents a Task that a player has to complete. Each type of fundamental task has its own class that
 * extends this. This class holds the information of the task as contained in the Tasks table of the database, and
 * contains database utilities and methods for common processes. It also handles some virtual block mechanics.
 */
public class Task
{
    //All of these values are final so I make them public for ease

    /**
     * The unique TaskID of this task
     */
    public final int iTaskID;

    /**
     * The type of the task, e.g, "command". Can only be any one of the values of FundamentalTaskType
     */
    public final FundamentalTaskType type;

    /**
     * The number of the task in the group. 1 is the first task in the group.
     */
    public final int iOrder;

    /**
     * Any extra information about the task as given in the database
     */
    public final String szDetails;

    /**
     * Stores a reference to the parent group.
     */
    protected final Group group;

    /**
     * Constructs a task from the DB
     * @param type The type of the task
     * @param iTaskID The ID of the task as in the DB
     * @param iOrder The order within the group which this task should be completed - 1 indexed
     * @param szDetails The details of the task
     * @param group The group which this task is a member of
     */
    public Task(FundamentalTaskType type, int iTaskID, int iOrder, String szDetails, Group group)
    {
        this.type = type;
        this.szDetails = szDetails;
        this.iTaskID = iTaskID;
        this.iOrder = iOrder;
        this.group = group;
    }

    /**
     * Constructs a task whilst creating a new tutorial
     * @param type The type of the task
     * @param iOrder The order within the group which this task should be completed - 1 indexed
     * @param szDetails The details of the task
     * @param group The group which this task is a member of
     */
    public Task(FundamentalTaskType type, int iOrder, String szDetails, Group group)
    {
        this.type = type;
        this.szDetails = szDetails;
        this.iTaskID = -1;
        this.iOrder = iOrder;
        this.group = group;
    }

    public Group getParentGroup()
    {
        return group;
    }

    /**
     * Fetches a list of PlaythroughTasks for a particular group and location and returns a PlaythroughTask ArrayList.
     * @param plugin An instance of the TeachingTutorials plugin
     * @param dbConnection A database connection pointing to the database to fetch the data from
     * @param iLocationID The location ID of the location for which to get the tasks for
     * @param parentGroupPlaythrough The parent group of the tasks
     * @param player A reference to the player for whom these tasks are for
     * @return An ArrayList of PlaythroughTasks for tasks belonging to the group defined by parentGroup
     */
    public static ArrayList<PlaythroughTask> fetchTasksForLocation(TeachingTutorials plugin, DBConnection dbConnection, int iLocationID, GroupPlaythrough parentGroupPlaythrough, Player player)
    {
        //Initialises the list of tasks
        ArrayList<PlaythroughTask> tasks = new ArrayList<>();

        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            //Compiles the command to fetch the tasks and answers
            sql = "Select * FROM LocationTasks,Tasks WHERE LocationTasks.LocationID = "+iLocationID +" AND Tasks.GroupID = "+ parentGroupPlaythrough.getGroup().getGroupID() +" AND Tasks.TaskID = LocationTasks.TaskID ORDER BY 'Order' ASC";
            SQL = dbConnection.getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            while (resultSet.next())
            {
                int iTaskID = resultSet.getInt("Tasks.TaskID");
                String szType = resultSet.getString("Tasks.TaskType");
                int iOrder = resultSet.getInt("Tasks.Order");
                String szDetails = resultSet.getString("Tasks.Details");
                String szAnswers = resultSet.getString("LocationTasks.Answers");

                //The scoring, difficulty and rating system is not utilised in this release, so it can be mostly ignored
                float fTpllDifficulty = Float.parseFloat(resultSet.getString("LocationTasks.TpllDifficulty"));
                float fWEDifficulty = Float.parseFloat(resultSet.getString("LocationTasks.WEDifficulty"));
                float fColouringDifficulty = Float.parseFloat(resultSet.getString("LocationTasks.ColouringDifficulty"));
                float fDetailingDifficulty = Float.parseFloat(resultSet.getString("LocationTasks.DetailingDifficulty"));
                float fTerraDifficulty = Float.parseFloat(resultSet.getString("LocationTasks.TerraDifficulty"));

                float[] fDifficulties = new float[]{fTpllDifficulty, fWEDifficulty, fColouringDifficulty, fDetailingDifficulty, fTerraDifficulty};

                //Extract the task type
                FundamentalTaskType taskType = null;
                try
                {
                    taskType = FundamentalTaskType.valueOf(szType);
                }
                catch (IllegalArgumentException e)
                {
                    plugin.getLogger().log(Level.SEVERE, "A task was fetched which had an invalid task type value");
                }

                LocationTask locationTask;

                //Creates the correct child class depending on the task type, and adds details in
                switch (taskType)
                {
                    case tpll:
                        locationTask = new LocationTask(FundamentalTaskType.selection, iTaskID, iOrder, szDetails, parentGroupPlaythrough.getGroup(), iLocationID, szAnswers, fDifficulties);
                        Tpll tpll = new Tpll(plugin, player, locationTask, parentGroupPlaythrough);
                        tasks.add(tpll);
                        break;
                    case selection:
                        locationTask = new LocationTask(FundamentalTaskType.selection, iTaskID, iOrder, szDetails, parentGroupPlaythrough.getGroup(), iLocationID, szAnswers, fDifficulties);
                        Selection selection = new Selection(plugin, player, locationTask, parentGroupPlaythrough);
                        tasks.add(selection);
                        break;
                    case command:
                        locationTask = new LocationTask(FundamentalTaskType.command, iTaskID, iOrder, szDetails, parentGroupPlaythrough.getGroup(), iLocationID, szAnswers, fDifficulties);
                        Command command = new Command(plugin, player, locationTask, parentGroupPlaythrough, tasks);
                        tasks.add(command);
                        break;
                    case chat:
                        locationTask = new LocationTask(FundamentalTaskType.chat, iTaskID, iOrder, szDetails, parentGroupPlaythrough.getGroup(), iLocationID, szAnswers, fDifficulties);
                        Chat chat = new Chat(plugin, player, locationTask, parentGroupPlaythrough);
                        tasks.add(chat);
                        break;
                    case place:
                        locationTask = new LocationTask(FundamentalTaskType.place, iTaskID, iOrder, szDetails, parentGroupPlaythrough.getGroup(), iLocationID, szAnswers, fDifficulties);
                        Place place = new Place(plugin, player, locationTask, parentGroupPlaythrough);
                        tasks.add(place);
                        break;
                }
            }
        }
        catch (SQLException se)
        {
            plugin.getLogger().log(Level.SEVERE, ChatColor.RED + "SQL - SQL Error fetching Tasks with answers by LocationID and GroupID", se);
        }
        catch (Exception e)
        {
            plugin.getLogger().log(Level.SEVERE, ChatColor.RED + "SQL - Non SQL Error fetching Tasks with answers by LocationID and GroupID", e);
        }
        return tasks;
    }

    /**
     * Fetches all the tasks without their answers for a particular group and returns a Task ArrayList.
     * This can be used when creating a new location.
     * @param plugin An instance of the TeachingTutorials plugin
     * @param dbConnection A database connection pointing to the database to fetch the data from
     * @param parentGroupPlaythrough The parent group of the tasks
     * @param player A reference to the player for whom these tasks are for
     * @return An ArrayList of Tasks
     */
    public static ArrayList<PlaythroughTask> fetchTasksWithoutAnswers(TeachingTutorials plugin, DBConnection dbConnection, GroupPlaythrough parentGroupPlaythrough, Player player)
    {
        //Initialises the list of tasks
        ArrayList<PlaythroughTask> tasks = new ArrayList<>();

        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            plugin.getLogger().log(Level.INFO, ChatColor.AQUA +"Searching for tasks in group with group ID: "+ parentGroupPlaythrough.getGroup().getGroupID());

            //Compiles the command to fetch groups
            sql = "Select * FROM Tasks WHERE GroupID = "+ parentGroupPlaythrough.getGroup().getGroupID() +" ORDER BY 'Order' ASC";
            plugin.getLogger().log(Level.FINE, sql);

            //Creates the statement
            SQL = dbConnection.getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);

            //Goes through the result set and initialises the relevant Task objects
            while (resultSet.next())
            {
                int iTaskID = resultSet.getInt("TaskID");
                String szType = resultSet.getString("TaskType");
                int iOrder = resultSet.getInt("Order");
                String szDetails = resultSet.getString("Details");

                //Extract the task type
                FundamentalTaskType taskType = null;
                try
                {
                    taskType = FundamentalTaskType.valueOf(szType);
                }
                catch (IllegalArgumentException e)
                {
                    plugin.getLogger().log(Level.SEVERE, "A task was fetched which had an invalid task type value");
                }

                Task task = new Task(taskType, iTaskID, iOrder, szDetails, parentGroupPlaythrough.getGroup());

                //Creates the correct child class depending on the task type, and adds details in
                switch (taskType)
                {
                    case tpll:
                        Tpll tpll = new Tpll(plugin, player, task, parentGroupPlaythrough);
                        tasks.add(tpll);
                        break;
                    case selection:
                        Selection selection = new Selection(plugin, player, task, parentGroupPlaythrough);
                        tasks.add(selection);
                        break;
                    case command:
                        Command command = new Command(plugin, player, task, parentGroupPlaythrough, tasks);
                        tasks.add(command);
                        break;
                    case chat:
                        Chat chat = new Chat(plugin, player, task, parentGroupPlaythrough);
                        tasks.add(chat);
                        break;
                    case place:
                        Place place = new Place(plugin, player, task, parentGroupPlaythrough);
                        tasks.add(place);
                        break;
                }
            }
        }
        catch (SQLException se)
        {
            plugin.getLogger().log(Level.SEVERE, ChatColor.RED + "SQL - SQL Error fetching Tasks by LocationID and GroupID", se);
        }
        catch (Exception e)
        {
            plugin.getLogger().log(Level.SEVERE, ChatColor.RED + "SQL - Non SQL Error fetching Tasks by LocationID and GroupID", e);
        }
        return tasks;
    }
}
