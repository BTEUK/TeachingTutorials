package teachingtutorials.tutorialobjects;

import org.bukkit.Bukkit;
import teachingtutorials.utils.DBConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a tutorial recommendation
 */
public class TutorialRecommendation
{
    /** The ID of the recommendation */
    private final int ID;

    /** The UUID of the player whom this is a recommendation for */
    private final UUID recommendedFor;

    /** The UUID of the player who recommended this tutorial */
    private final UUID recommendedBy;

    /** The TutorialID of the tutorial being recommended */
    private final int iTutorialID;

    /** The LocationID of the location being recommended, or null if not specified */
    private final int iLocationID;

    /** The reason for the recommendation */
    private final String szReason;

    /** Whether this recommendation has been acted upon */
    private final boolean bDone;

    /** The time this recommendation was acted upon */
    private final LocalDateTime doneTime;

    /**
     * Used for constructing a Tutorial Recommendation from an entry in the database
     * @param ID The ID of the recommendation
     * @param recommendedFor The UUID of the player whom this is a recommendation for
     * @param recommendedBy The UUID of the player who recommended this tutorial
     * @param iTutorialID  The TutorialID of the tutorial being recommended
     * @param iLocationID The LocationID of the location being recommended, or -1 if not specified
     * @param reason The reason for the recommendation
     * @param done Whether this recommendation has been acted upon
     * @param doneTime The time this recommendation was acted upon
     */
    public TutorialRecommendation(int ID, UUID recommendedFor, UUID recommendedBy, int iTutorialID, int iLocationID, String reason, boolean done, LocalDateTime doneTime)
    {
        this.ID = ID;
        this.recommendedFor = recommendedFor;
        this.recommendedBy = recommendedBy;
        this.iTutorialID = iTutorialID;
        this.iLocationID = iLocationID;
        this.szReason = reason;
        this.bDone = done;
        this.doneTime = doneTime;
    }

    public int getTutorialID()
    {
        return iTutorialID;
    }

    public int getLocationID()
    {
        return iLocationID;
    }

    /**
     *
     * @param dbConnection A tutorials database connection
     * @param logger A logger to output to
     * @return The name of the Tutorial
     */
    public String getTutorialName(DBConnection dbConnection, Logger logger)
    {
        Tutorial tutorial = Tutorial.fetchByTutorialID(iTutorialID, dbConnection, logger);
        if (tutorial != null)
            return tutorial.getTutorialName();
        else
            return "";
    }

    public String getRecommenderName()
    {
        return Bukkit.getOfflinePlayer(recommendedBy).getName();
    }

    /**
     * Fetches a list of active tutorial recommendations for a given player
     * @param dbConnection A tutorials database connection
     * @param logger A logger to output to
     * @param recommendedFor The UUID of the player to fetch the recommended tutorials of
     * @return A list of tutorial recommendations
     */
    public static TutorialRecommendation[] fetchTutorialRecommendationsForPlayer(DBConnection dbConnection, Logger logger, UUID recommendedFor)
    {
        //SQL objects
        String sql;
        Statement SQL = null;
        ResultSet resultSet;

        TutorialRecommendation[] recommendations;

        //A count of the number of tutorial recommendations for this player
        int iCount = 0;

        try
        {
            SQL = dbConnection.getConnection().createStatement();
            sql = "SELECT * FROM TutorialRecommendations WHERE RecommendedFor = '"+recommendedFor+"' AND Done = 0";

            //Count the number of records
            resultSet = SQL.executeQuery(sql);
            while (resultSet.next())
            {
                iCount++;
            }

            recommendations = new TutorialRecommendation[iCount];

            resultSet = SQL.executeQuery(sql);

            for (int i = 0 ; i < iCount ; i++)
            {
                resultSet.next();

                LocalDateTime ldt;
                Timestamp ts = resultSet.getTimestamp("DoneTime");
                if (ts != null)
                    ldt = ts.toLocalDateTime();
                else
                    ldt = null;

                TutorialRecommendation recommendation = new TutorialRecommendation(resultSet.getInt("ID"), recommendedFor,
                        UUID.fromString(resultSet.getString("RecommendedBy")), resultSet.getInt("TutorialID"),
                        resultSet.getInt("LocationID"), resultSet.getString("Reason"),
                        resultSet.getBoolean("Done"), ldt);

                recommendations[i] = recommendation;
            }
        }
        catch (SQLException e)
        {
            logger.log(Level.WARNING, "Error fetching tutorial recommendations for "+recommendedFor, e);
            return new TutorialRecommendation[0];
        }

        return recommendations;
    }


    public static boolean addRecommendation(DBConnection dbConnection, Logger logger, UUID recommendedFor, UUID recommendedBy, int iTutorialID, int iLocationID,
                                            String szReason)
    {
        //SQL objects
        String sql;
        Statement SQL = null;

        try
        {
            sql = "INSERT INTO TutorialRecommendations (`RecommendedFor`, `RecommendedBy`, `TutorialID`, `LocationID`, `Reason`) VALUES ('"+recommendedFor+"', '"+recommendedBy+"', "+iTutorialID +", "+iLocationID+", '"+szReason+"')";
            SQL = dbConnection.getConnection().createStatement();

            SQL.executeUpdate(sql);

            return true;
        }
        catch (SQLException e)
        {
            logger.log(Level.WARNING, "Error fetching tutorial recommendations for "+recommendedFor, e);
            return false;
        }
    }

    /**
     * Updates recommendations when a tutorial is completed
     * @param dbConnection A tutorials database connection
     * @param logger A logger to output to
     * @param recommendedFor The UUID of the player to fetch the recommended tutorials of
     * @param iTutorialID The TutorialID of the tutorial just completed
     * @param iLocationID The LocationID of the location for the tutorial just complete
     * @return Whether the update completed successfully
     */
    public static boolean updateComplete(DBConnection dbConnection, Logger logger, UUID recommendedFor, int iTutorialID, int iLocationID)
    {
        //SQL objects
        String sql;
        Statement SQL = null;

        try
        {
            sql = "UPDATE TutorialRecommendations SET Done = 1, DoneTime = current_timestamp() WHERE RecommendedFor = '"+recommendedFor+"' AND " +
                    "TutorialID = "+iTutorialID +" AND (LocationID is NULL OR LocationID = "+iLocationID +")";
            SQL = dbConnection.getConnection().createStatement();

            SQL.executeUpdate(sql);

            return true;
        }
        catch (SQLException e)
        {
            logger.log(Level.WARNING, "Error fetching tutorial recommendations for "+recommendedFor, e);
            return false;
        }
    }
}
