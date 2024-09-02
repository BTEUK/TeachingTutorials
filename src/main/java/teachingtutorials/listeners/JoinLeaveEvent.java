package teachingtutorials.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.*;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.utils.Mode;
import teachingtutorials.utils.User;

import java.util.ArrayList;

public class JoinLeaveEvent implements Listener
{
    private final TeachingTutorials plugin;

    public JoinLeaveEvent(TeachingTutorials plugin)
    {
        Bukkit.getConsoleSender().sendMessage("[TeachingTutorials] " + ChatColor.GREEN + "JoinEvent loaded");
        this.plugin = plugin;
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void playerJoin(PlayerJoinEvent event)
    {
        User user = new User(event.getPlayer());
        user.fetchDetailsByUUID(TeachingTutorials.getInstance().getDBConnection());
        user.calculateRatings(TeachingTutorials.getInstance().getDBConnection());

        //Adds player to the main list of players
        plugin.players.add(user);

        //Set mode to idle
        user.currentMode = Mode.Idle;

        User.teleportPlayerToLobby(event.getPlayer(), plugin, plugin.getConfig().getLong("PlayerJoinTPDelay"));
    }

    @EventHandler
    public void playerLeave(PlayerQuitEvent event)
    {
        Player player = event.getPlayer();

        ArrayList<User> users = plugin.players;
        int i;

        for (i = 0 ; i < users.size() ; i++)
        {
            User user = users.get(i);
            //Found user
            if (user.player.getUniqueId().equals(player.getUniqueId()))
            {
                user.playerLeave(plugin);
                users.remove(i);
                i--;
                //Does not break because they may be there multiple times through an error,
                // and this would finally clear them
            }
        }

        User.teleportPlayerToLobby(event.getPlayer(), plugin, 0);
    }

} //End Class