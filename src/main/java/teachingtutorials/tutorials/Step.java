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
        //TP to location?

        Display display = new Display(player, szStepInstructions);
        display.Message();

        fetchAndInitialiseGroups();

        //Register the start of all groups
        int i;
        int iGroups = groups.size();

        for (i = 0; i < iGroups; i++)
        {
            groups.get(i).initialRegister();
        }
    }

    protected void groupFinished()
    {
        int i;
        int iGroups = groups.size();

        boolean bAllGroupsFinished = true;

        //Goes through all groups and checks if one of them is not finished yet
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
          //  parentStage.nextStep();
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


