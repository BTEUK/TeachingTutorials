package teachingtutorials.utils;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.session.SessionManager;
import com.sk89q.worldedit.session.SessionOwner;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;

public class WorldEdit
{
    public static ArrayList<Location> BlocksCalculator(String szCommand, double[] xz1, double[] xz2, int iY1, int iY2, String szWorld)
    {
        World bukkitWorld = Bukkit.getWorld(szWorld);

        //Refer back to elgamer code. See how the line works there. Add more debug output, work out where the blocks are actually being placed

//        World world = new BukkitWorld(Bukkit.getWorld(szWorld));
//        EditSession editSession = com.sk89q.worldedit.WorldEdit.getInstance().newEditSession(world);
//
//        com.sk89q.worldedit.WorldEdit.getInstance().getSessionManager().getIfPresent( )
//
//        BlockVector3 point1 = BlockVector3.at(xz1[0], iY1, xz1[1]);
//        BlockVector3 point2 = BlockVector3.at(xz1[0], iY2, xz1[1]);
//        CuboidRegion cuboidRegion = new CuboidRegion(world, point1, point2);
//        BlockArrayClipboard clipboard = new BlockArrayClipboard(cuboidRegion);
//        editSession.drawLine()
//
//        Operation operation = new ClipboardHolder();


        //In this method, see if you can utilise the world edit api
        ArrayList<Location> line = null;

        if (szCommand.startsWith("/line"))
        {
            line = new ArrayList<Location>();
            int dx = (int) Math.abs(xz1[0]-xz2[0]);
            int dy = Math.abs(iY1-iY2);
            int dz = (int) Math.abs(xz1[1]-xz2[1]);

            double dMax = Math.max(Math.max(dx, dz), dy);
            double minX = xz1[0] + 0.5d;
            double minY = iY1 + 0.5d;
            double minZ = xz1[1] + 0.5d;

            int x1 = (int) xz1[0];
            int x2 = (int) xz2[0];
            int y1 = iY1;
            int y2 = iY2;
            int z1 = (int) xz1[1];
            int z2 = (int) xz2[1];

            if (dx + dy + dz == 0)
            {
                Bukkit.getConsoleSender().sendMessage("All add to 0");
                line.add(new Location(bukkitWorld, xz1[0], iY1, xz1[1]));
            }
            else
            {
                int tipx;
                int tipy;
                int tipz;
                int domstep;
                if (dMax == dx)
                {
                    Bukkit.getConsoleSender().sendMessage("It was dx = " +dx);
                    for(domstep = 0; domstep <= dx; domstep++)
                    {
                        tipx = x1 + domstep * (x2 - x1 > 0 ? 1 : -1);
                        tipy = (int)Math.round((double)y1 + (double)domstep * (double)dy / (double)dx * (double)(y2 - y1 > 0 ? 1 : -1));
                        tipz = (int)Math.round((double)z1 + (double)domstep * (double)dz / (double)dx * (double)(z2 - z1 > 0 ? 1 : -1));
                        Bukkit.getConsoleSender().sendMessage(tipx +", " +tipy +", " +tipz);
                        line.add(new Location(bukkitWorld, tipx, tipy, tipz));
                    }
                }
                else if (dMax == dy)
                {
                    Bukkit.getConsoleSender().sendMessage("It was dy = " +dy);
                    for(domstep = 0; domstep <= dy; ++domstep)
                    {
                        tipy = y1 + domstep * (y2 - y1 > 0 ? 1 : -1);
                        tipx = (int)Math.round((double)x1 + (double)domstep * (double)dx / (double)dy * (double)(x2 - x1 > 0 ? 1 : -1));
                        tipz = (int)Math.round((double)z1 + (double)domstep * (double)dz / (double)dy * (double)(z2 - z1 > 0 ? 1 : -1));
                        Bukkit.getConsoleSender().sendMessage(tipx +", " +tipy +", " +tipz);
                        line.add(new Location(bukkitWorld, tipx, tipy, tipz));
                    }
                }
                else
                {
                    Bukkit.getConsoleSender().sendMessage("It was dz = " +dz);
                    for(domstep = 0; domstep <= dz; ++domstep)
                    {
                        tipz = z1 + domstep * (z2 - z1 > 0 ? 1 : -1);
                        tipy = (int)Math.round((double)y1 + (double)domstep * (double)dy / (double)dz * (double)(y2 - y1 > 0 ? 1 : -1));
                        tipx = (int)Math.round((double)x1 + (double)domstep * (double)dx / (double)dz * (double)(x2 - x1 > 0 ? 1 : -1));
                        Bukkit.getConsoleSender().sendMessage(tipx +", " +tipy +", " +tipz);
                        line.add(new Location(bukkitWorld, tipx, tipy, tipz));
                    }
                }
            }

//            line = new ArrayList<Location>();
//            Location l;
//
//            for (int i = 1; i < divider; i++)
//            {
//                l = new Location(Bukkit.getWorld(szWorld), minX + i * (diffX / divider), xz1[1], minZ + i * (diffZ / divider));
//                line.add(l);
//            }
        }
        return line;
    }

    public static BlockData BlockTypeCalculator(String szCommand)
    {
        BlockData blockData = Bukkit.createBlockData(Material.STONE);
        return blockData;
    }
}
