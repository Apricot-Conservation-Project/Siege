package siege;

import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.CommandHandler;
import arc.util.Time;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock;

import java.util.ArrayList;
import java.util.List;

import static mindustry.Vars.world;

public class RaiderTeam {
    public int id;
    public String stringID;
    public List<PersistentPlayer> players;
    public boolean open;

    // Should have no value until team setup ends
    public Team mindustryTeam;

    // Should be inaccessible after setup concludes.
    public List<PersistentPlayer> joinRequests;
    public List<PersistentPlayer> invitations;

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

        players = new ArrayList<>();
        open = false;
        stringID = "[blue]" + id + "[]";
    }

    public RaiderTeam(Player initialPlayer) {
        this();
        players.add(PersistentPlayer.fromPlayer(initialPlayer));
    }

    /**
     * Get all of this team's cores
     * @return All the cores belonging to the team
     */
    public Seq<CoreBlock.CoreBuild> getCores() {
        return mindustryTeam.cores();
    }

    /**
     * Destroys this team.
     */
    public void destroy() {
        SiegePlugin.announce("[accent]Team " + stringID + " has been destroyed!");

        Gamedata.deadRaiderTeams.add(this);
        Gamedata.raiderTeams.remove(this);

        // Kill all the team's units and blocks.
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
     * Finds a player's team
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
            if (args.length == 0) {
                teamsHelp(executor);
                return;
            }

            final List<String> commands = List.of(new String[]{"help", "list", "invite", "join", "quit", "create", "open", "close", "kick"});

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
                    default: System.out.println("Unreachable state. Error code 345374");
                }
            } else {
                switch (args[0]) {
                    case "help":     teamsHelp(executor); return;
                    case "list":     teamsList(executor); return;
                    case "quit":     teamsQuit(executor); return;
                    case "kick":     teamsKick(executor, args[1]); return;
                    default: executor.sendMessage("[red]This subcommand is no longer allowed, as the game has begun. Check /team help for available team commands.");
                }
            }
        }

        // Sends a help message listing the team commands
        private static void teamsHelp(Player executor) {
            String output;
            if (Gamedata.gameStarted) {
                output =
                        "\n[orange]team list[white]: List all current Raider teams." +
                        "\n[orange]team quit[white]: Quit your team and return to the Citadel team. You may attempt to join a new team afterward." +
                        "\n[orange]team kick [yellow]<player name/ID>[white]: Start a vote to remove a player from your team.\n";
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
                        "\n[orange]team kick [yellow]<player name/ID>[white]: Start a vote to remove a player from your team.\n";
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
                if (team.players.size() >= Constants.RAIDER_MAX_PLAYERS) {
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
            if (team.players.size() >= Constants.RAIDER_MAX_PLAYERS) {
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

            PersistentPlayer persistentExecutor = PersistentPlayer.fromPlayer(executor);

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

            if (team.joinRequests.contains(persistentExecutor)) {
                executor.sendMessage("[accent]You have already requested to join team " + team.stringID + ".");
                return;
            }

            // Join request is instantly accepted
            if (team.open || team.invitations.contains(persistentExecutor)) {
                if (team.players.size() >= Constants.RAIDER_MAX_PLAYERS) {
                    executor.sendMessage("[accent]Your join request would be accepted, however, team " + team.stringID + " is already full. You might want to consider creating or joining another team.");
                    return;
                }
                team.invitations.remove(persistentExecutor);
                team.players.add(persistentExecutor);
                for (PersistentPlayer player : team.players) {
                    player.currentPlayer.sendMessage(executor.name + " [accent] has joined the team.");
                }
                executor.sendMessage("[accent]Joined team " + team.stringID + ".");
                return;
            }

            // Join request is sent to be accepted or ignored by the recipient
            if (team.players.size() >= Constants.RAIDER_MAX_PLAYERS) {
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
        private static void teamsKick(Player executor, String targetPlayer) {
            // Start a vote to kick the target player from your team
            // TODO
        }
    }
}
