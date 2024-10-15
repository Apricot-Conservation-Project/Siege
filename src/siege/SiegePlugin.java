package siege;

import arc.*;
import arc.util.*;
import mindustry.game.EventType;
import mindustry.gen.*;
import mindustry.mod.*;

public class SiegePlugin extends Plugin {
    public static Gamedata gamedata;

    @Override
    public void init() {
        System.out.println("SiegePlugin loaded");

        gamedata = new Gamedata();

        Events.run(EventType.Trigger.update, () -> {
            alwaysUpdate();

            if (!gamedata.gameStarted()) {
                Setup.update();
            } else {
                gameUpdate();
            }
        });
    }

    /**
     * Ends the current game and loads the next.
     * @param winner The game's winner. 0 if the Citadel wins, otherwise is the ID of the winning team. -1 if the game is ended without a winner.
     */
    public void endGame(int winner) {
        /*switch (winner) {
            // ???
        }*/
    }

    // Manages constant processes that happen always
    private void alwaysUpdate() {
        PersistentPlayer.updatePlayers();
    }

    // Manages constant processes after setup
    private void gameUpdate() {
        checkTeams();
    }

    private void checkTeams() {
        gamedata.raiderTeams.removeIf(team -> team.players.isEmpty());

        if (gamedata.raiderTeams.isEmpty()) {
            endGame(0);
        }
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        RaiderTeam.Commands.registerCommands(handler);
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        /*base.register_server(handler);
        handler.removeCommand("host");
        handler.removeCommand("gameover");
        handler.removeCommand("runwave");
        handler.removeCommand("shuffle");
        handler.removeCommand("nextmap");
        handler.register("host", "[map(index)]", "Host the Siege game mode", args -> {
            if (!Vars.state.is(GameState.State.menu)) {
                Log.err("Stop the server first.");
                return;
            }
            mapReset(args);
        });

        handler.register("gameover", "[map(index)]", "End the Siege game", args -> {
            Call.sendMessage("[scarlet]Server[accent] has ended the plague game. Ending in 10 seconds...");
            Log.info("Game ended.");
            // TODO end the game
        });*/
    }



    public static void announce(String message) {
        for (Player player : Groups.player) {
            player.sendMessage(message);
        }
    }
}
