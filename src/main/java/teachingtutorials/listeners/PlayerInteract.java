package teachingtutorials.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import teachingtutorials.guis.MainMenu;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.utils.User;

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
        //Extract the player
        Player player = e.getPlayer();

        //Get the user
        User user = User.identifyUser(plugin, player);

        if (user == null)
        {
            plugin.getLogger().severe("User " + e.getPlayer().getName() + " can not be found!");
            e.getPlayer().sendMessage(ChatColor.RED +"User can not be found, please relog!");
            return;
        }

        if (e.getItem() != null)
        {
            if (player.getInventory().getItemInMainHand().equals(TeachingTutorials.menu))
            {
                e.setCancelled(true);
                //Check if the mainGui is not null.
                //If not then open it after refreshing its contents.
                //If no gui exists open the navigator.

                if (user.mainGui != null)
                    user.mainGui.refresh();
                else
                    user.mainGui = new MainMenu(plugin, user);
                user.mainGui.open(user);
            }
        }
    }
}
