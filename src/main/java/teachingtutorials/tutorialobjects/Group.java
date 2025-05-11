package teachingtutorials.tutorialobjects;

import teachingtutorials.tutorialplaythrough.fundamentalTasks.Task;

import java.util.ArrayList;

/**
 * Represents a Tutorials Group as stored in the DB
 */
public class Group
{
    /** The name of the group. This is not stored in the DB and has no use within the Tutorials system */
    private final String szName;

    /** The database's ID of the group */
    private final int iGroupID;

    //Used when creating a new tutorial
    /** The tasks forming this group */
    public ArrayList<Task> tasks = new ArrayList<>();

    /**
     * Used to construct a Group from the database
     * @param iGroupID The ID of the group in the DB
     */
    public Group(int iGroupID)
    {
        this.iGroupID = iGroupID;
        this.szName = "";
    }

    /**
     * Constructs a Group object for use whilst creating a new tutorial
     * @param szName The name of the group
     */
    public Group(String szName)
    {
        this.iGroupID = -1;
        this.szName = szName;
    }

    /**
     * @return A copy of the group ID of this group
     */
    public int getGroupID()
    {
        return this.iGroupID;
    }

    /**
     * @return A copy of the name of this group
     */
    public String getName()
    {
        return szName;
    }
}
