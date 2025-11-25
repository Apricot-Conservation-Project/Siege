package siege;

import arc.math.Mathf;
import arc.math.geom.Point2;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.world.Block;
import mindustry.world.Tile;

import static mindustry.Vars.world;

public class Keep {
    public static boolean keepExisted = false;

    /**
     * Handles the tasks that have to run when the keep dissolves
     */
    public static void keepDissolvedListener() {
        SiegePlugin.announce("[accent]The keep has dissolved. Central Citadel buildings are now vulnerable, but turrets can be built in its place.");
        // When the keep dissolves, all keep buildings should revert to standard health
        world.tiles.forEach(tile -> {
            if (tile.build != null && Keep.inKeep(tile.build) && tile.build.team() == Team.green) {
                System.out.println(tile);
                System.out.println(tile.build.health);
                System.out.println(tile.build.maxHealth);
                tile.build.health = tile.build.maxHealth;
            }
        });

        // Fix all citadel units
        Groups.unit.forEach(unit -> {
            if (unit.team == Team.green) {
                unit.damageMultiplier(1f);
            }
        });
    }

    /**
     * @return Whether the keep should exist at this moment
     */
    public static boolean keepExists() {
        return Gamedata.raiderTeams.size() > 1 || Gamedata.elapsedTimeSeconds() < Constants.GUARANTEED_KEEP_TIME_SECONDS;
    }

    /**
     * Checks if a tile is in the keep. Ignores whether the keep has dissolved or not.
     * @param tile The location to query
     * @return Whether that location could be in the keep
     */
    public static boolean inKeep(Tile tile) {
        return inKeep(new Point2(tile.x, tile.y));
    }

    /**
     * Checks if a tile is in the keep. Ignores whether the keep has dissolved or not.
     * @param tile The location to query
     * @return Whether that location could be in the keep
     */
    public static boolean inKeep(Point2 tile) {
        float worldMiddleX = (Vars.world.width()-1) / 2f;
        float worldMiddleY = (Vars.world.height()-1) / 2f;
        float manhattanDist = Math.abs(worldMiddleX - tile.x) + Math.abs(worldMiddleY - tile.y);
        return manhattanDist <= Constants.KEEP_RADIUS;
    }

    /**
     * Checks if a (theoretical) building is or would be in the keep. Ignores whether the keep has dissolved or not. Buildings must be fully inside the keep to count. Pretends that the specified block was placed at the given coordinates.
     * @param x The x position of the building
     * @param y The y position of the building
     * @param block The type of block that the building is
     * @return Whether that building would be in the keep
     */
    public static boolean inKeep(int x, int y, Block block) {
        float middleX = x;
        float middleY = y;
        if (block.size % 2 == 0) {
            middleX += 0.5f;
            middleY += 0.5f;
        }
        float diff = block.size / 2f - 0.5f;
        float[] offsets = new float[] { -diff, diff };
        boolean inside = true;
        for (float xOffset : offsets) {
            for (float yOffset : offsets) {
                if (!inKeep(new Point2(Mathf.round(middleX + xOffset), Mathf.round(middleY + yOffset)))) {
                    inside = false;
                }
            }
        }
        return inside;
    }

    /**
     * Checks if a building is in the keep. Ignores whether the keep has dissolved or not. Buildings must be fully inside the keep to count.
     * @param building The building to query
     * @return Whether the building could be in the keep
     */
    public static boolean inKeep(Building building) {
        return inKeep(building.tile.x, building.tile.y, building.block);
    }
}
