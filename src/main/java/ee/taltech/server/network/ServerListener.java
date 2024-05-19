package ee.taltech.server.network;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import ee.taltech.server.GameServer;
import ee.taltech.server.components.ItemTypes;
import ee.taltech.server.entities.Spell;
import ee.taltech.server.network.messages.game.GameLeave;
import ee.taltech.server.network.messages.game.GameLoaded;
import ee.taltech.server.network.messages.game.KeyPress;
import ee.taltech.server.network.messages.game.MouseClicks;
import ee.taltech.server.network.messages.lobby.*;
import ee.taltech.server.entities.PlayerCharacter;
import ee.taltech.server.components.Game;
import ee.taltech.server.components.Lobby;

public class ServerListener extends Listener {
    private GameServer server;

    /**
     * @param server GameServer, that holds the main data.
     */
    public ServerListener(GameServer server) {
        this.server = server;
    }

    /**
     * Creates a new Player on connection and adds it to the players list.
     * @param connection Contains info of the connection with client.
     */
    @Override
    public void connected(Connection connection) {
        server.connections.put(connection.getID(), -1); // When player connects they have no game ID yet so -1
    }

    /**
     * If a message is received the method is activated.
     * Every message has its sender connection(ID used mainly) and Data being sent.
     *
     * @param connection   Connection with the client.
     * @param incomingData Incoming data from the client.
     */
    @Override
    public void received(Connection connection, Object incomingData) {
        Class<?> dataClass = incomingData.getClass();
        if (dataClass == KeyPress.class || dataClass == MouseClicks.class || dataClass == GameLeave.class){
            gameMessagesListener(connection, incomingData); // If message is associated with game
        } else if (dataClass == LobbyCreation.class || dataClass == Join.class || dataClass == Leave.class
                || dataClass == GetLobbies.class || dataClass == StartGame.class) {
            lobbyMessagesListener(connection, incomingData); // If message is associated with lobby
        }
    }

    /**
     * Listen and react to game messages.
     *
     * @param connection connection that sent the message
     * @param incomingData message that was sent
     */
    private void gameMessagesListener(Connection connection, Object incomingData) {
        Integer gameId = server.connections.get(connection.getID());
        Game game = server.games.get(gameId);
        if (game != null) {
            PlayerCharacter player = game.gamePlayers.get(connection.getID());
            switch (incomingData) {
                case KeyPress key: // On KeyPress message
                    if (player != null) {
                        // Set the direction player should be moving.
                        if (key.action.equals(KeyPress.Action.UP) || key.action.equals(KeyPress.Action.DOWN)
                                || key.action.equals(KeyPress.Action.LEFT) || key.action.equals(KeyPress.Action.RIGHT)) {
                            player.setMovement(key);
                        } else {
                            game.setPlayerAction(key, player);
                        }
                    }
                    break;
                case MouseClicks mouse: // On MouseClicks message
                    if (player != null) {
                        // Set the direction player should be moving.
                        player.setMouseControl(mouse.leftMouse, (int) mouse.mouseXPosition, (int) mouse.mouseYPosition, mouse.type);

                        // *------------- HEALING POTION -------------*
                        if (mouse.type == ItemTypes.HEALING_POTION) {
                            game.healPlayer(player.playerID, mouse.extraField);
                        }
                        // *------------- SPELL -------------*
                        else if (mouse.type != ItemTypes.NOTHING && mouse.leftMouse) {
                            Spell spell = null;
                            if (mouse.type == ItemTypes.FIREBALL && player.mana >= 25) {
                                spell = new Spell(player, mouse.mouseXPosition, mouse.mouseYPosition, mouse.type);
                            } else if (mouse.type == ItemTypes.PLASMA && player.mana >= 15) {
                                spell = new Spell(player, mouse.mouseXPosition, mouse.mouseYPosition, mouse.type);
                            } else if (mouse.type == ItemTypes.METEOR && player.mana >= 33) {
                                spell = new Spell(player, mouse.mouseXPosition, mouse.mouseYPosition, mouse.type);
                            } else if (mouse.type == ItemTypes.KUNAI && player.mana >= 50) {
                                spell = new Spell(player, mouse.mouseXPosition, mouse.mouseYPosition, mouse.type);
                            }
                            if (spell != null) {
                                game.addSpell(spell);
                            }
                        }
                    }
                    break;
                case GameLeave message: // On GameLeave message
                    game.damagePlayer(message.playerID, 100); // Kill the player if they leave
                    server.playersToRemoveFromLobbies.put(message.playerID, game.lobby);
                    break;
                case GameLoaded message:
                    if (message.isLoaded()) {
                        game.addReadyPlayer(connection.getID());
                    }
                    break;
                default: // Ignore everything else
                    break;
            }
        }
    }

    /**
     * Listen and react to lobby messages.
     *
     * @param connection connection that sent the message
     * @param incomingData message that was sent
     */
    private void lobbyMessagesListener(Connection connection, Object incomingData) {
        Lobby lobby;
        // Triggers every time data is sent from client to server
        switch (incomingData) {
            case LobbyCreation createLobby:
                Lobby newLobby = new Lobby(createLobby.gameName, createLobby.hostId); // A new lobby is made
                server.lobbies.put(newLobby.lobbyId, newLobby); // Lobby is added to the whole lobbies list.
                server.server.sendToAllTCP(new LobbyCreation(createLobby.gameName, createLobby.hostId, newLobby.lobbyId));
                break;
            case Join joinMessage:
                // If a player joins a specific lobby shown on the screen.
                lobby = server.lobbies.get(joinMessage.gameId); // Get the lobby specified in the message.
                // Don't add more than 10 players to the lobby.
                if (lobby.players.size() < 10) {
                    lobby.addPlayer(joinMessage.playerId); // Player is added to the lobby.
                    server.server.sendToAllTCP(joinMessage);
                }
                break;
            case Leave leaveMessage:
                // If a player leaves the lobby.
                lobby = server.lobbies.get(leaveMessage.gameId); // Get the lobby specified in the message.
                lobby.removePlayer(leaveMessage.playerId); // Removes the player from the lobby's players list
                // Check if there are no players left in the lobby.
                if (lobby.players.isEmpty()) {
                    // Dismantle the lobby
                    server.lobbies.remove(leaveMessage.gameId); // Removes lobby from the lobbies HashMap.
                    server.server.sendToAllTCP(new LobbyDismantle(leaveMessage.gameId)); // Send out the removal of a lobby.
                } else {
                    server.server.sendToAllTCP(leaveMessage); // Send leave message to everyone connected
                }
                break;
            case GetLobbies ignored:
                //For every lobby in the HashMap, send out a GetLobbies message.
                for (Lobby existingLobby : server.lobbies.values()) {
                    GetLobbies requestedLobby = new GetLobbies(existingLobby.lobbyName,
                            existingLobby.lobbyId, existingLobby.players);
                    server.server.sendToTCP(connection.getID(), requestedLobby);
                }
                break;
            case StartGame startGame:
                lobby = server.lobbies.get(startGame.gameId);
                if (lobby.players.size() > 1) { // If there are more than 1 player in lobby
                    for (Integer playerId : lobby.players) {
                        server.server.sendToTCP(playerId, startGame); // Start game for players
                    }

                    // Create new game instance and add it to games list in GameServer
                    Game game = new Game(server, lobby);
                    server.games.put(lobby.lobbyId, game);
                    server.startGame(game);
                    server.lobbies.remove(startGame.gameId); // Remove lobby from gameServer lobby's list
                    server.server.sendToAllTCP(new LobbyDismantle(startGame.gameId)); // Remove lobby for clients
                }
                break;
            default:
                break;
        }
    }

    /**
     * Removes the player from the players list.
     * Makes the client-server connection disappear from the listener.
     * @param connection Connection with the client.
     */
    @Override
    public void disconnected(Connection connection) {
        // Triggers when client disconnects from the server.

        Game game = server.games.get(server.connections.get(connection.getID()));
        if (game != null) { // Kill the player if they close the game
            game.damagePlayer(connection.getID(), 100); // Kill the player
        }

        // Remove player from lobby
        for (Lobby lobby : server.lobbies.values()) {
            if (lobby.players.contains(connection.getID())) {
                server.playersToRemoveFromLobbies.put(connection.getID(), lobby);
                if (lobby.players.size() == 1) {
                    server.server.sendToAllTCP(new LobbyDismantle(lobby.lobbyId));
                } else {
                    server.server.sendToAllTCP(new Leave(lobby.lobbyId, connection.getID()));
                }
            }
        }

        server.connectionsToRemove.add(connection.getID()); // Remove player from connections
        super.disconnected(connection);
    }
}

