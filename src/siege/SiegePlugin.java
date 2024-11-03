package siege;

import arc.*;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.struct.Seq;
import arc.util.*;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.type.ItemSeq;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.modules.ItemModule;

import static mindustry.Vars.world;

public final class SiegePlugin extends Plugin {

    @Override
    public void init() {
        System.out.println("SiegePlugin loaded");

        Gamedata.reset();
        Setup.reset();

        Events.run(EventType.Trigger.update, SiegePlugin::update);

        Events.on(EventType.ResetEvent.class, hostEvent -> {
            System.out.println("ResetEvent Reset");
            Gamedata.reset();
            Setup.reset();
        });

        Events.on(EventType.PlayerConnect.class, event -> {
            //
        });

        Events.on(EventType.PlayerJoin.class, event -> {
            PersistentPlayer.fromPlayer(event.player).online = true;
            joinMessage(event.player);
        });

        Events.on(EventType.PlayerLeave.class, event -> {
            PersistentPlayer.fromPlayer(event.player).online = false;
            PersistentPlayer.fromPlayer(event.player).lastSeen = System.currentTimeMillis();
        });

        Events.on(EventType.BlockDestroyEvent.class, event -> {
            if (Constants.CORE_TYPES.contains(event.tile.build.block)) {
                coreDestroy(event.tile.build);
            }
        });

        Events.on(EventType.BuildSelectEvent.class, event -> {
            if (!Gamedata.gameStarted || (!event.breaking && Gamedata.getDeadZone(new Point2(event.tile.x, event.tile.y)))) {
                world.tile(event.tile.x, event.tile.y).setNet(Blocks.worldProcessor, Team.blue, 0);
                world.tile(event.tile.x, event.tile.y).setNet(Blocks.air);
            }
        });

        Events.on(EventType.BlockBuildEndEvent.class, event -> {
            if (event.tile.build.block instanceof CoreBlock) {
                Gamedata.reloadCore((CoreBlock.CoreBuild) event.tile.build);
            }
        });

        Events.on(EventType.TapEvent.class, event -> {
            if (event.tile.build != null && event.tile.build.block == Blocks.vault) {
                attemptCore(event.tile.build, event.player);
            }
        });
    }

    private static void update() {
        try {
            if (!Gamedata.gameOver) {
                alwaysUpdate();

                if (!Gamedata.gameStarted) {
                    Setup.update();
                } else if (!Gamedata.gameOver) {
                    gameUpdate();
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            Gamedata.dataDump();
            announce("[red]Exception thrown during tick update");
        }
    }

    /**
     * Called when a core is destroyed.
     * @param core The core which was destroyed
     */
    public static void coreDestroy(Building core) {
        // Notify team's players about dead core
        for (Player player : Groups.player) {
            if (player.team() == core.team) {
                player.sendMessage("[purple]Core destroyed at " + core.tileX() + ", " + core.tileY() + "!");
            }
        }

        Time.run(20f, () -> Gamedata.reloadCore((CoreBlock.CoreBuild) core));
    }

    /**
     * Attempts to convert a vault into a core. Only succeeds when rules are followed.
     * @param vault The vault to possibly convert into a core
     * @param executor The player attempting to build the core. Can be null to execute with no feedback.
     * @return Whether the vault was converted into a core
     */
    public static boolean attemptCore(Building vault, Player executor) {
        Team team = vault.team();
        ItemModule vaultContents = vault.items.copy();
        ItemModule coreContents = team.items().copy();
        // Can the vault satisfy local costs?
        for (ItemStack items : Constants.CONSTANT_CORE_PRICE_LOCAL) {
            if (!vaultContents.has(items.item, items.amount)) {
                if (executor != null) {
                    executor.sendMessage("[red]Could not build core. A vault needs to have " + Utilities.itemSeqToString(Constants.CONSTANT_CORE_PRICE_LOCAL) + " inside it in order to become a core.");
                }
                return false;
            }
        }
        // Spend local price and add remaining contents to core
        vaultContents.remove(Constants.CONSTANT_CORE_PRICE_LOCAL);
        coreContents.add(vaultContents);
        // Find total core-consuming price
        Seq<CoreBlock.CoreBuild> cores = team.cores();
        ItemSeq globalPrice = Constants.CONSTANT_CORE_PRICE_GLOBAL.copy();
        for (int i = 0; i < cores.size; i++) {
            globalPrice.add(Constants.PER_CORE_PRICE);
        }
        // Find harmonic distance
        double divisor = 0;
        for (CoreBlock.CoreBuild core : cores) {
            double dx = vault.tileX() - core.tileX();
            double dy = vault.tileY() - core.tileY();
            double dist2 = dx * dx + dy * dy + 1;
            divisor += 1.0f / Math.sqrt(dist2);
        }
        double harmonicFactor = Mathf.pow((float)cores.size, Constants.HARMONIC_CORE_COUNT_POWER_FACTOR) / divisor;
        globalPrice.add(Utilities.multiplyItemSeq(Constants.HARMONIC_DISTANCE_CORE_PRICE, harmonicFactor));
        // Can the team afford the price?
        for (ItemStack items : globalPrice) {
            if (!team.items().has(items.item, items.amount)) {
                // Does not have enough.
                if (executor != null) {
                    ItemSeq remaining = new ItemSeq();
                    for (ItemStack itemStack : globalPrice) {
                        if (!team.items().has(itemStack.item, itemStack.amount)) {
                            remaining.add(new ItemStack(itemStack.item, itemStack.amount - team.items().get(itemStack.item)));
                        }
                    }
                    executor.sendMessage("[red]Could not build core. You are missing " + Utilities.itemSeqToString(remaining) + ".");
                }
                return false;
            }
        }
        coreContents.remove(globalPrice);
        team.items().set(coreContents);

        final Block core = Blocks.coreShard;
        vault.tile.setNet(core, team, 0);
        Gamedata.reloadCore((CoreBlock.CoreBuild) vault.tile.build);

        return true;
    }

    /**
     * Ends the current game and loads the next.
     * @param winner The game's winner. 0 if the Citadel wins, otherwise is the ID of the winning team. -1 if the game is ended without a winner.
     */
    public static void endGame(int winner) {
        Gamedata.gameOver = true;

        if (winner == 0) {
            announce("[accent]The [green]Citadel[] has won the game!");
        } else if (winner == -1) {
            announce("[accent]Game ended without a winner.");
        } else {
            for (RaiderTeam team : Gamedata.raiderTeams) {
                if (team.id == winner) {
                    announce("[accent]Team " + team.stringID + " has won the game!");
                    break;
                }
            }
        }
        Time.run(1.5f * 60f, () -> {
            //Events.fire(EventType.GameOverEvent.class);
            // Only way I know how to end the game
            for (CoreBlock.CoreBuild core : Team.green.cores()) {
                core.tile.setAir();
            }
        });
    }

    private static void joinMessage(Player player) {
        if (RaiderTeam.getTeam(PersistentPlayer.fromPlayer(player)) != null) {
            return;
        }
        if (Gamedata.gameStarted) {
            player.sendMessage("[sky]Welcome to Siege! Siege is developed and hosted by the Apricot Conservation Project. You have joined after the beginning of the game, meaning you are on the Citadel team. To learn more about the Siege gamemode, run /siege. Have fun, and good luck!");
        } else if (!Gamedata.teamSetupPhase()) {
            player.sendMessage("[sky]Welcome to Siege! Siege is developed and hosted by the Apricot Conservation Project. You have joined after teams were determined, meaning you are on the Citadel team. The game will begin in " + (-Gamedata.elapsedTimeSeconds()) + " seconds. To learn more about the Siege gamemode, run /siege. Have fun, and good luck!");
        } else {
            player.sendMessage("[sky]Welcome to Siege! Siege is developed and hosted by the Apricot Conservation Project. Team setup is currently ongoing, if you would like to create or join a team, run /team. Team setup will end in " + (-Gamedata.elapsedTimeSeconds() - Constants.CORE_PLACEMENT_TIME_SECONDS) + " seconds. To learn more about the Siege gamemode, run /siege. Have fun, and good luck!");
        }
    }

    // Manages constant processes that happen always
    private static void alwaysUpdate() {
        checkTeams();
    }

    // Manages constant processes during the course of a game (does not run during setup or during game over)
    private static void gameUpdate() {
        deadZoneDamage();
    }

    private static void deadZoneDamage() {
        for (Unit unit : Groups.unit) {
            if (Gamedata.getDeadZone(unit.tileOn())  &&  !Constants.DEAD_ZONE_IMMUNE_TYPES.contains(unit.type)) {
                unit.health -= Constants.DEAD_ZONE_DAMAGE_CONSTANT_TICK + unit.maxHealth * Constants.DEAD_ZONE_DAMAGE_PERCENT_TICK;
                if (unit.health <= 0.0f && !unit.dead) {
                    unit.kill();
                }
            }
        }
    }

    private static void checkTeams() {
        Gamedata.raiderTeams.removeIf(team -> team.players.isEmpty());

        // Ensure players are in the correct team
        // Setup handles in case of team setup phase
        if (!Gamedata.teamSetupPhase()) {
            for (Player player : Groups.player) {
                PersistentPlayer persistentPlayer = PersistentPlayer.fromPlayer(player);
                RaiderTeam team = RaiderTeam.getTeam(persistentPlayer);
                if (team != null) {
                    if (team.mindustryTeam == null) {
                        continue;
                    }
                    if (player.team().id != team.mindustryTeam.id) {
                        player.team(team.mindustryTeam);
                    }
                } else if (Gamedata.gameStarted) {
                    player.team(Team.green);
                } else {
                    player.team(Team.blue);
                }
            }
        }

        if (Gamedata.gameStarted) {
            // Gamedata.raiderTeams may be modified inside this loop.
            RaiderTeam[] teams = Gamedata.raiderTeams.toArray(new RaiderTeam[0]);
            for (RaiderTeam team : teams) {
                if (team.mindustryTeam.cores().size == 0) {
                    team.destroy();
                }
            }

            if (Gamedata.raiderTeams.isEmpty()) {
                endGame(0);
            }
        }
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        RaiderTeam.Commands.registerCommands(handler);
        handler.<Player>register("siege", "Explain the Siege gamemode", (args, player) -> siegeHelp(player));
        handler.<Player>register("reset", "debug reset game", (args, player) -> {
            System.out.println("SiegePlugin reset");
            Gamedata.reset();
            Setup.reset();
        });
        handler.<Player>register("wincitadel", "debug force citadel win", (args, player) -> {
            System.out.println("Citadel win forced");
            for (RaiderTeam team : Gamedata.raiderTeams) {
                for (CoreBlock.CoreBuild core : team.mindustryTeam.cores()) {
                    core.tile.setAir();
                }
            }
        });
        handler.<Player>register("winraider", "debug force raider win", (args, player) -> {
            System.out.println("Raider win forced");
            for (CoreBlock.CoreBuild core : Team.green.cores()) {
                core.tile.setAir();
            }
        });
        handler.<Player>register("money", "free money", (args, player) -> {
            Team team = player.team();
            team.items().add(new ItemSeq(ItemStack.list(
                    Items.copper, 10000,
                    Items.lead, 10000,
                    Items.metaglass, 10000,
                    Items.graphite, 10000,
                    Items.sand, 10000,
                    Items.coal, 10000,
                    Items.titanium, 10000,
                    Items.thorium, 10000,
                    Items.scrap, 10000,
                    Items.silicon, 10000,
                    Items.plastanium, 10000,
                    Items.phaseFabric, 10000,
                    Items.surgeAlloy, 10000,
                    Items.sporePod, 10000,
                    Items.blastCompound, 10000,
                    Items.pyratite, 10000
            )));
        });
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

    private static void siegeHelp(Player executor) {
        executor.sendMessage("Not implemented");
        // TODO write a brief text to explain siege
    }



    public static void announce(String message) {
        System.out.println("Announced: " + message);
        for (Player player : Groups.player) {
            player.sendMessage(message);
        }
    }
}
