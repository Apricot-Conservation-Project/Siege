package siege;

import arc.util.CommandHandler;
import mindustry.gen.Player;

import java.util.ArrayList;
import java.util.List;

public class RaiderTeam {
    public int id;
    public List<Player> players;
    public boolean open;

    public RaiderTeam() {
        id = 0;
        boolean collision = true;
        while (collision) {
            id ++;
            collision = false;
            for (RaiderTeam team : SiegePlugin.gamedata.raiderTeams) {
                if (team.id == id) {
                    collision = true;
                }
            }
        }

        players = new ArrayList<Player>();
        open = false;
    }

    public RaiderTeam(Player initialPlayer) {
        this();
        players.add(initialPlayer);
    }

    /**
     * Finds a player's team
     * @param player Any player
     * @return The player's team if they are in one, otherwise null
     */
    public static RaiderTeam getTeam(Player player) {
        for (RaiderTeam team : SiegePlugin.gamedata.raiderTeams) {
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
            handler.<Player>register("team", "[command] [argument]", "Run /team to list team commands.", Commands::teamCommand);
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

            switch (args[0]) {
                case "help":     teamsHelp(executor); break;
                case "list":     teamsList(executor); break;
                case "invite": teamsInvite(executor, args[1]); break;
                case "join":     teamsJoin(executor, args[1]); break;
                case "quit":     teamsQuit(executor); break;
                case "create": teamsCreate(executor); break;
                case "open":     teamsOpen(executor); break;
                case "close":   teamsClose(executor); break;
                case "kick":     teamsKick(executor, args[1]); break;
                default: executor.sendMessage("[red]Invalid command. Try running /team help.");
            }
        }

        /**
         * Sends a help message listing the team commands
         */
        private static void teamsHelp(Player executor) {
            String output =
                    "\n[orange]team list[white]: List all current Raider teams." +
                    "\n[orange]team invite [yellow]<player name/ID>[white]: Invite a player to your team, or accept a join request." +
                    "\n[orange]team join [yellow]<team ID>[white]: Request to join a team, or accept a team's invitation." +
                    "\n[orange]team quit[white]: Quit your team and return to the Citadel team. You may attempt to join a new team afterward." +
                    "\n[orange]team create[white]: Create a new team." +
                    "\n[orange]team open[white]: Allow any player to join your team without approval." +
                    "\n[orange]team close[white]: Cease allowing players to join your team without approval." +
                    "\n[orange]team kick [yellow]<player name/ID>[white]: Start a vote to remove a player from your team.\n";

            executor.sendMessage(output);
        }

        /**
         * Lists all current teams
         */
        private static void teamsList(Player executor) {
            if (SiegePlugin.gamedata.raiderTeams.isEmpty()) {
                executor.sendMessage("[accent]There are currently no Raider teams.");
                return;
            }

            SiegePlugin.gamedata.raiderTeams.forEach(team -> {
                executor.sendMessage("\n[accent]Team ID: [blue]" + team.id);

                team.players.forEach(player -> {
                    executor.sendMessage("      [accent]Player ID: [blue]" + player.id + ":[white] " + player.name);
                });
            });

            executor.sendMessage("");
        }

        /**
         * Invites a player or accepts a join request to the executor's team
         */
        private static void teamsInvite(Player executor, String targetPlayer) {
            // Find player with associated name
            // Invite invitee to player's team
            // TODO
        }

        /**
         * Requests to join, or accepts an invitation to a team
         */
        private static void teamsJoin(Player executor, String teamID) {
            // Find team with id
            // Place request to join team
            // TODO
        }

        private static void teamsQuit(Player executor) {
            for (RaiderTeam team : SiegePlugin.gamedata.raiderTeams) {
                if (team.players.contains(executor)) {
                    team.players.remove(executor);
                    executor.sendMessage("[accent]Left team [blue]" + team.id);
                    return;
                }
            }

            executor.sendMessage("[accent]You are not currently in a team.");
        }

        private static void teamsCreate(Player executor) {
            if (getTeam(executor) != null) {
                executor.sendMessage("[red]You are already in a team!");
            }

            RaiderTeam newTeam = new RaiderTeam(executor);

            executor.sendMessage("[accent]Team created. ID: [blue]" + newTeam.id);

            SiegePlugin.gamedata.raiderTeams.add(newTeam);
        }

        private static void teamsOpen(Player executor) {
            RaiderTeam team = getTeam(executor);

            if (team == null) {
                executor.sendMessage("[red]You are not currently in a team!");
                return;
            }

            SiegePlugin.announce("Team " + team.id + " opened. Anyone can now join this team.");
            team.open = true;
        }

        private static void teamsClose(Player executor) {
            RaiderTeam team = getTeam(executor);

            if (team == null) {
                executor.sendMessage("[red]You are not currently in a team!");
                return;
            }

            SiegePlugin.announce("Team " + team.id + " closed. Joins now possible only by request.");
            team.open = false;
        }

        private static void teamsKick(Player executor, String targetPlayer) {
            // Start a vote to kick the target player from your team
            // TODO
        }
    }
}
