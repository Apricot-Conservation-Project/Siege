package siege;

import arc.math.Mathf;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.world.blocks.storage.CoreBlock;

public final class Setup {
    // Measured in elapsed seconds as returned by Gamedata
    private static int nextTimeReminder = 0;
    private static boolean changedToCorePlacement = false;
    public static boolean gameBegun = false;

    public static void reset() {
        nextTimeReminder = 0;
        changedToCorePlacement = false;
        gameBegun = false;
        startSetup();
    }

    /**
     * Handles all constantly updating processes during pre-game setup
     */
    public static void update() {
        updateRespawn();

        if (!changedToCorePlacement && !Gamedata.teamSetupPhase()) {
            changePhaseToCorePlacement();
        }

        if (-Gamedata.elapsedTimeSeconds() > Constants.CORE_PLACEMENT_TIME_SECONDS) {
            if (Gamedata.elapsedTimeSeconds() >= nextTimeReminder) {
                SiegePlugin.announce("[accent]" + (-Gamedata.elapsedTimeSeconds() - Constants.CORE_PLACEMENT_TIME_SECONDS) + " Seconds remaining in team setup phase.");
                nextTimeReminder += 20;
            }
        } else {
            if (Gamedata.elapsedTimeSeconds() >= nextTimeReminder) {
                SiegePlugin.announce("[accent]" + (-Gamedata.elapsedTimeSeconds()) + " Seconds remaining in core placement phase.");
                nextTimeReminder += 20;
            }
        }
    }

    // Makes sure all players are spawned in the game, and on the correct (blue) team
    private static void updateRespawn() {
        for (Player player : Groups.player) {
            if (player.dead()) {
                CoreBlock.playerSpawn(Team.green.cores().random().tile, player);
            }
            if (Gamedata.teamSetupPhase() && player.team() != Team.blue) {
                player.team(Team.blue);
            }
        }
    }

    private static void startSetup() {
        // Set the next time reminder to be the next multiple of 20 seconds away from team setup phase end
        nextTimeReminder = -20 * (int)( ((double)Constants.TEAM_SETUP_TIME_SECONDS - Mathf.FLOAT_ROUNDING_ERROR) / 20.0 ) - Constants.CORE_PLACEMENT_TIME_SECONDS;
    }

    private static void changePhaseToCorePlacement() {
        changedToCorePlacement = true;
        SiegePlugin.announce("[sky]Team setup has ended. Team configuration commands have been disabled. Teams may now place their Foundation cores. Cores are placed at the geometric median of all team members' positions.");
        SiegePlugin.announce("[accent]" + Constants.CORE_PLACEMENT_TIME_SECONDS + " Seconds remaining in core placement phase.");
        // Set the next time reminder to be the next multiple of 20 seconds away from core placement phase end
        nextTimeReminder = -20 * (int)( ((double)Constants.CORE_PLACEMENT_TIME_SECONDS - Mathf.FLOAT_ROUNDING_ERROR) / 20.0 );

        for (RaiderTeam team : Gamedata.raiderTeams) {
            team.mindustryTeam = Team.all[team.id + 7];
            team.stringID = "[#" + team.mindustryTeam.color.toString().substring(0, 6) + "]" + team.id + "[]";
        }
    }

    public static void beginGame() {
        gameBegun = true;

        Team.green.items().add(Constants.CITADEL_LOADOUT);
        for (RaiderTeam team : Gamedata.raiderTeams) {
            team.mindustryTeam.items().add(Constants.RAIDER_LOADOUT);
        }

        for (RaiderTeam team : Gamedata.raiderTeams) {
            // TODO find the geometric median of team's players and put a core there
        }

        SiegePlugin.announce("[sky]Cores have been placed. Raiders and the Citadel can now build and attack. Good luck, and have fun!");
    }
}
