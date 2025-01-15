package siege;

import arc.math.geom.Point2;
import mindustry.gen.Building;

import static mindustry.Vars.world;

public class Keep {
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
}
