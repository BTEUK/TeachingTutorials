package teachingtutorials;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import teachingtutorials.fundamentalTasks.Task;
import teachingtutorials.guis.AdminMenu;
import teachingtutorials.guis.CompulsoryTutorialMenu;
import teachingtutorials.guis.CreatorTutorialsMenu;
import teachingtutorials.guis.MainMenu;
import teachingtutorials.listeners.InventoryClicked;
import teachingtutorials.listeners.PlayerInteract;
import teachingtutorials.listeners.JoinEvent;
import teachingtutorials.tutorials.Group;
import teachingtutorials.tutorials.Stage;
import teachingtutorials.tutorials.Step;
import teachingtutorials.tutorials.Tutorial;
import teachingtutorials.utils.DBConnection;
import teachingtutorials.utils.User;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.UUID;

public class TeachingTutorials extends JavaPlugin
{
    static TeachingTutorials instance;
    static FileConfiguration config;

    String sql;

    Statement SQL = null;

    private DBConnection dbConnection;

    public ItemStack slot5;
    public static ItemStack menu;

    public ArrayList<User> players;

    @Override
    public void onEnable()
    {
        // Plugin startup logic
        TeachingTutorials.instance = this;
        TeachingTutorials.config = this.getConfig();
        saveDefaultConfig();

        players = new ArrayList<>();

        //-------------------------------------------------------------------------
        //----------------------------------MySQL----------------------------------
        //-------------------------------------------------------------------------
        boolean bSuccess;

        //Initiate connection
        dbConnection = new DBConnection();

        //Attempt set up from config and connect
        dbConnection.mysqlSetup(this);
        bSuccess = dbConnection.connect();

        //Test whether database connected
        if (bSuccess)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "[Teaching Tutorials] MySQL Connected");

            //Only creates tables if database connected properly
            createTables();
        }

        //----------------------------------------
        //-----------Load New Tutorials-----------
        //----------------------------------------

        //Makes the new tutorials folder if it doesn't already exist
        String szFolder = getDataFolder().getAbsolutePath()+"/NewTutorials";
        File folder = new File(szFolder);
        if (!folder.exists())
        {
            folder.mkdir();
        }

        //Makes the tutorial archive folder if it doesn't already exist
        String szArchiveFolder = getDataFolder().getAbsolutePath()+"/TutorialArchives";
        File archiveFolder = new File(szArchiveFolder);
        if (!archiveFolder.exists())
        {
            archiveFolder.mkdir();
        }

        //Goes through the folder and if files are found, interpret it
        //Folder is sent as it is needed
        for (int i = 0 ; i < folder.list().length ; i++)
        {
            File file = folder.listFiles()[i];
            interpretNewTutorial(file);
        }

        //---------------------------------------
        //--------------Create GUIs--------------
        //---------------------------------------

        MainMenu.initialize();
        AdminMenu.initialize();
        CompulsoryTutorialMenu.initialize();
        CreatorTutorialsMenu.initialize();

        //Create menu item
        menu = new ItemStack(Material.EMERALD);
        ItemMeta meta = menu.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Learning Menu");
        menu.setItemMeta(meta);

        //1 second timer - updates slot
        this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            public void run()
            {
                for (Player p : Bukkit.getOnlinePlayers())
                {
                    //Menu
                    slot5 = p.getInventory().getItem(4);
                    if (slot5 == null)
                    {
                        p.getInventory().setItem(4, menu);
                    }
                    else if (!slot5.equals(menu))
                    {
                        p.getInventory().setItem(4, menu);
                    }
                }
            }
        }, 0L, 20L);

        //---------------------------------------
        //---------------Listeners---------------
        //---------------------------------------

        //Handles welcome message and gamemode
        new JoinEvent(this);
        new PlayerInteract(this);
        new InventoryClicked(this);
    //    new Wand(this);

    }

    private void interpretNewTutorial(File file)
    {
        String[] szLines;

        //Holds all of the information for the new tutorial
        Tutorial tutorial = new Tutorial();

        //Read file into lines and fields
        int i;
        int iLines = 0;
        Scanner szFile;
        try
        {
            //Create scanner
            szFile = new Scanner(file);
            while (szFile.hasNextLine())
            {
                szFile.nextLine();
                iLines++;
            }

            //Reset scanner
            szFile.close();
            szFile = new Scanner(file);
            szLines = new String[iLines];

            for (i = 0 ; i < iLines ; i++)
            {
                szLines[0] = szFile.nextLine();
            }
            szFile.close();
        }
        catch (Exception e)
        {
            return;
        }

        i = 0;

        //Gets the tutorial name and author name
        String[] szFields = szLines[i].split(",");
        if (szFields.length != 7)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"The tutorial name, author and relevance line is incorrectly formatted");
            return;
        }
        else
        {
            tutorial.szTutorialName = szFields[0];

            try
            {
                tutorial.uuidAuthor = UUID.fromString(szFields[1]);
            }
            catch (IllegalArgumentException e)
            {
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Author UUID must be a real UUID");
                return;
            }

            for (int j = 2; j < 7 ; j++)
            {
                if (!szFields[i].matches("([0-9]|[1-9][0-9]|100)"))
                {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Tutorial config is not configured correctly." +
                            "Relevances must be between 0 and 100. Line: "+(i+1));
                    return;
                }
                tutorial.categoryUsage[j-2] = Integer.parseInt(szFields[i]);
            }
        }

        //Holds type of the last line interpreted
        String szType = "";

        //References the stage, step, group that we are currently creating
        Stage lastStage = null;
        Step lastStep = null;
        Group lastGroup = null;

        //Goes through each line and interprets the instructions
        for (i = 1 ; i < iLines ; i++)
        {
            //Stage
            if (szLines[i].startsWith("["))
            {
                szType = "Stage";
                Stage stage = new Stage(szLines[i].replace("[",""));
                tutorial.stages.add(stage);
                lastStage = stage;
            }
            //Step
            else if(szLines[i].startsWith("("))
            {
                if (!(szType.equals("Stage")||szType.equals("Task")))
                {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Tutorial config is not configured correctly, line: "+(i+1));
                    return;
                }
                szFields = szLines[i].split(",");
                if (szFields.length != 2)
                {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Tutorial config is not configured correctly, line: "+(i+1));
                    return;
                }
                szType = "Step";
                Step step = new Step(szFields[0].replace("(",""), szFields[1]);
                lastStage.steps.add(step);
                lastStep = step;
            }
            //Group
            else if(szLines[i].startsWith("{"))
            {
                if (!(szType.equals("Step")||szType.equals("Task")))
                {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Tutorial config is not configured correctly, line: "+(i+1));
                    return;
                }
                szFields = szLines[i].split(",");
                if (szFields.length != 1)
                {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Tutorial config is not configured correctly, line: "+(i+1));
                    return;
                }
                szType = "Group";
                Group group = new Group(szFields[0].replace("(",""));
                lastStep.groups.add(group);
                lastGroup = group;
            }
            //Task
            else if(szLines[i].startsWith("~"))
            {
                if (!(szType.equals("Group")||szType.equals("Task")))
                {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Tutorial config is not configured correctly, line: "+(i+1));
                    return;
                }
                szType = "Task";
                Task task;
                String szTaskType = szLines[i];
                switch (szTaskType)
                {
                    case "tpll":
                    case "selection":
                    case "command":
                    case "place":
                    case "chat":
                        task = new Task(szTaskType);
                        lastGroup.addTaskCreation(task);
                        break;
                    default:
                        Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Invalid task type, line: "+(i+1));
                }
            }
        } //End iteration through lines

        //If it has got to this stage, then the details are all sorted and stored in the tutorial object

        if (addNewTutorialToDB(tutorial))
        {
            //Moves file to the archive folder
            file.renameTo(new File(getDataFolder().getAbsolutePath()+"/TutorialArchives"));
        }
    }

    public boolean addNewTutorialToDB(Tutorial tutorial)
    {
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Inserting new tutorial into DB. Tutorial: "+tutorial.szTutorialName);
        int i, j, k, l;

        int iStages;
        int iSteps;
        int iGroups;
        int iTasks;

        int iTutorialID;
        int iStageID;
        int iStepID;
        int iGroupID;

        String sql;
        Statement SQL = null;
        ResultSet resultSet;

        //Insert the new tutorial into the tutorials table
        try
        {
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();
            sql = "INSERT INTO Tutorials (TutorialName) VALUES ('"+tutorial.szTutorialName+"')";
            SQL.executeUpdate(sql);

            sql = "Select LAST_INSERT_ID()";
            resultSet = SQL.executeQuery(sql);
            resultSet.next();
            iTutorialID = resultSet.getInt(1);
        }
        catch (Exception e)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Could not insert new tutorial into DB. Name: "+tutorial.szTutorialName);
            e.printStackTrace();
            return false;
        }

        //Add the relevances into the DB
        for (i = 0 ; i < 5 ; i++)
        {
            try
            {
                sql = "INSERT INTO CategoryPoints (TutorialID, Category, Relevance) VALUES (" + iTutorialID + ", '" + tutorial.szCategoryEnumsInOrder[i] + "', " +((float) tutorial.categoryUsage[i])/100+ ")";
                SQL.executeUpdate(sql);
            }
            catch (Exception e)
            {
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Could not insert relevance into DB. Tutorial: "+tutorial.szTutorialName);
                e.printStackTrace();
                continue;
            }
        }

        ArrayList<Stage> stages = tutorial.stages;
        iStages = stages.size();

        //Go through stages
        for (i = 0 ; i < iStages ; i++)
        {
            //Insert the new stage into the stages table
            Stage stage = stages.get(i);
            try
            {
                sql = "INSERT INTO Stages (StageName, TutorialID, Order) VALUES ('"+stage.getName()+"', "+iTutorialID+", "+(i+1)+")";
                SQL.executeUpdate(sql);

                sql = "Select LAST_INSERT_ID()";
                resultSet = SQL.executeQuery(sql);
                resultSet.next();
                iStageID = resultSet.getInt(1);
            }
            catch (Exception e)
            {
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Could not insert new stage into DB. Tutorial: "+tutorial.szTutorialName);
                e.printStackTrace();
                continue;
            }

            ArrayList<Step> steps = stage.steps;
            iSteps = steps.size();

            //Go through steps
            for (j = 0 ; j < iSteps ; j++)
            {
                //Insert the new step into the steps table
                Step step = steps.get(j);
                try
                {
                    sql = "INSERT INTO Steps (StepName, StageID, StepInStage, StepInstructions) VALUES ('"+step.getName()+"', "+iStageID+", "+(j+1)+", '"+step.getInstructions()+"')";
                    SQL.executeUpdate(sql);

                    sql = "Select LAST_INSERT_ID()";
                    resultSet = SQL.executeQuery(sql);
                    resultSet.next();
                    iStepID = resultSet.getInt(1);
                }
                catch (Exception e)
                {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Could not insert new step into DB. Tutorial: "+tutorial.szTutorialName);
                    e.printStackTrace();
                    continue;
                }

                ArrayList<Group> groups = step.groups;
                iGroups = steps.size();
                //Go through groups
                for (k = 0 ; k < iGroups ; k++)
                {
                    //Insert the new group into the groups table
                    Group group = groups.get(k);
                    try
                    {
                        sql = "INSERT INTO Groups (StepID)" +
                                " VALUES (" +iStepID+")";
                        SQL.executeUpdate(sql);

                        sql = "Select LAST_INSERT_ID()";
                        resultSet = SQL.executeQuery(sql);
                        resultSet.next();
                        iGroupID = resultSet.getInt(1);
                    }
                    catch (Exception e)
                    {
                        Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Could not insert new group into DB. Tutorial: "+tutorial.szTutorialName);
                        e.printStackTrace();
                        continue;
                    }

                    ArrayList<Task> tasks = group.getTasks();
                    iTasks = tasks.size();
                    //Go through tasks
                    for (l = 0 ; l < iTasks ; l++)
                    {
                        //Insert the new group into the groups table
                        Task task = tasks.get(l);
                        try
                        {
                            sql = "INSERT INTO Tasks (GroupID, TaskType, Order)" +
                                    " VALUES (" +iGroupID+", '"+task.type+"', "+(l+1)+")";
                            SQL.executeUpdate(sql);
                        }
                        catch (Exception e)
                        {
                            Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Could not insert new task into DB. Tutorial: "+tutorial.szTutorialName);
                            e.printStackTrace();
                            continue;
                        }
                    }

                }
            }

        }
        return true;
    }

    @Override
    public void onDisable()
    {
        // Plugin shutdown logic
    }

    public static TeachingTutorials getInstance()
    {
        return instance;
    }

    public Connection getConnection()
    {
        return (dbConnection.getConnection());
    }

    private boolean createTables()
    {
        boolean bSuccess = false;
        int iCount = -1;

        sql = "";

        FileReader fileReader = null;
        BufferedReader bufferedReader = null;

        try
        {
            this.saveResource("TutorialsDDL.sql", false);

            File file = new File("/home/container/plugins/TeachingTutorials/TutorialsDDL.sql");

            fileReader = new FileReader(file);

            bufferedReader = new BufferedReader(fileReader);
            sql = readAll(bufferedReader);
            sql.replace("TeachingTutorials", dbConnection.Database);

            sql.replace("\n", "");
            String[] statements = sql.split(";");

            for (int i = 0 ; i < statements.length ; i++)
            {
                SQL = dbConnection.getConnection().createStatement();

                //    Bukkit.getConsoleSender().sendMessage("[TeachingTutorials] [SQL]: " + sql);

                //Executes the update and returns how many rows were changed
                iCount = SQL.executeUpdate(statements[i]);

                //If only 1 record was changed, success is set to true
                if (iCount == 1)
                {
                    Bukkit.getConsoleSender().sendMessage("[TeachingTutorials]" +ChatColor.AQUA + "Created tables");
                    bSuccess = true;
                }
            }

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (SQLException se)
        {
          //  Bukkit.getConsoleSender().sendMessage("[TeachingTutorials] - SQL - SQL Error Creating Tables");
            se.printStackTrace();
        }
        catch (Exception e)
        {
         //   Bukkit.getConsoleSender().sendMessage("[TeachingTutorials] - SQL - Error Creating Tables");
            e.printStackTrace();
        }
        finally
        {
            try
            {
                bufferedReader.close();
            }
            catch (Exception e)
            {
            }
            try
            {
                fileReader.close();
            }
            catch (Exception e)
            {
            }
            dbConnection.disconnect();
        }
        return (bSuccess);
    }

    private String readAll(BufferedReader br)
    {
        StringBuilder sb = new StringBuilder("");
        //   int cp;
        try
        {
            String line;
            line = br.readLine();
            while (line != null)
            {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
            sb = new StringBuilder();
        }
        return sb.toString();
    }
}
