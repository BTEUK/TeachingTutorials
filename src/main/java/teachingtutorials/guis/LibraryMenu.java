package teachingtutorials.guis;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.newlocation.NewLocation;
import teachingtutorials.tutorials.Lesson;
import teachingtutorials.tutorials.Location;
import teachingtutorials.tutorials.Tutorial;
import teachingtutorials.utils.User;
import teachingtutorials.utils.Utils;

import java.util.ArrayList;

public class LibraryMenu
{
    public static Inventory inventory;
    public static String inventory_name = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Library";
    public static int iRows;
    public static TeachingTutorials plugin;

    public static String getInventoryName()
    {
        return inventory_name;
    }

    public static Inventory getGUI (User u)
    {
        //Declare variables
        int i;
        //The number of in use tutorials which also have at least one location
        int iAvailableTutorials;
        int iDiv;
        int iMod;

        //A list of all tutorials which are in use and have at least one location
        Tutorial[] allAvailableTutorials = getInUseTutorialsWithLocations();
        iAvailableTutorials = allAvailableTutorials.length;

        //Works out how many rows in the inventory are needed
        iDiv = iAvailableTutorials/9;
        iMod = iAvailableTutorials%9;

        if (iMod != 0 || iDiv == 0)
        {
            iDiv = iDiv + 1;
        }

        //Enables an empty row and then a row for the back button
        iRows = iDiv+2;

        //------------------------------

        //Create inventories
        inventory = Bukkit.createInventory(null, iRows * 9);
        inventory.clear();

        Inventory toReturn = Bukkit.createInventory(null, iRows * 9, inventory_name);

        //Inv slot 1 = the first one

        //Indicates that the creator has no tutorials if they don't own any
        if (iAvailableTutorials == 0)
        {
            Utils.createItem(inventory, Material.BOOKSHELF, 1, 5, ChatColor.BOLD +"" +ChatColor.GREEN +"There are no tutorials available to play currently");
        }

        //Adds back button
        Utils.createItem(inventory, Material.SPRUCE_DOOR, 1, iRows * 9, ChatColor.BOLD +"" +ChatColor.GREEN+"Back to main menu");

        //Creates the menu options
        for (i = 1 ; i <= allAvailableTutorials.length ; i++)
        {
            Utils.createItem(inventory, Material.KNOWLEDGE_BOOK, 1, i,(ChatColor.BOLD +"" +ChatColor.GREEN +allAvailableTutorials[i-1].szTutorialName),
                    ChatColor.DARK_GREEN+"Tutor: " +Bukkit.getOfflinePlayer(allAvailableTutorials[i-1].uuidAuthor).getName());
        }

        toReturn.setContents(inventory.getContents());
        return toReturn;
    }

    //Handles any actions when an item is clicked whilst a player is in the Library Menu
    public static void clicked(Player player, int slot, TeachingTutorials plugin)
    {
        //Finds the correct user for this player from the plugins list of users
        boolean bUserFound = false;

        ArrayList<User> users = plugin.players;
        int iLength = users.size();
        int i;
        User user = new User(player);

        for (i = 0 ; i < iLength ; i++)
        {
            if (users.get(i).player.getUniqueId().equals(player.getUniqueId()))
            {
                user = users.get(i);
                bUserFound = true;
                break;
            }
        }

        if (!bUserFound)
        {
            player.sendMessage(ChatColor.RED +"An error occurred. Please contact a support staff. Error: 1");
            player.sendMessage(ChatColor.RED +"Try relogging");
            return;
        }

        //Gets the available tutorials again
        Tutorial[] tutorials = getInUseTutorialsWithLocations();

        //Slot 0 indexed
        if (slot+1 == iRows*9)
        {
            //Back button
            player.closeInventory();
            player.openInventory(MainMenu.getGUI(user));
        }
        else if (slot+1 > tutorials.length)
        {
            //Do nothing, they've clicked on a blank space
        }
        else //They've clicked on an actual tutorial
        {
            player.closeInventory();

            //Creates a NewLocation object
            Lesson newLesson = new Lesson(user, plugin, tutorials[slot]);

            //Launches them into the new location adding process
            newLesson.startLesson();
        }
    }

    //Gets all in use tutorials which have at least one location
    private static Tutorial[] getInUseTutorialsWithLocations()
    {
        int iAvailableTutorials;
        int i;

        //Fetches all in use tutorials
        Tutorial[] allInUseTutorials = Tutorial.fetchAll(true);

        //Counts the amount of in use tutorials with at least one location
        iAvailableTutorials = 0;
        for (i = 0 ; i < allInUseTutorials.length ; i++)
        {
            if (Location.getAllLocationIDsForTutorial(allInUseTutorials[i].getTutorialID()).length != 0)
            {
                iAvailableTutorials++;
            }
        }

        //A list of all tutorials which are in use and have at least one location
        Tutorial[] allAvailableTutorials = new Tutorial[iAvailableTutorials];

        //Compiles the above list
        iAvailableTutorials = 0;
        for (i = 0 ; i < allInUseTutorials.length ; i++)
        {
            if (Location.getAllLocationIDsForTutorial(allInUseTutorials[i].getTutorialID()).length != 0)
            {
                allAvailableTutorials[iAvailableTutorials] = allInUseTutorials[i];
                iAvailableTutorials++;
            }
        }
        return allAvailableTutorials;
    }
}
