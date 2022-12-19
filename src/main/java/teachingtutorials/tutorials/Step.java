package teachingtutorials.tutorials;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.utils.Display;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class Step
{
    private String szName;
    private String szStepInstructions;
    private Player player;
    private TeachingTutorials plugin;
    public Stage parentStage;
    public boolean bStepFinished;
    protected int iStepID;
    protected int iStepInStage;
    private int iGroupInStepLocationCreation;

    //Groups are completed asynchronously.
    //Tasks in groups are completed synchronously
    public ArrayList<Group> groups = new ArrayList<>();

    public Step(int iStepID, int iStepInStage, Player player, TeachingTutorials plugin, Stage parentStage, String szStepInstructions)
    {
        this.player = player;
        this.plugin = plugin;
        this.parentStage = parentStage;
        this.bStepFinished = false;
        this.iStepID = iStepID;
        this.iStepInStage = iStepInStage;
        this.szStepInstructions = szStepInstructions;
    }

    public Step(String szName, String szInstructions)
    {
        this.szName = szName;
        this.szStepInstructions = szInstructions;
    }

    //Getters
    public String getName()
    {
        return szName;
    }
    public String getInstructions()
    {
        return szStepInstructions;
    }

    private void fetchAndInitialiseGroups()
    {
        groups = Group.fetchGroupsByStepID(player, plugin, this);
    }

    public void startStep()
    {
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Step "+iStepInStage +" starting");

        //TP to location?

        Display display = new Display(player, szStepInstructions);
        display.Message();

        //Fetches the details of groups and stores them in memory
        fetchAndInitialiseGroups();
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] " +groups.size() +" groups fetched");

        //If a location is being created, groups are made synchronous rather than asynchronous
        if (parentStage.bLocationCreation)
        {
            //Register the start of the first group
            groups.get(0).initialRegister();
            iGroupInStepLocationCreation = 1;
        }
        else
        {
            //Register the start of all groups
            int i;
            int iGroups = groups.size();

            for (i = 0; i < iGroups; i++)
            {
                Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Registering group "+(i+1));
                groups.get(i).initialRegister();
            }
        }
    }

    protected void groupFinished()
    {
        int i;
        int iGroups = groups.size();

        boolean bAllGroupsFinished = true;

        //Different logic needed as location creation groups are performed in sync
        if (parentStage.bLocationCreation)
        {
            //iGroupInStepLocationCreation is 1 indexed
            if (iGroupInStepLocationCreation == groups.size()) //If the current group is the last group
                bAllGroupsFinished = true;
            else
                bAllGroupsFinished = false;
        }
        else
        {
            //Goes through all groups and checks if one of them is not finished yet
            for (i = 0 ; i < iGroups ; i++)
            {
                if (!groups.get(i).groupFinished)
                {
                    bAllGroupsFinished = false;
                    break;
                }
            }
        }

        if (bAllGroupsFinished == true)
        {
            this.bStepFinished = true;
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Step "+iStepInStage +" finished");
            parentStage.nextStep();
        }
    }

    public static ArrayList<Step> fetchStepsByStageID(Player player, TeachingTutorials plugin, Stage stage)
    {
        ArrayList<Step> steps = new ArrayList<>();

        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            //Compiles the command to fetch steps
            sql = "Select * FROM Steps WHERE StageID = "+stage.iStageID +" ORDER BY 'StepInStage' ASC";
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            while (resultSet.next())
            {
                Step step = new Step(resultSet.getInt("StepID"), resultSet.getInt("StepInStage"), player, plugin, stage, resultSet.getString("StepInstructions"));
                steps.add(step);
            }
        }
        catch(SQLException se)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - SQL Error fetching Steps by StageID");
            se.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return steps;
    }
}


