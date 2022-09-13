package teachingtutorials.utils.plugins;

import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import org.bukkit.*;

import com.onarandombox.MultiverseCore.MultiverseCore;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.utils.Utils;

public class Multiverse
{
    public static boolean createVoidWorld(String name)
    {
        MultiverseCore core = (MultiverseCore) Bukkit.getServer().getPluginManager().getPlugin("Multiverse-Core");

        if (core == null)
        {
            TeachingTutorials.getInstance().getLogger().severe(Utils.chat("&cMultiverse is a dependency of PlotSystem!"));
            return false;
        }

        MVWorldManager worldManager = core.getMVWorldManager();

        worldManager.addWorld(
                name,
                World.Environment.NORMAL,
                null,
                WorldType.FLAT,
                false,
                "VoidGen:{biome:PLAINS}"
        );

        MultiverseWorld MVWorld = worldManager.getMVWorld(name);
        MVWorld.setGameMode(GameMode.CREATIVE);
        MVWorld.setAllowAnimalSpawn(false);
        MVWorld.setAllowMonsterSpawn(false);
        MVWorld.setDifficulty(Difficulty.PEACEFUL);
        MVWorld.setEnableWeather(false);
        MVWorld.setHunger(false);

        //Get world from bukkit.
        World world = Bukkit.getWorld(name);

        if (world == null) {
            TeachingTutorials.getInstance().getLogger().warning("World is null!");
            return false;
        }

        //Disable daylightcycle.
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setTime(6000);

        //Disable fire tick.
        world.setGameRule(GameRule.DO_FIRE_TICK, false);

        //Disable random tick.
        world.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);

        Bukkit.getLogger().info("Created new world with name " + name);

        return true;
    }

    public static boolean hasWorld(String name) {

        MultiverseCore core = (MultiverseCore) Bukkit.getServer().getPluginManager().getPlugin("Multiverse-Core");

        if (core == null) {
            Bukkit.getLogger().severe(Utils.chat("&cMultiverse is a dependency of PlotSystem!"));
            return false;
        }

        //If the world exists return true.

        MVWorldManager worldManager = core.getMVWorldManager();

        MultiverseWorld world = worldManager.getMVWorld(name);

        return world != null;
    }

    public static boolean deleteWorld(String name) {

        MultiverseCore core = (MultiverseCore) Bukkit.getServer().getPluginManager().getPlugin("Multiverse-Core");

        if (core == null) {
            Bukkit.getLogger().severe(Utils.chat("&cMultiverse is a dependency of PlotSystem!"));
            return false;
        }

        //If world exists delete it.
        MVWorldManager worldManager = core.getMVWorldManager();

        MultiverseWorld world = worldManager.getMVWorld(name);

        if (world == null) {
            return false;
        } else {
            worldManager.deleteWorld(name);
            return true;
        }

    }
}