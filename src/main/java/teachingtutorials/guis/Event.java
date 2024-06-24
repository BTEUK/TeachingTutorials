package teachingtutorials.guis;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.utils.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Handles tutorial Events. Data fields refer to those of the events table of the database
 */
public class Event
{
    public Player player;
    public EventType eventType;
    public Timestamp timestamp;
    public int iData;

    /**
     * Constructs a new event
     * @param player The player object for the event
     * @param eventType The event type
     * @param timestamp The time that the event was added to the DB
     * @param iData The data of the event - The tutorial ID of the tutorial the event includes a reference to
     */
    public Event(Player player, EventType eventType, Timestamp timestamp, int iData)
    {
        this.player = player;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.iData = iData;
    }

    /**
     * Gets a list of the current events in the events table
     * @param dbConnection The database connection object
     * @return A list of events
     */
    public static ArrayList<Event> getLatestEvents(DBConnection dbConnection)
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
            SQL = dbConnection.getConnection().createStatement();

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
                int iData = resultSet.getInt("Data");

                //Creates an event object to store the details
                Event event = new Event(player, eventType, timestamp, iData);

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

    //SQL Updates

    /**
     * Removes this event from the database
     */
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

    /**
     * Adds an event to the events table
     * @param eventType The event that is to take place
     * @param userUUID The UUID of the player undergoing to event
     * @param iData Any data - refers to the TutorialID of any tutorials the event refers to
     * @param dbConnection The database connection object
     * @return True if the event was successfully added to the DB, false if not
     */
    public static boolean addEvent(EventType eventType, UUID userUUID, int iData, DBConnection dbConnection)
    {
        String sql;
        Statement SQL = null;

        int iCount = 0;

        try
        {
            SQL = dbConnection.getConnection().createStatement();
            sql = "INSERT INTO `Events` (`UUID`,`EventName`,`Data`) VALUES('" +userUUID +"', '"+eventType.toString()+"', "+iData +")";
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"" +sql);
            iCount = SQL.executeUpdate(sql);
        }
        catch (SQLException se)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - SQL Error adding event");
            se.printStackTrace();
            return false;
        }
        catch (Exception e)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - Non-SQL Error adding event");
            e.printStackTrace();
            return false;
        }
        if (iCount != 1)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"[TeachingTutorials] - SQL - Update failed, count not equal to 1");
            return false;
        }
        else
            return true;
    }
}
