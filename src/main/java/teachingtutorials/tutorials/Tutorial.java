package teachingtutorials.tutorials;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import teachingtutorials.TeachingTutorials;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.UUID;

public class Tutorial
{
    //Declare member variables
    private int iTutorialID;
    public String szTutorialName;
    public UUID uuidAuthor;
    public boolean bCompulsory;
    public boolean bInUse;

    public int[] categoryUsage;

    public final String[] szCategoryEnumsInOrder;

    public ArrayList<Stage> stages;

    //--------------------------------------------------
    //-------------------Constructors-------------------
    //--------------------------------------------------
    public Tutorial()
    {
        szCategoryEnumsInOrder = new String[]{"tpll", "worldedit", "colouring", "detail", "terraforming"};
        stages = new ArrayList<>();
        categoryUsage = new int[5];
    }

    //---------------------------------------------------
    //----------------------Getters----------------------
    //---------------------------------------------------
    public int getTutorialID()
    {
        return iTutorialID;
    }

    //---------------------------------------------------
    //----------------------Setters----------------------
    //---------------------------------------------------
    public void setTutorialID(int iTutorialID)
    {
        this.iTutorialID = iTutorialID;
    }


    //---------------------------------------------------
    //--------------------SQL Fetches--------------------
    //---------------------------------------------------

    //Fetches all tutorials in the DB
    public static Tutorial[] fetchAll(boolean bInUseOnly)
    {
        //Declare variables
        Tutorial[] tutorials;

        int iCount = 0;

        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            //Compiles the command to fetch tutorials
            if (bInUseOnly)
                sql = "SELECT * FROM `Tutorials` WHERE `Tutorials`.`InUse` = 1";
            else
                sql = "SELECT * FROM `Tutorials`";
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);

            //Counts the amount of tutorials
            while (resultSet.next())
            {
                iCount++;
            }

            //Initiates the array with length of the number of tutorials fetched
            tutorials = new Tutorial[iCount];

            //Executes the query again
            resultSet = SQL.executeQuery(sql);
            for (int i = 0 ; i < iCount ; i++)
            {
                //Moves to the next tutorial
                resultSet.next();

                //Initiates the tutorial object
                tutorials[i] = new Tutorial();

                //Stores the information
                tutorials[i].iTutorialID = resultSet.getInt("TutorialID");
                tutorials[i].szTutorialName = resultSet.getString("TutorialName");
                tutorials[i].uuidAuthor = UUID.fromString(resultSet.getString("Author"));
                tutorials[i].bCompulsory = resultSet.getBoolean("Compulsory");
            }

            //Compiles the command to fetch category relevancies
            if (bInUseOnly)
                sql = "SELECT * FROM `Tutorials`,`CategoryPoints` WHERE `Tutorials`.`InUse` = 1 AND `Tutorials`.`TutorialID` = `CategoryPoints`.`TutorialID`";
            else
                sql = "SELECT * FROM `Tutorials`,`CategoryPoints` WHERE `Tutorials`.`TutorialID` = `CategoryPoints`.`TutorialID`";

            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            while (resultSet.next())
            {
                int i;
                //Goes through until it finds the linked tutorial
                for (i = 0 ; i < iCount ; i++)
                {
                    if (tutorials[i].iTutorialID == resultSet.getInt("CategoryPoints.TutorialID"))
                    {
                        switch (resultSet.getString("Category"))
                        {
                            case "tpll":
                                tutorials[i].categoryUsage[0] = resultSet.getInt("CategoryPoints.Relevance");
                                break;
                            case "we":
                                tutorials[i].categoryUsage[1] = resultSet.getInt("CategoryPoints.Relevance");
                                break;
                            case "colouring":
                                tutorials[i].categoryUsage[2] = resultSet.getInt("CategoryPoints.Relevance");
                                break;
                            case "detail":
                                tutorials[i].categoryUsage[3] = resultSet.getInt("CategoryPoints.Relevance");
                                break;
                            case "terraforming":
                                tutorials[i].categoryUsage[4] = resultSet.getInt("CategoryPoints.Relevance");
                                break;
                            default:

                        }
                    }
                }
            }
        }
        catch (SQLException se)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - SQL Error fetching all in use tutorials");
            se.printStackTrace();
            tutorials = new Tutorial[0];
        }
        catch (Exception e)
        {
            e.printStackTrace();
            tutorials = new Tutorial[0];
        }
        return tutorials;
    }

    /**
     *
     * @return All in use tutorials which have at least one location
     */
    public static Tutorial[] getInUseTutorialsWithLocations()
    {
        int iAvailableTutorials;
        int iNumInUseTutorials;
        int i;

        //Fetches all in use tutorials
        Tutorial[] allInUseTutorials = Tutorial.fetchAll(true);
        iNumInUseTutorials = allInUseTutorials.length;

        //Boolean array storing whether each tutorial has at least one location
        boolean[] tutorialHasLocation = new boolean[iNumInUseTutorials];

        //Counts the amount of in use tutorials with at least one location
        iAvailableTutorials = 0;
        for (i = 0 ; i < iNumInUseTutorials ; i++)
        {
            tutorialHasLocation[i] = (Location.getAllLocationIDsForTutorial(allInUseTutorials[i].getTutorialID()).length != 0);

            if (tutorialHasLocation[i])
            {
                iAvailableTutorials++;
            }
        }

        //A list of all tutorials which are in use and have at least one location
        Tutorial[] allAvailableTutorials = new Tutorial[iAvailableTutorials];

        //Compiles a list of just tutorials with locations
        iAvailableTutorials = 0;
        for (i = 0 ; i < iNumInUseTutorials ; i++)
        {
            if (tutorialHasLocation[i])
            {
                allAvailableTutorials[iAvailableTutorials] = allInUseTutorials[i];
                iAvailableTutorials++;
            }
        }
        return allAvailableTutorials;
    }

    //Fetches all tutorials created by a user
    public static Tutorial[] fetchAllForUser(UUID uuid)
    {
        //Declare variables

        //Used to store the tutorials fetched from the DB
        Tutorial[] tutorials;

        int iCount = 0;

        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            //Compiles the command to fetch tutorials
            sql = "SELECT * FROM `Tutorials` WHERE `Tutorials`.`Author` = '" +uuid +"'";

            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            while (resultSet.next())
            {
                iCount++;
            }

            tutorials = new Tutorial[iCount];

            //Executes the query again (moves the cursor to the top)
            resultSet = SQL.executeQuery(sql);
            for (int i = 0 ; i < iCount ; i++)
            {
                resultSet.next();
                tutorials[i] = new Tutorial();
                tutorials[i].iTutorialID = resultSet.getInt("TutorialID");
                tutorials[i].szTutorialName = resultSet.getString("TutorialName");
                tutorials[i].uuidAuthor = UUID.fromString(resultSet.getString("Author"));
                tutorials[i].bCompulsory = resultSet.getBoolean("Compulsory");
                tutorials[i].bInUse = resultSet.getBoolean("InUse");
            }

            //Compiles the command to fetch category difficulties
            sql = "SELECT * FROM `Tutorials`,`CategoryPoints` WHERE `Tutorials`.`TutorialID` = `CategoryPoints`.`TutorialID` AND `Tutorials`.`Author` = '"+uuid+"'";
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            while (resultSet.next())
            {
                int i;
                //Goes through until it finds the linked tutorial
                for (i = 0 ; i < iCount ; i++)
                {
                    if (tutorials[i].iTutorialID == resultSet.getInt("CategoryPoints.TutorialID"))
                    {
                        switch (resultSet.getString("Category"))
                        {
                            case "tpll":
                                tutorials[i].categoryUsage[0] = resultSet.getInt("CategoryPoints.Relevance");
                                break;
                            case "we":
                                tutorials[i].categoryUsage[1] = resultSet.getInt("CategoryPoints.Relevance");
                                break;
                            case "colouring":
                                tutorials[i].categoryUsage[2] = resultSet.getInt("CategoryPoints.Relevance");
                                break;
                            case "detail":
                                tutorials[i].categoryUsage[3] = resultSet.getInt("CategoryPoints.Relevance");
                                break;
                            case "terraforming":
                                tutorials[i].categoryUsage[4] = resultSet.getInt("CategoryPoints.Relevance");
                                break;
                            default:

                        }
                    }
                }
            }
        }
        catch(SQLException se)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - SQL Error fetching all of a creators tutorials");
            se.printStackTrace();
            tutorials = new Tutorial[0];
        }
        catch (Exception e)
        {
            e.printStackTrace();
            tutorials = new Tutorial[0];
        }

        //Returns the list of tutorials
        return tutorials;
    }

    //Fetches the details of a tutorial by the tutorial ID
    public boolean fetchByTutorialID()
    {
        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            //Compiles the command to fetch the tutorial
            sql = "SELECT * FROM `Tutorials` WHERE `Tutorials`.`TutorialID` = " +this.iTutorialID;

            SQL = TeachingTutorials.getInstance().getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            resultSet.next();
            this.szTutorialName = resultSet.getString("TutorialName");
            this.uuidAuthor = UUID.fromString(resultSet.getString("Author"));
            this.bCompulsory = resultSet.getBoolean("Compulsory");
            this.bInUse = resultSet.getBoolean("InUse");

            return true;
        }
        catch (SQLException se)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TeachingTutorials] - SQL - SQL Error fetching tutorial details by tutorial id: " +this.iTutorialID);
            se.printStackTrace();
            return false;
        }
    }

    //---------------------------------------------------
    //--------------------SQL Updates--------------------
    //---------------------------------------------------

    //Changes the boolean value of whether a tutorial is compulsory or not
    //Will ensure that only one tutorial is compulsory at a time by setting all others to not compulsory if it is changed to compulsory
    public void toggleCompulsory()
    {
        //Declare variables
        String szSql;
        Statement SQL;

        //Attempts to update the database
        try
        {
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();
            if (this.bCompulsory)
                szSql = "UPDATE `Tutorials` SET `Compulsory` = 0 WHERE `TutorialID` = "+ this.iTutorialID;
            else
            {
                setAllTutorialsNotCompulsory();
                szSql = "UPDATE `Tutorials` SET `Compulsory` = 1 WHERE `TutorialID` = "+ this.iTutorialID;
            }
            SQL.executeUpdate(szSql);
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Set tutorial "+this.iTutorialID +" as compulsory");
            //Updates the value of the boolean stored in memory
            this.bCompulsory = !this.bCompulsory;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    //Sets all tutorials as not compulsory
    private static void setAllTutorialsNotCompulsory()
    {
        //Declare variables
        String szSql;
        Statement SQL;

        try
        {
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();
            szSql = "UPDATE `Tutorials` SET `Compulsory` = 0";
            SQL.executeUpdate(szSql);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    //Changes the boolean value of whether a tutorial is "in-use" or not
    public boolean toggleInUse()
    {
        //Declare variables
        String szSql;
        Statement SQL;

        try
        {
            //Updates the database
            SQL = TeachingTutorials.getInstance().getConnection().createStatement();
            if (this.bInUse)
            {
                szSql = "UPDATE `Tutorials` SET `InUse` = 0 WHERE `TutorialID` = "+ this.iTutorialID;
            }
            else if (Location.getAllLocationIDsForTutorial(this.iTutorialID).length > 0)
            {
                szSql = "UPDATE `Tutorials` SET `InUse` = 1 WHERE `TutorialID` = "+ this.iTutorialID;
            }
            else
            {
                return false;
            }

            //Updates the database
            SQL.executeUpdate(szSql);

            //Updates the value of the boolean stored in memory
            this.bInUse = !this.bInUse;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return true;
    }
}
