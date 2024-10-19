package siege;

import mindustry.gen.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
     * Attempts to return a player, given a string representing either ID or name.
     * Returns null if neither are found or if no ID is found and multiple players share the name.
     * Does not consider case.
     * @param s The string containing either ID or name.
     * @param executor The player who input the string s, if one exists. If not null, error messages may be sent here.
     * @return The requested player, or null.
     */
    public static PersistentPlayer fromString(String s, Player executor) {
        // Try to find player by ID, continue if s is invalid ID or if none are found.
        try {
            for (PersistentPlayer persistentPlayer : players) {
                if (persistentPlayer.currentPlayer.id == Integer.parseInt(s)) {
                    return persistentPlayer;
                }
            }
        } catch (NumberFormatException ignored) {}

        // Try to find player by name, not considering case. Fail if multiple names match, or if no names match.
        PersistentPlayer output = null;
        for (PersistentPlayer persistentPlayer : players) {
            if (Objects.equals(persistentPlayer.currentPlayer.plainName().toLowerCase(), s.toLowerCase())) {
                if (output != null) {
                    if (executor != null) {
                        executor.sendMessage("[red] Multiple players matching given name \"" + s.toLowerCase() + "\"[accent]. Consider using the player's ID instead.");
                    }
                    return null;
                }
                output = persistentPlayer;
            }
        }
        if (output != null) {
            return output;
        }

        if (executor != null) {
            executor.sendMessage("[red] Player [accent]\"" + s.toLowerCase() + "\"[red] not found.");
        }
        return null;
    }

    /**
     * Attempts to return a player, given a string representing either ID or name.
     * Returns null if neither are found or if no ID is found and multiple players share the name.
     * Does not consider case.
     * @param s The string containing either ID or name.
     * @return The requested player, or null.
     */
    public static PersistentPlayer fromString(String s) {
        return fromString(s, null);
    }
}
