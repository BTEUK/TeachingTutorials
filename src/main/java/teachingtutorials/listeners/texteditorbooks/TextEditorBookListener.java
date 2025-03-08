package teachingtutorials.listeners.texteditorbooks;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.utils.User;
import teachingtutorials.utils.Utils;

import java.util.List;

/**
 * A type of listener which handles text edits made through books
 */
public class TextEditorBookListener implements Listener
{
    /** A reference to the instance of the TeachingTutorials plugin */
    private final TeachingTutorials plugin;

    /** A reference to the interface object which defines the intended behaviour on book close */
    private final BookCloseAction bookCloseAction;

    /** A reference to the User */
    private final User user;

    /** The book item stack */
    private final ItemStack book;

    /**
     * Constructs the object, gets the book ready
     * @param plugin A reference to the instance of the TeachingTutorials plugin
     * @param user A reference to the User
     * @param szBookTitle The intended title for the book
     * @param bookCloseAction The action to perform on book close
     */

    public TextEditorBookListener(TeachingTutorials plugin, User user, String szBookTitle, BookCloseAction bookCloseAction, String... initialValue)
    {
        this.plugin = plugin;
        this.bookCloseAction = bookCloseAction;
        this.user = user;

        //Creates the book
        this.book = new ItemStack(Material.WRITABLE_BOOK, 1);

        //Extracts a reference to the book meta, and sets the title and initial value
        BookMeta bookMeta = (BookMeta) this.book.getItemMeta();
        bookMeta.setTitle(szBookTitle);
        if (initialValue.length > 0)
        {
            bookMeta.page(0, Component.text(initialValue[0]));
        }

        //Adds the meta of the book back in
        this.book.setItemMeta(bookMeta);
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
     * Gives the player the book, closes the current inventory and registers the listeners with the server's event listeners
     */
    public void startEdit(String szBookName)
    {
        //Gives the player the book item
        Utils.giveItem(user.player, this.book, szBookName);

        //Closes the current inventory
        user.player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);

        //Registers the book close listener
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Unregisters the listeners with the server's event listeners
     */
    public void unregister()
    {
        HandlerList.unregisterAll(this);
    }

    /**
     * Detects when a player closes a book after editing it, checks whether it is the relevant player and relevant book,
     * then detects the changes and stores the information in the book.
     * <p>Will then run the custom book close logic inserted into this listener at construction</p>
     * @param event
     */
    @EventHandler
    public void BookCloseEvent(PlayerEditBookEvent event)
    {
        //Check the player
        if (!event.getPlayer().equals(user.player))
            return;

        //Extracts the new content from the book
        String szNewContent = "";
        List<Component> pages = event.getNewBookMeta().pages();
        for (Component page: pages)
        {
            szNewContent = szNewContent + ((TextComponent) page).content() + " ";
        }
        // Removes the end space, the space after the last page is added in the loop but then needs to be removed
        szNewContent = szNewContent.substring(0, szNewContent.length() - 1);

        //Performs the predefined instructions upon book close
        boolean bSaveAnswers = bookCloseAction.runBookClose(event.getPreviousBookMeta(), event.getNewBookMeta(), this, szNewContent);

        if (bSaveAnswers)
        {
            //Saves the instructions in the book
            BookMeta bookMeta = (BookMeta) getBook().getItemMeta();
            bookMeta.pages(event.getNewBookMeta().pages());
            getBook().setItemMeta(bookMeta);
        }
    }
}
