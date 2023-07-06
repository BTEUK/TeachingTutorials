package teachingtutorials;

import net.buildtheearth.terraminusminus.generator.EarthGeneratorSettings;
import net.buildtheearth.terraminusminus.projection.GeographicProjection;
import net.buildtheearth.terraminusminus.projection.OutOfProjectionBoundsException;

public class Test
{
    public static void main (String[] args)
    {
        final GeographicProjection projection = EarthGeneratorSettings.parse(EarthGeneratorSettings.BTE_DEFAULT_SETTINGS).projection();
        try
        {
            double[] longLat = projection.toGeo(2810731+0.5d, -5391084+0.5d);
            System.out.println(longLat[1]);
            System.out.println(longLat[0]);
        }
        catch (OutOfProjectionBoundsException e)
        {
            //Player has selected an area outside of the projection
            return;
        }

    }
}
