package teachingtutorials.tutorials;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.Hologram;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class Step
{
    private String szName;
    private String szStepInstructions;
    private String szInstructionDisplayType;
    private Hologram instructions;
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
    public Step(int iStepID, int iStepInStage, String szStepName, Player player, TeachingTutorials plugin, Stage parentStage, String szStepInstructions, String szInstructionDisplayType)
    {
        this.player = player;
        this.plugin = plugin;
        this.parentStage = parentStage;
        this.bStepFinished = false;
        this.iStepID = iStepID;
        this.iStepInStage = iStepInStage;
        this.szName = szStepName;
        this.szStepInstructions = szStepInstructions;
        this.szInstructionDisplayType = szInstructionDisplayType;
        this.selectionCompleteHold = false;
    }

    //Used for adding a step to the DB
    public Step(String szName, String szInstructionDisplayType, String szInstructions)
    {
        this.szName = szName;
        this.szStepInstructions = szInstructions;
        this.szInstructionDisplayType = szInstructionDisplayType;
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

    public String getInstructionDisplayType()
    {
        return szInstructionDisplayType;
    }

    public boolean getSelectionCompleteHold()
    {
        return selectionCompleteHold;
    }

    private void fetchAndInitialiseGroups()
    {
//        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Fetching groups of step with ID: "+iStepID);
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

        //Displays the step instructions
        Location instructionLocation;
        if (iStepInStage == 1 && parentStage.isFirstStage())
        {
            if (parentStage.bLocationCreation)
                instructionLocation = parentStage.newLocation.getLocation().calculateBukkitStartLocation();
            else
                instructionLocation = parentStage.lesson.location.calculateBukkitStartLocation();
        }
        else
            instructionLocation = player.getLocation();

        switch (szInstructionDisplayType)
        {
            case "hologram":
                display = new Display(player, szStepInstructions);
                instructions = display.Hologram(ChatColor.AQUA +szName, instructionLocation);
                break;
            case "chat":
            default:
                display = new Display(player, szStepInstructions);
                display.Message();
                break;
        }

        //Fetches the details of groups and stores them in memory
        fetchAndInitialiseGroups();
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] " +groups.size() +" groups fetched");

        //If a location is being created, groups are made synchronous rather than asynchronous
        if (parentStage.bLocationCreation)
        {
            //Register the start of the first group
            currentGroup = groups.get(0);
            iGroupInStepLocationCreation = 1;
            currentGroup.initialRegister();
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Registered group "+iGroupInStepLocationCreation +" of step");
        }
        else
        {
            //Register the start of all groups
            int i;
            int iGroups = groups.size();

            for (i = 0; i < iGroups; i++)
            {
                groups.get(i).initialRegister();
                Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Registered group "+(i+1));
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
            {
                bAllGroupsFinished = true;

                //Remove hologram
                if (szInstructionDisplayType.equals("hologram"))
                    instructions.removeHologram();
            }
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
            //Remove hologram
            if (szInstructionDisplayType.equals("hologram"))
                instructions.removeHologram();

            this.bStepFinished = true;
            parentStage.nextStep();
        }
    }

    public void terminateEarly()
    {
        //Remove holograms
        if (szInstructionDisplayType.equals("hologram"))
            instructions.removeHologram();

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
                Step step = new Step(resultSet.getInt("StepID"), resultSet.getInt("StepInStage"), resultSet.getString("StepName") ,player, plugin, stage, resultSet.getString("StepInstructions"), resultSet.getString("InstructionDisplay"));
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


