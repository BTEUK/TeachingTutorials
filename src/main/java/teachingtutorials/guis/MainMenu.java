package teachingtutorials.guis;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.compulsory.Compulsory;
import teachingtutorials.tutorials.Lesson;
import teachingtutorials.tutorials.Tutorial;
import teachingtutorials.utils.User;
import teachingtutorials.utils.Utils;

public class MainMenu extends Gui
{
    private static final Component inventoryName = Component.text("Tutorials Menu", Style.style(TextDecoration.BOLD, NamedTextColor.DARK_AQUA));
    private static final int iNumRows = 3 * 9;
    private final TeachingTutorials plugin;
    private final User user;

    public MainMenu(TeachingTutorials plugin, User user)
    {
        super(iNumRows, inventoryName);
        this.plugin = plugin;
        this.user = user;

        createGui();
    }

    private void createGui()
    {
        //Initiates the current tutorial object
        Tutorial currentTutorial = new Tutorial();

        FileConfiguration config = TeachingTutorials.getInstance().getConfig();
        boolean bCompulsoryTutorialEnabled = config.getBoolean("Compulsory_Tutorial");

        //Get compulsory tutorial ID
        int iCompulsoryTutorialID;

        Tutorial[] compulsoryTutorials = Tutorial.fetchAll(true, true, TeachingTutorials.getInstance().getDBConnection());
        if (compulsoryTutorials.length == 0)
            iCompulsoryTutorialID = -1;
        else
            iCompulsoryTutorialID = compulsoryTutorials[0].getTutorialID();

        //'Continue' menu button
        ItemStack continueLearning_CompulsoryComplete;
        if (user.bInLesson)
        {
            //Get current tutorial ID and sets up the tutorial object from this
            int iTutorialIDCurrentLesson = Lesson.getTutorialOfCurrentLessonOfPlayer(user.player.getUniqueId(), TeachingTutorials.getInstance().getDBConnection());
            if (iTutorialIDCurrentLesson == -1)
            {
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"An error occurred. Player is in lesson but has no lesson in the database");
            }
            currentTutorial.setTutorialID(iTutorialIDCurrentLesson);
            currentTutorial.fetchByTutorialID(TeachingTutorials.getInstance().getDBConnection());

            //Sets up the menu icon with the name of the current tutorial
            continueLearning_CompulsoryComplete = Utils.createItem(Material.WRITABLE_BOOK, 1,
                    TutorialGUIUtils.optionTitle("Resume Your Lesson"),
                    TutorialGUIUtils.optionLore(currentTutorial.szTutorialName));

        }
        else
        {
            //Sets up the menu icon with the new tutorial's name
            Tutorial nextTutorial = Lesson.decideTutorial(user, TeachingTutorials.getInstance().getDBConnection());
            continueLearning_CompulsoryComplete = Utils.createItem(Material.WRITABLE_BOOK, 1,
                    TutorialGUIUtils.optionTitle("Start a new Tutorial:"),
                    TutorialGUIUtils.optionLore(nextTutorial.szTutorialName));
        }

        //Tutorial library
        ItemStack tutorialLibrary = Utils.createItem(Material.BOOKSHELF, 1,
                TutorialGUIUtils.optionTitle("Tutorial Library"),
                TutorialGUIUtils.optionLore("Browse all of our available tutorials"));


        //-----------------------------------------------------------------------
        //--------------------- Begin Adding the Menu Items ---------------------
        //-----------------------------------------------------------------------

        //Checks the system has the compulsory tutorial feature enabled
        if (bCompulsoryTutorialEnabled)
        {
            //User has already completed the compulsory tutorial
            if (user.bHasCompletedCompulsory)
            {
                //---- Compulsory Tutorial Option ----
                ItemStack compulsory;

                //User is currently redoing the compulsory tutorial
                if (user.bInLesson && (currentTutorial.getTutorialID() == iCompulsoryTutorialID))
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
                            if (performEvent(EventType.COMPULSORY, u, plugin))
                                //Deletes this gui
                                delete();
                        }
                    });
                }
                //User is currently in a different tutorial
                else if (user.bInLesson)
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
                            if (performEvent(EventType.COMPULSORY, u, plugin))
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
                        if (performEvent(EventType.LIBRARY, u, plugin))
                            //Deletes this gui
                            delete();
                    }
                });

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
                        if (performEvent(EventType.CONTINUE, u, plugin))
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
                if (user.bInLesson)
                {
                    //Player is in a lesson other than the compulsory but hasn't started the compulsory (compulsory added to system)
                    if (currentTutorial.getTutorialID() != iCompulsoryTutorialID)
                    {
                        ItemStack erroneousTutorial = Utils.createItem(Material.BOOK, 1,
                                TutorialGUIUtils.optionTitle("Continue Your Tutorial"),
                                TutorialGUIUtils.optionLore("You must then complete the starter tutorial"));

                        super.setItem(14 - 1, erroneousTutorial, new guiAction() {
                            @Override
                            public void rightClick(User u) {
                                leftClick(u);
                            }
                            @Override
                            public void leftClick(User u) {
                                if (performEvent(EventType.CONTINUE, u, plugin))
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
                                TutorialGUIUtils.optionLore("Gain the applicant rank"));

                        resumeCompulsory = Utils.createItem(Material.WRITABLE_BOOK, 1,
                                TutorialGUIUtils.optionTitle("Resume the Starter Tutorial"),
                                TutorialGUIUtils.optionLore("Gain the applicant rank"));

                        super.setItem(12 - 1, restartCompulsory, new guiAction() {
                            @Override
                            public void rightClick(User u) {
                                leftClick(u);
                            }
                            @Override
                            public void leftClick(User u) {
                                if (performEvent(EventType.COMPULSORY, u, plugin))
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
                                if (performEvent(EventType.CONTINUE, u, plugin))
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
                            TutorialGUIUtils.optionLore("Gain the applicant rank"));

                    super.setItem(14 - 1, beginCompulsory, new guiAction() {
                        @Override
                        public void rightClick(User u) {
                            leftClick(u);
                        }
                        @Override
                        public void leftClick(User u) {
                            if (performEvent(EventType.COMPULSORY, u, plugin))
                                //Deletes this gui
                                delete();
                        }
                    });
                }
            }
        }

        else // There is no compulsory tutorial
        {
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
                    if (performEvent(EventType.CONTINUE, u, plugin))
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
                    if (performEvent(EventType.LIBRARY, u, plugin))
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
                    if (performEvent(EventType.ADMIN_MENU, u, plugin))
                        //Deletes this gui
                        delete();
                }
            });
        }
    }

    public static boolean performEvent(EventType event, User user, TeachingTutorials plugin)
    {
        switch (event) {
            case COMPULSORY -> {
                //Starts the compulsory tutorial from the start
                Compulsory compulsory = new Compulsory(plugin, user);
                return compulsory.startLesson();
            }
            case CONTINUE -> {
                //Creates a lesson with the user
                Lesson lesson = new Lesson(user, plugin, false);
                return lesson.startLesson();
            }
            case ADMIN_MENU ->
            {
                user.mainGui = new AdminMenu(plugin, user);
                user.mainGui.open(user);
                return true;
            }
            //Here I use the library event as the action arising from clicking the library
            //If it is an externally added event via the database then this method is not called
            // and instead something different happens, because we use that event for when
            // a player clicks a tutorial on the tutorial library to start
            case LIBRARY ->
            {
                user.mainGui = new LibraryMenu(plugin, user, Tutorial.getInUseTutorialsWithLocations(TeachingTutorials.getInstance().getDBConnection()));
                user.mainGui.open(user);
                return true;
            }
            default ->
            {
                return false;
            }
        }
    }

    @Override
    public void refresh()
    {
        this.clearGui();
        this.createGui();

        this.open(user);
    }

    @Override
    public void delete()
    {
        super.delete();
        user.mainGui = null;
    }
}
