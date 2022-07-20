package teachingtutorials.guis;

/*
    Purpose = Displays a list of tutorials that a creator has made
 */

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.tutorials.Tutorial;
import teachingtutorials.utils.User;
import teachingtutorials.utils.Utils;

import java.util.ArrayList;
import java.util.UUID;

public class CreatorTutorialsMenu
{
    public static Inventory inventory;
    public static String inventory_name;
    public static TeachingTutorials plugin;

    public static void initialize()
    {
        inventory_name = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Your tutorials";
    }

    public static String getInventoryName()
    {
        return inventory_name;
    }

    public static Inventory getGUI (User u)
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

        if (iMod != 0 || iDiv == 0)
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
                Utils.createItem(inventory, Material.BOOKSHELF, 1, i,(ChatColor.BOLD +"" +ChatColor.GREEN +allTutorials[i-1].szTutorialName), ChatColor.DARK_GREEN+(Bukkit.getPlayer(UUID.fromString(allTutorials[i-1].szAuthor))).getName(), ChatColor.DARK_GREEN+"In Use");
            else
                Utils.createItem(inventory, Material.BOOKSHELF, 1, i,(ChatColor.GREEN +allTutorials[i-1].szTutorialName), ChatColor.DARK_GREEN +"By "+(Bukkit.getPlayer(UUID.fromString(allTutorials[i-1].szAuthor))).getName(), ChatColor.DARK_GREEN+"Not In Use");
        }

        toReturn.setContents(inventory.getContents());

        return toReturn;
    }

    //Left click is for toggling whether a tutorial is in use or not
    public static void leftClicked(Player player, int slot, TeachingTutorials plugin)
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

        Tutorial[] tutorials = user.getAllTutorials();

        //Slot 0 indexed

        if (slot+1 > tutorials.length)
        {
            //Do nothing, they've clicked on a blank space
        }
        else
        {
            tutorials[slot].triggerInUse();
            player.closeInventory();
            player.openInventory(CreatorTutorialsMenu.getGUI(user));
        }
    }

    //Right click is for creating a new location
    public static void rightClicked(Player player, int slot, ItemStack clicked, Inventory inv, TeachingTutorials plugin)
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

        Tutorial[] tutorials = user.getAllTutorials();

        //Slot 0 indexed

        if (slot+1 > tutorials.length)
        {
            //Do nothing, they've clicked on a blank space
        }
        else
        {
            player.closeInventory();
            player.openInventory(LocationCreationMenu.getGUI(tutorials[slot]));
        }
    }
}
