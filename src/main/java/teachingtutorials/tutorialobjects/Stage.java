package teachingtutorials.tutorialobjects;

import java.util.ArrayList;

/**
 * Represents a stage in the Tutorials system
 */
public class Stage
{
    /** The ID of the stage as in the DB */
    protected final int iStageID;

    /** The order in which this stage is played within the Tutorial */
    private final int iOrder;

    /** The name of the stage */
    private final String szName;

    //Used when creating a new tutorial
    /** The steps forming this stage */
    public ArrayList<Step> steps = new ArrayList<>();

    /**
     * Constructs a step from an entry in the database
     * @param iStageID The ID of the stage as in the DB
     * @param iOrder The order in which this stage is completed within the tutorial
     * @param szName The name of the stage
     */
    public Stage(int iStageID, int iOrder, String szName)
    {
        this.iStageID = iStageID;
        this.iOrder = iOrder;
        this.szName = szName;
    }

    /**
     * Constructs a Stage object for use whilst creating a new tutorial
     * @param szName The name of the stage
     * @param iOrder The order in which this stage is completed within the tutorial
     */
    public Stage(String szName, int iOrder)
    {
        this.iStageID = -1;
        this.iOrder = iOrder;
        this.szName = szName;
    }

    /**
     *
     * @return A copy of the stage ID of the stage
     */
    public int getStageID()
    {
        return iStageID;
    }

    /**
     *
     * @return A copy of the order in which this stage is completed within it's tutorial
     */
    public int getOrder()
    {
        return iOrder;
    }

    /**
     *
     * @return A copy of the name of the stage
     */
    public String getName()
    {
        return szName;
    }
}
