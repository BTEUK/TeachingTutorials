package teachingtutorials.guis;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.tutorials.Tutorial;
import teachingtutorials.utils.User;
import teachingtutorials.utils.Utils;

public class AdminMenu extends Gui
{
    private static final Component inventoryName = Component.text("Admin and Creator Menu", Style.style(TextDecoration.BOLD, NamedTextColor.DARK_AQUA));
    private static final int iNumRows = 3 * 9;
    private final TeachingTutorials plugin;
    private final User user;

    public AdminMenu(TeachingTutorials plugin, User user)
    {
        super(iNumRows, inventoryName);
        this.plugin = plugin;
        this.user = user;

        createGui();
    }

    private void createGui()
    {
        ItemStack setCompulsory = Utils.createItem(Material.IRON_DOOR, 1,
                TutorialGUIUtils.optionTitle("Set Compulsory Tutorial"),
                TutorialGUIUtils.optionLore("Admins only"));

        ItemStack myTutorials = Utils.createItem(Material.BOOKSHELF, 1,
                TutorialGUIUtils.optionTitle("My Tutorials"),
                TutorialGUIUtils.optionLore("- View the tutorials you have created"),
                TutorialGUIUtils.optionLore("- Add locations"),
                TutorialGUIUtils.optionLore("- Set a tutorial as in use or not in use"));

        ItemStack createTutorial = Utils.createItem(Material.WRITABLE_BOOK, 1,
                TutorialGUIUtils.optionTitle("Create a new Tutorial"),
                TutorialGUIUtils.optionLore("Create a new tutorial in game (coming soon)"));

        int iSlotMyTutorials;

        //Menu for admins
        if (user.player.hasPermission("TeachingTutorials.Admin"))
        {
            setItem(12 - 1, setCompulsory, new guiAction() {
                @Override
                public void rightClick(User u) {
                    leftClick(u);
                }
                @Override
                public void leftClick(User u) {
                    delete();
                    u.mainGui = new CompulsoryTutorialMenu(plugin, u, Tutorial.fetchAll(true, false));
                    u.mainGui.open(u);
                }
            });

            iSlotMyTutorials = 14 - 1;
        }
        //Menu for creators only
        else
        {
            iSlotMyTutorials = 12 - 1;
        }

        setItem(iSlotMyTutorials, myTutorials, new guiAction() {
            @Override
            public void rightClick(User u) {
                leftClick(u);
            }
            @Override
            public void leftClick(User u) {
                delete();
                u.mainGui = new CreatorTutorialsMenu(plugin, u, Tutorial.fetchAllByCreator(u.player.getUniqueId()));
                u.mainGui.open(u);
            }
        });

        setItem(16 - 1, createTutorial, new guiAction() {
            @Override
            public void rightClick(User u) {
//                leftClick(u);
            }
            @Override
            public void leftClick(User u) {
//                delete();
//                u.mainGui = new TutorialCreationMenu(plugin, u);
//                u.mainGui.open(u);
            }
        });

        setItem(27 - 1, Utils.createItem(Material.SPRUCE_DOOR, 1,
                        TutorialGUIUtils.backButton("Back to main menu")),
                new guiAction() {
            @Override
            public void rightClick(User u) {
                leftClick(u);
            }
            @Override
            public void leftClick(User u) {
                delete();
                u.mainGui = new MainMenu(plugin, u);
                u.mainGui.open(u);
            }
        });
    }

    @Override
    public void refresh()
    {
        this.clearGui();
        this.createGui();

        this.open(user);
    }
}