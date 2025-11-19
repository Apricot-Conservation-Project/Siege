package siege;

import arc.math.Mathf;
import arc.math.geom.Point2;
import mindustry.content.Blocks;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.storage.CoreBlock;

import java.util.ArrayList;
import java.util.List;

import static mindustry.Vars.world;

public final class Gamedata {
    public static List<RaiderTeam> deadRaiderTeams = new ArrayList<>();
    public static List<RaiderTeam> raiderTeams = new ArrayList<>();
    public static long startTime = 0;
    public static boolean gameStarted = false; // Owned by Setup
    public static boolean gameOver = false;
    //public int mapIndex;
    public static DeadZone deadZone;

    public static void reset() {
        startTime = System.currentTimeMillis() + 1000 * Constants.SETUP_TIME_SECONDS;
        raiderTeams = new ArrayList<>();
        deadRaiderTeams = new ArrayList<>();
        gameOver = false;
    }

    /**
     * @return Current phase of the game
     */
    public static GameState getGameState() {
        if (!gameStarted) {
            if (Setup.changedToCorePlacement) {
                return GameState.CorePlacement;
            } else {
                return GameState.TeamSetup;
            }
        } else if (!gameOver) {
            if (Keep.keepExists()) {
                return GameState.MidgameYesKeep;
            } else {
                return GameState.MidgameNoKeep;
            }
        } else {
            return GameState.GameOver;
        }
    }

    /**
     * @return Whether the game should have started yet. Prefer its variable form for most cases, as this is based only on time and could desync.
     */
    public static boolean gameStartTime() {
        return elapsedTimeSeconds() >= 0;
    }

    /**
     * @return Whether team configuration is still allowed
     */
    public static boolean teamSetupPhase() {
        return elapsedTimeSeconds() < -Constants.CORE_PLACEMENT_TIME_SECONDS;
    }

    /**
     * @return The number of seconds since the game's start time (end of setup). Will be negative if setup is still ongoing.
     */
    public static long elapsedTimeSeconds() {
        return (System.currentTimeMillis() - startTime) / 1000;
    }

    /**
     * @return All the player cores in the game
     */
    public static CoreBlock.CoreBuild[] getAllCores() {
        ArrayList<CoreBlock.CoreBuild> cores = Team.green.cores().list();

        for (RaiderTeam team : raiderTeams) {
            cores.addAll(List.of(team.getCores()));
        }

        return cores.toArray(new CoreBlock.CoreBuild[0]);
    }

    /**
     * @param coreType The type of core to search for
     * @return All player cores of the given type
     */
    public static CoreBlock.CoreBuild[] getAllCores(Block coreType) {
        CoreBlock.CoreBuild[] allCores = getAllCores();
        ArrayList<CoreBlock.CoreBuild> cores = new ArrayList<>();

        for (CoreBlock.CoreBuild core : allCores) {
            if (core.block == coreType) {
                cores.add(core);
            }
        }

        return cores.toArray(new CoreBlock.CoreBuild[0]);
    }

    public static void dataDump() {
        System.out.println("\nGamedata and Setup dump\n");
        System.out.println("deadRaiderTeams: " + deadRaiderTeams.toString());
        System.out.println("raiderTeams: " + raiderTeams.toString());
        System.out.println("startTime: " + startTime);
        System.out.println("gameStarted: " + gameStarted);
        System.out.println("gameOver: " + gameOver);
        System.out.println("keep exists: " + Keep.keepExists());
        System.out.println("elapsedTimeSeconds(): " + elapsedTimeSeconds());
        System.out.println("--- Setup ---");
        Setup.dataDump();
        System.out.println("\n");
    }
}
