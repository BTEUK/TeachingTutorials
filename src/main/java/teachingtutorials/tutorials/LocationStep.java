package teachingtutorials.tutorials;

import teachingtutorials.utils.Hologram;

/**
 * Step data specific to a location
 */
public class LocationStep// extend step?
{
    //Data stored in the LocationSteps table in the DB
    private double dStartLatitude;
    private double dStartLongitude;
    private String szInstructions;
    private double dHologramLocationX;
    private double dHologramLocationY;
    private double dHologramLocationZ;

    public static LocationStep getFromStepAndLocation(int iStepID, int iLocationID)
    {
        //Accesses the DB and fetches the information about the step location
        return new LocationStep();
    }
}
