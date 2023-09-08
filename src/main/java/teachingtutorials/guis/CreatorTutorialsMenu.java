package teachingtutorials.guis;

/**
    Purpose = Displays a list of tutorials that a creator has made
 */

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.newlocation.NewLocation;
import teachingtutorials.tutorials.Tutorial;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.Mode;
import teachingtutorials.utils.User;
import teachingtutorials.utils.Utils;

public class CreatorTutorialsMenu extends Gui
{
    private static final Component inventoryName = Component.text("Your tutorials", Style.style(TextDecoration.BOLD, NamedTextColor.DARK_AQUA));
    private final TeachingTutorials plugin;
    private final User user;

    private Tutorial[] tutorials;

    /**
     *
     * @param plugin An instance of the plugin
     * @param user The user for which the menu is being created for
     * @param tutorials A list of all of the tutorials of the user requesting the menu
     */
    public CreatorTutorialsMenu(TeachingTutorials plugin, User user, Tutorial[] tutorials)
    {
        super(getGUI(tutorials));
        this.plugin = plugin;
        this.user = user;
        this.tutorials = tutorials;

        setActions();
    }

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
            ItemStack noTutorials = Utils.createItem(Material.BARRIER, 1, Component.text("You have no tutorials", NamedTextColor.GREEN));
            inventory.setItem(5-1, noTutorials);
        }

        //Adds back button
        ItemStack back = Utils.createItem(Material.SPRUCE_DOOR, 1, Component.text("Back to creator menu", NamedTextColor.GREEN, TextDecoration.BOLD));
        inventory.setItem((iRows * 9)-1, back);

        //Creates the menu options
        for (i = 0 ; i < allTutorials.length ; i++)
        {
            ItemStack tutorial;
            //Sets tutorial name bold for tutorials in use
            if (allTutorials[i].bInUse)
                tutorial = Utils.createItem(Material.WRITTEN_BOOK, 1,
                        Component.text(allTutorials[i].szTutorialName, NamedTextColor.GREEN, TextDecoration.BOLD),
                        Component.text("In Use - Left click to remove from use", NamedTextColor.DARK_GREEN),
                        Component.text("Right click to add a new location", NamedTextColor.DARK_GREEN));
            else
                tutorial = Utils.createItem(Material.BOOK, 1,
                        Component.text(allTutorials[i].szTutorialName, NamedTextColor.GREEN),
                        Component.text("Not in Use - Left click to set in use", NamedTextColor.DARK_GREEN),
                        Component.text("Right click to add a new location", NamedTextColor.DARK_GREEN));
            inventory.setItem(i, tutorial);
        }

        toReturn.setContents(inventory.getContents());
        return toReturn;
    }

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
                u.mainGui = new AdminMenu(plugin, user);
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
        if (tutorials[iSlot].toggleInUse())
        {
            this.refresh();
        }
        else
        {
            Display display = new Display(u.player, ChatColor.RED +"There are no locations for this tutorial");
            display.Message();
        }
    }

    //Right click is for creating a new location
    private void rightClicked(User u, int iSlot)
    {
        //Only starts the new location process if the creator is idle
        if (user.currentMode.equals(Mode.Idle))
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
            Display display = new Display(user.player, (ChatColor.RED +"Complete or pause your current tutorial or location creation first"));
            display.Message();
        }
    }

    @Override
    public void refresh() {
        this.clearGui();
        this.getInventory().setContents(getGUI(tutorials).getContents());
        this.setActions();
        this.open(user);
    }
}
