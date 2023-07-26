package teachingtutorials.fundamentalTasks;

import net.buildtheearth.terraminusminus.generator.EarthGeneratorSettings;
import net.buildtheearth.terraminusminus.projection.GeographicProjection;
import net.buildtheearth.terraminusminus.projection.OutOfProjectionBoundsException;
import net.buildtheearth.terraminusminus.util.geo.LatLng;
import org.bukkit.Location;
import org.bukkit.World;

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

    //Converts lat/long to block coordinates
    public static Location convertToBukkitLocation(World world, double dLatitude, double dLongitude)
    {
        Location location = null;

        double[] xz = convertToMCCoordinates(dLatitude, dLongitude);

        if (xz != null)
        {
            location = new Location(world, xz[0], world.getHighestBlockYAt((int) xz[0], (int) xz[1]), xz[1]);
        }
        return location;
    }

    public static double[] convertToMCCoordinates(double dLatitude, double dLongitude)
    {
        double[] xz = null;

        final GeographicProjection projection = EarthGeneratorSettings.parse(EarthGeneratorSettings.BTE_DEFAULT_SETTINGS).projection();

        try
        {
            xz = projection.fromGeo(dLongitude, dLatitude);
        }
        catch (OutOfProjectionBoundsException e)
        {
            //Player has selected an area outside of the projection
        }
        return xz;
    }
}
