package teachingtutorials.teachingtutorials.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import teachingtutorials.teachingtutorials.TeachingTutorials;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public class User
{
    public Player player;

    public boolean bHasCompletedOnce;
    public boolean bInLesson;

    public User(Player player)
    {
        this.player = player;
    }

    public User() {

    }

    public void fetchDetailsByUUID()
    {
        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            //Compiles the command to add the new user
            sql = "Select * FROM PLayers WHERE UUID = "+player.getUniqueId();
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the update and returns the amount of records updated
            resultSet = SQL.executeQuery(sql);
            resultSet.next();

            this.bHasCompletedOnce = resultSet.getBoolean("CompletedCompulsory");
            this.bInLesson = resultSet.getBoolean("InLesson");

        }
        catch(SQLException se)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - SQL Error fetching user info by UUID");
            se.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
