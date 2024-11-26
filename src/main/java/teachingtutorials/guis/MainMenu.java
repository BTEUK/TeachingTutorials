package teachingtutorials.guis;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.tutorialplaythrough.Lesson;
import teachingtutorials.tutorialobjects.Tutorial;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.User;
import teachingtutorials.utils.Utils;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The main menu of the TeachingTutorials system from which all functions can be accessed
 */
public class MainMenu extends Gui
{
    /** Stores the name of the inventory */
    private static final Component inventoryName = TutorialGUIUtils.inventoryTitle("Tutorials Menu");

    /** Notes the size of the inventory */
    private static final int iNumRows = 27;

    /** A reference to the TeachingTutorials plugin instance */
    private final TeachingTutorials plugin;

    /** A reference to the user for which this menu is for */
    private final User user;

    /**
     *
     * @param plugin A reference to the instance of the TeachingTutorials plugin
     * @param user A reference to the user for which this menu is for
     */
    public MainMenu(TeachingTutorials plugin, User user)
    {
        super(iNumRows, inventoryName);
        this.plugin = plugin;
        this.user = user;

        //Adds the icons and actions to the menu
        addMenuOptions();
    }

    /**
     * Adds the menu options to the menu
     */
    private void addMenuOptions()
    {
        //Initiates the current tutorial object
        Tutorial currentTutorial = null;

        //Initiates the next tutorial object - decides the next tutorial
        Tutorial nextTutorial = Lesson.decideTutorial(user, plugin.getDBConnection(), plugin.getLogger());
        if (nextTutorial == null)
            return;

        FileConfiguration config = TeachingTutorials.getInstance().getConfig();
        boolean bCompulsoryTutorialEnabled = config.getBoolean("Compulsory_Tutorial");

        //Get compulsory tutorial ID
        int iCompulsoryTutorialID;

        Tutorial[] compulsoryTutorials = Tutorial.fetchAll(true, true, null, plugin.getDBConnection(), plugin.getLogger());
        if (compulsoryTutorials.length == 0)
            iCompulsoryTutorialID = -1;
        else
            iCompulsoryTutorialID = compulsoryTutorials[0].getTutorialID();

        //'Continue' menu button
        ItemStack continueLearning_CompulsoryComplete;
        if (user.hasIncompleteLessons())
        {
            //Get current tutorial ID and sets up the tutorial object from this
            int iTutorialIDCurrentLesson = Lesson.getTutorialOfCurrentLessonOfPlayer(user.player.getUniqueId(), plugin.getDBConnection(), plugin);
            plugin.getLogger().log(Level.INFO, "Player is currently playing a lesson on tutorial: "+iTutorialIDCurrentLesson);
            currentTutorial = Tutorial.fetchByTutorialID(iTutorialIDCurrentLesson, plugin.getDBConnection(), plugin.getLogger());

            //Sets up the menu icon with the name of the current tutorial
            continueLearning_CompulsoryComplete = Utils.createItem(Material.WRITABLE_BOOK, 1,
                    TutorialGUIUtils.optionTitle("Resume Your Lesson"),
                    TutorialGUIUtils.optionLore(currentTutorial.getTutorialName()));
        }
        else
        {
            //Sets up the menu icon with the new tutorial's name
            continueLearning_CompulsoryComplete = Utils.createItem(Material.WRITABLE_BOOK, 1,
                    TutorialGUIUtils.optionTitle("Start a new Tutorial:"),
                    TutorialGUIUtils.optionLore(nextTutorial.getTutorialName()));
        }

        //Tutorial library
        ItemStack tutorialLibrary = Utils.createItem(Material.BOOKSHELF, 1,
                TutorialGUIUtils.optionTitle("Tutorial Library"),
                TutorialGUIUtils.optionLore("Browse all of our available tutorials"));


        //-----------------------------------------------------------------------
        //--------------------- Begin Adding the Menu Items ---------------------
        //-----------------------------------------------------------------------

        //Checks the system has the compulsory tutorial feature enabled
        if (bCompulsoryTutorialEnabled && iCompulsoryTutorialID >= 0)
        {
            //Fetches the details of the compulsory tutorial
            Tutorial compulsoryTutorial = Tutorial.fetchByTutorialID(iCompulsoryTutorialID, plugin.getDBConnection(), plugin.getLogger());

            //User has already completed the compulsory tutorial
            if (user.bHasCompletedCompulsory)
            {
                //---- Compulsory Tutorial Option ----
                ItemStack compulsory;

                boolean bUserRedoingCompulsoryTutorial;
                if (user.hasIncompleteLessons())
                    bUserRedoingCompulsoryTutorial = currentTutorial.getTutorialID() == iCompulsoryTutorialID;
                else
                    bUserRedoingCompulsoryTutorial = false;
                //User is currently redoing the compulsory tutorial
                if (bUserRedoingCompulsoryTutorial)
                {
                    compulsory = Utils.createItem(Material.BOOK, 1,
                            TutorialGUIUtils.optionTitle("Restart the Starter Tutorial"),
                            TutorialGUIUtils.optionLore("Restart the starter tutorial again"));

                    super.setItem(11 - 1, compulsory, new guiAction() {
                        @Override
                        public void rightClick(User u) {
                            leftClick(u);
                        }
                        @Override
                        public void leftClick(User u) {
                            if (performEvent(EventType.RESTART, u, plugin, compulsoryTutorial))
                                //Deletes this gui
                                delete();
                        }
                    });
                }
                //User is currently in a different tutorial
                else if (user.hasIncompleteLessons())
                {
                    compulsory = Utils.createItem(Material.ENCHANTED_BOOK, 1,
                            TutorialGUIUtils.optionTitle("Restart the Starter Tutorial"),
                            TutorialGUIUtils.optionLore("Finish your current lesson first!"));
                    super.setItem(11 - 1, compulsory, new guiAction() {
                        @Override
                        public void rightClick(User u)
                        {
                            leftClick(u);
                        }
                        @Override
                        public void leftClick(User u)
                        {
                            //Do nothing
                        }
                    });

                }
                //User is not in any tutorial
                else
                {
                    compulsory = Utils.createItem(Material.BOOK, 1,
                            TutorialGUIUtils.optionTitle("Restart the Starter Tutorial"),
                            TutorialGUIUtils.optionLore("Refresh your essential knowledge"));

                    super.setItem(11 - 1, compulsory, new guiAction() {
                        @Override
                        public void rightClick(User u) {
                            leftClick(u);
                        }
                        @Override
                        public void leftClick(User u) {
                            if (performEvent(EventType.RESTART, u, plugin, compulsoryTutorial))
                                //Deletes this gui
                                delete();
                        }
                    });
                }

                //---------- Library Option ----------
                super.setItem(14 - 1, tutorialLibrary, new guiAction() {
                    @Override
                    public void rightClick(User u) {
                        leftClick(u);
                    }
                    @Override
                    public void leftClick(User u) {
                        if (performEvent(EventType.LIBRARY, u, plugin, null))
                            //Deletes this gui
                            delete();
                    }
                });

                //Decide on continue option
                Tutorial tutorialForContinue;
                if (user.hasIncompleteLessons())
                    tutorialForContinue = currentTutorial;
                else
                    tutorialForContinue = nextTutorial;

                //----- Continue Learning Option -----
                super.setItem(17 - 1, continueLearning_CompulsoryComplete, new guiAction() {
                    @Override
                    public void rightClick(User u)
                    {
                        leftClick(u);
                    }
                    @Override
                    public void leftClick(User u)
                    {
                        if (performEvent(EventType.CONTINUE, u, plugin, tutorialForContinue))
                            //Deletes this gui
                            delete();
                    }
                });
            }

            // They have not completed the compulsory tutorial
            else
            {
                ItemStack resumeCompulsory;
                ItemStack restartCompulsory;
                ItemStack beginCompulsory;

                //They are currently in the compulsory tutorial for the first time
                //OR: The user was in another lesson and someone added a compulsory tutorial to the system
                if (user.hasIncompleteLessons())
                {
                    //Player is in a lesson other than the compulsory but hasn't started the compulsory (compulsory added to system)
                    if (currentTutorial.getTutorialID() != iCompulsoryTutorialID)
                    {
                        ItemStack erroneousTutorial = Utils.createItem(Material.BOOK, 1,
                                TutorialGUIUtils.optionTitle("Continue Your Tutorial"),
                                TutorialGUIUtils.optionLore("You must then complete the starter tutorial"));

                        Tutorial finalCurrentTutorial = currentTutorial;
                        super.setItem(14 - 1, erroneousTutorial, new guiAction() {
                            @Override
                            public void rightClick(User u) {
                                leftClick(u);
                            }
                            @Override
                            public void leftClick(User u) {
                                if (performEvent(EventType.CONTINUE, u, plugin, finalCurrentTutorial))
                                    //Deletes this gui
                                    delete();
                            }
                        });
                    }

                    //Player is half way through the compulsory tutorial but hasn't ever finished it
                    else
                    {
                        restartCompulsory = Utils.createItem(Material.BOOK, 1,
                                TutorialGUIUtils.optionTitle("Restart the Starter Tutorial"),
                                TutorialGUIUtils.optionLore("Gain the " +config.getString("Compulsory_Tutorial_RankNew") +" rank"));

                        resumeCompulsory = Utils.createItem(Material.WRITABLE_BOOK, 1,
                                TutorialGUIUtils.optionTitle("Resume the Starter Tutorial"),
                                TutorialGUIUtils.optionLore("Gain the " +config.getString("Compulsory_Tutorial_RankNew") +" rank"));

                        super.setItem(12 - 1, restartCompulsory, new guiAction() {
                            @Override
                            public void rightClick(User u) {
                                leftClick(u);
                            }
                            @Override
                            public void leftClick(User u) {
                                if (performEvent(EventType.RESTART, u, plugin, compulsoryTutorial))
                                    //Deletes this gui
                                    delete();
                            }
                        });

                        super.setItem(16 - 1, resumeCompulsory, new guiAction() {
                            @Override
                            public void rightClick(User u) {
                                leftClick(u);
                            }
                            @Override
                            public void leftClick(User u) {
                                if (performEvent(EventType.CONTINUE, u, plugin, compulsoryTutorial))
                                    //Deletes this gui
                                    delete();
                            }
                        });
                    }
                }
                else // They have never started the compulsory tutorial
                {
                    beginCompulsory = Utils.createItem(Material.BOOK, 1,
                            TutorialGUIUtils.optionTitle("Begin the Starter Tutorial"),
                            TutorialGUIUtils.optionLore("Gain the " +config.getString("Compulsory_Tutorial_RankNew") +" rank"));

                    super.setItem(14 - 1, beginCompulsory, new guiAction() {
                        @Override
                        public void rightClick(User u) {
                            leftClick(u);
                        }
                        @Override
                        public void leftClick(User u) {
                            if (performEvent(EventType.RESTART, u, plugin, compulsoryTutorial))
                                //Deletes this gui
                                delete();
                        }
                    });
                }
            }
        }
        else // There is no compulsory tutorial
        {
            //Decide on continue option
            Tutorial tutorialForContinue;
            if (user.hasIncompleteLessons())
                tutorialForContinue = currentTutorial;
            else
                tutorialForContinue = nextTutorial;

            //Continue learning, compulsory complete
            super.setItem(16 - 1, continueLearning_CompulsoryComplete, new guiAction() {
                @Override
                public void rightClick(User u)
                {
                    leftClick(u);
                }
                @Override
                public void leftClick(User u)
                {
                    if (performEvent(EventType.CONTINUE, u, plugin, tutorialForContinue))
                        //Deletes this gui
                        delete();
                }
            });

            //Library
            super.setItem(12 - 1, tutorialLibrary, new guiAction() {
                @Override
                public void rightClick(User u) {
                    leftClick(u);
                }
                @Override
                public void leftClick(User u) {
                    if (performEvent(EventType.LIBRARY, u, plugin, null))
                        //Deletes this gui
                        delete();
                }
            });
        }

        //Admin and creator menu
        if (user.player.hasPermission("TeachingTutorials.Admin") || user.player.hasPermission("TeachingTutorials.Creator"))
        {
            //Admin and creator menu
            super.setItem(19 - 1, Utils.createItem(Material.LECTERN, 1,
                    TutorialGUIUtils.optionTitle("Creator Menu")), new guiAction() {
                @Override
                public void rightClick(User u) {
                    leftClick(u);
                }
                @Override
                public void leftClick(User u) {
                    if (performEvent(EventType.ADMIN_MENU, u, plugin, null))
                        //Deletes this gui
                        delete();
                }
            });
        }
    }

    /**
     * Performs an event arising from any click of an option on the main menu
     * @param event The type of event to perform
     * @param user A reference to the person performing this event
     * @param plugin A reference to the instance of the TeachingTutorials plugin
     * @param tutorialToPlay A tutorial object representing the tutorial to play - this may be null for some events
     * @return Whether the event performed successfully
     */
    public static boolean performEvent(EventType event, User user, TeachingTutorials plugin, Tutorial tutorialToPlay)
    {
        Lesson lesson;
        switch (event) {
            case RESTART:
                lesson = new Lesson(user, plugin, tutorialToPlay);
                return lesson.startLesson(true);
            case CONTINUE:
                //Creates a lesson with the user
                lesson = new Lesson(user, plugin, tutorialToPlay);
                return lesson.startLesson(false);
            case ADMIN_MENU:
                user.mainGui = new CreatorMenu(plugin, user);
                user.mainGui.open(user);
                return true;
            //Here I use the library event as the action arising from clicking the library
            //If it is an externally added event via the database then this method is not called
            // and instead something different happens, because we use that event for when
            // a player clicks a tutorial on the tutorial library to start
            case LIBRARY:
                user.mainGui = new LibraryMenu(plugin, user, Tutorial.getInUseTutorialsWithLocations(TeachingTutorials.getInstance().getDBConnection(), TeachingTutorials.getInstance().getLogger()));
                user.mainGui.open(user);
                return true;
            default:
                return false;
        }
    }

    /**
     * Clears items from the GUI, recreates the items and then opens the menu
     */
    @Override
    public void refresh()
    {
        this.clearGui();
        this.addMenuOptions();

        this.open(user);
    }

    /**
     * Resets the main gui of the user to null
     */
    @Override
    public void delete()
    {
        super.delete();
        user.mainGui = null;
    }
}
