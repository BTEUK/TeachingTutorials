package teachingtutorials.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.guis.*;
import teachingtutorials.utils.User;

import java.util.UUID;

public class InventoryClickedOrClosed implements Listener
{
    private TeachingTutorials plugin;

    public InventoryClickedOrClosed(TeachingTutorials plugin)
    {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getConsoleSender().sendMessage("[InventoryClicked]" + ChatColor.GREEN + " InventoryClicked loaded");
    }

    @EventHandler
    public void onClick(InventoryClickEvent e)
    {
        if (!(e.getWhoClicked() instanceof Player))
            return;

        User user = User.identifyUser(plugin, ((Player) e.getWhoClicked()));

        //If u is null, cancel.
        if (user == null) {
            plugin.getLogger().severe("User " + e.getWhoClicked().getName() + " can not be found!");
            e.getWhoClicked().sendMessage(ChatColor.RED +"User can not be found, please relog!");
            return;
        }


        UUID playerUUID = user.player.getUniqueId();

        UUID inventoryUUID = Gui.openInventories.get(playerUUID);

        if (inventoryUUID != null)
        {
            e.setCancelled(true);
            Gui gui = Gui.inventoriesByUUID.get(inventoryUUID);
            Gui.guiAction action = gui.getActions().get(e.getRawSlot());

            if (action != null)
            {
                if (e.isLeftClick())
                    action.leftClick(user);
                else
                    action.rightClick(user);
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {

        Player p = (Player) e.getPlayer();
        UUID playerUUID = p.getUniqueId();

        //Remove the player from the list of open inventories.
        Gui.openInventories.remove(playerUUID);

    }
}
