package teachingtutorials.utils;

import me.filoghost.holographicdisplays.api.HolographicDisplaysAPI;
import me.filoghost.holographicdisplays.api.hologram.VisibilitySettings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;

/**
 * Represents a hologram within the Tutorials system
 */
public class Hologram
{
    //Gets an instance of the holographic displays API
    HolographicDisplaysAPI api = HolographicDisplaysAPI.get(TeachingTutorials.getInstance());
    me.filoghost.holographicdisplays.api.hologram.Hologram hologram;

    //Todo: This class needs to be modified to use DecentHolograms instead of HolographicDisplays since it is
    // depreciated

    /**
     *
     * @param location A reference to a bukkit location object at which this hologram should be displayed
     * @param player A reference to the player to which this hologram should exclusively be displayed to
     * @param szTitle The title of the hologram - will appear on the first line
     * @param szText The main body of the hologram
     */
    public Hologram(Location location, Player player, String szTitle, String szText)
    {
        //Performs the hologram creation synchronously
        Bukkit.getScheduler().runTask(TeachingTutorials.getInstance(), () -> {
            //Creates the hologram
            hologram = api.createHologram(location);

            //Inserts the title
            hologram.getLines().appendText(szTitle);
            String[] szWords = szText.split(" ");

            //At the start of the for loop, represents the current line being compiled before
            // the current word has been added
            String szLine = "";

            //Represents the current line after the current word has been added
            String szLineNew;

            //Represents the current line after the current word has been added
            // but also after the text has been stripped of colour codes
            String szDisplayedText;

            //The maximum width a hologram line can be
            final int iMax_Width = TeachingTutorials.getInstance().getConfig().getInt("Hologram_Max_Width");

            boolean bCreateNewLineAfter;
            bCreateNewLineAfter = false;

            //Used when a new line is manually specified and the start of the next line needs to be stored to added to the szLine at the end of the loop after the new line is added
            String szStartOfNextLine = "";

            //Goes through each word in the text
            for (int iWord = 0; iWord < szWords.length ; iWord++)
            {
                //TODO: what if two line seperators together or in one word?
                //-Expand algorithm time
                //Checks to see if the word contains a new line separator
                if (szWords[iWord].contains(System.lineSeparator()))
                {
                    int iIndexOfLine = szWords[iWord].indexOf(System.lineSeparator());
                    if (iIndexOfLine == 0)
                    {
                        hologram.getLines().appendText(szLine);
                        szLine = "";
                    }
                    else if (iIndexOfLine == (szWords[iWord].length() - 1))
                    {
                        bCreateNewLineAfter = true;
                    }
                    else
                    {
                        bCreateNewLineAfter = true;
                        String[] twoWords = szWords[iWord].split(System.lineSeparator());
                        szWords[iWord] = twoWords[0];
                        szStartOfNextLine = twoWords[1];
                    }
                }

                //Adds the word to the new line being compiled
                szLineNew = szLine + szWords[iWord].replaceFirst(System.lineSeparator(), "");

                //Strips the text of colour codes so that the length can be accurately measured
                szDisplayedText = szLineNew.replace("&[A-Fa-f0-9]", "");

                //Checks to see whether the new line exceeds the maximum width
                if (szDisplayedText.length() > iMax_Width) //Line is Hologram_Max_Width without the space
                {
                    //If the new line is too big, needs to deal with it

                    //Unless the new line is purely one word (aka the old one was blank)
                    //How does the old one get blank? - if the same thing happened previously, see below

                    //Indicates that the previous line had one > Max_Width characters long word,
                    // so the new word must display on a new line
                    if (szLine == "")
                        //szLine remains as ""
                        hologram.getLines().appendText(szLineNew);

                    //Indicates that the line already had some words in it so display those
                    //Adding the new one to it takes it over the maximum
                    else
                    {
                        //Adds the previous line to the hologram, removing the trailing space
                        hologram.getLines().appendText(szLine.substring(0, szLine.length() - 1));

                        //Adds the new line if it is over max characters long
                        if (szWords[iWord].replace("&[A-Fa-f0-9]", "").length() > iMax_Width)
                        {
                            hologram.getLines().appendText(szWords[iWord]);
                            szLine = "";
                        }

                        //If this is the last word, can immediately append it on a new line
                        else if (iWord == szWords.length - 1)
                                hologram.getLines().appendText(szWords[iWord]);

                        else
                            //Sends the line with the new word just added into the next loop, adds the space
                            szLine = szWords[iWord] + " ";
                    }
                }

                //If this is the last worst, but the length didn't max out, we can append the new line
                else if (bCreateNewLineAfter)
                {
                    hologram.getLines().appendText(szLineNew);
                    szLine = szStartOfNextLine + " ";
                }
                else if (iWord == szWords.length - 1)
                {
                    hologram.getLines().appendText(szLineNew);
                }
                else
                {
                    //Sends the line with the new word just added into the next loop, adds the space
                    szLine = szLineNew + " ";
                }

                bCreateNewLineAfter = false;
                szStartOfNextLine = "";
            }

            //Sets the visibility
            VisibilitySettings visibilitySettings = hologram.getVisibilitySettings();
            visibilitySettings.setGlobalVisibility(VisibilitySettings.Visibility.HIDDEN);
            visibilitySettings.setIndividualVisibility(player, VisibilitySettings.Visibility.VISIBLE);
        });
    }

    /**
     * Deletes the holographic displays hologram
     */
    public void removeHologram()
    {
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
