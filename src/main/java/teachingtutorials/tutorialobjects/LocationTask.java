package teachingtutorials.tutorialobjects;

import teachingtutorials.TeachingTutorials;
import teachingtutorials.tutorialplaythrough.fundamentalTasks.FundamentalTaskType;
import teachingtutorials.tutorialplaythrough.fundamentalTasks.Task;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

/**
 * A type of task where the location information is known
 */
public class LocationTask extends Task
{
    /** The difficulty of the location task */
    private float fDifficulty;
    private float[] fDifficulties;

    /** The answers of the location task */
    private String szAnswers;

    /** The locationID of the location task */
    private final int iLocationID;

    /**
     * Constructs a location task during a lesson playthrough
     * @param type The type of the task
     * @param iTaskID The ID of the task in the DB
     * @param iOrder The order in which the task should be completed within its parent group
     * @param szDetails The extra details associated with the task
     * @param group A reference to the parent group of the task
     * @param iLocationID A copy of the locationID for the location with which the Location Task is associated
     * @param szAnswers The answers
     * @param fDifficulty The difficulty
     */
    public LocationTask(FundamentalTaskType type, int iTaskID, int iOrder, String szDetails, Group group, int iLocationID, String szAnswers, float fDifficulty)
    {
        super(type, iTaskID, iOrder, szDetails, group);
        this.iLocationID = iLocationID;
        this.szAnswers = szAnswers;
        this.fDifficulty = fDifficulty;
    }

    /**
     * Constructs a location task during a new location playthrough
     * @param task A reference to the task
     * @param iLocationID A copy of the locationID for the location with which the Location Task is associated
     */
    public LocationTask(Task task, int iLocationID)
    {
        super(task.type, task.iTaskID, task.iOrder, task.szDetails, task.getParentGroup());
        this.iLocationID = iLocationID;
    }

    /**
     * Sets the difficulties
     * @param fDifTpll
     * @param fDifWE
     * @param fDifColour
     * @param fDifDetail
     * @param fDifTerra
     */
    public void setDifficulties(float fDifTpll, float fDifWE, float fDifColour, float fDifDetail, float fDifTerra)
    {
        this.fDifficulties = new float[]{fDifTpll, fDifWE, fDifColour, fDifDetail, fDifTerra};
    }

    /**
     *
     * @return The answer
     */
    public String getAnswer()
    {
        return szAnswers;
    }

    /**
     *
     * @return The difficulty of this location task
     */
    public float getDifficulty()
    {
        return fDifficulty;
    }

    /**
     * Sets the answers
     * @param szAnswers The answers to set
     */
    public void setAnswers(String szAnswers)
    {
        this.szAnswers = szAnswers;
    }

    /**
     * Uses the data within this object to create a new location task in the DB
     * @param plugin A reference to the instance of the TeachingTutorials plugin
     * @return Whether the LocationTask was added successfully
     */
    public boolean storeNewData(TeachingTutorials plugin)
    {
        boolean bSuccess = false;

        String sql;
        Statement SQL = null;

        try
        {
            sql = "INSERT INTO `LocationTasks` (`LocationID`, `TaskID`, `Answers`, `TpllDifficulty`, `WEDifficulty`, `ColouringDifficulty`, `DetailingDifficulty`, `TerraDifficulty`) " +
                    "VALUES (" +iLocationID+", " +iTaskID+", '" +szAnswers+"'";

            SQL = plugin.getConnection().createStatement();

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
            plugin.getLogger().log(Level.SEVERE, "SQL - SQL Error adding LocationTask", se);
            bSuccess = false;
        }
        catch (Exception e)
        {
            plugin.getLogger().log(Level.SEVERE, "Non SQL Error adding LocationTask", e);
            bSuccess = false;
        }
        return bSuccess;
    }
}
