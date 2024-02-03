package teachingtutorials.tutorials;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.fundamentalTasks.Task;

import java.sql.SQLException;
import java.sql.Statement;

public class LocationTask extends Task
{
    private String szAnswers;
    private int iLocationID;
    private float[] fDifficulties;

    public LocationTask(int iLocationID, int iTaskID)
    {
        super(iTaskID);
        this.iLocationID = iLocationID;
    }

    public void setDifficulties(float fDifTpll, float fDifWE, float fDifColour, float fDifDetail, float fDifTerra)
    {
        this.fDifficulties = new float[]{fDifTpll, fDifWE, fDifColour, fDifDetail, fDifTerra};
    }

    public void setAnswers(String szAnswers)
    {
        this.szAnswers = szAnswers;
    }

    //Creates a new LocationTask in the database table
    public boolean storeNewData()
    {
        boolean bSuccess = false;

        String sql;
        Statement SQL = null;

        try
        {
            sql = "INSERT INTO `LocationTasks` (`LocationID`, `TaskID`, `Answers`, `TpllDifficulty`, `WEDifficulty`, `ColouringDifficulty`, `DetailingDifficulty`, `TerraDifficulty`) " +
                    "VALUES (" +iLocationID+", " +iTaskID+", '" +szAnswers+"'";

            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            for (int i = 0 ; i <=4 ; i++)
            {
                sql = sql +", "+fDifficulties[i];
            }

            sql = sql +")";
            SQL.executeUpdate(sql);
            bSuccess = true;
        }
        catch (SQLException se)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - SQL Error adding LocationTask");
            se.printStackTrace();
            bSuccess = false;
        }
        catch (Exception e)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] Non SQL Error adding LocationTask");
            e.printStackTrace();
            bSuccess = false;
        }
        return bSuccess;
    }
}
