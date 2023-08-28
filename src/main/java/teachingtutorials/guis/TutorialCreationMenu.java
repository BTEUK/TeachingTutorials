package teachingtutorials.guis;

import org.bukkit.inventory.Inventory;
import teachingtutorials.TeachingTutorials;

public class TutorialCreationMenu
{
    //Must take the lesson as input or something like that as it needs to store what the last addition was. (Cannot create a group after a stage for example, because step needs to be created first.
    //Rules are defined in the way that configs are validated.

    public static Inventory inventory;
    public static String inventory_name;
    public static TeachingTutorials plugin;

}
