package teachingtutorials.guis;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.newlocation.NewLocation;
import teachingtutorials.tutorialobjects.Tutorial;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.Mode;
import teachingtutorials.utils.User;
import teachingtutorials.utils.Utils;

/**
 * A menu listing the tutorials that the given creator has made and allows them to manage them
 */
public class CreatorTutorialsMenu extends Gui
{
    /** Stores the name of the inventory */
    private static final Component inventoryName = TutorialGUIUtils.inventoryTitle("My Tutorials");

    /** A reference to the TeachingTutorials plugin instance */
    private final TeachingTutorials plugin;

    /** A reference to the user for which this menu is for */
    private final User user;

    /** Stores a list of all tutorials by the relevant player */
    private Tutorial[] tutorials;

    /**
     *
     * @param plugin An instance of the plugin
     * @param user The user for which the menu is being created for
     * @param tutorials A list of all of the tutorials of the creator requesting the menu
     */
    public CreatorTutorialsMenu(TeachingTutorials plugin, User user, Tutorial[] tutorials)
    {
        super(calculateNumRows(tutorials.length)*9, inventoryName);
        this.plugin = plugin;
        this.user = user;
        this.tutorials = tutorials;

        //Adds the items
        addItems();
    }

    /**
     * Populates the menu with icons and actions
     */
    private void addItems()
    {
        //Indicates that the creator has no tutorials
        if (tutorials.length == 0)
            setItem(5-1, Utils.createItem(Material.BARRIER, 1, TutorialGUIUtils.optionTitle("You have no tutorials")));

        //Adds the tutorial icons
        int iNumTutorials = tutorials.length;
        if (iNumTutorials > 45)
            iNumTutorials = 45;
        for (int i = 0 ; i < iNumTutorials ; i++)
        {
            int finalI = i;
            setItem(i, Utils.createItem(Material.WRITTEN_BOOK, 1,
                            TutorialGUIUtils.optionTitle(tutorials[i].getTutorialName()),
                            TutorialGUIUtils.optionLore("Click to manage Tutorial")),
                    new guiAction() {
                        @Override
                        public void rightClick(User u) {
                            leftClick(u);
                        }

                        @Override
                        public void leftClick(User u) {
                            u.mainGui = new TutorialManageMenu(plugin, user, tutorials[finalI], CreatorTutorialsMenu.this);
                            u.mainGui.open(u);
                        }
                    });
        }

        //Adds back button
        ItemStack back = Utils.createItem(Material.SPRUCE_DOOR, 1, TutorialGUIUtils.backButton("Back to creator menu"));
        setItem((calculateNumRows(tutorials.length) * 9) - 1, back, new guiAction() {
            @Override
            public void rightClick(User u) {
                leftClick(u);
            }

            @Override
            public void leftClick(User u) {
                delete();
                u.mainGui = new CreatorMenu(plugin, user);
                u.mainGui.open(u);
            }
        });
    }

    /**
     * Clears items from the GUI, recreates the items and then opens the menu
     */
    @Override
    public void refresh() {
        //Clears items from the gui
        this.clearGui();

        //Adds the items back
        this.addItems();

        //Opens the gui
        this.open(user);
    }

    /**
     * Refreshes the name on the tutorial icons
     */
    public void refreshTutorialIcons()
    {
        //Adds the tutorial icons
        int iNumTutorials = tutorials.length;
        if (iNumTutorials > 45)
            iNumTutorials = 45;
        for (int i = 0 ; i < iNumTutorials ; i++)
        {
            setItem(i, Utils.createItem(Material.WRITTEN_BOOK, 1,
                            TutorialGUIUtils.optionTitle(tutorials[i].getTutorialName()),
                            TutorialGUIUtils.optionLore("Click to manage Tutorial")));
        }
    }

    /**
     * Calculates the number of rows needed to form an inventory for a given number of tutorials
     * @param iNumTutorials The number of tutorials
     * @return The number of rows needed to form an inventory
     */
    public static int calculateNumRows(int iNumTutorials)
    {
        int iDiv;
        int iMod;

        //Works out how many rows in the inventory are needed
        iDiv = iNumTutorials/9;
        iMod = iNumTutorials%9;

        if (iMod != 0 || iDiv == 0)
        {
            iDiv = iDiv + 1;
        }

        //Add row for the back button
        int iRows = iDiv+1;

        //Add an extra for pleasure
        if (iRows < 6)
            iRows++;

        else if (iRows == 6)
        {
            // Do nothing
        }
        else
        {
            iRows = 6;
        }

        //Enables an empty row and then a row for the back button
        return iRows;
    }
}
