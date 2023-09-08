package teachingtutorials.guis.locationcreatemenus;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.guis.Gui;
import teachingtutorials.tutorials.LocationStep;
import teachingtutorials.tutorials.Step;
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

    public StepEditorMenu(TeachingTutorials plugin, User user, Step step, LocationStep locationStep)
    {
        super(iInvSize, getName(step.getName()));
        this.plugin = plugin;
        this.user = user;
        this.step = step;
        this.locationStep = locationStep;

        setItems();
    }

    public void setItems()
    {
        //Set start location coordinates to current location
        ItemStack startLocation = Utils.createItem(Material.COMPASS, 1,
                Component.text("Set the step's start location", NamedTextColor.GREEN),
                Component.text("Set the start location to your current position and direction", NamedTextColor.DARK_GREEN));

        setItem(11, startLocation, new guiAction() {
            @Override
            public void rightClick(User u) {
                leftClick(u);
            }

            @Override
            public void leftClick(User u) {
                locationStep.setStartLocation(u.player.getLocation());
                step.tryNextStep();
            }
        });
    }

    public static Component getName(String szStepName)
    {
        Component inventoryName = Component.text("Step - " +szStepName, Style.style(TextDecoration.BOLD, NamedTextColor.DARK_AQUA));
        return inventoryName;
    }

    @Override
    public void refresh()
    {

    }
}
