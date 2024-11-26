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
        //Sets up the menu with the icons already in place
        super(getGUI(tutorials));
        this.plugin = plugin;
        this.user = user;
        this.tutorials = tutorials;

        //Adds the click actions
        setActions();
    }

    /**
     * Creates an inventory design for the current status of a creator's tutorials
     * @param allTutorials A list of tutorials to include in the menu
     * @return An inventory filled with icons, informing the user of the current state of their tutorials, including which
     * ones are in use
     */
    private static Inventory getGUI (Tutorial[] allTutorials)
    {
        //Declare variables
        int i;
        int iTutorials;
        int iDiv;
        int iMod;
        int iRows;

        Inventory inventory;

        //Works out how many rows in the inventory are needed
        iTutorials = allTutorials.length;
        iDiv = iTutorials/9;
        iMod = iTutorials%9;

        if (iMod != 0 || iDiv == 0)
        {
            iDiv = iDiv + 1;
        }

        //Enables an empty row and then a row for the back button
        iRows = iDiv+2;

        //------------------------------

        //Create inventories
        inventory = Bukkit.createInventory(null, iRows * 9);
        inventory.clear();

        Inventory toReturn = Bukkit.createInventory(null, iRows * 9, inventoryName);

        //Inv slot 0 = the first one

        //Indicates that the creator has no tutorials if they don't own any
        if (allTutorials.length == 0)
        {
            ItemStack noTutorials = Utils.createItem(Material.BARRIER, 1, TutorialGUIUtils.optionTitle("You have no tutorials"));
            inventory.setItem(5-1, noTutorials);
        }

        //Adds back button
        ItemStack back = Utils.createItem(Material.SPRUCE_DOOR, 1, TutorialGUIUtils.backButton("Back to creator menu"));
        inventory.setItem((iRows * 9)-1, back);

        //Creates the menu options
        for (i = 0 ; i < allTutorials.length ; i++)
        {
            ItemStack tutorial;
            //Sets tutorial name bold for tutorials in use
            if (allTutorials[i].isInUse())
                tutorial = Utils.createItem(Material.WRITTEN_BOOK, 1,
                        TutorialGUIUtils.optionTitle(allTutorials[i].getTutorialName()).decoration(TextDecoration.BOLD, true),
                        TutorialGUIUtils.optionLore("In Use - Left click to remove from use"),
                        TutorialGUIUtils.optionLore("Right click to add a new location"));
            else
                tutorial = Utils.createItem(Material.BOOK, 1,
                        TutorialGUIUtils.optionTitle(allTutorials[i].getTutorialName()),
                        TutorialGUIUtils.optionLore("Not in Use - Left click to set in use"),
                        TutorialGUIUtils.optionLore("Right click to add a new location"));
            inventory.setItem(i, tutorial);
        }

        toReturn.setContents(inventory.getContents());
        return toReturn;
    }

    /**
     * Sets the click actions for each slot of the menu
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

        //------------------------------

        //Inv slot 0 = the first one

        //Adds back button
        setAction((iRows * 9) - 1, new guiAction() {
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

        //Creates the actions for clicking on the tutorials
        for (i = 0 ; i < tutorials.length ; i++)
        {
            int iSlot = i;

            setAction(i, new guiAction() {
                @Override
                public void rightClick(User u) {
                    rightClicked(u, iSlot);
                }

                @Override
                public void leftClick(User u) {
                    leftClicked(u, iSlot);
                }
            });
        }
    }

    //Left click is for toggling whether a tutorial is in use or not
    private void leftClicked(User u, int iSlot)
    {
        if (tutorials[iSlot].toggleInUse(plugin))
        {
            this.refresh();
        }
        else
        {
            u.player.sendMessage(Display.errorText("There are no locations for this tutorial"));
        }
    }

    //Right click is for creating a new location
    private void rightClicked(User u, int iSlot)
    {
        //Only starts the new location process if the creator is idle
        if (user.getCurrentMode().equals(Mode.Idle))
        {
            delete();
            u.mainGui = null;

            //Creates a NewLocation object
            NewLocation newLocation = new NewLocation(user, tutorials[iSlot], plugin);

            //Launches them into the new location adding process
            newLocation.launchNewLocationAdding();
        }
        else
        {
            user.player.sendMessage(Display.errorText("Complete or pause your current tutorial or location creation first"));
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
