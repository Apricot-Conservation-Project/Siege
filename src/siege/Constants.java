package siege;

import mindustry.content.Blocks;
import mindustry.world.Block;

public class Constants {
    public static final int RAIDER_MAX_PLAYERS = 3;

    public static final Block DEAD_ZONE_FILLER_FLOOR = Blocks.redStone;
    // Fx to indicate keep borders

    public static final int TEAM_SETUP_TIME_SECONDS = 120;
    public static final int CORE_PLACEMENT_TIME_SECONDS = 60;
    public static final int SETUP_TIME_SECONDS = TEAM_SETUP_TIME_SECONDS + CORE_PLACEMENT_TIME_SECONDS;
}
