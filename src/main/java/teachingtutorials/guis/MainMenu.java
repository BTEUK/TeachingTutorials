package teachingtutorials.guis;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
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
        FileConfiguration config = TeachingTutorials.getInstance().getConfig();
        boolean bCompulsoryTutorialEnabled = config.getBoolean("Compulsory_Tutorial");

        //Continue learning menu button
        ItemStack continueLearning_CompulsoryComplete;
        if (user.bInLesson)
            continueLearning_CompulsoryComplete = Utils.createItem(Material.WRITABLE_BOOK, 1,
                    Component.text("Continue learning", NamedTextColor.GREEN),
                    Component.text("Continue your lesson", NamedTextColor.DARK_GREEN));
        else
            continueLearning_CompulsoryComplete = Utils.createItem(Material.WRITABLE_BOOK, 1,
                    Component.text("Continue learning", NamedTextColor.GREEN),
                    Component.text("Start another tutorial", NamedTextColor.DARK_GREEN));

        //Tutorial library
        ItemStack tutorialLibrary = Utils.createItem(Material.BOOKSHELF, 1,
                Component.text("Tutorial Library", NamedTextColor.GREEN),
                Component.text("Browse all of our available tutorials", NamedTextColor.DARK_GREEN));

        //The system has the compulsory tutorial feature enabled
        if (bCompulsoryTutorialEnabled)
        {
            if (user.bHasCompletedCompulsory)
            {
                //Restart compulsory tutorial
                ItemStack restartCompulsory = Utils.createItem(Material.ENCHANTED_BOOK, 1,
                        Component.text("Restart Compulsory Tutorial", NamedTextColor.GREEN));
                super.setItem(11 - 1, restartCompulsory, new guiAction() {
                    @Override
                    public void rightClick(User u)
                    {
                        leftClick(u);
                    }
                    @Override
                    public void leftClick(User u)
                    {
                        //Deletes this gui
                        delete();

                        performEvent(EventType.COMPULSORY, u, plugin);
                    }
                });

                //Library
                super.setItem(14 - 1, tutorialLibrary, new guiAction() {
                    @Override
                    public void rightClick(User u) {
                        leftClick(u);
                    }
                    @Override
                    public void leftClick(User u) {
                        //Deletes this gui
                        delete();

                        performEvent(EventType.LIBRARY, u, plugin);
                    }
                });

                //Continue learning
                super.setItem(17 - 1, continueLearning_CompulsoryComplete, new guiAction() {
                    @Override
                    public void rightClick(User u)
                    {
                        leftClick(u);
                    }
                    @Override
                    public void leftClick(User u)
                    {
                        //Deletes this gui
                        delete();

                        performEvent(EventType.CONTINUE, u, plugin);
                    }
                });
            }
            else // They have not completed the compulsory tutorial
            {
                ItemStack continue_CompulsoryNotComplete;
                if (user.bInLesson)
                {
                    continue_CompulsoryNotComplete = Utils.createItem(Material.BOOK, 1,
                            Component.text("Continue Compulsory Tutorial", NamedTextColor.GREEN),
                            Component.text("Gain the applicant rank", NamedTextColor.DARK_GREEN));
                }
                else
                {
                    continue_CompulsoryNotComplete = Utils.createItem(Material.BOOK, 1,
                            Component.text("Start Compulsory Tutorial", NamedTextColor.GREEN),
                            Component.text("Gain the applicant rank", NamedTextColor.DARK_GREEN));
                }
                super.setItem(14 - 1, continue_CompulsoryNotComplete, new guiAction() {
                    @Override
                    public void rightClick(User u) {
                        leftClick(u);
                    }
                    @Override
                    public void leftClick(User u) {
                        //Deletes this gui
                        delete();

                        performEvent(EventType.COMPULSORY, u, plugin);
                    }
                });
            }
        }
        else
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
                    //Deletes this gui
                    delete();

                    performEvent(EventType.CONTINUE, u, plugin);
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
                    //Deletes this gui
                    delete();

                    performEvent(EventType.LIBRARY, u, plugin);
                }
            });
        }

        //Admin and creator menu
        if (user.player.hasPermission("TeachingTutorials.Admin") || user.player.hasPermission("TeachingTutorials.Creator"))
        {
            //Admin and creator menu
            super.setItem(19 - 1, Utils.createItem(Material.LECTERN, 1,
                    Component.text("Creator Menu", NamedTextColor.GREEN)), new guiAction() {
                @Override
                public void rightClick(User u) {
                    leftClick(u);
                }
                @Override
                public void leftClick(User u) {
                    //Deletes this gui
                    delete();
                    performEvent(EventType.ADMIN_MENU, u, plugin);
                }
            });
        }
    }

    public static void performEvent(EventType event, User user, TeachingTutorials plugin)
    {
        switch (event) {
            case COMPULSORY -> {
                //Starts the compulsory tutorial
                Compulsory compulsory = new Compulsory(plugin, user);
                compulsory.startLesson();
            }
            case CONTINUE -> {
                //Creates a lesson with the user
                Lesson lesson = new Lesson(user, plugin, false);
                lesson.startLesson();
            }
            case ADMIN_MENU ->
            {
                user.mainGui = new AdminMenu(plugin, user);
                user.mainGui.open(user);
            }
            case LIBRARY ->
            {
                user.mainGui = new LibraryMenu(plugin, user, Tutorial.getInUseTutorialsWithLocations());
                user.mainGui.open(user);
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
}
