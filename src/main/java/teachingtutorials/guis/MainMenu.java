package teachingtutorials.guis;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.compulsory.Compulsory;
import teachingtutorials.utils.User;
import teachingtutorials.utils.Utils;

import java.util.ArrayList;

public class MainMenu
{
    public static Inventory inventory;
    public static String inventory_name;
    public static int inv_rows = 3 * 9;
    public static TeachingTutorials plugin;

    public static void initialize()
    {
        inventory_name = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Tutorial Menu";

        inventory = Bukkit.createInventory(null, inv_rows);

    }

    public static String getInventoryName()
    {
        return inventory_name;
    }

    public static Inventory getGUI (User u)
    {
        Inventory toReturn = Bukkit.createInventory(null, inv_rows, inventory_name);

        inventory.clear();

        if (u.bHasCompletedCompulsory)
        {
            Utils.createItem(inventory, Material.BOOKSHELF, 1, 26,(ChatColor.GREEN +"Continue Learning"), ChatColor.DARK_GREEN+"Start the next tutorial");
            Utils.createItem(inventory, Material.BOOKSHELF, 1, 2,(ChatColor.GREEN +"Restart Compulsory Tutorials"), ChatColor.DARK_GREEN+"");
        }
        else if (u.bInLesson)
        {
            Utils.createItem(inventory, Material.BOOKSHELF, 1, 26,(ChatColor.GRAY +"Continue Learning"),
                    ChatColor.GRAY+"You have not completed the\ncompulsory tutorials yet");
            Utils.createItem(inventory, Material.BOOKSHELF, 1, 2,(ChatColor.GREEN +"Continue Compulsory Tutorials"), ChatColor.DARK_GREEN+"Gain the applicant rank");
        }
        else
        {
            Utils.createItem(inventory, Material.BOOKSHELF, 1, 26,(ChatColor.GRAY +"Continue Learning"),
                    ChatColor.GRAY+"You have not completed the\ncompulsory tutorials yet");
            Utils.createItem(inventory, Material.BOOKSHELF, 1, 2,(ChatColor.GREEN +"Start Compulsory Tutorials"), ChatColor.DARK_GREEN+"Gain the applicant rank");
        }

        toReturn.setContents(inventory.getContents());

        return toReturn;
    }

    public static void clicked(Player player, int slot, ItemStack clicked, Inventory inv, TeachingTutorials plugin)
    {
        if (clicked.getItemMeta().getDisplayName().equalsIgnoreCase((ChatColor.GREEN +"Continue Learning")))
        {
            player.sendMessage(ChatColor.AQUA + "Welcome back");
            player.closeInventory();
        }
        else if (clicked.getItemMeta().getDisplayName().equalsIgnoreCase(ChatColor.GRAY +"Continue Learning"))
        {
            player.sendMessage(ChatColor.RED +"You have not completed the compulsory tutorials yet");
            player.closeInventory();
        }
        //Compulsory tutorials
        else if (slot == 1) //0 Indexed
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
                }
            }

            if (bUserFound)
            {
                player.closeInventory();
                Compulsory compulsory = new Compulsory(plugin, user);
                compulsory.startLesson();
            }
            else
            {
                player.sendMessage(ChatColor.RED +"An error occurred. Please contact a dev.");
            }
        }
    }
}
