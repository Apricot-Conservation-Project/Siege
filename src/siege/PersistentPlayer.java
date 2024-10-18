package siege;

import mindustry.gen.Player;

import java.util.ArrayList;
import java.util.List;

public class PersistentPlayer {
    public boolean online;
    public long lastSeen;
    public Player currentPlayer;
    private static List<PersistentPlayer> players = new ArrayList<>();

    /**
     * Returns a player's PersistentPlayer if one has already been made, otherwise creates a new instance for the given player.
     * @param targetPlayer The player to return a respective PersistentPlayer
     * @return The target player's respective PersistentPlayer
     */
    public static PersistentPlayer fromPlayer(Player targetPlayer) {
        for (PersistentPlayer persistentPlayer : players) {
            if (persistentPlayer.currentPlayer.uuid().equals(targetPlayer.uuid())) {
                return persistentPlayer;
            }
            if (persistentPlayer.currentPlayer.ip().equals(targetPlayer.ip())) {
                return persistentPlayer;
            }
        }

        return new PersistentPlayer(targetPlayer);
    }

    // Private constructor, because fromPlayer should be used instead.
    private PersistentPlayer(Player p) {
        currentPlayer = p;
        online = true;
        players.add(this);
    }


    /**
     * Updates the status of all players. Should be called frequently.
     */
    /*
    public static void updatePlayers() {
        for (PersistentPlayer player : players) {
            player.update();
        }
    }

    // Updates the player's status.
    private void update() {
        System.out.println(currentPlayer.);
        //if (currentPlayer.lastUpdated)
        // check if an online player goes offline
        // check if an offline player comes online
    }*/
}
