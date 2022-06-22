package teachingtutorials.utils;

import org.bukkit.entity.Player;

public class Display
{
    Player player;
    String szText;

    public Display(Player player, String text)
    {
        this.player = player;
        this.szText = text;
    }

    public void Message()
    {
        player.sendMessage(szText);
    }

    public void Hologram()
    {

    }
}
