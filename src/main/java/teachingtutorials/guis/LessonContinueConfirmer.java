package teachingtutorials.guis;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.tutorialobjects.LessonObject;
import teachingtutorials.tutorialplaythrough.Lesson;
import teachingtutorials.utils.User;
import teachingtutorials.utils.Utils;

public class LessonContinueConfirmer extends Gui
{
    /** A reference to the instance of the TeachingTutorials plugin */
    private final TeachingTutorials plugin;

    /** The user whom this menu is for */
    private final User user;

    /** The Lesson which is to be restarted or resumed */
    private final LessonObject lessonToContinue;

    /** The message to display to the user */
    private final String szMessage;

    /** A reference to the parent Gui */
    private final Gui parentGui;

    /**
     *
     * @param plugin A reference to the instance of the TeachingTutorials plugin
     * @param user The user whom this menu is for
     * @param lessonToContinue The Lesson which is to be restarted or resumed
     * @param szMessage The message to display to the user
     */
    public LessonContinueConfirmer(TeachingTutorials plugin, User user, Gui parentGui, LessonObject lessonToContinue, String szMessage)
    {
        super(27, TutorialGUIUtils.inventoryTitle("Resume or continue lesson?"));
        this.plugin = plugin;
        this.parentGui = parentGui;
        this.user = user;
        this.lessonToContinue = lessonToContinue;
        this.szMessage = szMessage;

        addItems();
    }

    /**
     * Adds the icons and actions to the menu
     */
    private void addItems()
    {
        //Info
        super.setItem(4, Utils.createItem(Material.KNOWLEDGE_BOOK, 1,
                TutorialGUIUtils.optionLore(szMessage)), new guiAction() {
            @Override
            public void rightClick(User u) {
            }
            @Override
            public void leftClick(User u)
            {
            }
        });

        //Restart lesson
        super.setItem(12 - 1, Utils.createItem(Material.BOOK, 1,
                TutorialGUIUtils.optionTitle("Restart the lesson")), new guiAction() {
            @Override
            public void rightClick(User u) {
                leftClick(u);
            }
            @Override
            public void leftClick(User u)
            {
                Lesson lessonPlaythrough = new Lesson(user, plugin, lessonToContinue);
                lessonPlaythrough.startLesson(true);
            }
        });

        //Resume compulsory
        ItemStack resumeCompulsory = Utils.createItem(Material.WRITABLE_BOOK, 1,
                TutorialGUIUtils.optionTitle("Resume the lesson"));

        super.setItem(16 - 1, resumeCompulsory, new guiAction() {
            @Override
            public void rightClick(User u) {
                leftClick(u);
            }
            @Override
            public void leftClick(User u) {
                Lesson lessonPlaythrough = new Lesson(user, plugin, lessonToContinue);
                lessonPlaythrough.startLesson(false);
            }
        });
        
        //Back button
        ItemStack back = Utils.createItem(Material.SPRUCE_DOOR, 1,
                TutorialGUIUtils.optionTitle("Back"));
        super.setItem(26, back, new guiAction() {
            @Override
            public void rightClick(User u) {
                leftClick(u);
            }
            @Override
            public void leftClick(User u) {
                user.mainGui = parentGui;
                user.mainGui.open(user);
                delete();
            }
        });
    }

    /**
     * Refresh the gui.
     * This usually involves clearing the content and recreating it.
     */
    @Override
    public void refresh()
    {

    }
}
