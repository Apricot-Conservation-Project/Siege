package siege;

import arc.math.geom.Point2;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.UnitTypes;
import mindustry.entities.Effect;
import mindustry.gen.Unit;
import mindustry.type.ItemSeq;
import mindustry.type.ItemStack;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.blocks.environment.Floor;

import java.util.List;

public final class Constants {
    public static final int RAIDER_MAX_PLAYERS = 3; // Maximum count of players that can serve on the same Raider team
    public static final long OFFLINE_TIMEOUT_PERIOD = 5 * 60 * 1000; // Time (milliseconds) that no players can be present in the server before the game automatically ends
    public static final long AFK_TIMEOUT_PERIOD = 10 * 60 * 1000; // Time (milliseconds) that no players can be active in the server before the game automatically ends
    public static final boolean CITADEL_WINS_ON_RAIDER_TIMEOUT = true;
    public static final int TEAM_STARTING_ID = 7; // Lowest ID that will be allocated to raider teams

    public static final Block DEAD_ZONE_FILLER_FLOOR = Blocks.denseRedStone;

    public static final Seq<Block> CORE_TYPES = new Seq<>(new Block[]{Blocks.coreShard, Blocks.coreFoundation, Blocks.coreNucleus});

    public static final Seq<Floor> NO_CORE_FLOORS = new Seq<>(new Floor[]{Blocks.grass.asFloor(), Blocks.slag.asFloor()});

    public static final float CORE_PLACEMENT_MIN_DISTANCE = 120f;
    public static final float CORE_PLACEMENT_CITADEL_MIN_DISTANCE = 225f;
    public static final int MAX_DISTANCE_TO_VIABLE_CORE_LOCATION = 80; // The farthest the algorithm for core placement will adjust an invalid core.

    public static final float SHARD_DEAD_ZONE_RADIUS = 30f;
    public static final float FOUNDATION_DEAD_ZONE_RADIUS = 40f;
    public static final float NUCLEUS_DEAD_ZONE_RADIUS = 50f;

    public static final int VOTEKICK_LENGTH_PREGAME_MS = 30 * 1000;
    public static final int VOTEKICK_LENGTH_MS = 90 * 1000;

    public static final int TEAM_SETUP_TIME_SECONDS = 20;
    public static final int CORE_PLACEMENT_TIME_SECONDS = 10;
    public static final int SETUP_TIME_SECONDS = TEAM_SETUP_TIME_SECONDS + CORE_PLACEMENT_TIME_SECONDS;

    public static final int GUARANTEED_KEEP_TIME_SECONDS = 10 * 60;
    public static final int KEEP_RADIUS = 70;
    public static final Effect KEEP_EFFECT = Fx.hitSquaresColor;
    public static final float DEAD_ZONE_DAMAGE_CONSTANT_TICK = 55f / 60f; // Absolute damage every tick
    public static final float DEAD_ZONE_DAMAGE_PERCENT_TICK = 1.8f / 100f / 60f; // Percent of max health every tick
    public static final List<UnitType> DEAD_ZONE_IMMUNE_TYPES = List.of(new UnitType[] {
            UnitTypes.alpha,
            UnitTypes.beta,
            UnitTypes.gamma,
    });

    // Item prices lower than this should be discarded.
    public static final int MINIMUM_CORE_PRICE_ITEMS = 500;
    // The factor of the harmonic core price for which it is guaranteed that a full core can pay it
    // At harmonic factors above this it may be necessary to build closer cores or expand storage before building the desired core.
    // For every core, the price will be simulated for if the harmonic factor was at this value, and any prices exceeding that which could be stored if all current cores were shards will be subtracted down to that value, and that subtraction applies to the final amount.
    public static final float GUARANTEED_HARMONIC_FACTOR = 15f;
    // If you have less than this many cores, prices will scale by what fraction of this number you have
    public static final int RAMP_UP_CORE_COUNT = 3;

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
            Items.silicon, 1000,
            Items.copper, 500,
            Items.lead, 100
    ));

    // Black magic
    public static final float HARMONIC_CORE_COUNT_POWER_FACTOR = 0.6f;
    public static final ItemSeq HARMONIC_DISTANCE_CORE_PRICE = new ItemSeq(ItemStack.list(
            Items.phaseFabric, 50,
            Items.silicon, 250,
            Items.surgeAlloy, 150
    ));

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

    public static final List<Block> TURRET_BLOCKS = List.of(
            Blocks.duo,             Blocks.scatter,
            Blocks.scorch,          Blocks.hail,
            Blocks.wave,            Blocks.lancer,
            Blocks.arc,             Blocks.parallax,
            Blocks.swarmer,         Blocks.salvo,
            Blocks.segment,         Blocks.tsunami,
            Blocks.fuse,            Blocks.ripple,
            Blocks.cyclone,         Blocks.foreshadow,
            Blocks.spectre,         Blocks.meltdown,
            Blocks.breach,          Blocks.diffuse,
            Blocks.sublimate,       Blocks.titan,
            Blocks.disperse,        Blocks.afflict,
            Blocks.lustre,          Blocks.scathe,
            Blocks.smite,           Blocks.malign
    );

    public static final List<UnitType> CORE_UNITS = List.of(
            UnitTypes.alpha,
            UnitTypes.beta,
            UnitTypes.gamma,
            UnitTypes.evoke,
            UnitTypes.incite,
            UnitTypes.emanate
    );

    public static final List<UnitType> NONCOMBAT_UNITS = List.of(
            UnitTypes.flare,
            UnitTypes.mono,
            UnitTypes.poly,
            UnitTypes.mega
    );

    public static final List<UnitType> COMBAT_UNITS = List.of(
            UnitTypes.mace,
            UnitTypes.dagger,
            UnitTypes.crawler,
            UnitTypes.fortress,
            UnitTypes.scepter,
            UnitTypes.reign,
            UnitTypes.vela,
            UnitTypes.nova,
            UnitTypes.pulsar,
            UnitTypes.quasar,
            UnitTypes.corvus,
            UnitTypes.atrax,
            UnitTypes.merui,
            UnitTypes.cleroi,
            UnitTypes.anthicus,
            UnitTypes.tecta,
            UnitTypes.collaris,
            UnitTypes.spiroct,
            UnitTypes.arkyid,
            UnitTypes.toxopid,
            UnitTypes.elude,
            UnitTypes.eclipse,
            UnitTypes.horizon,
            UnitTypes.zenith,
            UnitTypes.antumbra,
            UnitTypes.avert,
            UnitTypes.obviate,
            UnitTypes.quell,
            UnitTypes.disrupt,
            UnitTypes.quad,
            UnitTypes.oct,
            UnitTypes.risso,
            UnitTypes.minke,
            UnitTypes.bryde,
            UnitTypes.sei,
            UnitTypes.omura,
            UnitTypes.retusa,
            UnitTypes.oxynoe,
            UnitTypes.cyerce,
            UnitTypes.aegires,
            UnitTypes.navanax,
            UnitTypes.stell,
            UnitTypes.locus,
            UnitTypes.precept,
            UnitTypes.vanquish,
            UnitTypes.conquer,
            UnitTypes.latum,
            UnitTypes.renale
    );

    public static final ObjectSet<Block> ALWAYS_BANNED_BLOCKS = new ObjectSet<>();
    public static final ObjectSet<Block> BANNED_BLOCKS_CITADEL = new ObjectSet<>();
    public static final ObjectSet<Block> BANNED_BLOCKS_CITADEL_KEEP_ON = new ObjectSet<>();
    public static final ObjectSet<Block> BANNED_BLOCKS_RAIDERS = new ObjectSet<>();
    public static final ObjectSet<Block> BANNED_BLOCKS_RAIDERS_KEEP_ON = new ObjectSet<>();

    public static final ObjectSet<UnitType> ALWAYS_BANNED_UNITS = new ObjectSet<>();
    public static final ObjectSet<UnitType> BANNED_UNITS_CITADEL = new ObjectSet<>();
    public static final ObjectSet<UnitType> BANNED_UNITS_CITADEL_KEEP_ON = ObjectSet.with(Seq.with(COMBAT_UNITS));
    public static final ObjectSet<UnitType> BANNED_UNITS_RAIDERS = new ObjectSet<>();
    public static final ObjectSet<UnitType> BANNED_UNITS_RAIDERS_KEEP_ON = new ObjectSet<>();



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
