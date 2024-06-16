package teachingtutorials.utils;

import java.sql.*;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import teachingtutorials.TeachingTutorials;

/**
 * @date 10 Aug 2020
 * @time 11:41:25
 */

public class DBConnection
{
    private String DB_CON;

    private String HOST;
    private int PORT;
    public String Database;
    private String USER;
    private String PASSWORD;

    Connection connection = null;

    boolean bIsConnected;

    public void main(String[] args)
    {
        DBConnection test1 = new DBConnection();
        test1.connect();
    }

    // ----------------------------------------------------------------------
    // Constructor
    // ----------------------------------------------------------------------
    public DBConnection()
    {
        reset() ;
        return ;
    }

    //Setters
    private void reset()
    {
        this.bIsConnected = false ;
        return ;
    }

    public void mysqlSetup(TeachingTutorials plugin)
    {
        FileConfiguration config = plugin.getConfig();
        this.HOST = config.getString("MySQL_host");
        this.PORT = config.getInt("MySQL_port");
        this.Database = config.getString("MySQL_database");
        this.USER = config.getString("MySQL_username");
        this.PASSWORD = config.getString("MySQL_password");

        this.DB_CON = "jdbc:mysql://" + this.HOST + ":"
                + this.PORT + "/" + this.Database + "?&useSSL=false&";
    }


    public void externalMySQLSetup(String szHost, int iPort, String szDatabaseName, String szUsername, String szPassword)
    {
        this.HOST = szHost;
        this.PORT = iPort;
        this.Database = szDatabaseName;
        this.USER = szUsername;
        this.PASSWORD = szPassword;

        this.DB_CON = "jdbc:mysql://" + this.HOST + ":"
                + this.PORT + "/" + this.Database + "?&useSSL=false&";
    }


    public boolean connect()
    {
        Bukkit.getConsoleSender().sendMessage("Username: "+this.USER);
       // Bukkit.getConsoleSender().sendMessage("Password: "+this.PASSWORD);
        try
        {
            //	System.out.println(this.getClass().getName() +" : Connecting la la la");
            DriverManager.getDriver(DB_CON);
            connection = DriverManager.getConnection(DB_CON, USER, PASSWORD);
            if (this.connection != null)
            {
                this.bIsConnected = true;
                return true;
            }
            else
                return false;
        }
        catch (SQLException e)
        {
            if (e.toString().contains("Access denied"))
            {
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL Error - Access denied");
                e.printStackTrace();
            }
            else if (e.toString().contains("Communications link failure"))
            {
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL Error - Communications link failure");
            }
            else
            {
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL Error - Other SQLException - "+e.getMessage());
                e.printStackTrace();
            }
            return false;
        }
        catch (Exception e)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - Error - Other Exception whilst connecting to database- "+e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    public void disconnect()
    {
        try
        {
            this.connection.close() ;
            this.bIsConnected = false ;
            //	System.err.println( this.getClass().getName() + ":: disconnected." ) ;
        }
        catch ( SQLException se )
        {
            System.err.println( this.getClass().getName() + ":: SQL error " + se ) ;
            se.printStackTrace() ;
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - DBConnection - SQLException");
        }
        catch ( Exception e )
        {
            System.err.println( this.getClass().getName() + ":: error " + e ) ;
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - DBConnection - Exception");
            e.printStackTrace() ;
        }
        finally
        {
        }
        return ;
    }

    public Connection getConnection()
    {
        try
        {
            if (connection.isValid(0) && connection != null)
            {

            }
            else
            {
                Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] - SQL - Connecting to DB...");
                if (connect())
                    Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN +"[TeachingTutorials] - SQL - Successfully connected to the DB");
                else
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"[TeachingTutorials] - SQL - Failed to connect to the DB");
            }
        }
        catch (SQLException e)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"[TeachingTutorials] - SQL - Failed to get the connection to the DB");
            e.printStackTrace();
        }
        return connection;
    }
}
//End Class

//Created by Mr Singh
//modified by Bluecarpet in London
