package teachingtutorials.listeners.texteditorbooks;

import org.bukkit.inventory.meta.BookMeta;

public interface BookCloseAction
{
    /**
     * Performs the action on book close.
     * @param oldBookMeta The previous metadata of the book just closed.
     * @param newBookMeta The new metadata of the book just closed.
     * @param textEditorBookListener A reference to the book listener itself which calls this. Enables unregistering to be called
     * @param szNewContent The combined content of all pages in the new book. This is always provided for convenience
     */
    void runBookClose(BookMeta oldBookMeta, BookMeta newBookMeta, TextEditorBookListener textEditorBookListener, String szNewContent);
}
