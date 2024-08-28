package teachingtutorials.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
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

    public Hologram Hologram(String szTitle, Location location)
    {
        Hologram hologram = new Hologram(location, player, szTitle, szText);
        return hologram;
    }

    public enum DisplayType
    {
        hologram, chat, action_bar
    }

    /**
     * Produces a non italitised red Component for the given text
     * @param szText The text
     */
    public static Component errorText(String szText)
    {
        return Component.text(szText, NamedTextColor.RED).decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Produces a non italitised aqua Component for the given text
     * @param szText The text
     */
    public static Component aquaText(String szText)
    {
        return Component.text(szText, NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false);
    }
}
