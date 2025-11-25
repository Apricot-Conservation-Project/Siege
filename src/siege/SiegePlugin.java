package siege;

import arc.*;
import arc.func.Cons;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.*;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.game.EventType;
import mindustry.game.Rules;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.net.Administration;
import mindustry.type.ItemSeq;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.modules.ItemModule;

import java.awt.desktop.SystemEventListener;
import java.util.random.RandomGenerator;

public final class SiegePlugin extends Plugin {

    public static long PlayersLastSeen;
    public static long PlayersLastActive;

    public static RandomGenerator GENERATOR = RandomGenerator.getDefault();
    /**
     * Initialize the plugin - Runs as soon as the mod loads
     * Sets up events and rules
     */
    @Override
    public void init() {
        System.out.println("SiegePlugin loaded");

        Gamedata.reset();
        Setup.reset();
        RuleSetter.initRules();
        PlayersLastSeen = System.currentTimeMillis();
        PlayersLastActive = System.currentTimeMillis();

        Vars.netServer.admins.addActionFilter((action) -> {
            // Refresh AFK clock
            PersistentPlayer.fromPlayer(action.player).lastActed = System.currentTimeMillis();

            if (action.type == Administration.ActionType.placeBlock) {
                // Disallow building if:
                // Before game start
                boolean blockEarly = !Gamedata.gameStarted;
                // On "undecided" team
                boolean blockBlue = action.player.team() == Team.blue;
                // Trying to build in the dead zone
                // - This still works even before deadzone is written because the cache is always up to date
                boolean blockDeadZone = DeadZone.insideDeadZone(action.tile.x, action.tile.y, action.block, DeadZone.isUpdatingDeadZone());
                // Trying to build a turret inside of the keep
                boolean blockKeepTurrets = action.player.team() == Team.green && Keep.keepExists() && Constants.TURRET_BLOCKS.contains(action.block) && Keep.inKeep(action.tile.x, action.tile.y, action.block);
                // Trying to build a block that's currently banned for your team
                boolean blockBanned = RuleSetter.getBannedBlocks(action.player.team()).contains(action.block);
                if (blockEarly || blockBlue || blockDeadZone || blockKeepTurrets || blockBanned) {
                    return false;
                }
            }

            return true;
        });

        Events.run(EventType.Trigger.update, SiegePlugin::update);

        Events.on(EventType.ResetEvent.class, hostEvent -> {
            System.out.println("ResetEvent Reset");
            Gamedata.reset();
            Setup.reset();
            RuleSetter.initRules();
        });

        Events.on(EventType.PlayerConnect.class, event -> {
            //
        });

        Events.on(EventType.PlayerJoin.class, event -> {
            PersistentPlayer.fromPlayer(event.player).online = true;
            joinMessage(event.player);
            PersistentPlayer persistentPlayer = PersistentPlayer.fromPlayer(event.player);
            if (Gamedata.getGameState() == GameState.TeamSetup) {
                event.player.team(Team.blue);
                persistentPlayer.spawn(Team.green);
            } else {
                RaiderTeam playerTeam = RaiderTeam.getTeam(persistentPlayer);
                if (playerTeam != null) {
                    event.player.team(playerTeam.mindustryTeam);
                    persistentPlayer.spawn(playerTeam.mindustryTeam);
                } else {
                    event.player.team(Team.green);
                    persistentPlayer.spawn(Team.green);
                }
            }
        });

        Events.on(EventType.PlayerLeave.class, event -> {
            // LastSeen is not necessarily correct, but it is correct when no players are online (!player.online or Groups.Players.isEmpty)
            PersistentPlayer.fromPlayer(event.player).online = false;
            PersistentPlayer.fromPlayer(event.player).lastSeen = System.currentTimeMillis();
            PlayersLastSeen = System.currentTimeMillis();
        });

        Events.on(EventType.BlockDestroyEvent.class, event -> {
            if (Constants.CORE_TYPES.contains(event.tile.block())) {
                coreDestroy(event.tile.build);
            }
        });

        Events.on(EventType.BlockBuildBeginEvent.class, event -> {
            if (Keep.keepExists() && event.team == Team.green && Keep.inKeep(event.tile.build)) {
                // Make keep buildings invincible
                event.tile.build.health = Float.MAX_VALUE;
            }
        });

        Events.on(EventType.BlockBuildEndEvent.class, event -> {
            if (event.tile.build.block instanceof CoreBlock) {
                DeadZone.reloadCore((CoreBlock.CoreBuild) event.tile.build);
            }
        });

        Events.on(EventType.TapEvent.class, event -> {
            PersistentPlayer persistentPlayer = PersistentPlayer.fromPlayer(event.player);
            switch (persistentPlayer.clickAction) {
                case None -> {
                    break;
                }
                case Destroy -> {
                    if (event.tile.build != null && event.tile.build.team == event.player.team()) {
                        event.tile.build.kill();
                    }
                    persistentPlayer.clickAction = ClickAction.None;
                    return;
                }
                case Uncore -> {
                    if (event.tile.build != null && event.tile.build.block instanceof CoreBlock && event.tile.build.team == event.player.team()) {
                        coreDestroy(event.tile.build);
                        event.tile.build.tile.setNet(Blocks.air);
                    }
                    persistentPlayer.clickAction = ClickAction.None;
                    return;
                }
            }

            if (event.tile.build != null && event.tile.build.block == Blocks.vault) {
                if (System.currentTimeMillis() > persistentPlayer.lastAttemptedCore + 200) {
                    attemptCore(event.tile.build, event.player);
                }
                persistentPlayer.lastAttemptedCore = System.currentTimeMillis();
            }
        });

        Events.on(EventType.UnitCreateEvent.class, event -> {
            if (event.unit.team == Team.green && Keep.keepExists()) {
                event.unit.damageMultiplier(0f);
            }

            // Kill unit if not allowed
            if (RuleSetter.getBannedUnits(event.unit.team).contains(event.unit.type)) {
                event.unit.kill();
                // Call.unitDestroy(event.unit.id); TODO is this necessary for synchronization?
                announce("[orange]The unit built at [accent]" + (int)event.unit.x + ", " + (int)event.unit.y + "[] is not allowed at this time and has been killed. Run the [accent]/siege[] command to learn more.");
                return;
            }

            UnitOwner.fromUnit(event.unit); // Register the unit
            // This doesn't seem to register core units.
            // A workaround was placed in UnitOwner to automatically scan and update every unit
            // But it would be more efficient to only have to pull from a seq of UnitOwners rather than re-generate all of them from Groups.unit
            // TODO: Find a way to register every unit, including core units
        });

        Events.on(EventType.UnitDestroyEvent.class, event -> {
            UnitOwner.deregister(event.unit); // Deregister the unit
        });
    }

    /**
     * Manages all tick updates
     * Delegates to sub-functions for various game stages
     */
    private static void update() {
        int code = 0;
        try {
            RuleSetter.update(); code = 101;
            DeadZone.update(); code = 102;
            UnitOwner.update(); code = 103;

            if (!Gamedata.gameOver) {
                alwaysUpdate(); code = 201;

                if (!Gamedata.gameStarted) {
                    Setup.update(); code = 301;
                } else if (!Gamedata.gameOver) {
                    gameUpdate(); code = 401;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            Gamedata.dataDump();
            announce("[red]Exception thrown during tick update. Error code " + code);
        }
    }

    // Manages constant processes that happen always
    private static void alwaysUpdate() {
        checkTeams();
        fixUnits();

        if (Keep.keepExists() && Core.graphics.getFrameId() % 10 == 0) {
            displayKeepFx();
        }
    }

    // Manages constant processes during the course of a game (does not run during setup or during game over)
    private static void gameUpdate() {
        deadZoneDamage();
        if (Keep.keepExisted && !Keep.keepExists()) {
            Keep.keepDissolvedListener();
        }
        Keep.keepExisted = Keep.keepExists();

        if (Team.green.cores().size == 0) {
            if (Gamedata.raiderTeams.size() == 1) {
                System.out.println("ending game, raider winner, no more citadel cores");
                endGame(Gamedata.raiderTeams.get(0).id);
                return;
            } else {
                System.out.println("ending game, no winner, no more citadel cores");
                endGame(-1);
                return;
            }
        }

        // Timeout if no actions are made for long enough or if players are disconnected for too long
        if (Groups.player.isEmpty() && System.currentTimeMillis() > PlayersLastSeen + Constants.OFFLINE_TIMEOUT_PERIOD) {
            announce("[accent]All players were offline, and the game has timed out.");
            endGame(-1);
            return;
        }
        if (System.currentTimeMillis() > PlayersLastActive + Constants.AFK_TIMEOUT_PERIOD) {
            announce("[accent]All players were AFK, and the game has timed out.");
            endGame(-1);
            return;
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

        // WIP measure, later on this should be a gradual process
        Time.run(20f * 60f, () -> DeadZone.reloadCore((CoreBlock.CoreBuild) core));
    }

    /**
     * Attempts to convert a vault into a core. Only succeeds when rules are followed.
     * @param vault The vault to possibly convert into a core
     * @param executor The player attempting to build the core. Can be null to execute with no feedback.
     * @return Whether the vault was converted into a core
     */
    public static boolean attemptCore(Building vault, Player executor) {
        int lowX = vault.tile.x - 1;
        int lowY = vault.tile.y - 1;
        int highX = vault.tile.x + 1;
        int highY = vault.tile.y + 1;
        for (int x = lowX; x <= highX; x ++) {
            for (int y = lowY; y <= highY; y ++) {
                if (DeadZone.hardGetDeadZone(new Point2(x, y))) {
                    executor.sendMessage("[red]Could not build core. This vault is within the dead zone.");
                    return false;
                }
                if (Vars.world.tile(x, y).floor() == Blocks.grass.asFloor()) {
                    executor.sendMessage("[red]Could not build core. Cores cannot be built on guaranteed safe regions.");
                    return false;
                }
            }
        }

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
        if (!vault.items.equals(coreContents)) {
            vaultContents.remove(Constants.CONSTANT_CORE_PRICE_LOCAL);
            coreContents.add(vaultContents);
        }
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

        // Guarantee harmonic factor
        // See: Constants.GUARANTEED_HARMONIC_FACTOR
        ItemSeq guaranteedPrice = globalPrice.copy(); // GlobalPrice at this point is just constant and per-core price
        guaranteedPrice.add(Utilities.multiplyItemSeq(Constants.HARMONIC_DISTANCE_CORE_PRICE, Constants.GUARANTEED_HARMONIC_FACTOR));
        int byItemStorage = cores.size * Blocks.coreShard.itemCapacity;
        ItemSeq subtraction = new ItemSeq();
        for (ItemStack items : guaranteedPrice) {
            if (items.amount > byItemStorage) {
                subtraction.add( new ItemStack(items.item, -(items.amount - byItemStorage) ));
            }
        }

        globalPrice.add(Utilities.multiplyItemSeq(Constants.HARMONIC_DISTANCE_CORE_PRICE, harmonicFactor));
        globalPrice.add(subtraction);
        ItemSeq adjustedGlobalPrice = new ItemSeq();
        for (ItemStack items : globalPrice) {
            if (items.amount >= Constants.MINIMUM_CORE_PRICE_ITEMS) {
                int amount = items.amount;
                if (cores.size < Constants.RAMP_UP_CORE_COUNT) {
                    float amountRatio = (float)cores.size / (float)Constants.RAMP_UP_CORE_COUNT;
                    amount = (int) Math.ceil(amount / amountRatio);
                }
                adjustedGlobalPrice.add(new ItemStack(items.item, amount));
            }
        }
        // Can the team afford the price?
        for (ItemStack items : adjustedGlobalPrice) {
            if (!team.items().has(items.item, items.amount)) {
                // Does not have enough.
                if (executor != null) {
                    ItemSeq remaining = new ItemSeq();
                    for (ItemStack itemStack : adjustedGlobalPrice) {
                        if (!team.items().has(itemStack.item, itemStack.amount)) {
                            remaining.add(new ItemStack(itemStack.item, itemStack.amount - team.items().get(itemStack.item)));
                        }
                    }
                    executor.sendMessage("[red]Could not build core. You are missing " + Utilities.itemSeqToString(remaining) + ".");
                }
                return false;
            }
        }
        coreContents.remove(adjustedGlobalPrice);
        team.items().set(coreContents);

        final Block core = Blocks.coreShard;
        vault.tile.setNet(core, team, 0);
        DeadZone.reloadCore((CoreBlock.CoreBuild) vault.tile.build);

        return true;
    }

    /**
     * Ends the current game and loads the next.
     * @param winner The game's winner. 0 if the Citadel wins, otherwise is the ID of the winning team. -1 if the game is ended without a winner.
     */
    public static void endGame(int winner) {
        System.out.println("Ending the game");
        Gamedata.gameOver = true;
        boolean endedGame = false;

        if (winner == 0) {
            announce("[accent]The [green]Citadel[] has won the game!");
            Events.fire(new EventType.GameOverEvent(Team.green));
            endedGame = true;
        } else if (winner == -1) {
            announce("[accent]Game ended without a winner.");
        } else {
            for (RaiderTeam team : Gamedata.raiderTeams) {
                if (team.id == winner) {
                    announce("[accent]Team " + team.stringID + " has won the game!");
                    Events.fire(new EventType.GameOverEvent(team.mindustryTeam));
                    endedGame = true;
                    break;
                }
            }
        }

        if (!endedGame) {
            Events.fire(new EventType.GameOverEvent(Team.derelict));
        }
    }

    /**
     * Sends a welcome message to the specified player.
     * @param player The player to welcome
     */
    private static void joinMessage(Player player) {
        if (RaiderTeam.getTeam(PersistentPlayer.fromPlayer(player)) != null) {
            return;
        }
        if (Gamedata.gameStarted) {
            player.sendMessage("[sky]Welcome to Siege! Siege is developed and hosted by the Apricot Conservation Project. You have joined after the beginning of the game, meaning you are on the Citadel team. To learn more about the Siege gamemode, run /siege. Have fun, and good luck!");
        } else if (!Gamedata.teamSetupPhase()) {
            player.sendMessage("[sky]Welcome to Siege! Siege is developed and hosted by the Apricot Conservation Project. You have joined after teams were determined, meaning you are on the Citadel team. The game will begin in " + (-Gamedata.elapsedTimeSeconds()) + " seconds. To learn more about the Siege gamemode, run /siege. Have fun, and good luck!");
        } else {
            player.sendMessage("[sky]Welcome to Siege! Siege is developed and hosted by the Apricot Conservation Project. Team setup is currently ongoing, if you would like to create or join a Raider team, run /team. Team setup will end in " + (-Gamedata.elapsedTimeSeconds() - Constants.CORE_PLACEMENT_TIME_SECONDS) + " seconds. To learn more about the Siege gamemode, run /siege. Have fun, and good luck!");
        }
    }

    private static long previousDeadZoneCheck = 0L;
    // Inflicts a tick of dead zone damage to all units within it
    private static void deadZoneDamage() {
        if (previousDeadZoneCheck == 0L) {
            previousDeadZoneCheck = System.currentTimeMillis() - (1000 / 60);
        }
        float elapsedTimeSeconds = (System.currentTimeMillis() - previousDeadZoneCheck) / 1000f;
        float elapsedTimeTicks = elapsedTimeSeconds * 60f;
        float constantDamage = Constants.DEAD_ZONE_DAMAGE_CONSTANT_TICK * elapsedTimeTicks;
        float percentDamage = Constants.DEAD_ZONE_DAMAGE_PERCENT_TICK * elapsedTimeTicks;
        previousDeadZoneCheck = System.currentTimeMillis();
        for (Unit unit : Groups.unit) {
            if (DeadZone.getDeadZone(unit.tileOn())  &&  !Constants.DEAD_ZONE_IMMUNE_TYPES.contains(unit.type)) {
                unit.health -= constantDamage + unit.maxHealth * percentDamage;
                if (unit.health <= 0.0f && !unit.dead) {
                    unit.kill();
                }
            }
        }
    }

    // Displays an effect across the border of the keep
    private static void displayKeepFx() {
        // Put effects where the manhattan distance from center is equal to or the largest less than the keep radius
        float worldMiddleX = (Vars.world.width()-1) / 2f;
        float worldMiddleY = (Vars.world.height()-1) / 2f;
        float widthX = Constants.KEEP_RADIUS;
        if (Math.floor(worldMiddleX) != worldMiddleX) {
            widthX -= 0.5f;
        }
        for (float diffX = -widthX; diffX <= widthX; diffX += 1f) {
            float diffY = Constants.KEEP_RADIUS - Math.abs(diffX);
            float x = worldMiddleX + diffX;
            float y1 = worldMiddleY + diffY;
            float y2 = worldMiddleY - diffY;

            Call.effect(Constants.KEEP_EFFECT, x * 8f, y1 * 8f, 0, Color.green);
            if (y1 != y2) //TODO this can be done branchless
                Call.effect(Constants.KEEP_EFFECT, x * 8f, y2 * 8f, 0, Color.green);
        }
    }

    // Verifies teams contain players and sets player teams to the correct values
    private static void checkTeams() {
        if (stopteamfix) {
            return;
        }
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
            int winnerCode = 0;
            for (RaiderTeam team : teams) {
                if (team.mindustryTeam.cores().size == 0) {
                    team.destroy();
                } else if (team.TimeOffline() > Constants.OFFLINE_TIMEOUT_PERIOD) {
                    announce("[accent]Team " + team.stringID + " has timed out due to offline players.");
                    team.destroy();
                    if (!Constants.CITADEL_WINS_ON_RAIDER_TIMEOUT) {
                        winnerCode = -1;
                    }
                } else if (team.TimeAFK() > Constants.AFK_TIMEOUT_PERIOD) {
                    announce("[accent]Team " + team.stringID + " has timed out due to afk players.");
                    team.destroy();
                    if (!Constants.CITADEL_WINS_ON_RAIDER_TIMEOUT) {
                        winnerCode = -1;
                    }
                }
            }

            if (Gamedata.raiderTeams.isEmpty()) {
                endGame(winnerCode);
            }
        }
    }

    // Workaround for core unit spawning - kills them all
    // TODO find a better way PLEASE
    private static void fixUnits() {
        for (Unit unit : Groups.unit) {
            if (Constants.CORE_UNITS.contains(unit.type) && !unit.isPlayer()) {
                unit.kill();
            }
        }
    }

    public static boolean stopteamfix = false;

    @Override
    public void registerClientCommands(CommandHandler handler) {
        RaiderTeam.Commands.registerCommands(handler);
        handler.<Player>register("siege", "Explain the Siege gamemode", (args, player) -> siegeHelp(player));
        handler.<Player>register("uncore", "Remove a core without collateral damage", (args, player) -> uncore(player));
        handler.<Player>register("destroy", "Destroy a tile from your team", (args, player) -> destroy(player));
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
                    Items.copper, 100000,
                    Items.lead, 100000,
                    Items.metaglass, 100000,
                    Items.graphite, 100000,
                    Items.sand, 100000,
                    Items.coal, 100000,
                    Items.titanium, 100000,
                    Items.thorium, 100000,
                    Items.scrap, 100000,
                    Items.silicon, 100000,
                    Items.plastanium, 100000,
                    Items.phaseFabric, 100000,
                    Items.surgeAlloy, 100000,
                    Items.sporePod, 100000,
                    Items.blastCompound, 100000,
                    Items.pyratite, 100000
            )));
        });
        handler.<Player>register("core", "place a core at your location", (args, player) -> {
            Team team = player.team();
            Tile tile = Vars.world.tile(player.tileX(), player.tileY());
            tile.setNet(Blocks.coreShard, team, 0);
            DeadZone.reloadCore((CoreBlock.CoreBuild) tile.build);
        });
        handler.<Player>register("stopteamfix", "meow", (args, player) -> {
            stopteamfix = true;
        });
        handler.<Player>register("startteamfix", "meow", (args, player) -> {
            stopteamfix = false;
        });
        handler.register("datadump", "yowch", (args, player) -> {
            Gamedata.dataDump();
        });

        handler.register("test", "test", (args, player) -> {
            Events.fire(new EventType.GameOverEvent(Team.derelict));
            //Call.
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
            endgame(-1);
        });*/
    }

    private static void siegeHelp(Player executor) {
        executor.sendMessage("[orange]Siege[accent] is [purple]ACP[]'s second gamemode. Siege is a battle between a central team - the [green]Citadel[] - and a number of outer teams - the [red]Raiders[] - for dominance.\nMost of the map is covered in the Dead Zone. This is a region that very quickly kills any non-core units that attempt to navigate it. Every core creates a protective bubble around itself, and higher tier cores are able to protect more. Building cores starts off cheap, but gets more expensive with each core.\nTo make a core, a vault has to be given phase fabric and thorium, then clicked. Build more cores and expand outward to reach other teams and win!\nRaiders are encouraged to make multiple teams. During the first " + Mathf.round(Constants.GUARANTEED_KEEP_TIME_SECONDS / 60f) + " minutes of the game, or multiple raider teams are present, the center of the map becomes the Keep, a region that makes Citadel blocks invincible, but prevents them from building turrets in the area. Raiders can kill their raider opponents in order to dissolve the Keep, attack the Citadel next, and hopefully, win!");
    }

    private static void uncore(Player executor) {
        PersistentPlayer.fromPlayer(executor).clickAction = ClickAction.Uncore;
    }

    private static void destroy(Player executor) {
        PersistentPlayer.fromPlayer(executor).clickAction = ClickAction.Destroy;
    }



    public static void announce(String message) {
        System.out.println("Announced: " + message);
        for (Player player : Groups.player) {
            player.sendMessage(message);
        }
    }
}
