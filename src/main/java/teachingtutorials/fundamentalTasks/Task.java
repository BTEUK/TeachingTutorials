package teachingtutorials.fundamentalTasks;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.tutorials.Group;
import teachingtutorials.tutorials.Lesson;
import teachingtutorials.utils.VirtualBlockLocation;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class Task
{
    public int iTaskID;

    /**
     * The type of the task, e.g, "command". This is not enumerated, but may be in the future.
     */
    public String type;

    /**
     * The number of the task in the group. 1 is the first task in the group.
     */
    public int iOrder;

    /**
     * Any extra information about the task as given in the database
     */
    public String szDetails;

    public float fDifficulties[] = new float[5];

    float fDifficulty;
    float fPerformance;
    float fFinalScore;

    Player player;
    TeachingTutorials plugin;
    protected Group parentGroup;

    public boolean bActive;

    //This really ought to be made final
    protected final boolean bCreatingNewLocation;

    /**
     * A list of virtual blocks which are to be displayed as a result of task completion
     */
    protected ConcurrentHashMap<VirtualBlockLocation, BlockData> virtualBlocks;

    public void register()
    {
        bActive = true;
    }

    public Task(int iTaskID)
    {
        this.iTaskID = iTaskID;
        this.bCreatingNewLocation = false;
    }

    public Task(String type, String szDetails)
    {
        this.type = type;
        this.szDetails = szDetails;
        this.bCreatingNewLocation = false;
    }

    /**
     * Used when creating a task object from the database when running a lesson. Any other use may cause issues.
     * @param plugin
     * @param player
     * @param parentGroup
     * @param iTaskID
     * @param iOrder
     * @param szType
     * @param szDetails
     * @param bCreatingNewLocation Whether this task is initialised during a new location creation
     */
    public Task(TeachingTutorials plugin, Player player, Group parentGroup, int iTaskID, int iOrder, String szType, String szDetails, boolean bCreatingNewLocation)
    {
        this.plugin = plugin;
        this.player = player;
        this.parentGroup = parentGroup;
        this.iTaskID = iTaskID;
        this.iOrder = iOrder;
        this.szDetails = szDetails;
        this.type = szType;

        this.virtualBlocks = new ConcurrentHashMap<>();

        this.bCreatingNewLocation = bCreatingNewLocation;
    }

    /**
     * Calculates some scores and then tells the parent group that its active task has just been finished
     */
    protected void taskComplete()
    {
        if (!parentGroup.parentStep.parentStage.bLocationCreation)
        {
            fFinalScore = fDifficulty*fPerformance;

            //Add scores to the totals
            ((Lesson) parentGroup.parentStep.parentStage.tutorialPlaythrough).fTpllScoreTotal = ((Lesson) parentGroup.parentStep.parentStage.tutorialPlaythrough).fTpllScoreTotal + fFinalScore;
            ((Lesson) parentGroup.parentStep.parentStage.tutorialPlaythrough).fTpllDifTotal = ((Lesson) parentGroup.parentStep.parentStage.tutorialPlaythrough).fTpllDifTotal + fDifficulty;
        }
        parentGroup.taskFinished();
    }

    /**
     * Removes virtual blocks and sets task to inactive
     */
    public void unregister()
    {
        removeVirtualBlocks();
        bActive = false;
    }

    /**
     * Adds all of the virtual blocks of this task to the plugin's virtual blocks list
     */
    public void displayVirtualBlocks()
    {
        int iSize = virtualBlocks.size();
        VirtualBlockLocation[] locations = virtualBlocks.keySet().toArray(VirtualBlockLocation[]::new);
        BlockData[] blockData = virtualBlocks.values().toArray(BlockData[]::new);

        for (int i = 0; i < iSize; i++)
        {
            //The put will overwrite any existing virtual blocks at this location
            plugin.virtualBlocks.put(locations[i], blockData[i]);
        }
    }

    /**
     * Removes all of the virtual blocks of this task from the plugin's virtual blocks list
     */
    protected void removeVirtualBlocks()
    {
        int iSize = virtualBlocks.size();
        VirtualBlockLocation[] locations = virtualBlocks.keySet().toArray(VirtualBlockLocation[]::new);

        for (int i = 0; i < iSize; i++)
        {
            plugin.virtualBlocks.remove(locations[i]);
        }
        plugin.getLogger().info(ChatColor.AQUA +"All virtual blocks from task with task id " +iTaskID +" removed");
    }

    public void newLocationSpotHit()
    {

    }

    /**
     * Fetches the tasks and the answers for a particular group and location
     * @param plugin
     * @param iLocationID
     * @param parentGroup
     * @param player
     * @return
     */
    public static ArrayList<Task> fetchTasks(TeachingTutorials plugin, int iLocationID, Group parentGroup, Player player)
    {
        ArrayList<Task> tasks = new ArrayList<>();

        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;
        int iCount = 0;

        try
        {
            //Compiles the command to fetch the tasks and answers
            sql = "Select * FROM LocationTasks,Tasks WHERE LocationTasks.LocationID = "+iLocationID +" AND Tasks.GroupID = "+parentGroup.getGroupID() +" AND Tasks.TaskID = LocationTasks.TaskID ORDER BY 'Order' ASC";
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            while (resultSet.next())
            {
                iCount++;
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

                //Creates the correct child class depending on the task type, and add details in
                switch (szType)
                {
                    case "tpll":
                        TpllListener tpllListener = new TpllListener(plugin, player, parentGroup, iTaskID, iOrder, szDetails, szAnswers, fTpllDifficulty);
                        tasks.add(tpllListener);
                        break;
                    case "selection":
                        Selection selection = new Selection(plugin, player, parentGroup, iTaskID, iOrder, szDetails, szAnswers, fWEDifficulty);
                        tasks.add(selection);
                        break;
                    case "command":
                        Command command = new Command(plugin, player, parentGroup, iTaskID, iOrder, szDetails, szAnswers, fWEDifficulty, tasks);
                        tasks.add(command);
                        break;
                    case "chat":
                        Chat chat = new Chat(plugin, player, parentGroup, iTaskID, iOrder, szDetails, szAnswers, fWEDifficulty);
                        tasks.add(chat);
                        break;
                    case "place":
                        Place place = new Place(plugin, player, parentGroup, iTaskID, iOrder, szDetails, szAnswers, fColouringDifficulty);
                        tasks.add(place);
                        break;
                }
            }
        }
        catch(SQLException se)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - SQL Error fetching Tasks by LocationID and GroupID");
            se.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return tasks;
    }

    public static ArrayList<Task> fetchTasksWithoutAnswers(TeachingTutorials plugin, Group parentGroup, Player player)
    {
        ArrayList<Task> tasks = new ArrayList<>();

        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"      Searching for tasks in group with group ID: "+parentGroup.getGroupID());
            //Compiles the command to fetch groups
            sql = "Select * FROM Tasks WHERE GroupID = "+parentGroup.getGroupID() +" ORDER BY 'Order' ASC";
            Bukkit.getConsoleSender().sendMessage(sql);
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            while (resultSet.next())
            {
                int iTaskID = resultSet.getInt("TaskID");
                String szType = resultSet.getString("TaskType");
                int iOrder = resultSet.getInt("Order");
                String szDetails = resultSet.getString("Details");

                switch (szType)
                {
                    case "tpll":
                        TpllListener tpllListener = new TpllListener(plugin, player, parentGroup, iTaskID, iOrder, szDetails);
                        tasks.add(tpllListener);
                        break;
                    case "selection":
                        Selection selection = new Selection(plugin, player, parentGroup, iTaskID, iOrder, szDetails);
                        tasks.add(selection);
                        break;
                    case "command":
                        Command command = new Command(plugin, player, parentGroup, iTaskID, iOrder, szDetails, tasks);
                        tasks.add(command);
                        break;
                    case "chat":
                        Chat chat = new Chat(plugin, player, parentGroup, iTaskID, iOrder, szDetails);
                        tasks.add(chat);
                        break;
                    case "place":
                        Place place = new Place(plugin, player, parentGroup, iTaskID, iOrder, szDetails);
                        tasks.add(place);
                        break;
                }
            }
        }
        catch (SQLException se)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "      [TeachingTutorials] - SQL - SQL Error fetching Tasks by LocationID and GroupID");
            se.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return tasks;
    }
}
