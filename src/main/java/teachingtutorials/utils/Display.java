package teachingtutorials.utils;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

public class Display
{
    Player player;
    boolean bRawText;
    String szText;
    net.kyori.adventure.text.TextComponent textComponent;

    public Display(Player player, String text)
    {
        this.player = player;
        this.szText = Utils.chat(text);
        this.bRawText = true;
    }

    public Display(Player player, net.kyori.adventure.text.TextComponent text)
    {
        this.player = player;
        this.textComponent = text;
        this.bRawText = false;
    }

    public void Message()
    {
        if (bRawText)
            player.spigot().sendMessage(ChatMessageType.CHAT, TextComponent.fromLegacyText(szText));
        else
            player.sendMessage(textComponent);
    }

    public void ActionBar()
    {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(szText));
    }

    public void Title(String szSubtitle, int iFadeInTicks, int iStayTicks, int iFadeOutTicks)
    {
        //The title is the szText of the display
        player.sendTitle(szText, szSubtitle, iFadeInTicks, iStayTicks, iFadeOutTicks);
    }

    public void Hologram()
    {

    }
}
