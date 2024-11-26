package teachingtutorials.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.guis.locationcreatemenus.StepEditorMenu;
import teachingtutorials.tutorialobjects.LocationStep;
import teachingtutorials.utils.User;

import java.util.List;

/**
 * A type of listener which handles text edits made through books
 */
public class TextEditorBookListener implements Listener
{
    /** A reference to the instance of the TeachingTutorials plugin */
    private final TeachingTutorials plugin;

    /** The location step object for which this text editor is editing a field of */
    private final LocationStep locationStep;

    /** A reference to the User */
    private final User user;

    /** The step editor menu which owns this text editor */
    private final StepEditorMenu stepEditorMenu;

    /** The book item stack */
    private final ItemStack book;

    /**
     * Should be true if you intend to use this text editor to edit an instruction, and false if
     * you intend to use this text editor to edit the video walkthrough link
     */
    private final boolean bWasInstructions;

    /**
     * Constructs the object, gets the book ready
     * @param locationStep The location step object for which this text editor is editing a field of
     * @param stepEditorMenu The step editor menu which owns this text editor
     * @param bWasInstructions Should be true if you intend to use this text editor to edit an instruction, and false if
     *                         you intend to use this text editor to edit the video walkthrough link
     */
    public TextEditorBookListener(TeachingTutorials plugin, LocationStep locationStep, StepEditorMenu stepEditorMenu, User user, boolean bWasInstructions)
    {
        this.plugin = plugin;
        this.bWasInstructions = bWasInstructions;

        this.locationStep = locationStep;
        this.stepEditorMenu = stepEditorMenu;
        this.user = user;

        //Creates the book
        this.book = new ItemStack(Material.WRITABLE_BOOK, 1);

        //Extracts the book meta reference, and sets the title
        BookMeta videoLinkBookMeta = (BookMeta) this.book.getItemMeta();
        videoLinkBookMeta.setTitle(locationStep.getStep().getName());

        //Adds the meta of the book back in
        this.book.setItemMeta(videoLinkBookMeta);
    }

    /**
     * Returns a reference to the book
     * @return An ItemStack of 1 book
     */
    public ItemStack getBook()
    {
        return book;
    }

    /**
     * Registers the listeners with the server's event listeners
     */
    public void register()
    {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Unregisters the listeners with the server's event listeners
     */
    private void unregister()
    {
        HandlerList.unregisterAll(this);
    }

    /**
     * Detects when a player closes a book after editing it, checks whether it is the relevant player and relevant book,
     * then detects the changes and stores the information in the correct place
     * @param event
     */
    @EventHandler
    public void BookCloseEvent(PlayerEditBookEvent event)
    {
        //Check the player
        if (!event.getPlayer().equals(user.player))
            return;

        //We can't actually also check the book because the event doesn't give that
        //We can only check the title
        if (!event.getNewBookMeta().getTitle().equalsIgnoreCase(locationStep.getStep().getName()))
            return;

        //Extracts the new content from the book
        String szNewContent = "";
        List<Component> pages = event.getNewBookMeta().pages();
        for (Component page: pages)
        {
            szNewContent = szNewContent + ((TextComponent) page).content() + " ";

        }
        //Removes the end space, the space after the last page is added in the loop but then needs to be removed
        szNewContent = szNewContent.substring(0, szNewContent.length() - 1);

        //Edits the step instructions or video link
        if (bWasInstructions)
            locationStep.setInstruction(szNewContent, locationStep.getStep().getInstructionDisplayType(), user.player, locationStep.getStep().getName());
        else
            locationStep.setVideoLink(szNewContent);

        //Saves the instructions in the book
        BookMeta bookMeta = (BookMeta) book.getItemMeta();
        bookMeta.pages(event.getNewBookMeta().pages());
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
}
