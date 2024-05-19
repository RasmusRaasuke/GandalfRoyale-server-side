package ee.taltech.server;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;
import com.esotericsoftware.kryonet.Server;
import ee.taltech.server.ai.Grid;
import ee.taltech.server.network.ServerListener;
import ee.taltech.server.network.messages.game.*;
import ee.taltech.server.network.messages.lobby.*;
import ee.taltech.server.components.Game;
import ee.taltech.server.components.Lobby;
import ee.taltech.server.components.ItemTypes;
import ee.taltech.server.world.MapObjectData;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GameServer {
    public final Server server;
    public ConcurrentHashMap<Integer, Integer> connections;
    public ConcurrentHashMap<Integer, Lobby> lobbies;
    public ConcurrentHashMap<Integer, Game> games;

    private ConcurrentHashMap<Integer, Thread> gameThreads;

    public ConcurrentLinkedQueue<Integer> connectionsToRemove;
    public ConcurrentLinkedQueue<Integer> lobbiesToRemove;
    public ConcurrentLinkedQueue<Integer> gamesToRemove;
    public ConcurrentHashMap<Integer, Lobby> playersToRemoveFromLobbies;

    /**
     * Main constructor for the server.
     */
    public GameServer() {
        this.lobbies = new ConcurrentHashMap<>(); // Contains gameIds: lobby
        this.connections = new ConcurrentHashMap<>(); // Contains playerId: gameId
        this.games = new ConcurrentHashMap<>(); // Contains gameIds: game
        this.gameThreads = new ConcurrentHashMap<>();

        this.server = new Server();

        // Removal lists for avoiding concurrent modification
        connectionsToRemove = new ConcurrentLinkedQueue<>();
        lobbiesToRemove = new ConcurrentLinkedQueue<>();
        gamesToRemove = new ConcurrentLinkedQueue<>();
        playersToRemoveFromLobbies = new ConcurrentHashMap<>();

        Grid.setGrid(Grid.readGridFromFile()); // Read and set grid from the file

        server.start();
        try { // Establishes a connection with ports
            server.bind(8080, 8081);
        } catch (IOException e) {
            throw new NoSuchElementException(e);
        }

        registerKryos(); // Add sendable data structures.                                     ,
        server.addListener(new ServerListener(this)); // Creates a new listener, to listen to messages and connections.

        globalLoop();
    }

    /**
     * Method for creating communication channels with the clients.
     * This should be identical to the client side, else it won't work.
     */
    public void registerKryos() {
        // For registering allowed sendable data objects.
        Kryo kryo = server.getKryo();
        kryo.register(java.util.ArrayList.class);
        kryo.register(MapObjectData.class);
        kryo.register(float[].class);
        kryo.register(PlayZoneCoordinates.class);
        kryo.register(Position.class);
        kryo.register(ActionTaken.class);
        kryo.register(Join.class);
        kryo.register(Leave.class);
        kryo.register(LobbyCreation.class);
        kryo.register(LobbyDismantle.class);
        kryo.register(GetLobbies.class);
        kryo.register(StartGame.class);
        kryo.register(PlayZoneUpdate.class);
        kryo.register(KeyPress.class);
        kryo.register(ItemTypes.class);
        kryo.register(MouseClicks.class);
        kryo.register(KeyPress.Action.class);
        kryo.register(Position.class);
        kryo.register(SpellPosition.class);
        kryo.register(SpellDispel.class);
        kryo.register(UpdateHealth.class);
        kryo.register(UpdateMana.class);
        kryo.register(ItemPickedUp.class);
        kryo.register(CoinPickedUp.class);
        kryo.register(HealingPotionUsed.class);
        kryo.register(ItemDropped.class);
        kryo.register(MobPosition.class);
        kryo.register(UpdateMobHealth.class);
        kryo.register(GameLeave.class);
        kryo.register(GameOver.class);
        kryo.register(GameLoaded.class);
        kryo.addDefaultSerializer(KeyPress.Action.class, DefaultSerializers.EnumSerializer.class);
        kryo.addDefaultSerializer(ItemTypes.class, DefaultSerializers.EnumSerializer.class);
    }

    public void globalTick() { // A new method for global updates
        System.out.println(games);
        System.out.println(lobbies);
        // *--------------- REMOVE CONNECTION ---------------*
        if (!connectionsToRemove.isEmpty()) {
            for (Integer connection : connectionsToRemove) {
                connections.remove(connection);
            }
            connectionsToRemove.clear();
        }

        // *--------------- REMOVE LOBBIES ---------------*
        if (!lobbiesToRemove.isEmpty()) {
            for (Integer lobbyId : lobbiesToRemove) {
                lobbies.remove(lobbyId);
            }
            lobbiesToRemove.clear();
        }

        // *--------------- REMOVE GAMES ---------------*
        if (!gamesToRemove.isEmpty()) {
            for (Integer gameId : gamesToRemove) {
                games.remove(gameId);
                // Potentially stop the associated thread here
                Thread gameThread = gameThreads.remove(gameId);
                if (gameThread != null) {
                    gameThread.interrupt();
                }
            }
            gamesToRemove.clear();
        }

        // *--------------- REMOVE PLAYERS FROM LOBBIES ---------------*
        if (!playersToRemoveFromLobbies.isEmpty()) {
            for (Map.Entry<Integer, Lobby> lobbyEntry : playersToRemoveFromLobbies.entrySet()) {
                Integer playerId = lobbyEntry.getKey();
                Lobby lobby = lobbyEntry.getValue();
                lobby.removePlayer(playerId);
            }
            playersToRemoveFromLobbies.clear();
        }
    }

    public void globalLoop() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                globalTick();
            }
        }, 0, 1000);
    }

    public void startGame(Game game) {
        System.out.println("game is on going");
        Thread gameThread = new Thread(new GameTickrateLoop(game, this, gamesToRemove));
        gameThreads.put(game.gameId, gameThread);
        gameThread.start();
    }


    /**
     * Start server.
     *
     * @param args empty
     */
    public static void main(String[] args) {
        Headless.loadHeadless();
        new GameServer();
    }

    public void gamesToRemove(Integer gameId) {
    }
}
