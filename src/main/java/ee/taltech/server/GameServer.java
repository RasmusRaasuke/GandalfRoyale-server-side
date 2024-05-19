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

public class GameServer {
    public final Server server;
    public final Map<Integer, Integer> connections;
    public final Map<Integer, Lobby> lobbies;
    public final Map<Integer, Game> games;

    private final Map<Integer, Thread> gameThreads = new ConcurrentHashMap<>();

    public final List<Integer> connectionsToRemove;
    public final List<Integer> lobbiesToRemove;
    public final List<Integer> gamesToRemove;
    public final Map<Integer, Lobby> playersToRemoveFromLobbies;

    /**
     * Main constructor for the server.
     */
    public GameServer() {
        this.lobbies = new HashMap<>(); // Contains gameIds: lobby
        this.connections = new HashMap<>(); // Contains playerId: gameId
        this.games = new HashMap<>(); // Contains gameIds: game

        this.server = new Server();

        // Removal lists for avoiding concurrent modification
        connectionsToRemove = new ArrayList<>();
        lobbiesToRemove = new ArrayList<>();
        gamesToRemove = new ArrayList<>();
        playersToRemoveFromLobbies = new HashMap<>();

        Grid.setGrid(Grid.readGridFromFile()); // Read and set grid from the file

        server.start();
        try { // Establishes a connection with ports
            server.bind(8080, 8081);
        } catch (IOException e) {
            throw new NoSuchElementException(e);
        }

        GlobalTickrate globalTick = new GlobalTickrate(); // Create a running TPS loop.
        Thread tickRateThread = new Thread(globalTick); // Run TPS parallel to other processes.
        tickRateThread.start();

        registerKryos(); // Add sendable data structures.
        server.addListener(new ServerListener(this)); // Creates a new listener, to listen to messages and connections.
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
        // *--------------- REMOVE CONNECTION ---------------*
        for (Integer connection : connectionsToRemove) {
            connections.remove(connection);
        }
        connectionsToRemove.clear();

        // *--------------- REMOVE LOBBIES ---------------*
        for (Integer lobbyId : lobbiesToRemove) {
            lobbies.remove(lobbyId);
        }
        lobbiesToRemove.clear();

        // *--------------- REMOVE GAMES ---------------*
        for (Integer gameId : gamesToRemove) {
            games.remove(gameId);
            // Potentially stop the associated thread here
            Thread gameThread = gameThreads.remove(gameId);
            if (gameThread != null) {
                try {
                    gameThread.interrupt();
                    gameThread.join(1000); // Wait up to 1 second

                } catch (InterruptedException e) {
                    System.err.println("Error stopping game thread: " + e.getMessage());
                }
            }
        }
        gamesToRemove.clear();

        // *--------------- REMOVE PLAYERS FROM LOBBIES ---------------*
        for (Map.Entry<Integer, Lobby> lobbyEntry : playersToRemoveFromLobbies.entrySet()) {
            Integer playerId = lobbyEntry.getKey();
            Lobby lobby = lobbyEntry.getValue();
            lobby.removePlayer(playerId);
        }
        playersToRemoveFromLobbies.clear();
    }

    public void startGame(Game game) {
        Thread gameThread = new Thread(new TickRateLoop(game, this));
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
}
