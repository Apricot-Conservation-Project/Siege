package siege;

import arc.math.geom.Point2;
import arc.struct.Seq;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.UnitTypes;
import mindustry.type.ItemSeq;
import mindustry.type.ItemStack;
import mindustry.type.UnitType;
import mindustry.world.Block;

import java.util.List;

public final class Constants {
    public static final int RAIDER_MAX_PLAYERS = 3;
    public static final int TEAM_STARTING_ID = 7;

    public static final Block DEAD_ZONE_FILLER_FLOOR = Blocks.denseRedStone;

    public static final Seq<Block> CORE_TYPES = new Seq<>(new Block[]{Blocks.coreShard, Blocks.coreFoundation, Blocks.coreNucleus});

    public static final float CORE_PLACEMENT_MIN_DISTANCE = 120f;
    public static final float CORE_PLACEMENT_CITADEL_MIN_DISTANCE = 225f;
    public static final int MAX_DISTANCE_TO_VIABLE_CORE_LOCATION = 80; // The farthest the algorithm for core placement will adjust an invalid core.

    public static final float SHARD_DEAD_ZONE_RADIUS = 30f;
    public static final float FOUNDATION_DEAD_ZONE_RADIUS = 40f;
    public static final float NUCLEUS_DEAD_ZONE_RADIUS = 50f;
    // Fx to indicate keep borders
    public static final float DEAD_ZONE_DAMAGE_CONSTANT_TICK = 55f / 60f; // Absolute damage every tick
    public static final float DEAD_ZONE_DAMAGE_PERCENT_TICK = 0.018f / 60f; // Percent of max health every tick
    public static final List<UnitType> DEAD_ZONE_IMMUNE_TYPES = List.of(new UnitType[] {
            UnitTypes.alpha,
            UnitTypes.beta,
            UnitTypes.gamma,
    });

    // Must be charged from the vault
    public static final ItemSeq CONSTANT_CORE_PRICE_LOCAL = new ItemSeq(ItemStack.list(
            Items.thorium, 1000,
            Items.phaseFabric, 200
    ));

    // Chargeable from vault or team storage
    public static final ItemSeq CONSTANT_CORE_PRICE_GLOBAL = new ItemSeq(ItemStack.list(
            Items.plastanium, 400,
            Items.silicon, 300,
            Items.copper, 1000,
            Items.lead, 200
    ));

    // Chargeable from vault or team storage
    // Charged according to total number of cores built before the desired core
    public static final ItemSeq PER_CORE_PRICE = new ItemSeq(ItemStack.list(
            Items.plastanium, 200,
            Items.thorium, 500,
            Items.phaseFabric, 1000,
            Items.silicon, 1000
    ));

    // Black magic
    public static final float HARMONIC_CORE_COUNT_POWER_FACTOR = 0.6f;
    public static final ItemSeq HARMONIC_DISTANCE_CORE_PRICE = new ItemSeq(ItemStack.list(
            Items.phaseFabric, 50,
            Items.silicon, 250,
            Items.surgeAlloy, 150
    ));

    public static final int TEAM_SETUP_TIME_SECONDS = 120;
    public static final int CORE_PLACEMENT_TIME_SECONDS = 60;
    public static final int SETUP_TIME_SECONDS = TEAM_SETUP_TIME_SECONDS + CORE_PLACEMENT_TIME_SECONDS;

    public static final int VOTEKICK_LENGTH_PREGAME_MS = 30 * 1000;
    public static final int VOTEKICK_LENGTH_MS = 90 * 1000;

    public static final ItemSeq RAIDER_LOADOUT = new ItemSeq(ItemStack.list(
            Items.copper, 3600,
            Items.lead, 3600,
            Items.graphite, 600,
            Items.titanium, 800,
            Items.silicon, 500,
            Items.metaglass, 600
    ));
    public static final ItemSeq CITADEL_LOADOUT = new ItemSeq(ItemStack.list(
            Items.copper, 500,
            Items.lead, 300
    ));



    public static final class Performance {
        // All 16 points within an integer grid from -1 to 2 (All point offsets within a Foundation core from that core's tile x and y)
        // Arranged in order such that a collision should be found as early in the list as possible.
        public static final Point2[] COLLISION_OFFSETS = new Point2[] {
                new Point2(-1, -1),
                new Point2(-1, 2),
                new Point2(2, -1),
                new Point2(2, 2),
                new Point2(-1, 0),
                new Point2(-1, 1),
                new Point2(2, 0),
                new Point2(2, 1),
                new Point2(0, -1),
                new Point2(1, -1),
                new Point2(0, 2),
                new Point2(1, 2),
                new Point2(0, 0),
                new Point2(0, 1),
                new Point2(1, 0),
                new Point2(1, 1)
        };
    }
}
