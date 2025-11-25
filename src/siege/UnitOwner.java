package siege;

import arc.struct.Seq;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Unit;

public class UnitOwner {
    public final Unit unit;
    private float externalHealth;
    private boolean wasInKeep;

    private static Seq<UnitOwner> unitOwners = new Seq<>();

    private UnitOwner(Unit unit) {
        this.unit = unit;
        this.externalHealth = unit.health;
        this.wasInKeep = false;
        unitOwners.add(this);
    }

    public static void update() {
        for (Unit unit : Groups.unit) {
            fromUnit(unit).updateUnit();
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
