package siege;

import arc.struct.Seq;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.UnitTypes;
import mindustry.type.ItemSeq;
import mindustry.type.ItemStack;
import mindustry.type.UnitType;
import mindustry.world.Block;

import java.util.List;

public class Constants {
    public static final int RAIDER_MAX_PLAYERS = 3;

    public static final Block DEAD_ZONE_FILLER_FLOOR = Blocks.denseRedStone;

    public static final Seq<Block> CORE_TYPES = new Seq<>(new Block[]{Blocks.coreShard, Blocks.coreFoundation, Blocks.coreNucleus});

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

    public static final int TEAM_SETUP_TIME_SECONDS = 120;
    public static final int CORE_PLACEMENT_TIME_SECONDS = 60;
    public static final int SETUP_TIME_SECONDS = TEAM_SETUP_TIME_SECONDS + CORE_PLACEMENT_TIME_SECONDS;

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
}
