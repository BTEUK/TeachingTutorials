package teachingtutorials.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.guis.MainMenu;

public class InventoryClicked implements Listener
{
    public InventoryClicked(TeachingTutorials plugin)
    {
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

        //If in the Menu GUI
        if (title.equals(MainMenu.getInventoryName()))
        {
            e.setCancelled(true);
            if (e.getCurrentItem() == null)
            {
                return;
            }
            MainMenu.clicked((Player) e.getWhoClicked(), e.getSlot(), e.getCurrentItem(), e.getInventory());
        }
    }
}
