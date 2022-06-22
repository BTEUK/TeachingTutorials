package teachingtutorials.tutorials;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.utils.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class Tutorial
{
    public String szTutorialName;
    public String szAuthor;

    int iTutorialID;

    public int[] categoryUsage;

    public final String[] szCategoryEnumsInOrder;

    public ArrayList<Stage> stages;

    public Tutorial()
    {
        szCategoryEnumsInOrder = new String[]{"tpll", "worldedit", "colouring", "detail", "terraforming"};
        stages = new ArrayList<>();
        categoryUsage = new int[5];
    }
}
