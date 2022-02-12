package teachingtutorials.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import teachingtutorials.guis.MainMenu;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.utils.User;

import java.util.ArrayList;

public class PlayerInteract implements Listener
{
    private TeachingTutorials plugin;

    public PlayerInteract(TeachingTutorials plugin)
    {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
        Bukkit.getConsoleSender().sendMessage("[TeachingTutorials]" + ChatColor.GREEN + " PlayerInteract loaded");
        this.plugin = plugin;
    }

    @EventHandler
    public void interactEvent(PlayerInteractEvent e)
    {
        Player player;

        player = e.getPlayer();

        //Get the user
        User user = new User();

        ArrayList<User> users = plugin.players;
        int iLength = users.size();
        int i;

        for (i = 0 ; i < iLength ; i++)
        {
            if (users.get(i).player.getUniqueId().equals(player.getUniqueId()))
            {
                user = users.get(i);
            }
        }

        if (player.getOpenInventory().getType() != InventoryType.CRAFTING && e.getPlayer().getOpenInventory().getType() != InventoryType.CREATIVE)
        {
            return;
        }

        if (player.getInventory().getItemInMainHand().equals(TeachingTutorials.menu))
        {
            e.setCancelled(true);
            player.openInventory(MainMenu.getGUI(user));
        }
    }
}
