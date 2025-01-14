package teachingtutorials.tutorialobjects;

import teachingtutorials.TeachingTutorials;
import teachingtutorials.tutorialplaythrough.fundamentalTasks.FundamentalTaskType;
import teachingtutorials.tutorialplaythrough.fundamentalTasks.Task;
import teachingtutorials.utils.Category;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

/**
 * A type of task where the location information is known
 */
public class LocationTask extends Task
{
    /** The difficulties of the location task, in the same order as the categories in the Category enum */
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

        this.fDifficulties = new float[]{0f, 0f, 0f, 0f, 0f};
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
     * Sets the difficulty in the category specified
     * @param category The category to set the difficulty in
     */
    public void setDifficulty(Category category, float fDifficulty)
    {
        switch (category)
        {
            case tpll -> fDifficulties[0] = fDifficulty;
            case worldedit -> fDifficulties[1] = fDifficulty;
            case colouring -> fDifficulties[2] = fDifficulty;
            case detail -> fDifficulties[3] = fDifficulty;
            case terraforming -> fDifficulties[4] = fDifficulty;
        }
    }

    /**
     * Sets all of the difficulties together
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
     * @return Gets the difficulty of this location task in the category specified
     */
    public float getDifficulty(Category category)
    {
        switch (category)
        {
            case tpll ->
            {
                return fDifficulties[0];
            }
            case worldedit ->
            {
                return fDifficulties[1];
            }
            case colouring ->
            {
                return fDifficulties[2];
            }
            case detail ->
            {
                return fDifficulties[3];
            }
            //Including terraforming:
            default ->
            {
                return fDifficulties[4];
            }
        }
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
