package teachingtutorials.guis;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.tutorialplaythrough.Lesson;
import teachingtutorials.tutorialobjects.Tutorial;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.User;
import teachingtutorials.utils.Utils;

import java.util.logging.Level;

/**
 * A menu which displays all of the tutorials available for a player to play through and allows them to select one to do
 */
public class LibraryMenu extends Gui
{
    /** Stores the name of the inventory */
    private static final Component inventoryName = TutorialGUIUtils.inventoryTitle("Library");

    /** A reference to the TeachingTutorials plugin instance */
    private final TeachingTutorials plugin;

    /** A reference to the user for which this menu is for */
    private final User user;

    /** Stores a list of all in use tutorials */
    private Tutorial[] tutorials;

    /**
     *
     * @param plugin An instance of the plugin
     * @param user The user for which the menu is being created for
     * @param tutorials A list of all in use tutorials which have locations
     */
    public LibraryMenu(TeachingTutorials plugin, User user, Tutorial[] tutorials)
    {
        //Sets up the menu with the icons already in place
        super(getGUI(tutorials));
        this.plugin = plugin;
        this.user = user;
        this.tutorials = tutorials;

        //Adds the click-actions to the menu
        setActions();
    }

    /**
     * Creates an inventory with icons representing a library of available tutorials
     * @param tutorials A list of all in-use tutorials
     * @return An inventory of icons
     */
    public static Inventory getGUI (Tutorial[] tutorials)
    {
        //Declare variables
        int i;
        int iTutorials;
        int iDiv;
        int iMod;
        int iRows;

        Inventory inventory;

        //Works out how many rows in the inventory are needed
        iTutorials = tutorials.length;
        iDiv = iTutorials/9;
        iMod = iTutorials%9;

        if (iMod != 0 || iDiv == 0)
        {
            iDiv = iDiv + 1;
        }

        //Enables an empty row and then a row for the back button
        iRows = iDiv+2;

        //Create inventory
        inventory = Bukkit.createInventory(null, iRows * 9);
        inventory.clear();

        Inventory toReturn = Bukkit.createInventory(null, iRows * 9, inventoryName);

        //Indicates that there are no tutorials in the system
        if (iTutorials == 0)
        {
            ItemStack noTutorials = Utils.createItem(Material.BARRIER, 1,
                    TutorialGUIUtils.optionTitle("There are no tutorials available to play currently"),
                    TutorialGUIUtils.optionLore("Ask a server admin to get some created"));
            inventory.setItem(5-1, noTutorials);
        }

        //Adds the tutorials to the menu options
        //Inv slot 0 = the first one
        ItemStack tutorial;
        for (i = 0 ; i < iTutorials ; i++)
        {
            tutorial = Utils.createItem(Material.KNOWLEDGE_BOOK, 1,
                    TutorialGUIUtils.optionTitle(tutorials[i].getTutorialName()).decoration(TextDecoration.BOLD, true),
                    TutorialGUIUtils.optionLore("Tutor - " +Bukkit.getOfflinePlayer(tutorials[i].getUUIDOfAuthor()).getName()));
            inventory.setItem(i, tutorial);
        }

        //Adds a back button
        ItemStack back = Utils.createItem(Material.SPRUCE_DOOR, 1, TutorialGUIUtils.backButton("Back to main menu"));
        inventory.setItem((iRows * 9) - 1, back);

        toReturn.setContents(inventory.getContents());
        return toReturn;
    }

    /**
     * Adds the click-actions to the menu slots of this library menu
     */
    private void setActions()
    {
        //Declare variables
        int i;
        int iTutorials;
        int iDiv;
        int iMod;
        int iRows;

        //Works out how many rows in the inventory are needed
        iTutorials = tutorials.length;
        iDiv = iTutorials/9;
        iMod = iTutorials%9;

        if (iMod != 0 || iDiv == 0)
        {
            iDiv = iDiv + 1;
        }

        //Enables an empty row and then a row for the back button
        iRows = iDiv+2;

        //Adds back button
        setAction((iRows * 9) - 1, new Gui.guiAction() {
            @Override
            public void rightClick(User u) {
                leftClick(u);
            }

            @Override
            public void leftClick(User u) {
                delete();
                u.mainGui = new MainMenu(plugin, user);
                u.mainGui.open(u);
            }
        });

        //Initiates the current tutorial object
//        Tutorial currentTutorial = null;

        int iTutorialIDCurrentLesson = -1;

        if (user.hasIncompleteLessons())
        {
            //Get current lesson's tutorial ID and sets up the tutorial object from this
            iTutorialIDCurrentLesson = Lesson.getTutorialOfCurrentLessonOfPlayer(user.player.getUniqueId(), TeachingTutorials.getInstance().getDBConnection(), TeachingTutorials.getInstance());
        }

        //Inv slot 0 = the first one
        //Adds the actions of each slot
        for (i = 0 ; i < tutorials.length ; i++)
        {
            int iSlot = i;
            int finalITutorialIDCurrentLesson = iTutorialIDCurrentLesson;
            setAction(iSlot, new Gui.guiAction() {
                @Override
                public void rightClick(User user) {
                    leftClick(user);
                }

                @Override
                public void leftClick(User user) {
                    boolean startTheLesson = false;

                    if (user.hasIncompleteLessons())
                    {
                        if (finalITutorialIDCurrentLesson != tutorials[iSlot].getTutorialID())
                            user.player.sendMessage(Display.errorText("You cannot start a new tutorial before you finish your current one"));
                        else
                            startTheLesson = true;
                    }
                    else
                    {
                        startTheLesson = true;
                    }

                    if (startTheLesson)
                    {
                        //Creates a Lesson object
                        Lesson newLesson = new Lesson(user, plugin, tutorials[iSlot]);

                        //Launches them into the new lesson
                        if (newLesson.startLesson(false))
                        {
                            //Delete the gui
                            delete();
                            user.mainGui = null;
                        }
                        //It might fail due to the user being in a lesson currently, in which case a message will already have been displayed
                        else if (user.hasIncompleteLessons())
                        {
                        }
                        else
                            user.player.sendMessage(Display.errorText("A problem occurred, please let staff know"));
                    }
                }
            });
        }
    }

    /**
     * Clears items from the GUI, recreates the items and then opens the menu
     */
    @Override
    public void refresh() {
        //Clears items from the gui
        this.clearGui();

        //Adds icons to the gui
        this.getInventory().setContents(getGUI(tutorials).getContents());

        //Adds click actions to the gui
        this.setActions();

        //Opens the gui
        this.open(user);
    }
}
