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

public class LocationCreationMenu
{
    //Must take the lesson as input or something like that as it needs to store what the last addition was. (Cannot create a group after a stage for example, because step needs to be created first.
    //Rules are defined in the way that configs are validated.

    public static Inventory inventory;
    public static String inventory_name;
    public static TeachingTutorials plugin;
    public static int inv_rows = 3 * 9;

    public static void initialize()
    {
        inventory = Bukkit.createInventory(null, inv_rows);
    }

    public static String getInventoryName()
    {
        return inventory_name;
    }

    public static Inventory getGUI(Tutorial tutorial)
    {
        inventory_name = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Location Creation Menu - "+tutorial.szTutorialName;

        Inventory toReturn = Bukkit.createInventory(null, inv_rows, inventory_name);

        inventory.clear();

        //Inv slot 1 = the first one

        //Creates the menu options
      //  Utils.createItem(inventory, Material.BOOKSHELF, 1, i,(ChatColor.BOLD +"" +ChatColor.GREEN +allTutorials[i-1].szTutorialName), ChatColor.DARK_GREEN+(Bukkit.getPlayer(UUID.fromString(allTutorials[i-1].szAuthor))).getName(), ChatColor.DARK_GREEN+"In Use");

        toReturn.setContents(inventory.getContents());

        return toReturn;
    }

}
