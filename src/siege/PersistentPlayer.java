package siege;

import mindustry.gen.Player;

import java.util.ArrayList;
import java.util.List;

public class PersistentPlayer {
    public boolean online;
    public Player player;
    private static List<PersistentPlayer> players = new ArrayList<>();

    /**
     * Returns a player's PersistentPlayer if one has already been made, otherwise creates a new instance for the given player.
     * @param targetPlayer The player to return a respective PersistentPlayer
     * @return The target player's respective PersistentPlayer
     */
    public static PersistentPlayer fromPlayer(Player targetPlayer) {
        for (PersistentPlayer persistentPlayer : players) {
            // TODO Compare UUID or other identifying feature to grab the same persistentPlayer after a player disconnects and reconnects
            if (persistentPlayer.player.equals(targetPlayer)) {
                return persistentPlayer;
            }
        }

        return new PersistentPlayer(targetPlayer);
    }

    // Private constructor, because fromPlayer should be used instead.
    private PersistentPlayer(Player p) {
        player = p;
        online = true;
        players.add(this);
    }

    /**
     * Updates the status of all players. Should be called frequently.
     */
    public static void updatePlayers() {
        for (PersistentPlayer player : players) {
            player.update();
        }
    }

    // Updates the player's status.
    private void update() {
        // check if an online player goes offline
        // check if an offline player comes online
    }
}
