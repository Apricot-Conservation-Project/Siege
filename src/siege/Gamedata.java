package siege;

import java.util.ArrayList;
import java.util.List;

public final class Gamedata {
    public static List<RaiderTeam> deadRaiderTeams;
    public static List<RaiderTeam> raiderTeams;
    public static long startTime;
    public static boolean gameStarted; // Owned by Setup
    public static boolean gameOver;
    //public int mapIndex;

    public static void reset() {
        startTime = System.currentTimeMillis() + 1000 * Constants.SETUP_TIME_SECONDS;
        raiderTeams = new ArrayList<>();
        deadRaiderTeams = new ArrayList<>();
        gameOver = false;
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
}
