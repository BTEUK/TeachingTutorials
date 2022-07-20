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

public class JoinEvent implements Listener
{
    private final TeachingTutorials plugin;

    public JoinEvent(TeachingTutorials plugin)
    {
        Bukkit.getConsoleSender().sendMessage("[TeachingTutorials] " + ChatColor.GREEN + "åJoinEvent loaded");
        this.plugin = plugin;
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void playerJoin(PlayerJoinEvent event)
    {
        User user = new User(event.getPlayer());
        user.fetchDetailsByUUID();
        user.calculateRatings();
        user.refreshScoreboard();

        //Adds player to the main list of players
        plugin.players.add(user);

        //Set mode to idle
        user.currentMode = Mode.Idle;
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
            User user = users.get(i);
            //Found user
            if (user.player.getUniqueId().equals(player.getUniqueId()))
            {
                users.remove(i);
                user.playerLeave();
                //Does not break, as they may be there multiple times through an error,
                // and this would finally clear them
            }
        }
    }

} //End Class