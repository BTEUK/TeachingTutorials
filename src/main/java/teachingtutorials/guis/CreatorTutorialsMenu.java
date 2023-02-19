package teachingtutorials.guis;

/*
    Purpose = Displays a list of tutorials that a creator has made
 */

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.newlocation.NewLocation;
import teachingtutorials.tutorials.Tutorial;
import teachingtutorials.utils.User;
import teachingtutorials.utils.Utils;

import java.util.ArrayList;

public class CreatorTutorialsMenu
{
    public static Inventory inventory;
    public static String inventory_name;
    public static int iRows;
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

        //Fetches user's tutorials
        u.fetchAllTutorials();

        //Stores user's tutorials locally
        Tutorial[] allTutorials = u.getAllTutorials();

        //Works out how many rows in the inventory are needed
        iTutorials = allTutorials.length;
        iDiv = iTutorials/9;
        iMod = iTutorials%9;

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
        if (allTutorials.length == 0)
        {
            Utils.createItem(inventory, Material.BOOKSHELF, 1, 5, ChatColor.BOLD +"" +ChatColor.GREEN +"You have no tutorials");
        }

        //Adds back button
        Utils.createItem(inventory, Material.SPRUCE_DOOR, 1, iRows * 9, ChatColor.BOLD +"" +ChatColor.GREEN+"Back to creator menu");

        //Creates the menu options
        for (i = 1 ; i <= allTutorials.length ; i++)
        {
            //Sets tutorial name bold for tutorials in use
            if (allTutorials[i-1].bInUse)
                Utils.createItem(inventory, Material.BOOKSHELF, 1, i,(ChatColor.BOLD +"" +ChatColor.GREEN +allTutorials[i-1].szTutorialName),
                        ChatColor.DARK_GREEN+"In Use - Left click to remove from use",
                        ChatColor.DARK_GREEN+"Right click to add a new location");
            else
                Utils.createItem(inventory, Material.BOOKSHELF, 1, i,(ChatColor.GREEN +allTutorials[i-1].szTutorialName),
                        ChatColor.DARK_GREEN+"Not In Use - Left click to set in use",
                        ChatColor.DARK_GREEN+"Right click to add a new location");
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

        //Stores the list of the creator's tutorials locally
        Tutorial[] tutorials = user.getAllTutorials();

        //Slot 0 indexed
        if (slot+1 > tutorials.length)
        {
            //Do nothing, they've clicked on a blank space
        }
        else if (slot+1 == iRows*9)
        {
            //Back button
            player.closeInventory();
            player.openInventory(AdminMenu.getGUI(user));
        }
        else
        {
            tutorials[slot].toggleInUse();
            player.closeInventory();
            player.openInventory(CreatorTutorialsMenu.getGUI(user));
        }
    }

    //Right click is for creating a new location
    public static void rightClicked(Player player, int slot, TeachingTutorials plugin)
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
        else if (slot+1 == iRows*9)
        {
            //Back button
            player.closeInventory();
            player.openInventory(AdminMenu.getGUI(user));
        }
        else
        {
            player.closeInventory();

            //Creates a NewLocation object
            NewLocation newLocation = new NewLocation(user, tutorials[slot], plugin);

            //Launches them into the new location adding process
            newLocation.launchNewLocationAdding();
        }
    }
}
