package teachingtutorials.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.guis.locationcreatemenus.StepEditorMenu;
import teachingtutorials.tutorials.LocationStep;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.User;

import java.util.List;

public class TextEditorBookListener implements Listener
{
    private TeachingTutorials plugin;
    private User user;
    private LocationStep locationStep;
    private StepEditorMenu stepEditorMenu;
    private Display.DisplayType displayType;
    private String szStepName;
    private ItemStack book;

    private boolean bWasInstructions; //If not then it was video link

    //Used for creating an instructions editor book
    public TextEditorBookListener(TeachingTutorials plugin, User user, LocationStep locationStep, StepEditorMenu stepEditorMenu, Display.DisplayType displayType, String szStepName, ItemStack book)
    {
        this.bWasInstructions = true;
        this.plugin = plugin;
        this.user = user;
        this.locationStep = locationStep;
        this.stepEditorMenu = stepEditorMenu;
        this.displayType = displayType;
        this.szStepName = szStepName;
        this.book = book;
    }

    //Used for creating a video link editor book
    public TextEditorBookListener(TeachingTutorials plugin, User user, LocationStep locationStep, StepEditorMenu stepEditorMenu, String szStepName, ItemStack book)
    {
        this.bWasInstructions = false;
        this.plugin = plugin;
        this.user = user;
        this.locationStep = locationStep;
        this.stepEditorMenu = stepEditorMenu;
        this.szStepName = szStepName;
        this.book = book;
    }

    public void register()
    {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void BookCloseEvent(PlayerEditBookEvent e)
    {
        //Check the player
        if (!e.getPlayer().equals(user.player))
            return;

        //We can't actually also check the book because the event doesn't give that
        //We can only check the title
        if (!e.getNewBookMeta().getTitle().equalsIgnoreCase(szStepName))
            return;

        //Extracts the new content from the book
        String szNewContent = "";
        List<Component> pages = e.getNewBookMeta().pages();
        for (Component page: pages)
        {
            szNewContent = szNewContent + ((TextComponent) page).content() + " ";

        }
        //Removes the end space, the space after the last page is added in the loop but then needs to be removed
        szNewContent = szNewContent.substring(0, szNewContent.length() - 1);

        //Edits the step instructions or video link
        if (bWasInstructions)
            locationStep.setInstruction(szNewContent, displayType, user.player, szStepName);
        else
            locationStep.setVideoLink(szNewContent);

        //Saves the instructions in the book
        BookMeta bookMeta = (BookMeta) book.getItemMeta();
        bookMeta.pages(e.getNewBookMeta().pages());
        this.book.setItemMeta(bookMeta);

        //Reopen the feature menu
        user.mainGui = stepEditorMenu;
        user.mainGui.refresh();

        //Unregisters this listener
        unregister();

        //Removes the book
        user.player.getInventory().getItemInMainHand().setAmount(0);

        //Informs the menu that some instructions were edited
        if (bWasInstructions)
            stepEditorMenu.instructionsEdited();
    }

    private void unregister()
    {
        HandlerList.unregisterAll(this);
    }
}
