package teachingtutorials.tutorials;

import net.md_5.bungee.api.ChatMessageType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

/**
 * @purpose A static class to handle updating a tracker scoreboard
 */
public class TrackerScoreboard
{
 //   private final int iTotalTasks = 0;

    public static void updateScoreboard(Player player, String szTutorialName, String szFirstStage, String szFirstStep)
    {
        ScoreboardManager SBM = Bukkit.getScoreboardManager();
        Scoreboard SB = SBM.getNewScoreboard();
        Objective progress = SB.registerNewObjective("Progress", "dummy", "" +ChatColor.AQUA +"Progress", RenderType.INTEGER);

        Score tutorialName = progress.getScore(ChatColor.AQUA +"1. Tutorial - "+szTutorialName);
        tutorialName.setScore(0);

        Score stageName = progress.getScore(ChatColor.AQUA +"2. Stage - "+szFirstStage);
        stageName.setScore(0);

        Score stepName = progress.getScore(ChatColor.AQUA +"3. Step - "+szFirstStep);
        stepName.setScore(0);

        progress.setDisplaySlot(DisplaySlot.SIDEBAR);

        player.setScoreboard(SB);
    }
}
