package teachingtutorials;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import teachingtutorials.fundamentalTasks.Chat;
import teachingtutorials.fundamentalTasks.Task;
import teachingtutorials.fundamentalTasks.TpllListener;
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

        String szArchiveFolder = getDataFolder().getAbsolutePath()+"/TutorialArchives";
        File archiveFolder = new File(szArchiveFolder);
        if (!archiveFolder.exists())
        {
            archiveFolder.mkdir();
        }

        //Goes through the folder and if files are found, interpret it
        //Folder is sent as it is needed
        while (folder.list().length != 0)
        {
            File file = folder.listFiles()[0];
            interpretNewTutorial(file);
        }

        //---------------------------------------
        //--------------Create GUIs--------------
        //---------------------------------------

        MainMenu.initialize();

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
        Tutorial tutorial = new Tutorial();
        int i;
        int iLines = 0;

        try
        {
            Scanner szFile = new Scanner(file);
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
        }
        catch (Exception e)
        {
            return;
        }

        i = 0;

        //Gets the tutorial name and author name
        String[] szFields = szLines[i].split(",");
        if (szFields.length < 1 || szFields.length > 2)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"The tutorial name and author line is incorrectly formatted");
            return;
        }
        tutorial.szTutorialName = szFields[0];
        if (szFields.length == 2)
        {
            tutorial.szAuthor = szFields[1];
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
                szType = "Step";
                Step step = new Step(szLines[i].replace("(",""));
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
                if (szFields.length != 6)
                {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Tutorial config is not configured correctly, line: "+(i+1));
                    return;
                }
                for (int j = 1 ; j < 6 ; j++)
                {
                    if (!szFields[i].matches("([0-9]|[1-9][0-9]|100)"))
                    {
                        Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Tutorial config is not configured correctly." +
                                "Difficulty ratings must be between 0 and 100. Line: "+(i+1));
                        return;
                    }
                }
                szType = "Group";
                Group group = new Group(szFields[0].replace("(",""), Integer.parseInt(szFields[1]), Integer.parseInt(szFields[2]), Integer.parseInt(szFields[3]), Integer.parseInt(szFields[4]), Integer.parseInt(szFields[5]));
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
        addNewTutorialToDB(tutorial);

        //Moves file to the archive folder
        file.renameTo(new File(getDataFolder().getAbsolutePath()+"/TutorialArchives"));
    }

    public void addNewTutorialToDB(Tutorial tutorial)
    {

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
                if (iCount != 1)
                {
                    //  Bukkit.getConsoleSender().sendMessage("[TeachingTutorials]" +ChatColor.AQUA + "Created tables");
                    bSuccess = false;
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
