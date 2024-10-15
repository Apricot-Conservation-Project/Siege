package siege;

import java.util.ArrayList;
import java.util.List;

public class Gamedata {
    public List<RaiderTeam> raiderTeams;
    public long startTime;
    //public int mapIndex;

    public Gamedata() {
        startTime = System.currentTimeMillis() + 1000 * Constants.SETUP_TIME_SECONDS;
        raiderTeams = new ArrayList<>();
    }

    /**
     * @return Whether the game has started yet (setup has concluded)
     */
    public boolean gameStarted() {
        return elapsedTimeSeconds() > 0;
    }

    /**
     * @return The number of seconds since the game's start time (end of setup). Will be negative if setup is still ongoing.
     */
    public long elapsedTimeSeconds() {
        return (System.currentTimeMillis() - startTime) / 1000;
    }
}
