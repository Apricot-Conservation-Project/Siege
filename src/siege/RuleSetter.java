package siege;

import arc.struct.Seq;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.UnitTypes;
import mindustry.core.GameState;
import mindustry.game.Rules;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.blocks.defense.turrets.PowerTurret;

import static mindustry.Vars.state;

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
        rules.modeName = "Siege";

        rules.enemyCoreBuildRadius = 8 * (Constants.NUCLEUS_DEAD_ZONE_RADIUS + EXTRA_NO_BUILD_RADIUS);
        rules.canGameOver = false;
        // rules.playerDamageMultiplier = 0;
        rules.buildSpeedMultiplier = BUILD_SPEED_MULTIPLIER;
        rules.coreIncinerates = true;
        rules.fire = false;
        rules.hideBannedBlocks = true;

        for (Team team : Team.all) {
            rules.teams.get(team).rtsAi = true;
        }

        UnitTypes.alpha.weapons = new Seq<>();
        UnitTypes.beta.weapons = new Seq<>();
        UnitTypes.gamma.weapons = new Seq<>();
        UnitTypes.poly.weapons = new Seq<>();
        UnitTypes.mega.weapons = new Seq<>();
        UnitTypes.flare.weapons = new Seq<>();

        ((ItemTurret) Blocks.foreshadow).ammoTypes.get(Items.surgeAlloy).buildingDamageMultiplier = 0;
        ((PowerTurret) Blocks.malign).shootType.buildingDamageMultiplier = 0;

        state.rules = rules.copy();
        Call.setRules(rules);
        pushRules();
    }

    /**
     * Forcefully pushes the current ruleset to all players.
     */
    public static void pushRules() {
        state.rules = rules.copy();
        for (Player player : Groups.player) {
            for (int i = 0; i < 5; i ++) {
                Call.setRules(player.con(), rules);
            }
        }
    }
}
