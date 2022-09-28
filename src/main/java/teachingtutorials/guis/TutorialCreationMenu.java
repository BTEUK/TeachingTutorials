package teachingtutorials.guis;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.tutorials.Tutorial;
import teachingtutorials.utils.User;
import teachingtutorials.utils.Utils;

import java.util.UUID;

public class TutorialCreationMenu
{
    //Must take the lesson as input or something like that as it needs to store what the last addition was. (Cannot create a group after a stage for example, because step needs to be created first.
    //Rules are defined in the way that configs are validated.

    public static Inventory inventory;
    public static String inventory_name;
    public static TeachingTutorials plugin;

    public static void initialize()
    {
        inventory_name = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Tutorial Creation Menu - ";

    }

    public static String getInventoryName()
    {
        return inventory_name;
    }

    public static Inventory getGUI(User u)
    {
        //Declare variables
        int i;
        int iTutorials;
        int iDiv;
        int iMod;

        //Get number of rows

        //Fetches users tutorials
        u.fetchAllTutorials();

        //Stores users tutorials locally
        Tutorial[] allTutorials = u.getAllTutorials();

        iTutorials = allTutorials.length;
        iDiv = iTutorials/9;
        iMod = iTutorials%9;

        if (iMod != 0 )
        {
            iDiv = iDiv + 1;
        }

        //------------------------------

        //Create inventories

        inventory = Bukkit.createInventory(null, iDiv * 9);
        inventory.clear();

        Inventory toReturn = Bukkit.createInventory(null, iDiv * 9, inventory_name);

        //Inv slot 1 = the first one

        //Creates the menu options
        for (i = 1 ; i <= allTutorials.length ; i++)
        {
            //Sets tutorial name bold for tutorials in use
            if (allTutorials[i-1].bInUse)
                Utils.createItem(inventory, Material.BOOKSHELF, 1, i,(ChatColor.BOLD +"" +ChatColor.GREEN +allTutorials[i-1].szTutorialName), ChatColor.DARK_GREEN+Bukkit.getPlayer(allTutorials[i-1].uuidAuthor).getName(), ChatColor.DARK_GREEN+"In Use");
            else
                Utils.createItem(inventory, Material.BOOKSHELF, 1, i,(ChatColor.GREEN +allTutorials[i-1].szTutorialName), ChatColor.DARK_GREEN+Bukkit.getPlayer(allTutorials[i-1].uuidAuthor).getName(), ChatColor.DARK_GREEN+"Not In Use");
        }

        toReturn.setContents(inventory.getContents());

        return toReturn;
    }

}
