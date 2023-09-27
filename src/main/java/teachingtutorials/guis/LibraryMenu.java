package teachingtutorials.guis;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.tutorials.Lesson;
import teachingtutorials.tutorials.Tutorial;
import teachingtutorials.utils.User;
import teachingtutorials.utils.Utils;

public class LibraryMenu extends Gui
{
    private static final Component inventoryName = Component.text("Library", Style.style(TextDecoration.BOLD, NamedTextColor.DARK_AQUA));
    private final TeachingTutorials plugin;
    private final User user;

    private Tutorial[] tutorials;

    /**
     *
     * @param plugin An instance of the plugin
     * @param user The user for which the menu is being created for
     * @param tutorials A list of all in use tutorials which have locations
     */
    public LibraryMenu(TeachingTutorials plugin, User user, Tutorial[] tutorials)
    {
        super(getGUI(tutorials));
        this.plugin = plugin;
        this.user = user;
        this.tutorials = tutorials;

        setActions();
    }

    private static Inventory getGUI (Tutorial[] tutorials)
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
            ItemStack noTutorials = Utils.createItem(Material.BARRIER, 1, Component.text("There are no tutorials available to play currently", NamedTextColor.GREEN));
            inventory.setItem(5-1, noTutorials);
        }

        //Adds the tutorials to the menu options
        ItemStack tutorial;
        for (i = 0 ; i < iTutorials ; i++)
        {
            tutorial = Utils.createItem(Material.KNOWLEDGE_BOOK, 1,
                    Component.text(tutorials[i].szTutorialName, NamedTextColor.GREEN, TextDecoration.BOLD),
                    Component.text("Tutor: " +Bukkit.getOfflinePlayer(tutorials[i].uuidAuthor).getName(), NamedTextColor.DARK_GREEN));
            inventory.setItem(i, tutorial);
        }

        //Adds back button
        ItemStack back = Utils.createItem(Material.SPRUCE_DOOR, 1, Component.text("Back to main menu", NamedTextColor.GREEN, TextDecoration.BOLD));
        inventory.setItem((iRows * 9) - 1, back);

        //Inv slot 0 = the first one
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

        //Inv slot 0 = the first one
        //Adds the actions of each slot
        for (i = 0 ; i < tutorials.length ; i++)
        {
            int iSlot = i;
            setAction(iSlot, new Gui.guiAction() {
                @Override
                public void rightClick(User u) {
                    leftClick(u);
                }

                @Override
                public void leftClick(User u)
                {
                    //Creates a NewLocation object
                    Lesson newLesson = new Lesson(user, plugin, tutorials[iSlot]);

                    //Launches them into the new location adding process
                    if (newLesson.startLesson())
                        delete();
                        user.mainGui = null;
                }
            });
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
