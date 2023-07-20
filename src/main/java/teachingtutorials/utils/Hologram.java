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
                double[] xzAddition;
                float fYaw = player.getLocation().getYaw();

                //Calculates where to move the hologram to based on the player's location
                if (fYaw < -135 || fYaw > 135)
                {
                    xzAddition = new double[]{0, -4.5};
                }
                else if (fYaw < -45)
                {
                    xzAddition = new double[]{4.5, 0};
                }
                else if (fYaw > 45)
                {
                    xzAddition = new double[]{-4.5, 0};
                }
                else
                {
                    xzAddition = new double[]{0, 4.5};
                }

                //Moves the hologram
                location.set(location.getX() +xzAddition[0], location.getY(), location.getZ() +xzAddition[1]);

                //Raises or lowers the hologram
                int iHeight = location.getWorld().getHighestBlockYAt(location.getBlockX(), location.getBlockZ());
                location.set(location.getX(), iHeight + 2.1, location.getZ());

                //Creates the hologram
                hologram = api.createHologram(location);

                Position position = hologram.getPosition();

                //Inserts the text
                hologram.getLines().appendText(szTitle);
                String[] szWords = szText.split(" ");

                String szLine = "";
                String szLineNew;
                String szDisplayedText;

                for (int iWord = 0; iWord < szWords.length ; iWord++)
                {
                    szLineNew = szLine + szWords[iWord] +" ";
                    szDisplayedText = szLineNew.replace("&[A-Fa-f0-9]", "");
                    if (szDisplayedText.length() > TeachingTutorials.getInstance().getConfig().getInt("Hologram_Max_Width") + 1) //Line is Hologram_Max_Width without the space
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
                        if (iWord == szWords.length - 1)
                        {
                            hologram.getLines().appendText(szWords[iWord]);
                        }
                    }
                    else if (iWord == szWords.length - 1)
                    {
                        hologram.getLines().appendText(szLineNew);
                    }
                    else
                    {
                        szLine = szLineNew;
                    }
                }

                Bukkit.getConsoleSender().sendMessage(hologram.getLines().size() +"");
                //Shifts the hologram up if it is too tall
                if (hologram.getLines().size() > 7)
                {
                    position = position.add(0, 0.2 * (hologram.getLines().size() - 7) , 0);
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
