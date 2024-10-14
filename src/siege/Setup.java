package siege;

import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.world.blocks.storage.CoreBlock;

public final class Setup {
    /**
     * Handles all constantly updating processes during pre-game setup
     */
    public static void update() {
        updateRespawn();
    }

    // Makes sure all players are spawned in the game
    private static void updateRespawn() {
        for (Player player : Groups.player) {
            if (player.dead()) {
                CoreBlock.playerSpawn(Team.green.cores().random().tile, player);
            }
        }
    }
}
