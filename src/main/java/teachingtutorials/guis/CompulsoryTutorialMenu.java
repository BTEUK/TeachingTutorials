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
import teachingtutorials.tutorials.Tutorial;
import teachingtutorials.utils.User;
import teachingtutorials.utils.Utils;


public class CompulsoryTutorialMenu extends Gui
{
    private static final Component inventoryName = Component.text("Select Compulsory Tutorial", Style.style(TextDecoration.BOLD, NamedTextColor.DARK_AQUA));
    private final TeachingTutorials plugin;
    private final User user;

    private Tutorial[] tutorials;

    private static final Material compulsoryBlock = Material.LECTERN;
    private static final Material nonCompulsoryBlock = Material.BOOKSHELF;

    public CompulsoryTutorialMenu(TeachingTutorials plugin, User user, Tutorial[] tutorials)
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

        //Create inventories
        inventory = Bukkit.createInventory(null, iRows * 9);
        inventory.clear();

        Inventory toReturn = Bukkit.createInventory(null, iRows * 9, inventoryName);

        //Indicates that the creator has no tutorials if they don't own any
        if (iTutorials == 0)
        {
            ItemStack noTutorials = Utils.createItem(Material.BARRIER, 1, Component.text("There are no in-use tutorials on the system", NamedTextColor.GREEN));
            inventory.setItem(5-1, noTutorials);
        }

        //Adds back button
        ItemStack back = Utils.createItem(Material.SPRUCE_DOOR, 1, Component.text("Back to creator menu", NamedTextColor.GREEN, TextDecoration.BOLD));
        inventory.setItem((iRows * 9)-1, back);

        //Inv slot 0 = the first one
        //Add the tutorials to the gui
        for (i = 0 ; i < tutorials.length ; i++)
        {
            ItemStack tutorial;
            if (tutorials[i].bCompulsory)
            {
                tutorial = Utils.createItem(compulsoryBlock, 1,
                        Component.text(tutorials[i].szTutorialName, NamedTextColor.GREEN, TextDecoration.BOLD),
                        Component.text(Bukkit.getPlayer(tutorials[i].uuidAuthor).getName(), NamedTextColor.DARK_GREEN));
            }
            else
            {
                tutorial = Utils.createItem(nonCompulsoryBlock, 1,
                        Component.text(tutorials[i].szTutorialName, NamedTextColor.GREEN),
                        Component.text(Bukkit.getPlayer(tutorials[i].uuidAuthor).getName(), NamedTextColor.DARK_GREEN));
            }
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

        //Inv slot 0 = the first one
        //Adds the actions of each slot
        for (i = 0 ; i < tutorials.length ; i++)
        {
            int iSlot = i;
            setAction(iSlot, new guiAction() {
                @Override
                public void rightClick(User u) {
                    leftClick(u);
                }

                @Override
                public void leftClick(User u)
                {
                    //Toggles whether the tutorial is compulsory
                    tutorials[iSlot].toggleCompulsory();

                    //Refreshes the display
                    refresh();
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
