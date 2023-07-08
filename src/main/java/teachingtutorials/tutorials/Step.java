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
    private Group currentGroup;

    private boolean selectionCompleteHold;

    //Groups are completed asynchronously.
    //Tasks in groups are completed synchronously
    public ArrayList<Group> groups = new ArrayList<>();

    //Used for creating a step in a lesson
    public Step(int iStepID, int iStepInStage, String szStepName, Player player, TeachingTutorials plugin, Stage parentStage, String szStepInstructions)
    {
        this.player = player;
        this.plugin = plugin;
        this.parentStage = parentStage;
        this.bStepFinished = false;
        this.iStepID = iStepID;
        this.iStepInStage = iStepInStage;
        this.szName = szStepName;
        this.szStepInstructions = szStepInstructions;
        this.selectionCompleteHold = false;
    }

    public Step(String szName, String szInstructions)
    {
        this.szName = szName;
        this.szStepInstructions = szInstructions;
        this.selectionCompleteHold = false;
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
    public boolean getSelectionCompleteHold()
    {
        return selectionCompleteHold;
    }

    private void fetchAndInitialiseGroups()
    {
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"  Fetching groups of step with ID: "+iStepID);
        groups = Group.fetchGroupsByStepID(player, plugin, this);
    }

    public void holdSelectionComplete()
    {
        selectionCompleteHold = true;

        //Changes the hold back to false in 0.5 seconds
        this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run()
            {
                selectionCompleteHold = false;
            }
        }, 10L);

    }

    public void startStep()
    {
        //Inform console of step starting
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"  [TeachingTutorials] Step "+iStepInStage +" starting for " +player.getName());

        //Display step title
        Display display = new Display(player, " ");

        //Wait a second before sending it if it is the first step in a stage. We don't want to override the stage title
        if (iStepInStage == 1)
        {
            final Display finalDisplay = display;
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    finalDisplay.Title(ChatColor.AQUA +"Step " +iStepInStage +" - " +szName, 10, 60, 12);
                }
            }, 76L);
        }
        else
        {
            display.Title(ChatColor.AQUA +"Step " +iStepInStage +" - " +szName, 10, 60, 12);
        }

        //TP to location?

        //Displays the step instructions as a message
        display = new Display(player, szStepInstructions);
        display.Message();

        //Fetches the details of groups and stores them in memory
        fetchAndInitialiseGroups();
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"  [TeachingTutorials] " +groups.size() +" groups fetched");

        //If a location is being created, groups are made synchronous rather than asynchronous
        if (parentStage.bLocationCreation)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"  [TeachingTutorials] Registered group "+iGroupInStepLocationCreation +" of step");
            //Register the start of the first group
            currentGroup = groups.get(0);
            iGroupInStepLocationCreation = 1;
            currentGroup.initialRegister();
        }
        else
        {
            //Register the start of all groups
            int i;
            int iGroups = groups.size();

            for (i = 0; i < iGroups; i++)
            {
                Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"  [TeachingTutorials] Registering group "+(i+1));
                groups.get(i).initialRegister();
                Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"  [TeachingTutorials] Registered group "+(i+1));
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
            {
                bAllGroupsFinished = false;
                Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"  [TeachingTutorials] Registering group "+iGroupInStepLocationCreation);
                iGroupInStepLocationCreation++;
                currentGroup = groups.get(iGroupInStepLocationCreation-1);
                currentGroup.initialRegister();
                Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"  [TeachingTutorials] Registered group "+iGroupInStepLocationCreation);
            }
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
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"  [TeachingTutorials] Step "+iStepInStage +" finished");
            parentStage.nextStep();
        }
    }

    public void terminateEarly()
    {
        if (parentStage.bLocationCreation)
        {
            currentGroup.terminateEarly();
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"  [TeachingTutorials] Unregistered group "+iGroupInStepLocationCreation);
        }

        else
        {
            int i;
            int iGroups = groups.size();
            for (i = 0; i < iGroups; i++)
            {
                Group group = groups.get(i);
                group.terminateEarly();
                Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"  [TeachingTutorials] Unregistered group "+(i+1));
            }
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
                Step step = new Step(resultSet.getInt("StepID"), resultSet.getInt("StepInStage"), resultSet.getString("StepName") ,player, plugin, stage, resultSet.getString("StepInstructions"));
                steps.add(step);
            }
        }
        catch(SQLException se)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "  [TeachingTutorials] - SQL - SQL Error fetching Steps by StageID");
            se.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return steps;
    }
}


