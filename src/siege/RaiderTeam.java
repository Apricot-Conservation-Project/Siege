package siege;

import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.struct.Seq;
import arc.util.CommandHandler;
import arc.util.Time;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import static mindustry.Vars.tilesize;
import static mindustry.Vars.world;
import static siege.Utilities.geometricMedian;

public class RaiderTeam {
    public int id;
    public String stringID;
    public Seq<PersistentPlayer> players;
    public boolean open;

    // Should have no value until team setup ends
    public Team mindustryTeam;

    // Should be inaccessible after setup concludes.
    public Seq<PersistentPlayer> joinRequests;
    public Seq<PersistentPlayer> invitations;

    protected long votekickEnd = Long.MAX_VALUE;
    protected PersistentPlayer votekickTarget = null;
    protected boolean votekickOngoing = false;
    protected Seq<PersistentPlayer> yesVoters;
    protected Seq<PersistentPlayer> noVoters;
    protected Seq<PersistentPlayer> abstainVoters;

    public RaiderTeam() {
        id = 0;
        boolean collision = true;
        while (collision) {
            id ++;
            collision = false;
            for (RaiderTeam team : Gamedata.raiderTeams) {
                if (team.id == id) {
                    collision = true;
                    break;
                }
            }
        }

        players = new Seq<>(false);
        joinRequests = new Seq<>(false);
        invitations = new Seq<>(false);
        yesVoters = new Seq<>(false);
        noVoters = new Seq<>(false);
        abstainVoters = new Seq<>(false);
        open = false;
        stringID = "[blue]" + id + "[]";
    }

    public RaiderTeam(Player initialPlayer) {
        this();
        players.add(PersistentPlayer.fromPlayer(initialPlayer));
    }

    /**
     * Performs time-based actions for the team. Should only be called after the game has started.
     */
    public void update() {
        updateVotekick();
    }

    private void updateVotekick() {
        int yeses = yesVoters.size;
        int nos = noVoters.size;
        int abstains = abstainVoters.size;
        int totalPlayers = players.size;
        int nonvoters = totalPlayers - (yeses + nos + abstains);
        // Judge by yeses and nos only if the votekick ends by time
        if (System.currentTimeMillis() > votekickEnd) {
            if (yeses > nos) {
                votekickPasses();
            } else {
                votekickFails();
            }
        }
        // End early if it is not possible for nos to overtake yeses or vice versa
        else if (yesVoters.size > nos + nonvoters) {
            votekickPasses();
        }
        else if (noVoters.size > yeses + nonvoters) {
            votekickFails();
        }
    }

    /**
     * Finds the location that a team would place their starting core
     * @param adjust Whether to adjust the result to avoid terrain and other conflicting cores
     * @return A tuple containing the index of the tile where the core would go, and a boolean representing whether the core had to be adjusted significantly. Can be null if the operation fails to locate a valid position.
     */
    public Utilities.Tuple<Point2, Boolean> corePlacementPosition(boolean adjust) {
        // Get position of all online players
        List<Point2D.Float> points = new ArrayList<>();
        for (PersistentPlayer player : players) {
            if (player.online) {
                // x, y = 8 * tileX, tileY
                points.add(new Point2D.Float(player.currentPlayer.x / tilesize, player.currentPlayer.y / tilesize));
            }
        }

        Point2D.Float coreFloat = geometricMedian(points.toArray(new Point2D.Float[0]), 0.05f);
        Point2 adjustedCorePosition = new Point2(Mathf.round(coreFloat.x - 0.5f), Mathf.round(coreFloat.y - 0.5f));
        if (!adjust) {
            return new Utilities.Tuple<>(adjustedCorePosition, false);
        }

        final float coreDistance2 = Mathf.sqr(Constants.CORE_PLACEMENT_MIN_DISTANCE);
        final float coreCitadelDistance2 = Mathf.sqr(Constants.CORE_PLACEMENT_CITADEL_MIN_DISTANCE);
        CoreBlock.CoreBuild[] cores = Gamedata.getAllCores();

        // If within core no-core radius, leave core no-core radius.
        for (CoreBlock.CoreBuild core : cores) {
            float radius = Constants.CORE_PLACEMENT_MIN_DISTANCE;
            if (core.team() == Team.green) {
                radius = Constants.CORE_PLACEMENT_CITADEL_MIN_DISTANCE;
            }
            if (adjustedCorePosition.dst(core.tileX(), core.tileY()) < 5 + radius - Constants.MAX_DISTANCE_TO_VIABLE_CORE_LOCATION) {
                if (adjustedCorePosition.x == core.tileX() && adjustedCorePosition.y == core.tileY()) {
                    adjustedCorePosition.x ++;
                }
                int dx = adjustedCorePosition.x - core.tileX();
                int dy = adjustedCorePosition.y - core.tileY();
                float currentDistance = Mathf.sqrt(dx * dx + dy * dy);
                float distanceScale = radius / currentDistance;
                adjustedCorePosition = new Point2((int) (core.tileX() + distanceScale * dx), (int) (core.tileY() + distanceScale * dy));
            }
        }

        // Progressively scan further out from the desired location until an accessible point is found and confirmed to be the closest point
        Point2 closestCorable = null;
        float closestDistance2 = Float.MAX_VALUE;
        int radius = 0;
        float maxDistanceToCheck2 = Mathf.sqr(Constants.MAX_DISTANCE_TO_VIABLE_CORE_LOCATION);
        while (true) {
            if (radius > Constants.MAX_DISTANCE_TO_VIABLE_CORE_LOCATION) {
                System.out.println("Brute force core search timed out for team " + id);
                return null;
            }

            ArrayList<Point2> samples = new ArrayList<>(radius * 8);
            if (radius == 0) {
                samples = new ArrayList<>(1);
                samples.add(adjustedCorePosition);
            } else { // Add all points in the outer square to samples
                int[] yOffsets = new int[] {-radius, radius};
                for (int yOffset : yOffsets) {
                    for (int xOffset = -radius + 1; xOffset <= radius - 1; xOffset++) {
                        // Discard samples incapable of being useful
                        if ((xOffset * xOffset + yOffset * yOffset) > maxDistanceToCheck2) {
                            continue;
                        }
                        samples.add(new Point2(xOffset + adjustedCorePosition.x, yOffset + adjustedCorePosition.y));
                    }
                }
                int[] xOffsets = new int[] {-radius, radius};
                for (int xOffset : xOffsets) {
                    for (int yOffset = -radius; yOffset <= radius; yOffset++) {
                        if ((xOffset * xOffset + yOffset * yOffset) > maxDistanceToCheck2) {
                            continue;
                        }
                        samples.add(new Point2(xOffset + adjustedCorePosition.x, yOffset + adjustedCorePosition.y));
                    }
                }
            }

            samples: for (Point2 sample : samples) {
                // Check for terrain collisions
                for (Point2 offset : Constants.Performance.COLLISION_OFFSETS) {
                    Point2 corner = new Point2(sample.x + offset.x, sample.y + offset.y);
                    if (corner.x < 0 || corner.x >= world.width() || corner.y < 0 || corner.y >= world.height()) {
                        continue samples; // Cores cannot be out of bounds
                    }
                    Tile tile = world.tile(corner.x, corner.y);
                    if (tile.solid() || !tile.floor().placeableOn) {
                        continue samples; // Cores cannot be placed in solid blocks or on deep liquids or void
                    }
                }
                // Check for core vicinity
                for (CoreBlock.CoreBuild core : cores) {
                    if (sample.dst2(core.tileX(), core.tileY()) < coreDistance2) {
                        continue samples; // Cores cannot be placed too close to other cores
                    }
                    if (core.team() == Team.green && sample.dst2(core.tileX(), core.tileY()) < coreCitadelDistance2) {
                        continue samples;// Cores cannot be placed too close to citadel cores
                    }
                }

                float distance2 = sample.dst2(adjustedCorePosition);
                if (distance2 < closestDistance2) {
                    closestDistance2 = distance2;
                    closestCorable = sample;
                }
            }

            if (closestDistance2 <= radius * radius) {
                break;
            }

            radius ++;
        }

        return new Utilities.Tuple<>(closestCorable, /*cycles > 1 || */radius != 0);
    }

    /**
     * Get all of this team's cores
     * @return All the cores belonging to the team
     */
    public CoreBlock.CoreBuild[] getCores() {
        return mindustryTeam.cores().list().toArray(new CoreBlock.CoreBuild[0]);
    }

    /**
     * Destroys this team.
     */
    public void destroy() {
        SiegePlugin.announce("[accent]Team " + stringID + " has been destroyed!");

        Gamedata.deadRaiderTeams.add(this);
        Gamedata.raiderTeams.remove(this);

        killAll();

        // Put all players into Citadel team
        for (PersistentPlayer player : players) {
            if (player.online) {
                if (player.currentPlayer.team() != Team.green) {
                    player.currentPlayer.team(Team.green);
                }
                if (player.currentPlayer.dead()) {
                    CoreBlock.playerSpawn(Team.green.cores().random().tile, player.currentPlayer);
                }
            }
        }
    }

    /**
     * Call when a team must be disqualified due to a failure to a fundamental game action.
     * Does not explain error to team members.
     */
    public void technicalErrorDisqualified() {
        for (Player player : Groups.player) {
            PersistentPlayer persistentPlayer = PersistentPlayer.fromPlayer(player);
            if (!this.players.contains(persistentPlayer)) {
                player.sendMessage("[orange]Team " + stringID + " has been disqualified for failure to accomplish a fundamental game action.");
            } else {
                player.team(Team.green);
                if (player.dead()) {
                    CoreBlock.playerSpawn(Team.green.cores().random().tile, player);
                }
            }
        }

        Gamedata.raiderTeams.remove(this);

        killAll();
    }

    // Kill all the team's units and blocks.
    private void killAll() {
        for (int x = 0; x < world.width(); x++) {
            for (int y = 0; y < world.height(); y++) {
                Tile tile = world.tile(x, y);
                if (tile.build != null && tile.team() == mindustryTeam) {
                    Time.run(Mathf.random(60f * 6), tile.build::kill);
                }
            }
        }
        for (Unit u : Groups.unit) {
            if (u.team == mindustryTeam) {
                u.kill();
            }
        }
    }

    private void votekickPasses() {
        announceTeam("[blue]Votekick passed. " + votekickTarget.currentPlayer.name() + "[blue] has been kicked from the team and returned to the Citadel.");
        players.remove(votekickTarget);
        CoreBlock.playerSpawn(Team.green.cores().random().tile, votekickTarget.currentPlayer);
        votekickClear();
    }

    private void votekickFails() {
        announceTeam("[blue]Votekick failed. " + votekickTarget.currentPlayer.name() + "[blue] will remain in the team.");
        votekickClear();
    }

    private void votekickClear() {
        votekickEnd = Long.MAX_VALUE;
        votekickTarget = null;
        votekickOngoing = false;
        yesVoters.clear();
        noVoters.clear();
        abstainVoters.clear();
    }

    /**
     * Sends a message to all team members.
     * @param message The message to send to the team members
     */
    public void announceTeam(String message) {
        for (PersistentPlayer persistentPlayer : players) {
            if (persistentPlayer.online) {
                persistentPlayer.currentPlayer.sendMessage(message);
            }
        }
    }

    /**
     * Sends a message to all team members but those excluded.
     * @param message The message to send to the team members
     * @param excluded The players that will not receive the message
     */
    public void announceTeam(String message, PersistentPlayer... excluded) {
        playerLoop: for (PersistentPlayer persistentPlayer : players) {
            for (PersistentPlayer excludedPlayer : excluded) {
                if (persistentPlayer.equals(excludedPlayer)) {
                    continue playerLoop;
                }
            }
            if (persistentPlayer.online) {
                persistentPlayer.currentPlayer.sendMessage(message);
            }
        }
    }

    /**
     * Sends a message to all team members but those excluded.
     * @param message The message to send to the team members
     * @param excluded The players that will not receive the message
     */
    public void announceTeam(String message, Player... excluded) {
        playerLoop: for (PersistentPlayer persistentPlayer : players) {
            for (Player excludedPlayer : excluded) {
                if (persistentPlayer.currentPlayer.equals(excludedPlayer)) {
                    continue playerLoop;
                }
            }
            if (persistentPlayer.online) {
                persistentPlayer.currentPlayer.sendMessage(message);
            }
        }
    }



    /**
     * Finds a player's team.
     * @param player Any player
     * @return The player's team if they are in one, otherwise null
     */
    public static RaiderTeam getTeam(PersistentPlayer player) {
        for (RaiderTeam team : Gamedata.raiderTeams) {
            if (team.players.contains(player)) {
                return team;
            }
        }

        return null;
    }

    /**
     * Finds a raider team given their internal team.
     * @param team The mindustry base team belonging to the desired raider team
     * @return The raider team associated with the given Team
     */
    public static RaiderTeam getTeam(Team team) {
        if (team.id < Constants.TEAM_STARTING_ID) {
            return null;
        }
        for (RaiderTeam raiderTeam : Gamedata.raiderTeams) {
            if (raiderTeam.mindustryTeam == team) {
                return raiderTeam;
            }
        }
        return null;
    }





    public static class Commands {
        /**
         * Registers team-related commands
         * @param handler The CommandHandler passed from the Plugin superclass
         */
        public static void registerCommands(CommandHandler handler) {
            handler.register("team", "[command] [argument]", "Run /team to list team commands.", Commands::teamCommand);
        }

        /**
         * Executes any teams * command, based on
         * @param args The arguments provided by the player, if any
         * @param executor The player running the teams command
         */
        private static void teamCommand(String[] args, Player executor){
            try {
                if (args.length == 0) {
                    teamsHelp(executor);
                    return;
                }
                if (args.length == 1) {
                    // Pass the error down to the command
                    args = new String[]{args[0], null};
                }

                final List<String> commands = List.of(new String[]{"help", "list", "invite", "join", "quit", "create", "open", "close", "kick", "vote"});

                if (!commands.contains(args[0])) {
                    executor.sendMessage("[red]Invalid command. Try running /team help.");
                    return;
                }

                if (Gamedata.teamSetupPhase()) {
                    switch (args[0]) {
                        case "help":     teamsHelp(executor); return;
                        case "list":     teamsList(executor); return;
                        case "invite": teamsInvite(executor, args[1]); return;
                        case "join":     teamsJoin(executor, args[1]); return;
                        case "quit":     teamsQuit(executor); return;
                        case "create": teamsCreate(executor); return;
                        case "open":     teamsOpen(executor); return;
                        case "close":   teamsClose(executor); return;
                        case "kick":     teamsKick(executor, args[1]); return;
                        case "vote":     teamsVote(executor, args[1]); return;
                        default: System.out.println("Unreachable state. Error code 345374");
                    }
                } else {
                    switch (args[0]) {
                        case "help":     teamsHelp(executor); return;
                        case "list":     teamsList(executor); return;
                        case "quit":     teamsQuit(executor); return;
                        case "kick":     teamsKick(executor, args[1]); return;
                        case "vote":     teamsVote(executor, args[1]); return;
                        default: executor.sendMessage("[red]This subcommand is no longer allowed, as the game has begun. Check /team help for available team commands.");
                    }
                }
            } catch (Exception e) {
                    e.printStackTrace();
                    Gamedata.dataDump();
                    SiegePlugin.announce("[red]Exception thrown during team command");
            }
        }

        // Sends a help message listing the team commands
        private static void teamsHelp(Player executor) {
            String output;
            if (Gamedata.gameStarted) {
                output =
                        "\n[orange]team list[white]: List all current Raider teams." +
                        "\n[orange]team quit[white]: Quit your team and return to the Citadel team. You may attempt to join a new team afterward." +
                        "\n[orange]team kick [yellow]<player name/ID>[white]: Start a vote to remove a player from your team." +
                        "\n[orange]team vote [yellow]<yes/no/abstain>[white]: Vote whether or not to kick a player.\n";
            } else {
                output =
                        "\n[accent]Currently in team setup phase." +
                        "\n[orange]team list[white]: List all current Raider teams." +
                        "\n[orange]team invite [yellow]<player name/ID>[white]: Invite a player to your team, or accept a join request." +
                        "\n[orange]team join [yellow]<team/player name/ID>[white]: Request to join a team, or accept a team's invitation." +
                        "\n[orange]team quit[white]: Quit your team and return to the Citadel team. You may attempt to join a new team afterward." +
                        "\n[orange]team create[white]: Create a new team." +
                        "\n[orange]team open[white]: Allow any player to join your team without approval." +
                        "\n[orange]team close[white]: Cease allowing players to join your team without approval." +
                        "\n[orange]team kick [yellow]<player name/ID>[white]: Start a vote to remove a player from your team." +
                        "\n[orange]team vote [yellow]<yes/no/abstain>[white]: Vote whether or not to kick a player.\n";
            }

            executor.sendMessage(output);
        }

        // Lists all current teams
        private static void teamsList(Player executor) {
            if (Gamedata.raiderTeams.isEmpty()) {
                executor.sendMessage("[accent]There are currently no Raider teams.");
                return;
            }

            Gamedata.raiderTeams.forEach(team -> {
                executor.sendMessage("\n[accent]Team ID: " + team.stringID);

                team.players.forEach(player -> {
                    String message = "      [accent]Player ID: [blue]" + player.currentPlayer.id + ":[white] " + player.currentPlayer.name;
                    if (!player.online) {
                        message += " [white]([red]offline[])";
                    }
                    executor.sendMessage(message);
                });
            });

            executor.sendMessage("");
        }

        // Invites a player or accepts a join request to the executor's team
        private static void teamsInvite(Player executor, String targetPlayerString) {
            RaiderTeam team = RaiderTeam.getTeam(PersistentPlayer.fromPlayer(executor));

            if (team == null) {
                executor.sendMessage("[red]You are not currently in a team.");
                return;
            }

            PersistentPlayer targetPlayer = PersistentPlayer.fromString(targetPlayerString, executor);

            if (targetPlayer == null) {
                // Error messages have already been sent.
                return;
            }

            if (team.players.contains(targetPlayer)) {
                executor.sendMessage("[accent]You are already in the same team.");
                return;
            }

            if (team.invitations.contains(targetPlayer)) {
                executor.sendMessage(targetPlayer.currentPlayer.name + "[accent] has already been invited.");
                return;
            }

            // Invitation is immediately accepted
            if (team.joinRequests.contains(targetPlayer)) {
                if (team.players.size >= Constants.RAIDER_MAX_PLAYERS) {
                    executor.sendMessage("[accent]Your invitation would be accepted, however, your team is already full. In order for another player to join, one of yours must leave.");
                    return;
                }
                team.joinRequests.remove(targetPlayer);
                team.players.add(targetPlayer);
                for (PersistentPlayer player : team.players) {
                    player.currentPlayer.sendMessage(executor.name + " [accent]added " + targetPlayer.currentPlayer.name + " [accent]to the team.");
                }
                targetPlayer.currentPlayer.sendMessage("[accent]You have been added to team " + team.stringID + ".");
                return;
            }

            // Invitation is sent, to be accepted or ignored by the recipient
            if (team.players.size >= Constants.RAIDER_MAX_PLAYERS) {
                executor.sendMessage("[accent]Your invitation has been sent, however, your team is already full. In order for another player to join, one of yours must leave.");
            }
            for (PersistentPlayer player : team.players) {
                player.currentPlayer.sendMessage(executor.name + " [accent]invited " + targetPlayer.currentPlayer.name + " [accent]to the team.");
            }
            team.invitations.add(targetPlayer);
        }

        // Requests to join, or accepts an invitation to a team
        // Players who are already in a team should be able to request to join (switch) as well, so long as they are in the team setup phase.
        private static void teamsJoin(Player executor, String targetString) {
            if (targetString == null || targetString.isEmpty()) {
                executor.sendMessage("[red]You must specify a team or player!");
                return;
            }

            int id = -1;
            try {
                id = Integer.parseInt(targetString);
            } catch (NumberFormatException ignored) {}

            RaiderTeam team = null;
            for (RaiderTeam t : Gamedata.raiderTeams) {
                if (t.id == id) {
                    team = t;
                }
            }

            if (team == null) {
                PersistentPlayer targetPlayer = PersistentPlayer.fromString(targetString, executor);
                if (targetPlayer == null) {
                    executor.sendMessage("[red]No matching teams found.");
                    return;
                }
                team = RaiderTeam.getTeam(targetPlayer);
                if (team == null) {
                    executor.sendMessage("[red]Player [accent]" + targetPlayer.currentPlayer.name + "[red] is not in a team.");
                    return;
                }
            }

            PersistentPlayer persistentExecutor = PersistentPlayer.fromPlayer(executor);

            if (team.players.contains(persistentExecutor)) {
                executor.sendMessage("[red]You are already in this team!");
                return;
            }

            if (team.joinRequests.contains(persistentExecutor)) {
                executor.sendMessage("[accent]You have already requested to join team " + team.stringID + ".");
                return;
            }

            // Join request is instantly accepted
            if (team.open || team.invitations.contains(persistentExecutor)) {
                if (team.players.size >= Constants.RAIDER_MAX_PLAYERS) {
                    executor.sendMessage("[accent]Your join request would be accepted, however, team " + team.stringID + " is already full. You might want to consider creating or joining another team.");
                    return;
                }
                team.invitations.remove(persistentExecutor);
                team.players.add(persistentExecutor);
                team.announceTeam(executor.name + "[accent] has joined the team.", persistentExecutor);
                executor.sendMessage("[accent]Joined team " + team.stringID + ".");
                return;
            }

            // Join request is sent to be accepted or ignored by the recipient
            if (team.players.size >= Constants.RAIDER_MAX_PLAYERS) {
                executor.sendMessage("[accent]Your join request was placed, but keep in mind that the team is already full. If another player leaves, they may accept you, but consider creating or joining another team.");
            }
            for (PersistentPlayer player : team.players) {
                player.currentPlayer.sendMessage(executor.name + " [accent] has requested to join the team.");
            }
            team.joinRequests.add(persistentExecutor);
        }

        // Leaves the current team
        private static void teamsQuit(Player executor) {
            PersistentPlayer persistentExecutor = PersistentPlayer.fromPlayer(executor);
            for (RaiderTeam team : Gamedata.raiderTeams) {
                if (team.players.contains(persistentExecutor)) {
                    team.players.remove(persistentExecutor);
                    executor.sendMessage("[accent]Left team " + team.stringID);
                    return;
                }
            }

            executor.sendMessage("[accent]You are not currently in a team.");
        }

        // Creates a new team
        private static void teamsCreate(Player executor) {
            if (getTeam(PersistentPlayer.fromPlayer(executor)) != null) {
                executor.sendMessage("[red]You are already in a team!");
                return;
            }

            RaiderTeam newTeam = new RaiderTeam(executor);

            executor.sendMessage("[accent]Team created. ID: " + newTeam.stringID);

            Gamedata.raiderTeams.add(newTeam);
        }

        // Opens the current team to unrestricted joining
        private static void teamsOpen(Player executor) {
            RaiderTeam team = getTeam(PersistentPlayer.fromPlayer(executor));

            if (team == null) {
                executor.sendMessage("[red]You are not currently in a team!");
                return;
            }

            SiegePlugin.announce("Team " + team.stringID + " opened. Anyone can now join this team.");
            team.open = true;
        }

        // Closes the current team from unrestricted joining
        private static void teamsClose(Player executor) {
            RaiderTeam team = getTeam(PersistentPlayer.fromPlayer(executor));

            if (team == null) {
                executor.sendMessage("[red]You are not currently in a team!");
                return;
            }

            SiegePlugin.announce("Team " + team.stringID + " closed. Joins now possible only by request.");
            team.open = false;
        }

        // Starts a vote to kick a player from the team.
        private static void teamsKick(Player executor, String targetString) {
            RaiderTeam team = getTeam(PersistentPlayer.fromPlayer(executor));

            if (team == null) {
                executor.sendMessage("[red]You are not currently in a team!");
                return;
            }

            PersistentPlayer targetPlayer = PersistentPlayer.fromString(targetString, executor);
            if (targetPlayer == null) {
                return; // Error messages already sent
            }
            if (PersistentPlayer.fromPlayer(executor).equals(targetPlayer)) {
                executor.sendMessage("[orange]You cannot votekick yourself! Use /team quit instead.");
                return;
            }
            if (team.votekickOngoing) {
                if (team.votekickTarget.equals(targetPlayer)) {
                    executor.sendMessage("[orange]The target player is already in a votekick.");
                } else {
                    executor.sendMessage("[red]A votekick is already underway for a different player. You must wait until it is complete.");
                }
                return;
            }

            int votekickTime;
            if (Gamedata.gameStarted) {
                votekickTime = Constants.VOTEKICK_LENGTH_MS;
            } else {
                votekickTime = Constants.VOTEKICK_LENGTH_PREGAME_MS;
            }
            team.votekickTarget = targetPlayer;
            team.votekickEnd = System.currentTimeMillis() + votekickTime;
            executor.sendMessage("[accent]Started vote to kick " + targetPlayer.currentPlayer.name + "[accent] from the team. You have " + (votekickTime / 1000) + " seconds to vote.");
            team.announceTeam("[accent]A vote has been started to kick " + targetPlayer.currentPlayer.name + "[accent] from the team. You have " + (votekickTime / 1000) + " seconds to vote.", executor);
        }

        // Votes in a votekick
        private static void teamsVote(Player executor, String voteString) {
            PersistentPlayer persistentExecutor = PersistentPlayer.fromPlayer(executor);
            RaiderTeam team = getTeam(persistentExecutor);

            if (team == null) {
                executor.sendMessage("[red]You are not currently in a team!");
                return;
            }

            if (!team.votekickOngoing) {
                executor.sendMessage("[orange]There is currently no ongoing team votekick.");
                return;
            }

            enum Vote {
                Yes, No, Abstain
            }
            Vote vote = switch (voteString.toLowerCase().charAt(0)) {
                case 'y' -> Vote.Yes;
                case 'n' -> Vote.No;
                case 'a' -> Vote.Abstain;
                default -> null;
            };
            if (vote == null) {
                executor.sendMessage("[red]You can vote Yes (y), No (n), or Abstain (a).");
                return;
            }

            if (team.yesVoters.contains(persistentExecutor)) {
                if (vote == Vote.Yes) {
                    executor.sendMessage("[orange]You have already voted yes.");
                }
                else if (vote == Vote.No) {
                    executor.sendMessage("[accent]Your vote has been changed to no.");
                    team.yesVoters.remove(persistentExecutor);
                    team.noVoters.add(persistentExecutor);
                } else {
                    executor.sendMessage("[accent]Your vote has been changed to abstain.");
                    team.yesVoters.remove(persistentExecutor);
                    team.abstainVoters.add(persistentExecutor);
                }
            }
            else if (team.noVoters.contains(persistentExecutor)) {
                if (vote == Vote.Yes) {
                    executor.sendMessage("[accent]Your vote has been changed to yes.");
                    team.noVoters.remove(persistentExecutor);
                    team.yesVoters.add(persistentExecutor);
                } else if (vote == Vote.No) {
                    executor.sendMessage("[orange]You have already voted no.");
                } else {
                    executor.sendMessage("[accent]Your vote has been changed to abstain.");
                    team.noVoters.remove(persistentExecutor);
                    team.abstainVoters.add(persistentExecutor);
                }
            }
            else if (team.abstainVoters.contains(persistentExecutor)) {
                if (vote == Vote.Yes) {
                    executor.sendMessage("[accent]Your vote has been changed to yes.");
                    team.abstainVoters.remove(persistentExecutor);
                    team.yesVoters.add(persistentExecutor);
                } else if (vote == Vote.No) {
                    executor.sendMessage("[accent]Your vote has been changed to no.");
                    team.abstainVoters.remove(persistentExecutor);
                    team.noVoters.add(persistentExecutor);
                } else {
                    executor.sendMessage("[orange]You have already voted to abstain.");
                }
            }
            else {
                if (vote == Vote.Yes) {
                    executor.sendMessage("[accent]You have voted yes.");
                    team.yesVoters.add(persistentExecutor);
                } else if (vote == Vote.No) {
                    executor.sendMessage("[orange]You have voted no.");
                    team.noVoters.add(persistentExecutor);
                } else {
                    executor.sendMessage("[accent]You have voted to abstain.");
                    team.abstainVoters.add(persistentExecutor);
                }
            }
        }
    }
}
