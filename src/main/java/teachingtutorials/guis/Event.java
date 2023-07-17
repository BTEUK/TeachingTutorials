package teachingtutorials.guis;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;

import java.sql.*;
import java.util.ArrayList;
import java.util.UUID;

public class Event
{
    public Player player;
    public EventType eventType;
    public Timestamp timestamp;

    public Event(Player player, EventType eventType, Timestamp timestamp)
    {
        this.player = player;
        this.eventType = eventType;
        this.timestamp = timestamp;
    }

    //SQL Fetches
    public static ArrayList<Event> getLatestEvents()
    {
        ArrayList<Event> events = new ArrayList<>();

        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        boolean bDoNotAdd;

        try
        {
            //Compiles the command to fetch steps
            sql = "Select * FROM Events";
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            while (resultSet.next())
            {
                bDoNotAdd = false;

                //Extracts the event details
                Player player = Bukkit.getPlayer(UUID.fromString(resultSet.getString("UUID")));
                if (player == null)
                    continue;
                EventType eventType = EventType.valueOf(resultSet.getString("EventName"));
                Timestamp timestamp = resultSet.getTimestamp("Timestamp");

                //Creates an event object to store the details
                Event event = new Event(player, eventType, timestamp);

                //Go through entire event list and check for any duplicate players and sort them
                for (int i = 0 ; i < events.size() ; i++)
                {
                    Event previousEvent = events.get(i);
                    if (previousEvent.player.getUniqueId().equals(event.player.getUniqueId()))
                    {
                        if (previousEvent.timestamp.before(event.timestamp))
                        {
                            //Delete the old one from the DB
                            previousEvent.remove();

                            //Delete the old one (and replace with the new)
                            events.remove(i);
                        }
                        else
                        {
                            bDoNotAdd = true;

                            //Delete the new one from the DB
                            event.remove();
                        }
                    }
                }

                if (!bDoNotAdd)
                    events.add(event);
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
        return events;
    }

    public void remove()
    {
        String sql;
        Statement SQL = null;

        int iCount;

        try
        {
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Removes the answers
            sql = "Delete FROM Events WHERE UUID = '" +this.player.getUniqueId() +"' AND Timestamp = '"+this.timestamp+"'";
            iCount = SQL.executeUpdate(sql);
        }
        catch (SQLException se)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - SQL Error deleting event");
            se.printStackTrace();
        }
        catch (Exception e)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - Non-SQL Error deleting event");
            e.printStackTrace();
        }
    }
}
