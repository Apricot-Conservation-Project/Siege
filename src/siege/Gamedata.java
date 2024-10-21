package siege;

import java.util.ArrayList;
import java.util.List;

public final class Gamedata {
    public static List<RaiderTeam> deadRaiderTeams = new ArrayList<>();
    public static List<RaiderTeam> raiderTeams = new ArrayList<>();
    public static long startTime = 0;
    public static boolean gameStarted = false; // Owned by Setup
    public static boolean gameOver = false;
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

    public static void dataDump() {
        System.out.println("\nGamedata and Setup dump\n");
        System.out.println("deadRaiderTeams: " + deadRaiderTeams.toString());
        System.out.println("raiderTeams: " + raiderTeams.toString());
        System.out.println("startTime: " + startTime);
        System.out.println("gameStarted: " + gameStarted);
        System.out.println("gameOver: " + gameOver);
        System.out.println("elapsedTimeSeconds(): " + elapsedTimeSeconds());
        System.out.println("--- Setup ---");
        Setup.dataDump();
        System.out.println("\n");
    }
}
