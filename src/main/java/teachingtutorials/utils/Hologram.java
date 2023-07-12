package teachingtutorials.utils;

import me.filoghost.holographicdisplays.api.HolographicDisplaysAPI;
import me.filoghost.holographicdisplays.api.Position;
import me.filoghost.holographicdisplays.api.hologram.VisibilitySettings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;

public class Hologram
{
    HolographicDisplaysAPI api = HolographicDisplaysAPI.get(TeachingTutorials.getInstance());
    me.filoghost.holographicdisplays.api.hologram.Hologram hologram;

    public Hologram(Location location, Player player, String szTitle, String szText)
    {
        //Performs the hologram creation synchronously
        Bukkit.getScheduler().runTask(TeachingTutorials.getInstance(), new Runnable()
        {
            @Override
            public void run()
            {
                //Raises the height of the hologram
                location.set(location.getX(), location.getY() + 2.1, location.getZ() + 4.5);

                //Creates the hologram
                hologram = api.createHologram(location);

                Position position = hologram.getPosition();

                //Inserts the text
                hologram.getLines().appendText(szTitle);
                String[] szWords = szText.split(" ");

                String szLine = "";
                String szLineNew;

                for (int iWord = 0; iWord < szWords.length ; iWord++)
                {
                    szLineNew = szLine + szWords[iWord] +" ";
                    if (szLineNew.length() > TeachingTutorials.getInstance().getConfig().getInt("Hologram_Max_Width") + 1) //Line is Hologram_Max_Width without the space
                    {
                        //Indicates that the line just added had one, >40 characters long word, so must display on a new line
                        if (szLine.equals(""))
                        {
                            //Adds the line to the hologram, removing the trailing space
                            hologram.getLines().appendText(szLineNew.substring(0, szLineNew.length() - 1));
                        }
                        else //Indicates that the line already had some words in it so display those
                        {
                            //Adds the line to the hologram, removing the trailing space
                            hologram.getLines().appendText(szLine.substring(0, szLine.length() - 1));
                        }
                        szLine = szWords[iWord] +" ";
                    }
                    else
                    {
                        szLine = szLineNew;
                    }
                    if (iWord == szWords.length - 1)
                    {
                        hologram.getLines().appendText(szLineNew);
                    }
                }

                //Shifts the hologram up if it is too tall
                if (hologram.getLines().size() > 8)
                {
                    position.add(0, 0.3 * hologram.getLines().size() - 8 , 0);
                    hologram.setPosition(position);
                }

                //Sets the visibility
                VisibilitySettings visibilitySettings = hologram.getVisibilitySettings();
                visibilitySettings.setGlobalVisibility(VisibilitySettings.Visibility.HIDDEN);
                visibilitySettings.setIndividualVisibility(player, VisibilitySettings.Visibility.VISIBLE);

            }
        });
    }

    public void removeHologram()
    {
        //Deletes the hologram
        //Performs the hologram deletion synchronously
        Bukkit.getScheduler().runTask(TeachingTutorials.getInstance(), new Runnable()
        {
            @Override
            public void run()
            {
                hologram.delete();
            }
        });
    }
}
