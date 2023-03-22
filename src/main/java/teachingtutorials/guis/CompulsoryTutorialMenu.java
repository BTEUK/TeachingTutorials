package teachingtutorials.guis;

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

public class CompulsoryTutorialMenu
{
    public static Inventory inventory;
    public static String inventory_name;
    public static int iRows;
    public static TeachingTutorials plugin;

    private static Tutorial[] tutorials;

    private static final Material compulsoryBlock = Material.LECTERN;
    private static final Material nonCompulsoryBlock = Material.BOOKSHELF;

    public static void initialize()
    {
        inventory_name = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Select Compulsory Tutorial";
    }

    public static String getInventoryName()
    {
        return inventory_name;
    }

    public static Inventory getGUI ()
    {
        //Declare variables
        int i;
        int iTutorials;
        int iDiv;
        int iMod;

        //Admins can only select from tutorials which have been set by creators as in use
        tutorials = Tutorial.fetchAll(true);

        //Works out how many rows in the inventory are needed
        iTutorials = tutorials.length;
        iDiv = iTutorials/9;
        iMod = iTutorials%9;

        if (iMod != 0 || iDiv == 0)
        {
            iDiv = iDiv + 1;
        }

        //Enables an empty row and then a row for the back button
        iRows = iDiv+2;

        //Create inventories
        inventory = Bukkit.createInventory(null, iRows * 9);
        inventory.clear();

        Inventory toReturn = Bukkit.createInventory(null, iRows * 9, inventory_name);

        //Indicates that the creator has no tutorials if they don't own any
        if (iTutorials == 0)
        {
            Utils.createItem(inventory, Material.BARRIER, 1, 5, ChatColor.BOLD +"" +ChatColor.GREEN +"There are no in-use tutorials on the system");
        }

        //Adds back button
        Utils.createItem(inventory, Material.SPRUCE_DOOR, 1, iRows * 9, ChatColor.BOLD +"" +ChatColor.GREEN+"Back to creator menu");

        //Inv slot 1 = the first one
        //Add the tutorials to the gui
        for (i = 1 ; i <= tutorials.length ; i++)
        {
            if (tutorials[i-1].bCompulsory)
                Utils.createItem(inventory, compulsoryBlock, 1, i,(ChatColor.GREEN +""+ChatColor.BOLD +tutorials[i-1].szTutorialName), ChatColor.DARK_GREEN+(Bukkit.getPlayer(tutorials[i-1].uuidAuthor)).getName());
            else
                Utils.createItem(inventory, nonCompulsoryBlock, 1, i,(ChatColor.GREEN +tutorials[i-1].szTutorialName),
                        ChatColor.DARK_GREEN+(Bukkit.getPlayer(tutorials[i-1].uuidAuthor)).getName());
        }

        toReturn.setContents(inventory.getContents());

        return toReturn;
    }

    public static void clicked(Player player, int slot, ItemStack clicked, Inventory inv, TeachingTutorials plugin)
    {
        //Slot 0 indexed

        //When player clicks on one of the tutorials
        if (clicked.getType().equals(compulsoryBlock) || clicked.getType().equals(nonCompulsoryBlock))
        {
            //Toggles whether this tutorial is compulsory or not - ID is used to identify the tutorial
            tutorials[slot].toggleCompulsory();

            //Refreshes the display
            player.closeInventory();
            player.openInventory(CompulsoryTutorialMenu.getGUI());
            return;
        }

        if (slot+1 == iRows*9)
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

            //Back button
            player.closeInventory();
            player.openInventory(AdminMenu.getGUI(user));
        }
    }
}
