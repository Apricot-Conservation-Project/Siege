package siege;

import arc.math.Mathf;
import arc.math.geom.Point2;
import mindustry.content.Blocks;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.storage.CoreBlock;

import java.util.ArrayList;
import java.util.List;

import static mindustry.Vars.world;

public final class Gamedata {
    public static List<RaiderTeam> deadRaiderTeams = new ArrayList<>();
    public static List<RaiderTeam> raiderTeams = new ArrayList<>();
    public static long startTime = 0;
    public static boolean gameStarted = false; // Owned by Setup
    public static boolean gameOver = false;
    //public int mapIndex;

    // All true for setup, then cache set up when game begins
    // Should not be read or used at all during setup.
    private static boolean[][] deadZoneCache;
    // The floor beneath the dead zone
    private static Floor[][] actualFloor;
    private static Floor[][] actualOverlay;

    public static void reset() {
        startTime = System.currentTimeMillis() + 1000 * Constants.SETUP_TIME_SECONDS;
        raiderTeams = new ArrayList<>();
        deadRaiderTeams = new ArrayList<>();
        gameOver = false;
    }

    public static void initCache() {
        deadZoneCache = new boolean[world.width()][world.height()];
        actualFloor = new Floor[world.width()][world.height()];
        actualOverlay = new Floor[world.width()][world.height()];
        for (int x = 0; x < world.width(); x ++) {
            for (int y = 0; y < world.height(); y++) {
                deadZoneCache[x][y] = true;
                actualFloor[x][y] = world.tile(x, y).floor();
                actualOverlay[x][y] = world.tile(x, y).overlay();
            }
        }
    }

    /**
     * @return Whether the game should have started yet. Prefer its variable form for most cases, as this is based only on time and could desync.
     */
    public static boolean gameStartTime() {
        return elapsedTimeSeconds() >= 0;
    }

    /**
     * @return Whether team configuration is still allowed
     */
    public static boolean teamSetupPhase() {
        return elapsedTimeSeconds() < -Constants.CORE_PLACEMENT_TIME_SECONDS;
    }

    /**
     * @return The number of seconds since the game's start time (end of setup). Will be negative if setup is still ongoing.
     */
    public static long elapsedTimeSeconds() {
        return (System.currentTimeMillis() - startTime) / 1000;
    }

    /**
     * @return Whether the keep should exist at this moment
     */
    public static boolean keepExists() {
        return Gamedata.raiderTeams.size() > 1 || elapsedTimeSeconds() < Constants.GUARANTEED_KEEP_TIME_SECONDS;
    }

    /**
     * Checks if a tile is in the keep. Ignores whether the keep has dissolved or not.
     * @param tile The location to query
     * @return Whether that location could be in the keep
     */
    public static boolean inKeep(Point2 tile) {
        float worldMiddleX = world.width() / 2f;
        float worldMiddleY = world.height() / 2f;
        float manhattanDist = Math.abs(worldMiddleX - tile.x) + Math.abs(worldMiddleY - tile.y);
        return manhattanDist <= Constants.KEEP_RADIUS;
    }

    /**
     * Checks if a building is in the keep. Ignores whether the keep has dissolved or not. Buildings must be fully inside the keep to count.
     * @param building The building to query
     * @return Whether the building could be in the keep
     */
    public static boolean inKeep(Building building) {
        float middleX = building.tileX();
        float middleY = building.tileY();
        if (building.block.size % 2 == 0) {
            middleX += 0.5f;
            middleY += 0.5f;
        }
        float[] offsets = new float[] { -building.block.size / 2f, building.block.size / 2f };
        boolean inside = true;
        for (float xOffset : offsets) {
            for (float yOffset : offsets) {
                if (!inKeep(new Point2((int) (middleX + xOffset), (int) (middleY + yOffset)))) {
                    inside = false;
                }
            }
        }
        return inside;
    }

    /**
     * @return All the player cores in the game
     */
    public static CoreBlock.CoreBuild[] getAllCores() {
        ArrayList<CoreBlock.CoreBuild> cores = Team.green.cores().list();

        for (RaiderTeam team : raiderTeams) {
            cores.addAll(List.of(team.getCores()));
        }

        return cores.toArray(new CoreBlock.CoreBuild[0]);
    }

    /**
     * @param coreType The type of core to search for
     * @return All player cores of the given type
     */
    public static CoreBlock.CoreBuild[] getAllCores(Block coreType) {
        CoreBlock.CoreBuild[] allCores = getAllCores();
        ArrayList<CoreBlock.CoreBuild> cores = new ArrayList<>();

        for (CoreBlock.CoreBuild core : allCores) {
            if (core.block == coreType) {
                cores.add(core);
            }
        }

        return cores.toArray(new CoreBlock.CoreBuild[0]);
    }

    /**
     * Get the radius that the given core reveals around the dead zone
     * @param core
     * @return
     */
    public static float getCoreSafetyRadius(Block core) {
        if (core == Blocks.coreShard) {
            return Constants.SHARD_DEAD_ZONE_RADIUS;
        } else if (core == Blocks.coreFoundation) {
            return Constants.FOUNDATION_DEAD_ZONE_RADIUS;
        } else if (core == Blocks.coreNucleus) {
            return Constants.NUCLEUS_DEAD_ZONE_RADIUS;
        }

        throw new IllegalArgumentException("Unknown core type: " + core.toString());
    }

    public static float getCoreSafetyRadius2(Block core) {
        return Mathf.sqr(getCoreSafetyRadius(core));
    }

    /**
     * Finds if a given point is in the dead zone.
     * @param tile The world grid tile to check
     * @return Whether the tile is in the dead zone
     */
    public static boolean getDeadZone(Tile tile) {
        return deadZoneCache[tile.x][tile.y];
    }

    /**
     * Finds if a given point is in the dead zone.
     * @param point The world grid point to check
     * @return Whether the tile is in the dead zone
     */
    public static boolean getDeadZone(Point2 point) {
        return deadZoneCache[point.x][point.y];
    }

    /**
     * Finds if a given point is in the dead zone.
     * Expensive operation as compared to getDeadZone.
     * Writes result to cache.
     * @param tile The world grid tile to check
     * @return Whether the tile is in the dead zone
     */
    public static boolean hardGetDeadZone(Point2 tile) {
        CoreBlock.CoreBuild[] cores = getAllCores();

        boolean result = true;
        for (CoreBlock.CoreBuild core : cores) {
            float coreX = core.tileX();
            float coreY = core.tileY();
            if (core.block.size % 2 == 0) {
                coreX += 0.5f;
                coreY += 0.5f;
            }
            if ((Mathf.dst2(tile.x, tile.y, coreX, coreY)) < getCoreSafetyRadius2(core.block)) {
                result = false;
                break;
            }
        }

        updateDeadZone(tile, result);
        return result;
    }

    private static void updateDeadZone(Point2 tile, boolean deadZone) {
        deadZoneCache[tile.x][tile.y] = deadZone;
        if (deadZone) {
            world.tile(tile.x, tile.y).setFloorNet(Constants.DEAD_ZONE_FILLER_FLOOR);
        } else {
            world.tile(tile.x, tile.y).setFloorNet(actualFloor[tile.x][tile.y], actualOverlay[tile.x][tile.y]);
        }
    }

    /**
     * Rewrites the entire map floor.
     */
    public static void reloadFloor() {
        for (int x = 0; x < world.width(); x ++) {
            for (int y = 0; y < world.height(); y++) {
                Block floor = world.floor(x, y);
                Block desiredFloor;
                Block desiredOverlay;
                if (getDeadZone(new Point2(x, y))) {
                    desiredFloor = Constants.DEAD_ZONE_FILLER_FLOOR;
                    desiredOverlay = Blocks.air;
                } else {
                    desiredFloor = actualFloor[x][y];
                    desiredOverlay = actualOverlay[x][y];
                }
                if (floor != desiredFloor) {
                    world.tile(x, y).setFloorNet(desiredFloor, desiredOverlay);
                }
            }
        }
    }

    public static void reloadCore(CoreBlock.CoreBuild core) {
        // Reload deadzone cache around core
        float radius = Gamedata.getCoreSafetyRadius(core.block);

        int minX = Math.max(0, Mathf.floor(core.tileX() - radius - 1));
        int minY = Math.max(0, Mathf.floor(core.tileY() - radius - 1));
        int maxX = Math.min(world.width() - 1, Mathf.ceil(core.tileX() + radius + 1));
        int maxY = Math.min(world.width() - 1, Mathf.ceil(core.tileY() + radius + 1));
        for (int x = minX; x < maxX; x ++) {
            for (int y = minY; y < maxY; y++) {
                Gamedata.hardGetDeadZone(new Point2(x, y));
            }
        }
    }

    public static void dataDump() {
        System.out.println("\nGamedata and Setup dump\n");
        System.out.println("deadRaiderTeams: " + deadRaiderTeams.toString());
        System.out.println("raiderTeams: " + raiderTeams.toString());
        System.out.println("startTime: " + startTime);
        System.out.println("gameStarted: " + gameStarted);
        System.out.println("gameOver: " + gameOver);
        System.out.println("elapsedTimeSeconds(): " + elapsedTimeSeconds());
        System.out.println("--- Setup ---");
        Setup.dataDump();
        System.out.println("\n");
    }
}
