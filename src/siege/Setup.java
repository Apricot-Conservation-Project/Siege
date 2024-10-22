package siege;

import arc.math.Mathf;
import mindustry.content.Blocks;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import static mindustry.Vars.state;
import static mindustry.Vars.world;

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
    }

    /**
     * Performs Setup's last tasks, ending core placement and starting the game. Setup should not be interacted with past this point.
     */
    public static void beginGame() {
        Gamedata.gameStarted = true;

        // Give team loadouts
        Team.green.items().add(Constants.CITADEL_LOADOUT);
        for (RaiderTeam team : Gamedata.raiderTeams) {
            team.mindustryTeam.items().add(Constants.RAIDER_LOADOUT);
        }

        // Add raider team cores
        // Teams can be removed here if they have no players.
        RaiderTeam[] teams = Gamedata.raiderTeams.toArray(new RaiderTeam[0]);
        for (RaiderTeam team : teams) {
            // Get position of all online players
            List<Point2D.Float> points = new ArrayList<>();
            for (PersistentPlayer player : team.players) {
                if (player.online) {
                    // x, y = 8 * tileX, tileY
                    points.add(new Point2D.Float(player.currentPlayer.x / 8.0f, player.currentPlayer.tileOn().worldy() / 8.0f));
                }
            }
            // Kick team if it has no online players at game start
            if (points.isEmpty()) {
                SiegePlugin.announce("[orange]Team " + team.stringID + " was removed for having no online players at game start.");
                Gamedata.raiderTeams.remove(team);
                continue;
            }
            // Build team's core at the geometric median of player positions
            Point2D.Float median = geometricMedian(points.toArray(new Point2D.Float[0]), 0.05f);
            Tile tile = world.tile(Mathf.round(median.x - 0.5f), Mathf.round(median.y - 0.5f)); // Subtract 1 because of how foundations 'center' tile are measured
            tile.setNet(Blocks.coreFoundation, team.mindustryTeam, 0);
            state.teams.registerCore((CoreBlock.CoreBuild) tile.build);
        }

        long beginTime = System.currentTimeMillis();
        CoreBlock.CoreBuild[] cores = Gamedata.getAllCores();
        for (CoreBlock.CoreBuild core : cores) {
            Gamedata.reloadCore(core);
        }
        long endTime = System.currentTimeMillis();
        int elapsed = (int) (endTime - beginTime);
        System.out.println(elapsed + " ms to make deadzone (" + Mathf.round(elapsed / (1000f / 60f), 0.01f) + " ticks at 60TPS)");

        SiegePlugin.announce("[sky]Cores have been placed. Raiders and the Citadel can now build and attack. Good luck, and have fun!");
    }

    // Finds the point with the least sum distance to all given points, accurate to within the given precision.
    // Probably does not need to ever be touched again
    private static Point2D.Float geometricMedian(Point2D.Float[] points, float precision) {
        if (points.length == 0) {
            throw new IllegalArgumentException("Point array cannot have length zero.");
        }
        if (points.length == 1) {
            return points[0];
        }
        // Get average point to start geometric median approximation
        float sumX = 0, sumY = 0;
        for (Point2D.Float point : points) {
            sumX += point.x;
            sumY += point.y;
        }
        if (points.length == 2) {
            return new Point2D.Float(sumX / points.length, sumY / points.length);
        }
        final Point2D.Float mean = new Point2D.Float(sumX / points.length, sumY / points.length);
        // samples[0] = center point (starts at mean)
        Point2D.Float[] samples = new Point2D.Float[] {mean, new Point2D.Float(), new Point2D.Float(), new Point2D.Float(), new Point2D.Float()};
        // Start with precision equal to the greatest distance between any point and the mean
        float currentPrecision = Float.MIN_VALUE;
        for (Point2D.Float point : points) {
            float distance = (float) point.distance(mean);
            if (distance > currentPrecision) {
                currentPrecision = distance;
            }
        }
        Point2D.Float[] offsets = new Point2D.Float[] {new Point2D.Float(0, 0), new Point2D.Float(-currentPrecision, -currentPrecision), new Point2D.Float(-currentPrecision, currentPrecision), new Point2D.Float(currentPrecision, -currentPrecision), new Point2D.Float(currentPrecision, currentPrecision)};
        Point2D.Float median;
        // Test a center point and cross of four nearby points, the best median approximation becomes the center for the next round, if the center is best, finish.
        while (true) {
            double minDistance = Double.MAX_VALUE;
            int minIndex = -1;
            for (int i = 0; i < offsets.length; i++) {
                Point2D.Float offset = offsets[i];
                Point2D.Float sample = new Point2D.Float(samples[0].x + offset.x, samples[0].y + offset.y);
                double sumDistance = 0;
                for (Point2D.Float point : points) {
                    sumDistance += point.distance(sample);
                }
                if (sumDistance < minDistance - 0.00000000001) {
                    minDistance = sumDistance;
                    minIndex = i;
                }
            }
            if (minIndex == 0) {
                // Use successively finer precision until needs are satisfied
                if (currentPrecision <= precision) {
                    median = samples[0];
                    break;
                }
                currentPrecision = currentPrecision / 2f;
                offsets = new Point2D.Float[] {new Point2D.Float(0, 0), new Point2D.Float(-currentPrecision, -currentPrecision), new Point2D.Float(-currentPrecision, currentPrecision), new Point2D.Float(currentPrecision, -currentPrecision), new Point2D.Float(currentPrecision, currentPrecision)};
                continue;
            }
            samples[0] = samples[minIndex];
        }
        return median;
    }

    public static void dataDump() {
        System.out.println("nextTimeReminder: " + nextTimeReminder);
        System.out.println("changedToCorePlacement: " + changedToCorePlacement);
    }
}
