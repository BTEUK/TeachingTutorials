package teachingtutorials.teachingtutorials.guis;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import teachingtutorials.teachingtutorials.utils.User;
import teachingtutorials.teachingtutorials.utils.Utils;

public class MainMenu
{
    public static Inventory inv;
    public static String inventory_name;
    public static int inv_rows = 3 * 9;

    public static void initialize()
    {
        inventory_name = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Tutorial Menu";

        inv = Bukkit.createInventory(null, inv_rows);

    }

    public static String getInventoryName()
    {
        return inventory_name;
    }

    public static Inventory getGUI (User u)
    {
        Inventory toReturn = Bukkit.createInventory(null, inv_rows, inventory_name);

        inv.clear();

        if (u.bHasCompletedOnce)
        {
            Utils.createItem(inv, Material.BOOKSHELF, 1, 26,(ChatColor.GREEN +"Continue Learning"), ChatColor.DARK_GREEN+"Start the next tutorial");
        }
        else
        {
            Utils.createItem(inv, Material.BOOKSHELF, 1, 26,(ChatColor.GRAY +"Continue Learning"),
                    ChatColor.GRAY+"You have not completed the compulsory tutorials yet");
        }

        toReturn.setContents(inv.getContents());

        return toReturn;
    }

    public static void clicked(Player player, int slot, ItemStack clicked, Inventory inv)
    {
        if (clicked.getItemMeta().getDisplayName().equalsIgnoreCase((ChatColor.GREEN +"Continue Learning")))
        {
            player.sendMessage("Continue learning innit");
            player.closeInventory();
        }
        else if (clicked.getItemMeta().getDisplayName().equalsIgnoreCase(ChatColor.GRAY +"Continue Learning"))
        {
            player.sendMessage("Ye canny do that for ye haven't finished ye compulsory tutorials");
            player.closeInventory();
        }
    }
}
