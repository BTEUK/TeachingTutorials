package teachingtutorials.compulsory;

import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.tutorials.Lesson;
import teachingtutorials.utils.User;

import java.util.ArrayList;

public class Compulsory
{
    TeachingTutorials plugin;
    User user;
    Lesson lesson;

    public Compulsory(TeachingTutorials plugin, Player player)
    {
        this.plugin = plugin;

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
    }

    public void startLesson()
    {
        Lesson lesson = new Lesson(user, this.plugin, true);
        lesson.startLesson();
        user.player.sendMessage("Congrats");
    }
}
