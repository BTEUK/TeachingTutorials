package teachingtutorials.guis.locationcreatemenus;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.guis.Gui;
import teachingtutorials.guis.TutorialGUIUtils;
import teachingtutorials.listeners.texteditorbooks.BookCloseAction;
import teachingtutorials.listeners.texteditorbooks.TextEditorBookListener;
import teachingtutorials.tutorialobjects.LocationStep;
import teachingtutorials.tutorialplaythrough.PlaythroughMode;
import teachingtutorials.tutorialplaythrough.StepPlaythrough;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.User;
import teachingtutorials.utils.Utils;

/**
 * A menu accessible to a creator when creating a new location, used to set the step instructions,
 * the step start location and the hologram location of whatever step the creator is currently at
 */
public class StepEditorMenu extends Gui
{
    /** Notes the size of the inventory */
    private static final int iInvSize = 27;

    /** A reference to the TeachingTutorials plugin instance */
    private final TeachingTutorials plugin;

    /** A reference to the user for which this menu is for */
    private final User user;

    /** A reference to the step which the creator is playing through when making edits using this menu */
    private final StepPlaythrough stepPlaythrough;

    /** A reference to the LocationStep which this menu is designed to allowing editing of */
    private final LocationStep locationStep;

    /** A book editing listener used for specifying the instructions */
    private final TextEditorBookListener instructionsBookListener;

    /** A book editing listener used for specifying a link to a video walkthrough of this LocationStep */
    private final TextEditorBookListener videoLinkBookListener;

    /**
     *
     * @param plugin An instance of the TeachingTutorials plugin
     * @param user A reference to the user for which the menu is being created for
     * @param stepPlaythrough A reference to the step which the user is currently playing through
     * @param locationStep A reference to the LocationStep for which we are setting the information of
     */
    public StepEditorMenu(TeachingTutorials plugin, User user, StepPlaythrough stepPlaythrough, LocationStep locationStep)
    {
        super(iInvSize, getName(stepPlaythrough.getStep().getName()));
        this.plugin = plugin;
        this.user = user;
        this.stepPlaythrough = stepPlaythrough;
        this.locationStep = locationStep;

        //Sets up the books
        this.videoLinkBookListener = new TextEditorBookListener(plugin, user, this, locationStep.getStep().getName(), new BookCloseAction()
        {
            @Override
            public boolean runBookClose(BookMeta oldBookMeta, BookMeta newBookMeta, TextEditorBookListener textEditorBookListener, String szNewContent)
            {
                return bookClosed(oldBookMeta, newBookMeta, textEditorBookListener, szNewContent, false);
            }

            @Override
            public void runPostClose()
            {
            }
        }, locationStep.getVideoWalkthroughLink());

        this.instructionsBookListener = new TextEditorBookListener(plugin, user, this, locationStep.getStep().getName(), new BookCloseAction()
        {
            @Override
            public boolean runBookClose(BookMeta oldBookMeta, BookMeta newBookMeta, TextEditorBookListener textEditorBookListener, String szNewContent)
            {
                return bookClosed(oldBookMeta, newBookMeta, textEditorBookListener, szNewContent, true);
            }

            @Override
            public void runPostClose()
            {
            }
        }, locationStep.getInstructions());

        //Adds the items to the gui
        addMenuOptions();
    }

    /**
     *
     * @return A reference to the step playthrough managing this menu
     */
    public StepPlaythrough getStepPlaythrough()
    {
        return stepPlaythrough;
    }

    /**
     * @return Whether all of the extra information is set
     */
    public boolean getAllInformationSet()
    {
        return locationStep.isOtherInformationSet(plugin.getLogger());
    }

    /**
     * Creates and adds the menu options to this gui
     */
    public void addMenuOptions()
    {
        //Creates the menu item icons that are common to both menu designs
        ItemStack setStartLocation = Utils.createItem(Material.COMPASS, 1,
                TutorialGUIUtils.optionTitle("Set the step's start location"),
                TutorialGUIUtils.optionLore("Set the start location to your current position and direction"));

        ItemStack instructions = Utils.createItem(Material.WRITABLE_BOOK, 1,
                TutorialGUIUtils.optionTitle("Set the instructions"));

        ItemStack teleportToStart = Utils.createItem(Material.VILLAGER_SPAWN_EGG, 1,
                TutorialGUIUtils.optionTitle("Teleport back to the start location of this step"));

        ItemStack videoLink = Utils.createItem(Material.PAINTING, 1,
                TutorialGUIUtils.optionTitle("Set the video tutorial link if one exists"),
                TutorialGUIUtils.optionLore("This is specific to each location of the tutorial"));

        //Tests whether a hologram is needed for this step and adds different menu icons depending on that
        boolean bIsHologramNeeded = stepPlaythrough.getStep().getInstructionDisplayType().equals(Display.DisplayType.hologram);
        if (bIsHologramNeeded)
        {
            //Set start location coordinates to current location
            setItem(10, setStartLocation, new guiAction() {
                @Override
                public void rightClick(User u) {
                    leftClick(u);
                }

                @Override
                public void leftClick(User u) {
                    setStartLocation(u.player.getLocation());
                }
            });

            //Set instructions
            setItem(12, instructions, new guiAction() {
                @Override
                public void rightClick(User u) {
                    leftClick(u);
                }

                @Override
                public void leftClick(User u) {
                    //Sets up the book listener and registers it
                    instructionsBookListener.startEdit("Instructions editor book");

                    u.player.sendMessage(Display.colouredText("Use the instructions editor book to set the instructions", NamedTextColor.GREEN));
                    //The listener unregisters itself once the book is closed. We parse the location step by reference so it can edit the link itself

                    //step.tryNextStep() is called via instructionsEdited() from TextEditorBookListener once the book close event occurs
                }
            });

            //Set hologram coordinates to player's current location
            ItemStack hologramLocation = Utils.createItem(Material.FILLED_MAP, 1,
                    TutorialGUIUtils.optionTitle("Set the instructions hologram location"),
                    TutorialGUIUtils.optionLore("Set the instructions hologram to your current position"));

            setItem(14, hologramLocation, new guiAction() {
                @Override
                public void rightClick(User u) {
                    leftClick(u);
                }

                @Override
                public void leftClick(User u) {
                    locationStep.setHologramLocationToThatOfPlayer(stepPlaythrough, u.player, stepPlaythrough.getStep().getName());
                    stepPlaythrough.tryNextStep();
                }
            });

            //Set video link
            setItem(16, videoLink, new guiAction() {
                @Override
                public void rightClick(User u) {
                    leftClick(u);
                }

                @Override
                public void leftClick(User u) {
                    //Sets up the book listener and registers it
                    videoLinkBookListener.startEdit("Video link editor book");

                    u.player.sendMessage(Display.colouredText("Use the video link editor book to set the video link", NamedTextColor.GREEN));
                    //The listener unregisters itself once the book is closed. We parse the location step by reference so it can edit the link itself
                }
            });
        }
        else
        {
            //Set start location
            setItem(11, setStartLocation, new guiAction() {
                @Override
                public void rightClick(User u) {
                    leftClick(u);
                }

                @Override
                public void leftClick(User u) {
                    setStartLocation(u.player.getLocation());
                }
            });

            //Set instructions
            setItem(13, instructions, new guiAction() {
                @Override
                public void rightClick(User u) {
                    leftClick(u);
                }

                @Override
                public void leftClick(User u) {
                    //Sets up the book listener and registers it
                    instructionsBookListener.startEdit("Instructions editor book");

                    u.player.sendMessage(Display.colouredText("Use the instructions editor book to set the instructions", NamedTextColor.GREEN));
                    //The listener unregisters itself once the book is closed. We parse the location step by reference so it can edit the link itself

                    //step.tryNextStep() is called via instructionsEdited() from TextEditorBookListener once the book close event occurs
                }
            });

            //Set video link
            setItem(15, videoLink, new guiAction() {
                @Override
                public void rightClick(User u) {
                    leftClick(u);
                }

                @Override
                public void leftClick(User u) {
                    //Sets up the book listener and registers it
                    videoLinkBookListener.startEdit("Video link editor book");

                    u.player.sendMessage(Display.colouredText("Use the video link editor book to set the video link", NamedTextColor.GREEN));
                    //The listener unregisters itself once the book is closed. We parse the location step by reference so it can edit the link itself
                }
            });
        }

        //Teleport to start
        setItem(0, teleportToStart, new guiAction() {
            @Override
            public void rightClick(User u) {
                leftClick(u);
            }

            @Override
            public void leftClick(User u) {
                locationStep.teleportPlayerToStartOfStep(u.player, stepPlaythrough.getParentStage().getTutorialPlaythrough().getLocation().getWorld(), plugin);
            }
        });

        //Go to task editor menu

        //When this gets initialised the step has not yet started.

        //This is passed as a reference sort of thing, so the value will change
        //When this is first called on initialisation it will break because the current group/task are still at -1 so it won't be able to get the current one
        //We could just return null if it has not started yet

        LocationTaskEditorMenu editorMenu = stepPlaythrough.getCurrentTaskEditorMenu();
        if (editorMenu != null)
        {
            setItem(22, Utils.createItem(Material.IRON_DOOR, 1, TutorialGUIUtils.optionTitle("Task Editor Menu"), TutorialGUIUtils.optionLore("Click to edit the current task")), new guiAction() {
                @Override
                public void rightClick(User u) {
                    leftClick(u);
                }

                @Override
                public void leftClick(User u) {
                    u.mainGui = editorMenu;
                    u.mainGui.open(u);
                }
            });
        }

        //Links back to the navigator menu
        if (stepPlaythrough.getParentStage().getTutorialPlaythrough().getCurrentPlaythroughMode().equals(PlaythroughMode.EditingLocation))
        {
            super.setItem(18, Utils.createItem(Material.SPRUCE_DOOR, 1, TutorialGUIUtils.optionTitle("Back to Navigator"), TutorialGUIUtils.optionLore("Return to lesson mode")),
                    new guiAction() {
                        @Override
                        public void rightClick(User u) {
                            leftClick(u);
                        }
                        @Override
                        public void leftClick(User u) {
                            //If in edit mode, switch mode
                            if (StepEditorMenu.this.stepPlaythrough.getParentStage().getTutorialPlaythrough().getCurrentPlaythroughMode().equals(PlaythroughMode.EditingLocation))
                                stepPlaythrough.getParentStage().getTutorialPlaythrough().setCurrentPlaythroughMode(PlaythroughMode.PlayingLesson);

                            u.player.closeInventory();
                            stepPlaythrough.getParentStage().getTutorialPlaythrough().openNavigationMenu();
                        }
                    });
        }
        else if (stepPlaythrough.getParentStage().getTutorialPlaythrough().getCurrentPlaythroughMode().equals(PlaythroughMode.CreatingLocation))
        {
            super.setItem(18, Utils.createItem(Material.SPRUCE_DOOR, 1, TutorialGUIUtils.optionTitle("Back to Navigator"), TutorialGUIUtils.optionLore("")),
                    new guiAction() {
                        @Override
                        public void rightClick(User u) {
                            leftClick(u);
                        }
                        @Override
                        public void leftClick(User u) {
                            //If in edit mode, switch mode
                            if (StepEditorMenu.this.stepPlaythrough.getParentStage().getTutorialPlaythrough().getCurrentPlaythroughMode().equals(PlaythroughMode.EditingLocation))
                                stepPlaythrough.getParentStage().getTutorialPlaythrough().setCurrentPlaythroughMode(PlaythroughMode.PlayingLesson);

                            u.player.closeInventory();
                            stepPlaythrough.getParentStage().getTutorialPlaythrough().openNavigationMenu();
                        }
                    });
        }
    }

    /**
     * Updates the start location of the relevant step, updates the fall listener's safe location, and makes an attempt to move to the next step
     * @param playersLocation A location object referring to what the start location of the relevant LocationStep should be set to
     */
    private void setStartLocation(Location playersLocation)
    {
        //Updates the start location of the step
        locationStep.setStartLocation(playersLocation);
        //Updates the playthrough's safe location
        stepPlaythrough.getParentStage().getTutorialPlaythrough().setFallListenerSafeLocation(playersLocation);
        //Triggers the step to test whether it can finish and move to the next step
        stepPlaythrough.tryNextStep();
    }

    /**
     * Returns the name of the inventory
     * @param szStepName The title of the relevant step
     * @returns A string representation of a title for this GUI
     */
    public static Component getName(String szStepName)
    {
        Component inventoryName = TutorialGUIUtils.inventoryTitle("Step - " +szStepName);
        return inventoryName;
    }

    /**
     * Clears items from the GUI, recreates the items
     */
    @Override
    public void refresh()
    {
        this.clearGui();
        this.addMenuOptions();
    }

    /**
     * Triggers the step to test whether it can move forwards with the stage
     */
    public void instructionsEdited()
    {
        stepPlaythrough.tryNextStep();
    }


    /**
     * The procedure upon closure of a video link editor book
     * @param oldBookMeta The previous metadata of the book just closed.
     * @param newBookMeta The new metadata of the book just closed.
     * @param textEditorBookListener A reference to the book listener itself which calls this. Enables unregistering to be called.
     * @param szNewContent The combined content of all pages in the new book.
     * @param bWasInstructions Whether the book was created for editing instructions (true) or the video link (false)
     * @return Whether to save the text (always true)
     */
    private boolean bookClosed(BookMeta oldBookMeta, BookMeta newBookMeta, TextEditorBookListener textEditorBookListener, String szNewContent, boolean bWasInstructions)
    {
        //Edits the step instructions or video link
        if (bWasInstructions)
            locationStep.setInstruction(this.getStepPlaythrough(), szNewContent, locationStep.getStep().getInstructionDisplayType(), user.player, locationStep.getStep().getName());
        else
            locationStep.setVideoLink(szNewContent);

        //Reopen the feature menu
        user.mainGui = this;
        user.mainGui.refresh();

        //Unregisters this listener
        textEditorBookListener.unregister();

        //Removes the book
        user.player.getInventory().getItemInMainHand().setAmount(0);

        //Informs the menu that some instructions were edited
        if (bWasInstructions)
            instructionsEdited();

        return true;
    }
}
