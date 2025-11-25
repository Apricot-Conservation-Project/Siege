package siege;

import arc.struct.ObjectSet;
import arc.struct.Seq;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.UnitTypes;
import mindustry.game.Rules;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.blocks.defense.turrets.PowerTurret;
import mindustry.Vars;

public final class RuleSetter {
    public static final float EXTRA_NO_BUILD_RADIUS = 10f;
    public static final float BUILD_SPEED_MULTIPLIER = 3f;

    private static Rules rules = new Rules();

    private static final int forcePushPeriod = 30 * 60;
    private static int forcePushIndex = 0;

    /**
     * Updates rules over time. Should be called every tick.
     */
    public static void update() {
        forcePushIndex ++;
        if (forcePushIndex >= forcePushPeriod) {
            forcePushIndex = 0;
            pushRules();
        }
    }

    public static ObjectSet<Block> getBannedBlocks(Team team) {
        ObjectSet<Block> bannedBlocks = new ObjectSet<Block>(Constants.ALWAYS_BANNED_BLOCKS);
        if (team == Team.green) {
            bannedBlocks.addAll(Constants.BANNED_BLOCKS_CITADEL);
            if (Keep.keepExists()) {
                bannedBlocks.addAll(Constants.BANNED_BLOCKS_CITADEL_KEEP_ON);
            }
        } else {
            bannedBlocks.addAll(Constants.BANNED_BLOCKS_RAIDERS);
            if (Keep.keepExists()) {
                bannedBlocks.addAll(Constants.BANNED_BLOCKS_RAIDERS_KEEP_ON);
            }
        }
        return bannedBlocks;
    }

    public static ObjectSet<UnitType> getBannedUnits(Team team) {
        ObjectSet<UnitType> bannedUnits = new ObjectSet<UnitType>(Constants.ALWAYS_BANNED_UNITS);
        if (team == Team.green) {
            bannedUnits.addAll(Constants.BANNED_UNITS_CITADEL);
            if (Keep.keepExists()) {
                bannedUnits.addAll(Constants.BANNED_UNITS_CITADEL_KEEP_ON);
            }
        } else {
            bannedUnits.addAll(Constants.BANNED_UNITS_RAIDERS);
            if (Keep.keepExists()) {
                bannedUnits.addAll(Constants.BANNED_UNITS_RAIDERS_KEEP_ON);
            }
        }
        return bannedUnits;
    }

    /**
     * Updates the rules a player abides by
     * @param player The player to send updated rules to
     */
    public static void updatePlayerRules(Player player) {
        Rules tempRules = RuleSetter.rules.copy();
        tempRules.bannedBlocks = getBannedBlocks(player.team());
        tempRules.bannedUnits = getBannedUnits(player.team());
        for (int i = 0; i < 5; i++) { // Just making sure the packet gets there
            Call.setRules(player.con, tempRules);
        }
    }

    /**
     * Gets the current internal rules
     * @return The current internal rules
     */
    public static Rules getRules() {
        return rules;
    }

    /**
     * Initializes the base Siege rules
     */
    public static void initRules() {
        System.out.println("Rules pushed");
        rules.modeName = "Siege";

        rules.enemyCoreBuildRadius = 8 * (Constants.NUCLEUS_DEAD_ZONE_RADIUS + EXTRA_NO_BUILD_RADIUS);
        rules.canGameOver = false;
        //rules.playerDamageMultiplier = 0;
        rules.buildSpeedMultiplier = BUILD_SPEED_MULTIPLIER;
        rules.coreIncinerates = true;
        rules.fire = false;
        rules.hideBannedBlocks = true;
        rules.waves = false;

        for (Team team : Team.all) {
            rules.teams.get(team).rtsAi = true;
        }

        UnitTypes.alpha.weapons = new Seq<>();
        UnitTypes.beta.weapons = new Seq<>();
        UnitTypes.gamma.weapons = new Seq<>();
        UnitTypes.poly.weapons = new Seq<>(); // TODO let these use their healing powers
        UnitTypes.mega.weapons = new Seq<>();
        UnitTypes.flare.weapons = new Seq<>();

        ((ItemTurret) Blocks.foreshadow).ammoTypes.get(Items.surgeAlloy).buildingDamageMultiplier = 0;
        ((PowerTurret) Blocks.malign).shootType.buildingDamageMultiplier = 0;

        Vars.state.rules = rules.copy();
        pushRules();
    }

    /**
     * Forcefully pushes the current ruleset to all players.
     */
    public static void pushRules() {
        Vars.state.rules = rules.copy();
        Call.setRules(rules);
        for (Player player : Groups.player) {
            updatePlayerRules(player);
        }
    }
}
