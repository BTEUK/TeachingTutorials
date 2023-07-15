package teachingtutorials.utils.plugins;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.internal.platform.WorldGuardPlatform;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

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

            //Adds the creator as a member
            members.addPlayer(creator.getUniqueId());
        }
    }
}
