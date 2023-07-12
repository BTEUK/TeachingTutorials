package teachingtutorials.newlocation;

import net.buildtheearth.terraminusminus.generator.CachedChunkData;
import net.buildtheearth.terraminusminus.generator.ChunkDataLoader;
import net.buildtheearth.terraminusminus.generator.EarthGeneratorSettings;
import net.buildtheearth.terraminusminus.projection.GeographicProjection;
import net.buildtheearth.terraminusminus.projection.OutOfProjectionBoundsException;
import net.buildtheearth.terraminusminus.substitutes.ChunkPos;
import net.buildtheearth.terraminusminus.util.geo.LatLng;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.player.PlayerTeleportEvent;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.newlocation.elevation.ElevationManager;
import teachingtutorials.tutorials.Location;
import teachingtutorials.tutorials.Stage;
import teachingtutorials.tutorials.Tutorial;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.Mode;
import teachingtutorials.utils.User;
import teachingtutorials.utils.plugins.Multiverse;

import java.util.ArrayList;

//Holds the 4 stages that the New Location process can be in
enum NewLocationProcess
{
    startUp, inputtingAreaBounds, calculatingBounds, inputtingStartPosition, creatingLocationForDB, creatingNewWorld, generatingTerrain, inputtingAnswers
}

public class NewLocation
{
    private final EarthGeneratorSettings bteGeneratorSettings;
    private final GeographicProjection projection;
    private ChunkDataLoader loader;

    private User Creator;
    private Tutorial tutorial;
    private TeachingTutorials plugin;

    private int iStageIndex;
    private ArrayList<Stage> stages;

    //Keeps track of the current stage
    private Stage currentStage;

    public boolean bCompleteOrFinished = false;

    //Records what stage the new location creation process is at
    private NewLocationProcess stage;

    private ArrayList<LatLng> areaBounds;

    private int ixMin, ixMax, izMin, izMax;

    private int[][] iHeights;

    private Location location;

    //Stores the listeners globally so that they can be accessed and deregistered from outside of the class
    private AreaSelectionListener areaSelectionListener;
    private StartLocationListener startLocationListener;

    public NewLocation(User Creator, Tutorial tutorial, TeachingTutorials plugin)
    {
        //Stage set first since the projection creation can take a while, hence the startUp phase
        this.stage = NewLocationProcess.startUp;

        this.Creator = Creator;
        this.tutorial = tutorial;
        this.plugin = plugin;

        this.bteGeneratorSettings = EarthGeneratorSettings.parse(EarthGeneratorSettings.BTE_DEFAULT_SETTINGS);
        this.projection = bteGeneratorSettings.projection();
        this.loader = new ChunkDataLoader(bteGeneratorSettings);

        this.plugin.newLocations.add(this);
    }

    public int getTutorialID()
    {
        return this.tutorial.getTutorialID();
    }

    public User getCreator()
    {
        return this.Creator;
    }

    public String getTutorialName()
    {
        return this.tutorial.szTutorialName;
    }

    public Location getLocation() {
        return location;
    }

    //First method, started from the CreatorTutorialsMenu when they right click
    public void launchNewLocationAdding()
    {
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Starting new location adding");

        this.Creator.currentMode = Mode.Creating_New_Location;

        //Set up tpll listeners for area to generate and use and listen for when all points have been made
        areaSelectionListener = new AreaSelectionListener(this.Creator, plugin, this);
        areaSelectionListener.register();
        //Unregisters when the command "/tutorials endarea" is ran

        this.stage = NewLocationProcess.inputtingAreaBounds;

        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Registered area selection listener");
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Registered endarea listener");

        this.Creator.player.sendMessage(ChatColor.AQUA +"Use /tpll to draw an area in a circular order to generate the terrain for the location");
        this.Creator.player.sendMessage(ChatColor.AQUA +"Run /tutorials endarea once you are done");

        //Stores the area bounds globally in this class
        this.areaBounds = areaSelectionListener.getBounds();
    }

    //Called from the AreaSelectionListener once the selection has been made and the AreaSelectionListener unregistered
    public void AreaSelectionMade()
    {
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Area selection made");

        stage = NewLocationProcess.calculatingBounds;

        //---------------------------------------------------
        //----------Calculate range in the xz plane----------
        //---------------------------------------------------
        int i;

        //Amount of corners on the shape
        int iBounds = areaBounds.size();

        //Stores the corners of the box where blocks are to be generated, in decimal form
        double xMin, zMin, xMax, zMax;

        //Stores the mc coordinates of each corner as it gets converted from geographical to minecraft
        double[] xz;

        //Converts the first corner to minecraft coordinates
        LatLng areaBound = areaBounds.get(0);

        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Longitude: "+areaBound.getLng());
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Latitude: "+areaBound.getLat());

        try
        {
            xz = projection.fromGeo(areaBound.getLng(), areaBound.getLat());
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"x: "+xz[0]);
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"z: "+xz[1]);
        }
        catch (OutOfProjectionBoundsException e)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Could not create new location, one of the corners of the area is outside the projection");
            return;
        }

        //Stores this corner as the min and max
        xMin = xz[0];
        xMax = xz[0];
        zMin = xz[1];
        zMax = xz[1];

        //Gets the minecraft coordinates of the subsequent corners, and updates the minimum and maximum of the x and z coordinates
        for (i = 1 ; i < iBounds ; i++)
        {
            areaBound = areaBounds.get(i);
            try
            {
                xz = projection.fromGeo(areaBound.getLng(), areaBound.getLat());
            }
            catch (OutOfProjectionBoundsException e)
            {
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Could not create new location, one of the corners of the area is outside the projection");
                return;
            }

            if (xz[0] < xMin)
                xMin = xz[0];
            if (xz[0] > xMax)
                xMax = xz[0];
            if (xz[1] < zMin)
                zMin = xz[1];
            if (xz[1] > zMax)
                zMax = xz[1];
        }

        //Converts the min and maxes into integer form
        ixMin = (int) Math.round(xMin);
        ixMax = (int) Math.round(xMax);
        izMin = (int) Math.round(zMin);
        izMax = (int) Math.round(zMax);

        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Area of the minecraft world to be generated has been calculated");
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Range in x: " +ixMin +" to "+ixMax);
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Range in z: " +izMin +" to "+izMax);

        //Creates the listener for the start position coordinates
        startLocationListener = new StartLocationListener(this.Creator, plugin, this, ixMin, ixMax, izMin, izMax, projection);
        startLocationListener.register();

        this.stage = NewLocationProcess.inputtingStartPosition;
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Registered start location listener");

        //Prompts the creator to teleport (using tpll) to the start location of the tutorial
        this.Creator.player.sendMessage(ChatColor.AQUA +"Use /tpll to teleport to the desired start location");
    }

    //Called from the StartLocationListener once the start location has been dictated
    public void lessonStart(LatLng latLong)
    {
        //Updates the stage
        this.stage = NewLocationProcess.creatingLocationForDB;

        //Creates a new location object
        this.location = new Location(latLong, this.getTutorialID());

        //Adds the location to the database
        if (location.insertNewLocation())
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Inserted location into DB with LocationID "+location.getLocationID());
        else
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Could not insert location into DB");
            return;
        }

        String szWorldName = location.getLocationID()+"";

        Display errorCreatingWorldMessage;

        this.stage = NewLocationProcess.creatingNewWorld;

        //Creates the new world to store this location in
        try
        {
            if (Multiverse.createVoidWorld(szWorldName))
                Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Created new world");
            else
            {
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Could not create new world");
                errorCreatingWorldMessage = new Display(Creator.player, ChatColor.RED +"Could not create the world");
                errorCreatingWorldMessage.Message();
                return;
            }
        }
        catch (IllegalArgumentException e)
        {
            if (e.getMessage().equals("That world is already loaded!"))
            {
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Could not create new world. World already loaded");
                errorCreatingWorldMessage = new Display(Creator.player, ChatColor.RED +"Could not create the world. World already loaded");
                errorCreatingWorldMessage.Message();
                return;
            }
        }

        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Attempting to get world from Bukkit with name: "+szWorldName);

        World world = Bukkit.getWorld(szWorldName);
        if (world != null)
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"World object created in plugin with name: "+world.getName());
        else
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"World object for world with name: "+world.getName() +" could not be created in plugin");
            return;
        }

        this.stage = NewLocationProcess.generatingTerrain;

        //Generates the required area in the world
        Display generatingArea = new Display(Creator.player, ChatColor.AQUA +"Generating the area");
        generatingArea.Message();

        //For the future, an idea could be to generate terrain as soon as 3 area selection points have been made

        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    generateArea(world);
                }
                catch (OutOfProjectionBoundsException e)
                {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Out of bounds exception. Could not generate the required area in the world");
                }
                catch (Exception e)
                {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Could not generate the required area in the world: "+e.getMessage());
                    return;
                }
            }
        });
    }

    private void teleportCreatorAndStartLesson(World world)
    {
        double[] xz;

        //Converts the tpll coordinates to minecraft coordinates
        try
        {
            xz = projection.fromGeo(location.getStartCoordinates().getLng(), location.getStartCoordinates().getLat());
        }
        catch (OutOfProjectionBoundsException e)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"[TeachingTutorials] Unable to convert lat,long coordinates of start location to minecraft coordinates");
            return;
        }

        //Declares location object
        org.bukkit.Location tpLocation;

        //Start location must have been within the generated area to have got to this stage, so no out of bounds error should occur with the height
        try
        {
            tpLocation = new org.bukkit.Location(world, xz[0], iHeights[(int) (xz[0]-((ixMin>>4)<<4))][(int) (xz[1]-((izMin>>4)<<4))], xz[1]);
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Bukkit.Location object created for start location");
        }
        catch (Exception e)
        {
            tpLocation = new org.bukkit.Location(world, xz[0], 70, xz[1]);
            //    Display display = new Display(Creator.player, ChatColor.DARK_AQUA +"Error teleporting to start location, attempting again");
            //    display.Message();
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"[TeachingTutorials] Error occurred whilst creating Bukkit.Location object to tp to: "+e.getMessage());
        }

        //Teleports player to the start location
        Creator.player.teleport(tpLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Creator teleported to the start location, starting lesson to get answers");

        //Imports the stages of the tutorial, with no answers loaded
        this.stages = Stage.fetchStagesByTutorialIDWithoutLocationInformation(Creator.player, plugin, tutorial.getTutorialID(), location.getLocationID(), this);
        //Mode of each stage is automatically set to be Creating_New_Location

        //Sets stage number to 0
        this.iStageIndex = 0;

        //Starts the first stage
        nextStage();

        this.stage = NewLocationProcess.inputtingAnswers;
    }

    //Accessed after the end of each stage.
    //Called from Stage.endStage() asynchronously
    public void nextStage()
    {
        int iNumStages = stages.size();

        //1 indexed
        iStageIndex++;

        if (iStageIndex <= iNumStages)
        {
            currentStage = stages.get(iStageIndex-1);
            currentStage.startStage(1);
        }
        else
        {
            endLocation();
        }
    }

    public void terminateEarly()
    {
        //Unregisters the correct listeners
        switch (stage)
        {
            case inputtingAreaBounds:
                areaSelectionListener.deregister();
                break;
            case inputtingStartPosition:
                startLocationListener.deregister();
                break;
            case creatingLocationForDB:
                if(Location.deleteLocationByID(location.getLocationID()))
                    Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] - Location with LocationID = "+location.getLocationID() +" was deleted");
                else
                    Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] - Location with LocationID = "+location.getLocationID() +" could not be deleted");
                break;
            case creatingNewWorld:
            case generatingTerrain:
                //Delete the location in the DB
                if(Location.deleteLocationByID(location.getLocationID()))
                    Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] - Location with LocationID = "+location.getLocationID() +" was deleted");
                else
                    Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] - Location with LocationID = "+location.getLocationID() +" could not be deleted");

                //Delete the world
                Multiverse.deleteWorld(location.getLocationID()+"");
                break;
            case inputtingAnswers:
                //Remove tracker scoreboard
                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        getCreator().player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
                    }
                });

                //Remove the listeners
                currentStage.terminateEarly();

                //Delete the location in the DB
                if(Location.deleteLocationByID(location.getLocationID()))
                    Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] - Location with LocationID = "+location.getLocationID() +" was deleted");
                else
                    Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] - Location with LocationID = "+location.getLocationID() +" could not be deleted");

                //Delete the world
                Multiverse.deleteWorld(location.getLocationID()+"");
                break;
            default:
                break;
        }

        //Teleports the creator back to the lobby
        teleportToLobby();

        //Updates the status of the activity
        bCompleteOrFinished = true;

        //Removes the new location from the new locations list
        this.plugin.newLocations.remove(this);
    }

    private void endLocation()
    {
        //Informs the user of successful creation of a location
        Display display = new Display(Creator.player, ChatColor.UNDERLINE+""+ChatColor.DARK_GREEN +"Location Created!");
        display.Message();
        display.ActionBar();

        //Informs the console of successful creation of a location
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GREEN +"[TeachingTutorials] Location Created with LocationID: "+this.location.getLocationID());

        //Remove tracker scoreboard
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                getCreator().player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            }
        });

        //Changes the player's mode
        this.Creator.currentMode = Mode.Idle;

        //Teleports the creator back to the lobby
        teleportToLobby();

        //Updates the status of the activity
        bCompleteOrFinished = true;

        //Removes the new location from the new locations list
        this.plugin.newLocations.remove(this);
    }

    private void teleportToLobby()
    {
        FileConfiguration config = this.plugin.getConfig();

        String szLobbyTPType = config.getString("Lobby_TP_Type");

        //If a server switch is to occur
        if (szLobbyTPType.equals("Server"))
        {
            String szServerName = config.getString("Server_Name");

            //Switches the player's server after 40 seconds
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
            {
                @Override
                public void run()
                {
                    getCreator().player.performCommand("server " +szServerName);
                }
            }, 40L);
        }

        //If a player teleport is to occur
        else if (szLobbyTPType.equals("LobbyLocation"))
        {
            World tpWorld = Bukkit.getWorld(config.getString("Lobby_World"));
            if (tpWorld == null)
            {
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Cannot tp player to lobby, world null");
                Display display = new Display(getCreator().player, ChatColor.RED +"Cannot tp you to lobby");
                display.Message();
            }
            else
            {
                org.bukkit.Location location = new org.bukkit.Location(tpWorld, config.getDouble("Lobby_X"), config.getDouble("Lobby_Y"), config.getDouble("Lobby_Z"), config.getInt("Lobby_Yaw"), config.getInt("Lobby_Pitch"));

                //Teleports the player after 2 seconds
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
                {
                    @Override
                    public void run()
                    {
                        getCreator().player.teleport(location);
                    }
                }, 40L);
            }
        }
    }

    private void generateArea(World world) throws OutOfProjectionBoundsException
    {
        //UK121Generation(world);
        TerraMinusMinusGeneration(world);
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"[TeachingTutorials] Generated the area in the world");
        Display generatingArea = new Display(Creator.player, ChatColor.AQUA +"Area generated");
        generatingArea.Message();

        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                teleportCreatorAndStartLesson(world);
            }
        });
    }

    private void UK121Generation(World world) throws OutOfProjectionBoundsException
    {
        //Used to get the heights of an area
        ElevationManager elevationManager = new ElevationManager(projection);;

        //Gets heights for the area
        iHeights = elevationManager.getHeights(ixMin, ixMax, izMin, izMax);

        //Goes through the grid of heights and generates the block
        for (int x = 0; x < ixMax - ixMax; x++)
        {
            for (int z = 0; z < izMax - izMin; z++)
            {
                world.getBlockState(x, iHeights[x][z], z).setType(Material.GRASS_BLOCK);
            }
        }
    }

    private void TerraMinusMinusGeneration(World world)
    {
        CachedChunkData terraData;

        //Get chunks
        int ixMinChunk = ixMin >> 4;
        int ixMaxChunk = ixMax >> 4;
        int izMinChunk = izMin >> 4;
        int izMaxChunk = izMax >> 4;

        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Min X Chunk: "+ixMinChunk);
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Max X Chunk: "+ixMaxChunk);
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Min Z Chunk: "+izMinChunk);
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Max Z Chunk: "+izMaxChunk);

        iHeights = new int[(1+ixMaxChunk-ixMinChunk)<<4][(1+izMaxChunk-izMinChunk)<<4];

        //Go through each chunk and generate
        for (int ixChunk = ixMinChunk ; ixChunk <= ixMaxChunk ; ixChunk++)
        {
            for (int izChunk = izMinChunk ; izChunk <= izMaxChunk ; izChunk++)
            {
                Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Generating Chunk: "+ixChunk+","+izChunk);
                try
                {
                    terraData = loader.load(new ChunkPos(ixChunk, izChunk)).get();

                    for (int x = 0 ; x < 16 ; x++)
                    {
                        for (int z = 0 ; z < 16 ; z++)
                        {
                            final int iFinalxChunk = ixChunk;
                            final int iFinalX = x;
                            final int iFinalzChunk = izChunk;
                            final int iFinalZ = z;

                            iHeights[((ixChunk-ixMinChunk)<<4) + x][((izChunk-izMinChunk)<<4) + z] = terraData.groundHeight(x, z);

                            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                                @Override
                                public void run() {
                                    Block block = world.getBlockAt((iFinalxChunk << 4) + iFinalX, iHeights[((iFinalxChunk-ixMinChunk)<<4) + iFinalX][((iFinalzChunk-izMinChunk)<<4) + iFinalZ], (iFinalzChunk << 4) + iFinalZ);
                                    block.setType(Material.GRASS_BLOCK);
                                }
                            });


                            //world.getBlockState((ixChunk << 4) + x, iHeights[((ixChunk-ixMinChunk)<<4) + x][((izChunk-izMinChunk)<<4) + z], (izChunk << 4) + z).setType(Material.GRASS_BLOCK);
                            //This line no work
                        }
                    }
                }
                catch (Exception e)
                {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED+"[TeachingTutorials] Unable to generate block in the world: " +e.getMessage());
                    return;
                }
            }
        }
    }
}
