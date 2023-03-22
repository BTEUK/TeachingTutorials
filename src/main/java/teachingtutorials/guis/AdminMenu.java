package teachingtutorials.guis;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.utils.User;
import teachingtutorials.utils.Utils;

import java.util.ArrayList;

public class AdminMenu
{
    public static Inventory inventory;
    public static String inventory_name;
    public static int inv_rows = 3 * 9;
    public static TeachingTutorials plugin;

    public static void initialize()
    {
        inventory_name = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Admin and Creator Menu";

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

        //Inv slot 1 = the first one

        //Menu for admins
        if (u.player.hasPermission("TeachingTutorials.Admin"))
        {
            Utils.createItem(inventory, Material.IRON_BARS, 1, 12,(ChatColor.GREEN +"Set Compulsory Tutorial"), ChatColor.DARK_GREEN+"Admins only");
            Utils.createItem(inventory, Material.BOOKSHELF, 1, 14,(ChatColor.GREEN +"My Tutorials"), ChatColor.DARK_GREEN+"View the tutorials you have created", ChatColor.DARK_GREEN+"Add locations", ChatColor.DARK_GREEN+"Set tutorials as in use");
            Utils.createItem(inventory, Material.KNOWLEDGE_BOOK, 1, 16,(ChatColor.GREEN +"Create Tutorial"), ChatColor.DARK_GREEN+"Create a new tutorial in game");
        }
        //Menu for creators only
        else
        {
            Utils.createItem(inventory, Material.BOOKSHELF, 1, 12,(ChatColor.GREEN +"My Tutorials"), ChatColor.DARK_GREEN+"View the tutorials you have created", ChatColor.DARK_GREEN+"Add locations", ChatColor.DARK_GREEN+"Set tutorials as in use");
            Utils.createItem(inventory, Material.KNOWLEDGE_BOOK, 1, 16,(ChatColor.GREEN +"Create Tutorial"), ChatColor.DARK_GREEN+"Create a new tutorial in game");
        }

        Utils.createItem(inventory, Material.SPRUCE_DOOR, 1, 27, ChatColor.BOLD +"" +ChatColor.GREEN+"Back to main menu");

        toReturn.setContents(inventory.getContents());

        return toReturn;
    }

    public static void clicked(Player player, int slot, ItemStack clicked, Inventory inv, TeachingTutorials plugin)
    {
        //Slot 0 indexed

        //Setting the compulsory tutorial
        if (clicked.getItemMeta().getDisplayName().equals(ChatColor.GREEN +"Set Compulsory Tutorial"))
        {
            player.closeInventory();
            player.openInventory(CompulsoryTutorialMenu.getGUI());
            return;
        }

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

        else if (clicked.getItemMeta().getDisplayName().equals(ChatColor.GREEN +"My Tutorials"))
        {
            player.closeInventory();
            player.openInventory(CreatorTutorialsMenu.getGUI(user));
        }
        else if (slot+1 == 27)
        {
            player.closeInventory();
            player.openInventory(MainMenu.getGUI(user));
        }

    }
}