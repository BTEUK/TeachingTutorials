package teachingtutorials.utils.plugins;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.internal.platform.WorldGuardPlatform;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Set;

public class WorldGuard
{
    public static void setWorldPerms(org.bukkit.World bukkitWorld, Player creator)
    {
        WorldGuardPlatform platform = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform();
        World world = BukkitAdapter.adapt(bukkitWorld);
        RegionManager regionManager = platform.getRegionContainer().get(world);

        BlockVector3 blockVector3 = BlockVector3.ZERO;
        //Add instead of get
        ProtectedRegion protectedRegion = new ProtectedCuboidRegion(ProtectedRegion.GLOBAL_REGION, blockVector3, blockVector3);
        regionManager.addRegion(protectedRegion);

        if (protectedRegion == null)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"[TeachingTutorials] Could not set worldguard perms for world");
        }
        else
        {
            //Make the whole world protected
            protectedRegion.setFlag(Flags.PASSTHROUGH, StateFlag.State.DENY);

            //Gets the members and owners
            DefaultDomain owners = protectedRegion.getOwners();
            DefaultDomain members = protectedRegion.getMembers();

            //Clears all members and owners
            members.clear();
            owners.clear();

            //Adds the creator as an owner
            owners.addPlayer(creator.getUniqueId());
        }

        try {
            regionManager.saveChanges();
        } catch (StorageException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds a player to the global region of this world as a member
     * @param bukkitWorld The world to add the player to
     * @param player The player to add to the world as a member
     */
    public static void addToGlobalRegion(org.bukkit.World bukkitWorld, Player player)
    {
        WorldGuardPlatform platform = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform();
        World world = BukkitAdapter.adapt(bukkitWorld);
        RegionManager regionManager = platform.getRegionContainer().get(world);

        //Gets the global region
        ProtectedRegion protectedRegion = regionManager.getRegion(ProtectedRegion.GLOBAL_REGION);

        if (protectedRegion == null)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"[TeachingTutorials] Could not set worldguard perms for world");
        }
        else
        {
            //Gets the members
            DefaultDomain members = protectedRegion.getMembers();

            //Adds the creator as a member
            members.addPlayer(player.getUniqueId());
        }

        try {
            regionManager.saveChanges();
        } catch (StorageException e) {
            e.printStackTrace();
        }
    }

    public static void removeFromGlobalRegion(org.bukkit.World bukkitWorld, Player player)
    {
        WorldGuardPlatform platform = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform();
        World world = BukkitAdapter.adapt(bukkitWorld);
        RegionManager regionManager = platform.getRegionContainer().get(world);

        //Gets the global region
        ProtectedRegion protectedRegion = regionManager.getRegion(ProtectedRegion.GLOBAL_REGION);

        if (protectedRegion == null)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"[TeachingTutorials] Could not set worldguard perms for world");
        }
        else
        {
            //Gets the members
            DefaultDomain members = protectedRegion.getMembers();

            //Adds the creator as a member
            members.removePlayer(player.getUniqueId());
        }

        try {
            regionManager.saveChanges();
        } catch (StorageException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new cuboid world guard region
     * @param szName The ID of the new region
     * @param world The world to add this region to
     * @param player Player to add as a member
     * @param minimumPoint Minimum x/y block of the region
     * @param maximumPoint Maximum
     */
    public static void createNewRegion(String szName, World world, Player player, BlockVector3 minimumPoint, BlockVector3 maximumPoint)
    {
        WorldGuardPlatform platform = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform();
//        World world = BukkitAdapter.adapt(bukkitWorld);
        RegionManager regionManager = platform.getRegionContainer().get(world);


        ProtectedRegion protectedRegion = new ProtectedCuboidRegion(szName, minimumPoint, maximumPoint);
        regionManager.addRegion(protectedRegion);

        if (protectedRegion == null)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"[TeachingTutorials] Could not set worldguard perms for world");
        }
        else
        {
            //Gets the members and owners
            DefaultDomain owners = protectedRegion.getOwners();
            DefaultDomain members = protectedRegion.getMembers();

            //Clears all members and owners
            members.clear();
            owners.clear();

            //Adds the creator as a member
            members.addPlayer(player.getUniqueId());
        }

        try {
            regionManager.saveChanges();
        } catch (StorageException e) {
            e.printStackTrace();
        }
    }

    /**
     * Removes a world guard region
     * @param szRegionName The ID of the region to delete
     * @param world The world which this region belongs in
     * @return Whether the region got removed or not
     */
    public static boolean removeRegion(String szRegionName, World world)
    {
        WorldGuardPlatform platform = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform();
        RegionManager regionManager = platform.getRegionContainer().get(world);

        try
        {
            Set<ProtectedRegion> removedRegions = regionManager.removeRegion(szRegionName);
            regionManager.saveChanges();
            if (removedRegions.size() > 0)
            {
                Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Some regions were removed");
                return true;
            }
            else
            {
                Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] No regions were removed");
                return false;
            }

        }
        catch (StorageException e)
        {
            e.printStackTrace();
            return false;
        }
        catch (NullPointerException npe)
        {
            npe.printStackTrace();
            return false;
        }
    }
}
