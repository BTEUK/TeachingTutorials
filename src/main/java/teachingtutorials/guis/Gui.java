package teachingtutorials.guis;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import teachingtutorials.utils.User;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class Gui implements GuiInterface {

    public static final Map<UUID, Gui> inventoriesByUUID = new HashMap<>();
    public static final Map<UUID, UUID> openInventories = new HashMap<>();

    //Information about the gui.
    private final UUID uuid;
    private Inventory inv;
    private final Map<Integer, guiAction> actions;

    public Gui(int invSize, Component invName) {

        uuid = UUID.randomUUID();
        inv = Bukkit.createInventory(null, invSize, invName);
        actions = new HashMap<>();
        inventoriesByUUID.put(getUuid(), this);

    }

    public Gui(Inventory inv) {

        this.inv = inv;
        uuid = UUID.randomUUID();
        actions = new HashMap<>();
        inventoriesByUUID.put(getUuid(), this);

    }

    public Inventory getInventory() {
        return inv;
    }

    public interface guiAction {
        void rightClick(User u);
        void leftClick(User u);
    }

    public void setItem(int slot, ItemStack stack, guiAction action) {

        inv.setItem(slot, stack);
        if (action != null) {
            actions.put(slot, action);
        }

    }

    public void setItem(int slot, ItemStack stack) {

        setItem(slot, stack, null);

    }

    public void setAction(int slot, guiAction action) {

        if (action != null) {
            actions.put(slot, action);
        }

    }

    public void clearGui() {
        inv.clear();
        actions.clear();
    }

    public void open(User u) {

        u.player.openInventory(inv);
        openInventories.put(u.player.getUniqueId(), getUuid());

    }

    /**
     * Creates a new inventory with the new name, copies the inventory items from the old inventory to the new inventory,
     * then opens the new inventory.
     * @param newName The new name for the menu
     */
    public void editName(Component newName, User user)
    {
        //Create new inventory with new name
        Inventory newInventory = Bukkit.createInventory(null, inv.getSize(), newName);

        //Copy old inventory items to new inventory
        int iSize = inv.getSize();
        for (int i = 0 ; i < iSize ; i++)
        {
            newInventory.setItem(i, inv.getItem(i));
        }

        //Set the inventory of this GUI to the new inventory
        inv = newInventory;

        //Reopen the gui
        this.open(user);
    }

    public void delete() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            openInventories.remove(p.getUniqueId(), getUuid());
        }
        inventoriesByUUID.remove(getUuid());
    }

    public UUID getUuid() {
        return uuid;
    }

    public Map<Integer, guiAction> getActions() {
        return actions;
    }

}
