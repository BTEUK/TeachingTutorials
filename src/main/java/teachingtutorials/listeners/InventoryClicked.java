package teachingtutorials.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.guis.*;

public class InventoryClicked implements Listener
{
    private TeachingTutorials plugin;

    public InventoryClicked(TeachingTutorials plugin)
    {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getConsoleSender().sendMessage("[InventoryClicked]" + ChatColor.GREEN + " InventoryClicked loaded");
    }

    @EventHandler
    public void onClick(InventoryClickEvent e)
    {
        if (e.getCurrentItem() == null)
        {
            return;
        }

        if (e.getCurrentItem().hasItemMeta() == false)
        {
            return;
        }

        String title = e.getView().getTitle();

        //Determine what menu they are in

        //If in the Menu GUI
        if (title.equals(MainMenu.getInventoryName()))
        {
            e.setCancelled(true);
            if (e.getCurrentItem() == null)
            {
                return;
            }
            //Refer back to the MainMenu class
            MainMenu.clicked((Player) e.getWhoClicked(), e.getSlot(), e.getCurrentItem(), e.getInventory(), plugin);
        }
        else if (title.equals(CompulsoryTutorialMenu.getInventoryName()))
        {
            e.setCancelled(true);
            if (e.getCurrentItem() == null)
            {
                return;
            }
            CompulsoryTutorialMenu.clicked((Player) e.getWhoClicked(), e.getSlot(), e.getCurrentItem(), e.getInventory(), plugin);
        }
        else if (title.equals(CreatorTutorialsMenu.getInventoryName()))
        {
            e.setCancelled(true);
            if (e.getCurrentItem() == null)
            {
                return;
            }
            if (e.isRightClick())
                CreatorTutorialsMenu.rightClicked((Player) e.getWhoClicked(), e.getSlot(), plugin);
            else if (e.isLeftClick())
                CreatorTutorialsMenu.leftClicked((Player) e.getWhoClicked(), e.getSlot(), plugin);
        }
        else if (title.equals(AdminMenu.getInventoryName()))
        {
            e.setCancelled(true);
            if (e.getCurrentItem() == null)
            {
                return;
            }
            AdminMenu.clicked((Player) e.getWhoClicked(), e.getSlot(), e.getCurrentItem(), e.getInventory(), plugin);
        }
        else if (title.equals(LibraryMenu.getInventoryName()))
        {
            e.setCancelled(true);
            if (e.getCurrentItem() == null)
            {
                return;
            }
            LibraryMenu.clicked((Player) e.getWhoClicked(), e.getSlot(), plugin);
        }
    }
}
