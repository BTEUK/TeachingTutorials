package teachingtutorials.tutorials;

import teachingtutorials.fundamentalTasks.Task;

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
    public void storeNewData()
    {
        String sql;
        Statement SQL = null;

        try
        {
            sql = "INSERT INTO LocationTasks (LocationID, TaskID, Answers, TpllDifficulty, WEDifficulty, ColouringDifficulty, DetailingDifficulty, TerraDifficulty) " +
                    "VALUES (" +iLocationID+", " +iTaskID+", " +szAnswers;

            for (int i = 0 ; i <=4 ; i++)
            {
                sql = sql +", "+fDifficulties[i];
            }

            sql = sql +")";
            SQL.executeUpdate(sql);
        }
        catch (Exception e)
        {

        }
    }
}