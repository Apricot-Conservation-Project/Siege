package siege;

import arc.struct.Seq;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Unit;

public class UnitOwner {
    public final Unit unit;
    private float externalHealth;
    private float trueMaxHealth;
    private boolean wasInKeep;

    private static final Seq<UnitOwner> unitOwners = new Seq<>();
    private static long previousDeadZoneCheck = 0L;

    private UnitOwner(Unit unit) {
        this.unit = unit;
        this.externalHealth = unit.health;
        this.trueMaxHealth = unit.maxHealth;
        this.wasInKeep = false;
        unitOwners.add(this);
    }

    public static void update() {
        for (Unit baseUnit : Groups.unit) fromUnit(baseUnit);

        for (UnitOwner unit : unitOwners) unit.updateUnit();

        if (!Gamedata.gameStarted) return;
        if (Gamedata.gameOver) return;

        if (previousDeadZoneCheck == 0L) {
            previousDeadZoneCheck = System.currentTimeMillis() - (1000 / 60);
        }
        float elapsedTimeSeconds = (System.currentTimeMillis() - previousDeadZoneCheck) / 1000f;
        float elapsedTimeTicks = elapsedTimeSeconds * 60f;
        float constantDamage = Constants.DEAD_ZONE_DAMAGE_CONSTANT_TICK * elapsedTimeTicks;
        float percentDamage = Constants.DEAD_ZONE_DAMAGE_PERCENT_TICK * elapsedTimeTicks;
        previousDeadZoneCheck = System.currentTimeMillis();

        for (UnitOwner unit : unitOwners) {
            if (DeadZone.getDeadZone(unit.unit.tileOn())  &&  !Constants.DEAD_ZONE_IMMUNE_TYPES.contains(unit.unit.type)) {
                unit.unit.health -= constantDamage + unit.unit.maxHealth * percentDamage;
                if (unit.unit.health <= 0.0f && !unit.unit.dead) {
                    unit.unit.kill();
                } else {
                    unit.unit.maxHealth = unit.unit.health; // Healing strictly does not work within the deadzone.
                }
            } else {
                unit.unit.maxHealth = unit.trueMaxHealth; // Reset maxhealth to standard
            }
        }
    }

    public static UnitOwner fromUnit(Unit unit) {
        for (UnitOwner unitOwner : unitOwners) {
            if (unitOwner.unit.equals(unit)) {
                return unitOwner;
            }
        }

        return new UnitOwner(unit);
    }

    public static void deregister(UnitOwner unitOwner) {
        unitOwners.remove(unitOwner);
    }

    public static void deregister(Unit unit) {
        deregister(fromUnit(unit));
    }

    private void updateUnit() {
        if (unit.dead()) {
            deregister(this);
            return;
        }

        boolean inKeep = Keep.keepExists() && unit.team == Team.green && Keep.inKeep(unit.tileOn());
        if (inKeep && !wasInKeep) {
            externalHealth = unit.health;
            unit.health = Float.MAX_VALUE;
        }
        if (!inKeep && wasInKeep) {
            unit.health = externalHealth;
        }
        wasInKeep = inKeep;
    }
}
