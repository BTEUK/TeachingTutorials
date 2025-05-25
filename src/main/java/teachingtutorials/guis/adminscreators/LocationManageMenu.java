package teachingtutorials.guis.adminscreators;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.guis.Gui;
import teachingtutorials.guis.LessonContinueConfirmer;
import teachingtutorials.guis.TutorialGUIUtils;
import teachingtutorials.listeners.texteditorbooks.BookCloseAction;
import teachingtutorials.listeners.texteditorbooks.TextEditorBookListener;
import teachingtutorials.tutorialobjects.LessonObject;
import teachingtutorials.tutorialobjects.Location;
import teachingtutorials.tutorialplaythrough.Lesson;
import teachingtutorials.utils.*;

public class LocationManageMenu extends Gui
{
    /** A reference to the instance of the TeachingTutorials plugin */
    private final TeachingTutorials plugin;

    /** A reference to the user which this menu is for */
    private final User user;

    /** A reference to the parent Tutorial Locations Menu */
    private final TutorialLocationsMenu tutorialLocationsMenu;

    /** A reference to the Location which this menu is for */
    private final Location location;

    /** The list of lessons that this player has ongoing */
    private final LessonObject[] lessons;

    private final TextEditorBookListener nameEditor;

    public LocationManageMenu(TeachingTutorials plugin, User user, TutorialLocationsMenu tutorialLocationsMenu, Location location)
    {
        super(27, TutorialGUIUtils.inventoryTitle(location.getLocationName()));

        this.plugin = plugin;
        this.user = user;
        this.tutorialLocationsMenu = tutorialLocationsMenu;
        this.location = location;

        this.lessons = LessonObject.getUnfinishedLessonsOfPlayer(user.player.getUniqueId(), plugin.getDBConnection(), plugin.getLogger());

        this.nameEditor = new TextEditorBookListener(plugin, user, this, "Edit Location Name", new BookCloseAction() {
            @Override
            public boolean runBookClose(BookMeta oldBookMeta, BookMeta newBookMeta, TextEditorBookListener textEditorBookListener, String szNewContent) {
                //Unregister the listener
                textEditorBookListener.unregister();

                //Remove the book
                user.player.getInventory().getItemInMainHand().setAmount(0);

                if (szNewContent.length() > 100)
                    return false;
                else
                {
                    //Updates the name of the tutorial
                    location.updateName(plugin.getDBConnection(), plugin.getLogger(), szNewContent);
                    return true;
                }
            }

            @Override
            public void runPostClose()
            {
                //Updates the title of the menu and opens the inventory with the new title
                LocationManageMenu.super.editName(TutorialGUIUtils.inventoryTitle(location.getLocationName()), user);
                //Refreshes the parent menu with the new name
                tutorialLocationsMenu.refreshLocationIcons();
            }
        }, location.getLocationName());

        //Load the menu
        addItems();
    }

    /**
     * Adds the icons and actions to the menu
     */
    private void addItems()
    {
        //Toggle in use
        ItemStack inUseIcon;
        if (location.isInUse())
            inUseIcon = Utils.createItem(Material.KNOWLEDGE_BOOK, 1, TutorialGUIUtils.optionTitle("In Use"),
                    TutorialGUIUtils.optionLore("Click click to remove from use"));
        else
            inUseIcon = Utils.createItem(Material.BOOK, 1, TutorialGUIUtils.optionTitle("Not In Use"),
                    TutorialGUIUtils.optionLore("Click to set in use"));

        super.setItem(10, inUseIcon, new guiAction() {
            @Override
            public void rightClick(User u) {
                leftClick(u);
            }

            @Override
            public void leftClick(User u) {
                location.toggleInUse();
                refresh();
            }
        });

        //Teleport to location
        super.setItem(12, Utils.createItem(Material.MAP, 1, TutorialGUIUtils.optionTitle("Teleport to Location")), new guiAction() {
            @Override
            public void rightClick(User u) {
                leftClick(u);
            }

            @Override
            public void leftClick(User u) {
                double[] startCoordinates = Location.getStartCoordinatesOfLocation(plugin.getDBConnection(), plugin.getLogger(), location.getLocationID());
                if (startCoordinates == null)
                {

                }
                else
                {
                    org.bukkit.Location startLocation = GeometricUtils.convertToBukkitLocation(location.getWorld(), startCoordinates[0], startCoordinates[1]);
                    startLocation.setYaw((float) startCoordinates[2]);
                    startLocation.setPitch((float) startCoordinates[3]);
                    u.player.teleport(startLocation);
                    u.player.closeInventory();
                }
            }
        });


        //Start lesson with this location
        super.setItem(14, Utils.createItem(Material.LECTERN, 1, TutorialGUIUtils.optionTitle("Start Lesson"), TutorialGUIUtils.optionLore("Start a lesson with this location")), new guiAction() {
            @Override
            public void rightClick(User u) {
                leftClick(u);
            }

            @Override
            public void leftClick(User u) {
                startTutorial(location);
            }
        });

        //Change name
        ItemStack changeName = Utils.createItem(Material.NAME_TAG, 1, TutorialGUIUtils.optionTitle("Change Location Name"),
                TutorialGUIUtils.optionLore("Update the name of this location"));
        super.setItem(16, changeName, new guiAction() {
            @Override
            public void rightClick(User u) {
                leftClick(u);
            }
            @Override
            public void leftClick(User u) {
                u.player.sendMessage(Display.aquaText("Use the name editor book to enter a new name. It must be no more than 100 characters"));
                nameEditor.startEdit("Location Name Editor");
            }
        });

        //Back
        ItemStack back = Utils.createItem(Material.SPRUCE_DOOR, 1, TutorialGUIUtils.backButton("Back to tutorial locations menu"));
        setItem(26, back, new guiAction() {
            @Override
            public void rightClick(User u) {
                leftClick(u);
            }

            @Override
            public void leftClick(User u) {
                u.mainGui = tutorialLocationsMenu;
                u.mainGui.open(u);
            }
        });
    }

    /**
     * Handles the logic when a player wishes to start a tutorial
     * @param locationToStart A reference to the Location that the player wishes to start
     * @return
     */
    public boolean startTutorial(Location locationToStart)
    {
        //Check whether the player already has a current lesson for this tutorial
        boolean bLessonFound = false;
        for (LessonObject lesson : lessons)
        {
            //Open confirmation menu
            //If location matters then check that
            if (locationToStart != null)
            {
                if (locationToStart.getLocationID() == lesson.getLocation().getLocationID())
                {
                    bLessonFound = true;

                    user.mainGui = new LessonContinueConfirmer(plugin, user, this, lesson, "You have a lesson at this location already");
                    user.mainGui.open(user);
                    //Break, let the other menu take over
                    break;
                }
            }
        }

        //If player doesn't have current lesson for this tutorial then create a new one
        if (!bLessonFound)
        {
            Lesson lesson = new Lesson(user, plugin, locationToStart);
            return lesson.startLesson(true);
        }
        else
        {
            return true;
        }
    }

    /**
     * Refresh the gui.
     * This usually involves clearing the content and recreating it.
     */
    @Override
    public void refresh()
    {
        //Clears items from the gui
        this.clearGui();

        //Adds the items back
        this.addItems();

        //Opens the gui
        this.open(user);
    }
}
