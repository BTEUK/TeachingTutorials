package teachingtutorials.guis;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.compulsory.Compulsory;
import teachingtutorials.tutorials.Lesson;
import teachingtutorials.utils.User;
import teachingtutorials.utils.Utils;

public class MainMenu
{
    public static Inventory inventory;
    public static String inventory_name;
    public static int inv_rows = 3 * 9;

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

        FileConfiguration config = TeachingTutorials.getInstance().getConfig();
        boolean bCompulsoryTutorialEnabled = config.getBoolean("Compulsory_Tutorial");

        //The system has the compulsory tutorial feature enabled
        if (bCompulsoryTutorialEnabled)
        {
            if (u.bHasCompletedCompulsory)
            {
                if (u.bInLesson)
                    Utils.createItem(inventory, Material.WRITABLE_BOOK, 1, 17,(ChatColor.GREEN +"Continue Learning"), ChatColor.DARK_GREEN+"Continue your lesson");
                else
                    Utils.createItem(inventory, Material.WRITABLE_BOOK, 1, 17,(ChatColor.GREEN +"Continue Learning"), ChatColor.DARK_GREEN+"Start the next tutorial");

                Utils.createItem(inventory, Material.ENCHANTED_BOOK, 1, 11,(ChatColor.GREEN +"Restart Compulsory Tutorial"));
                Utils.createItem(inventory, Material.BOOKSHELF, 1, 14, (ChatColor.GREEN +"Tutorial Library"), ChatColor.DARK_GREEN +"Browse all our available tutorials");
            }
            else
            {
                if (u.bInLesson)
                    Utils.createItem(inventory, Material.BOOK, 1, 17,(ChatColor.GREEN +"Continue Compulsory Tutorial"), ChatColor.DARK_GREEN+"Gain the applicant rank");
                else
                    Utils.createItem(inventory, Material.BOOK, 1, 17,(ChatColor.GREEN +"Start Compulsory Tutorial"), ChatColor.DARK_GREEN+"Gain the applicant rank");
            }
        }
        else
        {
            if (u.bInLesson)
                Utils.createItem(inventory, Material.WRITABLE_BOOK, 1, 16,(ChatColor.GREEN +"Continue Learning"), ChatColor.DARK_GREEN+"Continue your lesson");
            else
                Utils.createItem(inventory, Material.WRITABLE_BOOK, 1, 16,(ChatColor.GREEN +"Continue Learning"), ChatColor.DARK_GREEN+"Start the next tutorial");
            Utils.createItem(inventory, Material.BOOKSHELF, 1, 12, (ChatColor.GREEN +"Tutorial Library"), ChatColor.DARK_GREEN +"Browse all our available tutorials");
        }

        //Admin and creator menu
        if (u.player.hasPermission("TeachingTutorials.Admin") || u.player.hasPermission("TeachingTutorials.Creator"))
        {
            Utils.createItem(inventory, Material.LECTERN, 1, 19,(ChatColor.GREEN +"Creator Menu"));
        }

        toReturn.setContents(inventory.getContents());

        return toReturn;
    }

    public static void clicked(Player player, int slot, ItemStack clicked, Inventory inv, TeachingTutorials plugin)
    {
        //Identifies the user from the list of users based on the player
        User user = User.identifyUser(plugin, player);
        if (user == null)
            return;

        //Compulsory tutorial
        if (clicked.getItemMeta().getDisplayName().equalsIgnoreCase(ChatColor.GREEN +"Start Compulsory Tutorial") || clicked.getItemMeta().getDisplayName().equalsIgnoreCase(ChatColor.GREEN +"Continue Compulsory Tutorial"))
        {
            performEvent(EventType.COMPULSORY, user, plugin);
        }

        //Continue learning
        else if (clicked.getItemMeta().getDisplayName().equalsIgnoreCase((ChatColor.GREEN +"Continue Learning")))
        {
            performEvent(EventType.CONTINUE, user, plugin);
        }

        //Redo compulsory tutorial
        else if (slot == 10) //0 Indexed
        {
            performEvent(EventType.COMPULSORY, user, plugin);
        }

        //Admin/creator menu
        else if (slot == 18 && (player.hasPermission("TeachingTutorials.Admin") || player.hasPermission("TeachingTutorials.Creator")))
        {
            performEvent(EventType.ADMIN_MENU, user, plugin);
        }

        //Admin/creator menu
        else if (clicked.getItemMeta().getDisplayName().equalsIgnoreCase((ChatColor.GREEN +"Tutorial Library")))
        {
            performEvent(EventType.LIBRARY, user, plugin);
        }
    }

    public static void performEvent(EventType event, User user, TeachingTutorials plugin)
    {
        Player player = user.player;

        switch (event)
        {
            case COMPULSORY:
                player.closeInventory();
                //Starts the compulsory tutorial
                Compulsory compulsory = new Compulsory(plugin, user);
                compulsory.startLesson();
                break;
            case CONTINUE:
                player.closeInventory();
                //Creates a lesson with the user
                Lesson lesson = new Lesson(user, plugin, false);
                lesson.startLesson();
                break;
            case ADMIN_MENU:
                player.closeInventory();
                player.openInventory(AdminMenu.getGUI(user));
                break;
            case LIBRARY:
                player.closeInventory();
                player.openInventory(LibraryMenu.getGUI(user));
                break;
        }
    }
}
