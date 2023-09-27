package teachingtutorials.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class Utils {

    public static String chat (String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static ItemStack createItem(Material material, int amount, Component displayName, Component... loreString)
    {
        ItemStack item;

        item = new ItemStack(material);
        item.setAmount(amount);

        ItemMeta meta = item.getItemMeta();
        meta.displayName(displayName);
        List<Component> lore = new ArrayList<>(Arrays.asList(loreString));
        meta.lore(lore);
        item.setItemMeta(meta);

        return item;
    }

    public static ItemStack createPlayerSkull(Inventory inv, Player p, int amount, int invSlot, String displayName, String... loreString) {

        ItemStack item;

        List<String> lore = new ArrayList<String>();

        item = new ItemStack(Material.PLAYER_HEAD);

        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setDisplayName(Utils.chat(displayName));
        for (String s : loreString) {
            lore.add(Utils.chat(s));
        }
        meta.setLore(lore);
        meta.setOwningPlayer(p);
        item.setItemMeta(meta);

        inv.setItem(invSlot - 1,  item);

        return item;

    }

    public static boolean isPlayerInGroup(Player player, String group) {
        return player.hasPermission("group." + group);
    }

    public static void spawnFireWork(Player p) {

        Firework f = p.getWorld().spawn(p.getLocation(), Firework.class);
        FireworkMeta fm = f.getFireworkMeta();
        fm.addEffect(FireworkEffect.builder().flicker(true).trail(true).with(Type.BALL_LARGE).withColor(Color.RED).withColor(Color.BLUE).withColor(Color.WHITE).build());
        fm.setPower(1);
        f.setFireworkMeta(fm);


    }

    public static int getHighestYAt(World w, int x, int z) {

        for (int i = 255; i >= 0; i--) {
            if (w.getBlockAt(x, i, z).getType() != Material.AIR) {
                return i+1;
            }
        }
        return 0;
    }

    //Gives a player an item, it will be set in their main hand, if it does not already exist there.

    //If the main hand is empty, set it there.
    //If then main hand is slot 8 and includes the navigator, find the first empty slot available and set it there.
    //If no empty slots are available set it to slot 7.
    //If the main hand has an item swap the current item to an empty slot in the inventory.
    //If no empty slots are available overwrite it.

    public static void giveItem(Player p, ItemStack item, String name) {
        int emptySlot = getEmptyHotbarSlot(p);

        boolean hasItemAlready = p.getInventory().containsAtLeast(item, 1);

        //If we already have the item switch to current slot.
        if (hasItemAlready)
        {
            //Switch item to current slot.
            int slot = p.getInventory().first(item);

            p.getInventory().setItem(slot, p.getInventory().getItemInMainHand());
            p.getInventory().setItemInMainHand(item);
            p.sendMessage(Component.text("Set ", NamedTextColor.GREEN).append(Component.text(name, NamedTextColor.DARK_AQUA).append(Component.text(" to main hand.", NamedTextColor.GREEN))));
        }
        else if (emptySlot >= 0)
        {
            //The current slot is empty. This also implies no navigator, and thus the item does not yet exist in the inventory.
            //Set item to empty slot.
            p.getInventory().setItem(emptySlot, item);
            p.sendMessage(Component.text("Set ", NamedTextColor.GREEN).append(Component.text(name, NamedTextColor.DARK_AQUA).append(Component.text(" to slot " + (emptySlot + 1), NamedTextColor.GREEN))));

        }
        else
        {
            //Player has no empty slots and is holding the navigator and learning menu, set to item to slot 6.
            p.getInventory().setItem(6, item);
            p.sendMessage(Component.text("Set ", NamedTextColor.GREEN).append(Component.text(name, NamedTextColor.DARK_AQUA).append(Component.text(" to slot 8", NamedTextColor.GREEN))));

        }
    }

    //Return an empty hotbar slot, if no empty slot exists return -1.
    public static int getEmptyHotbarSlot(Player p) {

        //If main hand is empty return that slot.
        ItemStack heldItem = p.getInventory().getItemInMainHand();
        if (heldItem.getType() == Material.AIR) {
            return p.getInventory().getHeldItemSlot();
        }

        //Check if hotbar has an empty slot.
        for (int i = 0; i < 9; i++) {

            ItemStack item = p.getInventory().getItem(i);

            if (item == null) {
                return i;
            }
            if (item.getType() == Material.AIR) {
                return i;
            }
        }

        //No slot could be found, return -1.
        return -1;
    }
}
