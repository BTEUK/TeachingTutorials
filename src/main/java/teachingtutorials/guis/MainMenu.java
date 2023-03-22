package teachingtutorials.guis;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.compulsory.Compulsory;
import teachingtutorials.tutorials.Lesson;
import teachingtutorials.utils.Display;
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
            Utils.createItem(inventory, Material.WRITABLE_BOOK, 1, 26,(ChatColor.GREEN +"Continue Learning"), ChatColor.DARK_GREEN+"Start the next tutorial");
            Utils.createItem(inventory, Material.ENCHANTED_BOOK, 1, 2,(ChatColor.GREEN +"Restart Compulsory Tutorial"));
        }
        else if (u.bInLesson)
        {
            Utils.createItem(inventory, Material.BOOK, 1, 26,(ChatColor.GREEN +"Continue Compulsory Tutorial"), ChatColor.DARK_GREEN+"Gain the applicant rank");
        }
        else
        {
            Utils.createItem(inventory, Material.BOOK, 1, 26,(ChatColor.GREEN +"Start Compulsory Tutorial"), ChatColor.DARK_GREEN+"Gain the applicant rank");
        }


        if (u.player.hasPermission("TeachingTutorials.Admin") || u.player.hasPermission("TeachingTutorials.Creator"))
        {
            Utils.createItem(inventory, Material.LECTERN, 1, 19,(ChatColor.GREEN +"Creator Menu"));
        }

        toReturn.setContents(inventory.getContents());

        return toReturn;
    }
    //Finds the correct user for this player from the plugins list of users

    public static void clicked(Player player, int slot, ItemStack clicked, Inventory inv, TeachingTutorials plugin)
    {
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
            Display display = new Display(player, ChatColor.RED +"An error occurred. Please contact a support staff. Error: 1");
            display.Message();
            display = new Display(player, ChatColor.RED +"Try relogging");
            display.Message();
            return;
        }

        //Compulsory tutorial
        if (clicked.getItemMeta().getDisplayName().equalsIgnoreCase(ChatColor.GREEN +"Start Compulsory Tutorial"))
        {
            player.closeInventory();
            Compulsory compulsory = new Compulsory(plugin, user);
            //Starts the compulsory tutorial
            compulsory.startLesson();
        }

        //Continue learning
        else if (clicked.getItemMeta().getDisplayName().equalsIgnoreCase((ChatColor.GREEN +"Continue Learning")))
        {
            player.closeInventory();
            player.sendMessage(ChatColor.AQUA + "Welcome back");
            //Creates a lesson with the user
            Lesson lesson = new Lesson(user, plugin, false);
            lesson.startLesson();
        }

        //Redo compulsory tutorial
        else if (slot == 1) //0 Indexed
        {
            player.closeInventory();
            Compulsory compulsory = new Compulsory(plugin, user);
            //Starts the compulsory tutorial again
            compulsory.startLesson();
        }

        //Admin/creator menu
        else if (slot == 18 && (player.hasPermission("TeachingTutorials.Admin") || player.hasPermission("TeachingTutorials.Creator")))
        {
            player.closeInventory();
            player.openInventory(AdminMenu.getGUI(user));
        }
    }
}
