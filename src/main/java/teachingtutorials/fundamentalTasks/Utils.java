package teachingtutorials.fundamentalTasks;

import net.buildtheearth.terraminusminus.util.geo.LatLng;

public class Utils
{
    public static float geometricDistance(LatLng testCoords, double[] dTargetCoords)
    {
        //Tpll accuracy checker
        double dLatitude1 = testCoords.getLat();
        double dLatitude2 = dTargetCoords[0];

        double dLongitude1 = testCoords.getLng();
        double dLongitude2 = dTargetCoords[1];

        int iRadius = 6371000; // metres
        double φ1 = dLatitude1 * Math.PI/180; // φ, λ in radians
        double φ2 = dLatitude2 * Math.PI/180;
        double Δφ = (dLatitude2-dLatitude1) * Math.PI/180;
        double Δλ = (dLongitude2-dLongitude1) * Math.PI/180;

        double a = Math.sin(Δφ/2) * Math.sin(Δφ/2) +
                Math.cos(φ1) * Math.cos(φ2) *
                        Math.sin(Δλ/2) * Math.sin(Δλ/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        float fDistance = (float) (iRadius * c); // in metres

        return fDistance;
    }
}
