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
import java.util.UUID;

public class CompulsoryTutorialMenu
{
    public static Inventory inventory;
    public static String inventory_name;
    public static int inv_rows = 3 * 9;
    public static TeachingTutorials plugin;

    private static Tutorial[] tutorials;

    private static final Material compulsoryBlock = Material.LECTERN;
    private static final Material nonCompulsoryBlock = Material.BOOKSHELF;

    public static void initialize()
    {
        inventory_name = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Compulsory tutorial selection";

        inventory = Bukkit.createInventory(null, inv_rows);
    }

    public static String getInventoryName()
    {
        return inventory_name;
    }

    public static Inventory getGUI ()
    {
        int i;

        Inventory toReturn = Bukkit.createInventory(null, inv_rows, inventory_name);

        inventory.clear();

        //Admins can only select from tutorials which have been set by creators as in use
        tutorials = Tutorial.fetchAll(true);

        //Inv slot 1 = the first one
        //Add the tutorials to the gui
        for (i = 0 ; i < tutorials.length ; i++)
        {
            if (tutorials[i].bCompulsory)
                Utils.createItem(inventory, compulsoryBlock, 1, i,(ChatColor.GREEN+tutorials[i].szTutorialName), ChatColor.DARK_GREEN+tutorials[i].szAuthor);
            else
                Utils.createItem(inventory, nonCompulsoryBlock, 1, i,(ChatColor.GREEN+""+ChatColor.BOLD +tutorials[i].szTutorialName),
                        ChatColor.DARK_GREEN+Bukkit.getPlayer(UUID.fromString(tutorials[i].szAuthor)).getName());
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
            //Sets the current compulsory tutorial to not compulsory - ID is used to identify the tutorial
            tutorials[slot].triggerCompulsory();

            //Refreshes the display
            player.closeInventory();
            player.openInventory(CompulsoryTutorialMenu.getGUI());
        }
    }
}
