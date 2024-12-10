package teachingtutorials.tutorialobjects;

import teachingtutorials.TeachingTutorials;
import teachingtutorials.tutorialplaythrough.StagePlaythrough;
import teachingtutorials.utils.Display;

import java.util.ArrayList;
import java.util.logging.Level;

/**
 * Represents a Tutorials Step as stored in the DB
 */
public class Step
{
    /** The name of the step */
    private final String szName;

    /** The ID of the step in the DB */
    private final int iStepID;

    /** The position of the step within the stage */
    private final int iStepInStage;

    /** How the instructions should be displayed for this step */
    private final Display.DisplayType instructionDisplayType;

    //Used when creating a new tutorial
    /** The groups forming this step */
    public ArrayList<Group> groups = new ArrayList<>();


    /**
     * Constructs a step from data from the database
     * @param iStepID The stepID as in the database
     * @param iStepInStage
     * @param szStepName The name of the step
     * @param szInstructionDisplayType How the instructions should be displayed for this step - must be a value of
     *                                 Display.DisplayType
     */
    public Step(int iStepID, int iStepInStage, String szStepName, String szInstructionDisplayType)
    {
        this.iStepID = iStepID;
        this.iStepInStage = iStepInStage;
        this.szName = szStepName;

        //Extract the display type and store
        Display.DisplayType displayType;
        try
        {
            displayType = Display.DisplayType.valueOf(szInstructionDisplayType);
        }
        catch (IllegalArgumentException e)
        {
            TeachingTutorials.getInstance().getLogger().log(Level.SEVERE, "The step instruction display type was not properly specified ("+szInstructionDisplayType +"), reverting to chat", e);
            displayType = Display.DisplayType.chat;
        }
        this.instructionDisplayType = displayType;

    }

    /**
     * Constructs a Step object for use whilst creating a new tutorial
     * @param szName The name of the stage
     */
    public Step(String szName, int iOrder, String szInstructionDisplayType)
    {
        this.iStepID = -1;
        this.iStepInStage = iOrder;
        this.szName = szName;
        this.instructionDisplayType = Display.DisplayType.valueOf(szInstructionDisplayType);
    }


    /**
     * @return A copy of the name of this step
     */
    public String getName()
    {
        return szName;
    }

    /**
     * @return A copy of the step ID of this step
     */
    public int getStepID()
    {
        return iStepID;
    }

    /**
     * @return A copy of the instruction display type for this step
     */
    public Display.DisplayType getInstructionDisplayType()
    {
        return this.instructionDisplayType;
    }

    public int getStepInStage()
    {
        return iStepInStage;
    }
}
