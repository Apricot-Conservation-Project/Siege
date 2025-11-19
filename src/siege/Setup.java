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
    public static boolean changedToCorePlacement = false;

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
            team.mindustryTeam = Team.all[team.id + Constants.TEAM_STARTING_ID];
            team.stringID = "[#" + team.mindustryTeam.color.toString().substring(0, 6) + "]" + team.id + "[]";
        }

        DeadZone.initCache();
    }

    /**
     * Performs Setup's last tasks, ending core placement and starting the game. Setup should not be interacted with past this point.
     */
    public static void beginGame() {
        Gamedata.gameStarted = true;

        SiegePlugin.PlayersLastSeen = System.currentTimeMillis();
        SiegePlugin.PlayersLastActive = System.currentTimeMillis();

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

            long startTime = System.currentTimeMillis();
            Utilities.Tuple<Point2, Boolean> coreGetOutput = team.corePlacementPosition(true);
            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;
            System.out.println("Finished core search for team " + team.id + " in " + elapsedTime + " ms (" + (elapsedTime / (1000f / 60f)) + " ticks at 60TPS)");

            if (coreGetOutput == null) {
                for (PersistentPlayer player : team.players) {
                    if (player.online) {
                        player.currentPlayer.sendMessage("[red]Your core could not be placed, as your desired location was too distant from one in which it would be valid. You will be returned to the Citadel team.");
                    }
                }
                team.technicalErrorDisqualified();
                continue;
            }
            if (coreGetOutput.b) {
                for (PersistentPlayer player : team.players) {
                    if (player.online) {
                        player.currentPlayer.sendMessage("[accent]Your core's position had to be adjusted, as it would have been placed in an invalid position.");
                    }
                }
            }
            Point2 corePoint = coreGetOutput.a;
            Tile tile = world.tile(corePoint.x, corePoint.y); // Subtract 1 because of how foundations 'center' tile are measured
            tile.setNet(Blocks.coreFoundation, team.mindustryTeam, 0);
            state.teams.registerCore((CoreBlock.CoreBuild) tile.build);
        }

        // Give team loadouts
        Team.green.items().add(Constants.CITADEL_LOADOUT);
        for (RaiderTeam team : Gamedata.raiderTeams) {
            team.mindustryTeam.items().add(Constants.RAIDER_LOADOUT);
        }

        // Give keep blocks infinite health
        if (Keep.keepExists()) {
            world.tiles.forEach(tile -> {
                if (tile.build != null && Keep.inKeep(tile.build) && tile.build.team() == Team.green)
                    tile.build.health = Float.MAX_VALUE;
            });
        }
        Keep.keepExisted = Keep.keepExists();

        // Initialize dead zone
        long beginTime = System.currentTimeMillis();
        CoreBlock.CoreBuild[] cores = Gamedata.getAllCores();
        for (CoreBlock.CoreBuild core : cores) {
            DeadZone.reloadCore(core);
        }
        DeadZone.reloadFloor();
        long endTime = System.currentTimeMillis();
        int elapsed = (int) (endTime - beginTime);
        System.out.println(elapsed + " ms to generate and write floor (" + (elapsed / (1000f / 60f)) + " ticks at 60TPS)");

        SiegePlugin.announce("[sky]Cores have been placed. Raiders and the Citadel can now build and attack. Good luck, and have fun!");
    }

    public static void dataDump() {
        System.out.println("nextTimeReminder: " + nextTimeReminder);
        System.out.println("changedToCorePlacement: " + changedToCorePlacement);
    }
}
