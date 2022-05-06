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

    int[] categoryDifficulties;

    //Difficulties in categories
    int iTpllDifficulty;
    int iWEDifficulty;
    int iTerraDifficulty;
    int iColouringDifficulty;
    int iDetailingDifficulty;

    public ArrayList<Stage> stages;

    public Tutorial()
    {
        stages = new ArrayList<>();
    }

}
