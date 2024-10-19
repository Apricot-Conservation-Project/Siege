package siege;

import java.util.ArrayList;
import java.util.List;

public final class Gamedata {
    public static List<RaiderTeam> raiderTeams;
    public static long startTime;
    //public int mapIndex;

    public static void reset() {
        startTime = System.currentTimeMillis() + 1000 * Constants.SETUP_TIME_SECONDS;
        raiderTeams = new ArrayList<>();
    }

    /**
     * @return Whether the game has started yet (setup has concluded)
     */
    public static boolean gameStarted() {
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
