package teachingtutorials.teachingtutorials.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import teachingtutorials.teachingtutorials.TeachingTutorials;
import teachingtutorials.teachingtutorials.utils.User;

import java.util.ArrayList;

public class JoinEvent implements Listener
{
    private final TeachingTutorials plugin;

    public JoinEvent(TeachingTutorials plugin)
    {
        Bukkit.getConsoleSender().sendMessage("[TeachingTutorials]" + ChatColor.GREEN + " JoinEvent loaded");
        this.plugin = plugin;
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void playerJoin(PlayerJoinEvent event)
    {
        User user = new User(event.getPlayer());
        user.fetchDetailsByUUID();
    }

    @EventHandler
    public void playerLeave(PlayerQuitEvent event)
    {
        Player player = event.getPlayer();

        ArrayList<User> users = plugin.players;
        int iLength = users.size();
        int i;

        for (i = 0 ; i < iLength ; i++)
        {
            if (users.get(i).player.getUniqueId().equals(player.getUniqueId()))
            {
                users.remove(i);
            }
        }
    }

} //End Class