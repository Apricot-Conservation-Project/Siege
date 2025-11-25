package siege;

import arc.math.Mathf;
import arc.math.geom.Point2;
import mindustry.content.Blocks;
import mindustry.gen.Building;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.storage.CoreBlock;

import static mindustry.Vars.world;

public final class DeadZone {
    // All true for setup, then cache set up when game begins
    // Should not be read or used at all during setup.
    private static boolean[][] deadZoneCache;
    // The floor beneath the dead zone
    private static Floor[][] actualFloor;
    private static Floor[][] actualOverlay;

    // Are we currently in the process of updating the dead zone?
    private static boolean updatingDeadZone = false;
    private static int updateIndex = 0;
    // How many ticks should writing the dead zone be staggered across?
    private static final int UPDATE_DIVISIONS = 1000;

    /**
     * This should be called once every tick.
     */
    public static void update() {
        if (updatingDeadZone) {
            int i = updateIndex;
            int x = i % world.width();
            int y = i / world.width();
            while (x < world.width() && y < world.height()) {
                Block floor = world.floor(x, y);
                if (floor == Blocks.grass.asFloor()) {
                    // No dead zone on grass :)
                    deadZoneCache[x][y] = false;
                }
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

                i += UPDATE_DIVISIONS;
                x = i % world.width();
                y = i / world.width();
            }

            updateIndex ++;
            if (updateIndex == UPDATE_DIVISIONS) {
                updatingDeadZone = false;
            }
        }
    }

    public static boolean isUpdatingDeadZone() {
        return updatingDeadZone;
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
     * Get the radius that the given core reveals around the dead zone
     * @param core The core in question
     * @return The radius that the given core type should clear around itself of the deadzone
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
        if (tile == null) return true;
        return deadZoneCache[tile.x][tile.y];
    }

    /**
     * Finds if a given point is in the dead zone.
     * @param point The world grid point to check
     * @return Whether the tile is in the dead zone
     */
    public static boolean getDeadZone(Point2 point) {
        if (point == null) return true;
        if (point.x < 0 || point.y < 0 || point.x >= deadZoneCache.length || point.y >= deadZoneCache[0].length) return true;
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
        if (world.tile(tile.x, tile.y).floor() == Blocks.grass.asFloor()) {
            return false;
        }

        CoreBlock.CoreBuild[] cores = Gamedata.getAllCores();

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
     * Rewrites the entire map floor. Doesn't find new values, only reloads from memory.
     */
    public static void reloadFloor() {
        // Starts the reloading process
        updateIndex = 0;
        updatingDeadZone = true;
    }

    public static void reloadCore(CoreBlock.CoreBuild core) {
        // Reload deadzone cache around core
        float radius = getCoreSafetyRadius(core.block);

        int minX = Math.max(0, Mathf.floor(core.tileX() - radius - 1));
        int minY = Math.max(0, Mathf.floor(core.tileY() - radius - 1));
        int maxX = Math.min(world.width() - 1, Mathf.ceil(core.tileX() + radius + 1));
        int maxY = Math.min(world.width() - 1, Mathf.ceil(core.tileY() + radius + 1));
        for (int x = minX; x < maxX; x ++) {
            for (int y = minY; y < maxY; y++) {
                hardGetDeadZone(new Point2(x, y));
            }
        }
    }

    /**
     * Checks if a (theoretical) building would be inside the dead zone. Any portion being over the dead zone counts. Pretends that the specified block was placed at the given coordinates.
     * @param buildingX The x position of the building
     * @param buildingY The y position of the building
     * @param block The type of block that the building is
     * @param hard Whether to 'hard' get - and force a reload of - the building tiles
     * @return Whether the building would be in the dead zone
     */
    public static boolean insideDeadZone(int buildingX, int buildingY, Block block, boolean hard) {
        float middleX = buildingX;
        float middleY = buildingY;
        if (block.size % 2 == 0) {
            middleX += 0.5f;
            middleY += 0.5f;
        }
        float diff = (block.size - 1) / 2f;
        int lowX = (int)(middleX - diff);
        int lowY = (int)(middleY - diff);
        int highX = (int)(middleX + diff);
        int highY = (int)(middleY + diff);
        for (int x = lowX; x <= highX; x ++) {
            for (int y = lowY; y <= highY; y ++) {
                if (hard) {
                    if (hardGetDeadZone(new Point2(x, y))) {
                        return true;
                    }
                } else {
                    if (getDeadZone(new Point2(x, y))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks if a building is inside the dead zone. Any portion being over the dead zone counts.
     * @param building The building to query
     * @param hard Whether to 'hard' get - and force a reload of - the building tiles
     * @return Whether the building is in the dead zone
     */
    public static boolean insideDeadZone(Building building, boolean hard) {
        return insideDeadZone(building.tile.x, building.tile.y, building.block, hard);
    }
}
