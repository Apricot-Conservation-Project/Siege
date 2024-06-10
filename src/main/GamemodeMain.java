package main;

import arc.*;
import arc.graphics.Color;
import mindustry.world.*;
import arc.math.geom.*;
import arc.math.Mathf;
import arc.struct.*;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.Planets;
import mindustry.content.UnitTypes;
import mindustry.core.GameState;
import mindustry.core.GameState.State;
import mindustry.game.EventType;
import mindustry.game.Rules;
import mindustry.game.Team;
import mindustry.game.Teams;
import mindustry.gen.*;
import mindustry.mod.Plugin;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.Planet;
import mindustry.type.UnitType;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.blocks.defense.turrets.PowerTurret;
import mindustry.world.blocks.defense.turrets.Turret.TurretBuild;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;
import base.DBInterface;
import base.CustomPlayer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.*;

import base.Base;

import static mindustry.Vars.*;

public class GamemodeMain extends Plugin {

}
