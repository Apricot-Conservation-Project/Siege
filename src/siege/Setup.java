package siege;

import arc.math.Mathf;
import arc.math.geom.Point2;
import mindustry.content.Blocks;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock;

import static mindustry.Vars.*;

/**
 * Manages game setup. Should not be used once Gamedata.gameStarted is true.
 */
public final class Setup {
    // Measured in elapsed seconds as returned by Gamedata
    private static int nextTimeReminder = 0;
    private static boolean changedToCorePlacement = false;

    public static void reset() {
        nextTimeReminder = 0;
        changedToCorePlacement = false;
        Gamedata.gameStarted = false;
        startSetup();
    }

    /**
     * Handles all constantly updating processes during pre-game setup
     */
    public static void update() {
        updateRespawn();

        if (Gamedata.gameStartTime()) {
            // Sets a flag which prevents update from being called again
            Setup.beginGame();
        }

        if (!changedToCorePlacement && !Gamedata.teamSetupPhase()) {
            changePhaseToCorePlacement();
            return;
        }

        if (Gamedata.elapsedTimeSeconds() >= nextTimeReminder) {
            if (!changedToCorePlacement) {
                if ( (-Gamedata.elapsedTimeSeconds() - Constants.CORE_PLACEMENT_TIME_SECONDS) > 0 ) {
                    SiegePlugin.announce("[accent]" + (-Gamedata.elapsedTimeSeconds() - Constants.CORE_PLACEMENT_TIME_SECONDS) + " Seconds remaining in team setup phase.");
                }
            } else {
                if ( (-Gamedata.elapsedTimeSeconds()) > 0 ) {
                    SiegePlugin.announce("[accent]" + (-Gamedata.elapsedTimeSeconds()) + " Seconds remaining in core placement phase.");
                }
            }
            nextTimeReminder += 20;
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

        if (Gamedata.raiderTeams.isEmpty()) {
            SiegePlugin.announce("[sky]Team setup has ended without any teams created.");
            Gamedata.gameStarted = true;
            SiegePlugin.endGame(-1);
            return;
        }

        SiegePlugin.announce("[sky]Team setup has ended. Team configuration commands have been disabled. Teams may now place their Foundation cores. Cores are placed at the geometric median of all team members' positions.");
        SiegePlugin.announce("[accent]You have " + Constants.CORE_PLACEMENT_TIME_SECONDS + " seconds to move to your desired core location.");
        // Set the next time reminder to be the next multiple of 20 seconds away from core placement phase end
        nextTimeReminder = -20 * (int)( ((double)Constants.CORE_PLACEMENT_TIME_SECONDS - Mathf.FLOAT_ROUNDING_ERROR) / 20.0 );

        for (RaiderTeam team : Gamedata.raiderTeams) {
            team.mindustryTeam = Team.all[team.id + 7];
            team.stringID = "[#" + team.mindustryTeam.color.toString().substring(0, 6) + "]" + team.id + "[]";
        }

        Gamedata.initCache();
    }

    /**
     * Performs Setup's last tasks, ending core placement and starting the game. Setup should not be interacted with past this point.
     */
    public static void beginGame() {
        Gamedata.gameStarted = true;

        // Add raider team cores
        // Teams can be removed here if they have no players.
        RaiderTeam[] teams = Gamedata.raiderTeams.toArray(new RaiderTeam[0]);
        for (RaiderTeam team : teams) {
            // Get position of all online players
            boolean onlinePlayer = false;
            for (PersistentPlayer player : team.players) {
                if (player.online) {
                    onlinePlayer = true;
                    break;
                }
            }
            // Kick team if it has no online players at game start
            if (!onlinePlayer) {
                SiegePlugin.announce("[orange]Team " + team.stringID + " was removed for having no online players at game start.");
                Gamedata.raiderTeams.remove(team);
                continue;
            }

            Point2 corePoint = team.corePlacementPosition();
            Tile tile = world.tile(corePoint.x, corePoint.y); // Subtract 1 because of how foundations 'center' tile are measured
            tile.setNet(Blocks.coreFoundation, team.mindustryTeam, 0);
            state.teams.registerCore((CoreBlock.CoreBuild) tile.build);
        }

        // Give team loadouts
        Team.green.items().add(Constants.CITADEL_LOADOUT);
        for (RaiderTeam team : Gamedata.raiderTeams) {
            team.mindustryTeam.items().add(Constants.RAIDER_LOADOUT);
        }

        // Initialize dead zone
        long beginTime = System.currentTimeMillis();
        CoreBlock.CoreBuild[] cores = Gamedata.getAllCores();
        for (CoreBlock.CoreBuild core : cores) {
            Gamedata.reloadCore(core);
        }
        Gamedata.reloadFloor();
        long endTime = System.currentTimeMillis();
        int elapsed = (int) (endTime - beginTime);
        System.out.println(elapsed + " ms to generate and write floor (" + Mathf.round(elapsed / (1000f / 60f), 0.01f) + " ticks at 60TPS)");

        SiegePlugin.announce("[sky]Cores have been placed. Raiders and the Citadel can now build and attack. Good luck, and have fun!");
    }

    public static void dataDump() {
        System.out.println("nextTimeReminder: " + nextTimeReminder);
        System.out.println("changedToCorePlacement: " + changedToCorePlacement);
    }
}
