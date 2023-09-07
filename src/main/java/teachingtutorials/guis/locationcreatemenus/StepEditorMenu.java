package teachingtutorials.guis.locationcreatemenus;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.guis.Gui;
import teachingtutorials.tutorials.Step;
import teachingtutorials.utils.User;

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

    public StepEditorMenu(TeachingTutorials plugin, User user, Step step)
    {
        super(iInvSize, getName(step.getName()));
        this.plugin = plugin;
        this.user = user;
        this.step = step;

        setItems();
    }

    public void setItems()
    {
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
