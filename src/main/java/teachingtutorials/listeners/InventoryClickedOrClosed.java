package teachingtutorials.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.guis.*;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.User;

import java.util.UUID;

/**
 * A listener which listens for when a user clicks on an inventory item or closes the inventory. This is used to support
 * features of the gui system
 */
public class InventoryClickedOrClosed implements Listener
{
    /** A reference to the instance of the TeachingTutorials plugin */
    private final TeachingTutorials plugin;

    /**
     * Constructs the listener object and registers the listener
     * @param plugin A reference to the instance of the TeachingTutorials plugin
     */
    public InventoryClickedOrClosed(TeachingTutorials plugin)
    {
        this.plugin = plugin;

        //Registers the listener
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Detects click events on inventories, identifies the player, identifies whether they have menu open and if so
     * returns the action for the slot they just clicked on and runs this action.
     * @param clickEvent An InventoryClickEvent event
     */
    @EventHandler
    public void onClick(InventoryClickEvent clickEvent)
    {
        //Check whether click was from a player
        if (!(clickEvent.getWhoClicked() instanceof Player player))
            return;

        //Identifies the user
        User user = User.identifyUser(plugin, player);

        //If u is null, cancel.
        if (user == null)
        {
            plugin.getLogger().severe("User " + player.getName() + " can not be found!");
            player.sendMessage(Display.errorText("User can not be found, please relog!"));
            return;
        }

        //Gets the player's uuid
        UUID playerUUID = user.player.getUniqueId();

        //Fetches the user's open menu
        UUID inventoryUUID = Gui.openInventories.get(playerUUID);

        //If the user had an open menu:
        if (inventoryUUID != null)
        {
            //Cancels the event
            clickEvent.setCancelled(true);

            //Gets the gui object
            Gui gui = Gui.inventoriesByUUID.get(inventoryUUID);

            //Gets the action for the slot that was clicked
            Gui.guiAction action = gui.getActions().get(clickEvent.getRawSlot());

            //Checks whether there is an action for the clicked-on slot
            if (action != null)
            {
                //Perform the left click or right click action
                if (clickEvent.isLeftClick())
                    action.leftClick(user);
                else
                    action.rightClick(user);
            }
        }
    }

    /**
     * Removes the player's player-gui mapping from the map of open inventories when an inventory is closed
     * @param closeEvent An InventoryCloseEvent event
     */
    @EventHandler
    public void onClose(InventoryCloseEvent closeEvent)
    {
        //Check whether click was from a player
        Player player = (Player) closeEvent.getPlayer();

        //Gets the clickers UUID
        UUID playerUUID = player.getUniqueId();

        //Remove the player from the map of open inventories.
        Gui.openInventories.remove(playerUUID);
    }
}
