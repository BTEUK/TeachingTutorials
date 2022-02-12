package teachingtutorials;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import teachingtutorials.guis.MainMenu;
import teachingtutorials.listeners.InventoryClicked;
import teachingtutorials.listeners.PlayerInteract;
import teachingtutorials.listeners.JoinEvent;
import teachingtutorials.utils.DBConnection;
import teachingtutorials.utils.User;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;

public final class TeachingTutorials extends JavaPlugin
{
    static TeachingTutorials instance;
    static FileConfiguration config;

    String sql;

    Statement SQL = null;
    ResultSet resultSet = null;

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

    @Override
    public void onDisable() {
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
            File file = new File("TutorialsDDL.sql");

            fileReader = new FileReader(file);

            bufferedReader = new BufferedReader(fileReader);
            sql = readAll(bufferedReader);
            sql.replace("TeachingTutorials", dbConnection.Database);
            SQL = dbConnection.getConnection().createStatement();

            //    Bukkit.getConsoleSender().sendMessage("[TeachingTutorials] [SQL]: " + sql);

            //Executes the update and returns how many rows were changed
            iCount = SQL.executeUpdate(sql);

            //If only 1 record was changed, success is set to true
            if (iCount == 1)
            {
                //  Bukkit.getConsoleSender().sendMessage("[TeachingTutorials]" +ChatColor.AQUA + "Created tables");
                bSuccess = true;
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
