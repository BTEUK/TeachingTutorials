package teachingtutorials.guis.locationcreatemenus;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.guis.Gui;
import teachingtutorials.listeners.TextEditorBookListener;
import teachingtutorials.tutorials.LocationStep;
import teachingtutorials.tutorials.Step;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.User;
import teachingtutorials.utils.Utils;

/**
 * A menu accessible to a creator when creating a new location, used to set the step instructions,
 * the step start location and the hologram location of whatever step the creator is currently at
 */
public class StepEditorMenu extends Gui
{
    private static final int iInvSize = 3 * 9;
    private final TeachingTutorials plugin;
    private final User user;
    private final Step step;
    private final LocationStep locationStep;

    private TextEditorBookListener bookListener;

    private ItemStack book;

    public StepEditorMenu(TeachingTutorials plugin, User user, Step step, LocationStep locationStep)
    {
        super(iInvSize, getName(step.getName()));
        this.plugin = plugin;
        this.user = user;
        this.step = step;
        this.locationStep = locationStep;

        this.book = new ItemStack(Material.WRITABLE_BOOK, 1);
        BookMeta bookMeta = (BookMeta) book.getItemMeta();
        bookMeta.setTitle(step.getName());
        this.book.setItemMeta(bookMeta);

        setItems();
    }

    public void setItems()
    {
        ItemStack setStartLocation = Utils.createItem(Material.COMPASS, 1,
                Component.text("Set the step's start location", NamedTextColor.GREEN),
                Component.text("Set the start location to your current position and direction", NamedTextColor.DARK_GREEN));

        ItemStack teleportToStart = Utils.createItem(Material.VILLAGER_SPAWN_EGG, 1,
                Component.text("Teleport back to the start location", NamedTextColor.GREEN));

        boolean bIsHologramNeeded = step.getInstructionDisplayType().equals(Display.DisplayType.hologram);
        if (bIsHologramNeeded)
        {
            //Set start location coordinates to current location
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
            ItemStack instructions = Utils.createItem(Material.WRITABLE_BOOK, 1,
                    Component.text("Set the instructions", NamedTextColor.GREEN));

            setItem(13, instructions, new guiAction() {
                @Override
                public void rightClick(User u) {
                    leftClick(u);
                }

                @Override
                public void leftClick(User u) {
                    //The book must have the step name as the title
                    Utils.giveItem(u.player, book, "Instructions editor book");
                    Display display = new Display(u.player, Component.text("Use the instructions editor book to set the instructions", NamedTextColor.GREEN));
                    display.Message();

                    //Closes the current inventory
                    u.player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);

                    //Sets up the book listener and registers it
                    bookListener = new TextEditorBookListener(plugin, u, locationStep, StepEditorMenu.this, step.getInstructionDisplayType(), step.getName(), book);
                    bookListener.register();

                    //step.tryNextStep() is called via instructionsEdited() from TextEditorBookListener once the book close event occurs
                }
            });

            //Set hologram coordinates to player's current location
            ItemStack hologramLocation = Utils.createItem(Material.FILLED_MAP, 1,
                    Component.text("Set the instructions hologram location", NamedTextColor.GREEN),
                    Component.text("Set the instructions hologram to your current position", NamedTextColor.DARK_GREEN));

            setItem(15, hologramLocation, new guiAction() {
                @Override
                public void rightClick(User u) {
                    leftClick(u);
                }

                @Override
                public void leftClick(User u) {
                    locationStep.setHologramLocationToThatOfPlayer(u.player, step.getName());
                    step.tryNextStep();
                }
            });
        }
        else
        {
            setItem(13, setStartLocation, new guiAction() {
                @Override
                public void rightClick(User u) {
                    leftClick(u);
                }

                @Override
                public void leftClick(User u) {
                    setStartLocation(u.player.getLocation());
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
                locationStep.teleportPlayerToStartOfStep(u.player, step.parentStage.tutorialPlaythrough.getLocation().getWorld(), plugin);
            }
        });
    }

    private void setStartLocation(Location playersLocation)
    {
        locationStep.setStartLocation(playersLocation);
        step.parentStage.tutorialPlaythrough.setFallListenerSafeLocation(playersLocation);
        step.tryNextStep();
    }

    public static Component getName(String szStepName)
    {
        Component inventoryName = Component.text("Step - " +szStepName, Style.style(TextDecoration.BOLD, NamedTextColor.DARK_AQUA));
        return inventoryName;
    }

    /**
     * Clears items from the GUI, recreates the items and then opens the menu
     */
    @Override
    public void refresh()
    {
        this.clearGui();
        this.setItems();

        this.open(user);
    }

    /**
     * Triggers the step to test whether it can move forwards with the stage
     */
    public void instructionsEdited()
    {
        step.tryNextStep();
    }
}
