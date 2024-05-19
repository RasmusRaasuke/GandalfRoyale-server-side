package ee.taltech.server;

import ee.taltech.server.components.Constants;
import ee.taltech.server.components.Game;
import ee.taltech.server.entities.Mob;
import ee.taltech.server.entities.PlayerCharacter;
import ee.taltech.server.entities.Spell;
import ee.taltech.server.entities.spawner.EntitySpawner;
import ee.taltech.server.network.messages.game.*;

import java.util.concurrent.ConcurrentLinkedQueue;

public class GameTickrateLoop implements Runnable {
    private final Game game;
    private final GameServer gameServer;
    private final ConcurrentLinkedQueue<Integer> gamesToRemove;
    public volatile boolean running = true;

    /**
     * @param game One game instance.
     */
    public GameTickrateLoop(Game game, GameServer gameServer, ConcurrentLinkedQueue<Integer> gamesToRemove) {
        this.game = game;
        this.gameServer = gameServer;
        this.gamesToRemove = gamesToRemove;
    }

    /**
     * The main loop of the TPS method.
     * This loop is run TPS/s.
     */
    public void run() {
        // TPS means ticks per second.
        double tps = 60;
        // Time since last tick.
        double delta = 0;
        // Time since last update
        long lastTime = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            // Logic to implement TPS in a second.
            delta += (now - lastTime) / (1000000000.0 / tps);
            lastTime = now;
            while (delta >= 1) {
                // Tick() to send out a tick calculation.
                tick();
                delta--;
            }
        }
    }

    /**
     * Method is called out every tick (in run() method).
     * Contains logic that needs to be updated every tick.
     */
    public void tick() {
        // *--------------- GAME LOOPS ---------------*
        if (game.loaded()) {

            // Item spawning to the world
            if (game.getStaringTicks() <= Constants.TICKS_TO_START_GAME) game.addTick(true);
            if (game.getStaringTicks() == Constants.TICKS_TO_START_GAME) { // Trigger only once
                new EntitySpawner(game);
                game.sendPlayZoneCoordinates();
            }


            for (PlayerCharacter player : game.gamePlayers.values()) {
                if (!game.getDeadPlayers().containsValue(player)) {
                    player.updatePosition();
                }
                if (player.getCollidingWithMob()) {
                    game.damagePlayer(player.playerID, Constants.MOB_DMG_PER_TIC);
                }
                if (player.mana != 100) {
                    player.regenerateMana();
                }
                if (player.getHealingTicks() > 0) {
                    player.regenerateHealth();
                }
                if (!game.getPlayZone().areCoordinatesInZone((int) player.getXPosition(), (int) player.getYPosition())
                        && !game.getDeadPlayers().containsValue(player)) {
                    game.damagePlayer(player.getPlayerID(), Constants.ZONE_DMG_PER_TIC);
                }
                for (Integer playerId : game.lobby.players) {
                    gameServer.server.sendToUDP(playerId, new Position(player.playerID, player.getXPosition(), player.getYPosition()));
                    gameServer.server.sendToUDP(playerId, new UpdateHealth(player.playerID, (int) player.health));
                    gameServer.server.sendToUDP(playerId, new UpdateMana(player.playerID, player.mana));
                    gameServer.server.sendToUDP(playerId, new PlayZoneUpdate(game.getPlayZone().getTimer(), game.getPlayZone().stage()));
                    gameServer.server.sendToUDP(playerId, new ActionTaken(player.playerID, player.getMouseLeftClick(),
                            game.gamePlayers.get(player.playerID).mouseXPosition,
                            game.gamePlayers.get(player.playerID).mouseYPosition));
                }
            }
            for (Spell spell : game.spells.values()) {
                spell.updatePosition();

                // Remove spells that are out of the world
                if (0 > spell.getSpellXPosition() || spell.getSpellXPosition() > 300
                        || 0 > spell.getSpellYPosition() || spell.getSpellYPosition() > 300) {
                    game.removeSpell(spell.getSpellId());
                }
                for (Integer playerId : game.lobby.players) {
                    gameServer.server.sendToUDP(playerId, new SpellPosition(spell.getPlayerId(), spell.getSpellId(),
                            spell.getSpellXPosition(), spell.getSpellYPosition(), spell.getType()));
                }
            }
            for (Mob mob : game.mobs.values()) {
                mob.updatePosition();
                for (Integer playerId : game.lobby.players) {
                    gameServer.server.sendToUDP(playerId, new MobPosition(mob.getId(), mob.getXPosition(), mob.getYPosition()));
                    gameServer.server.sendToUDP(playerId, new UpdateMobHealth(mob.getId(), mob.getHealth()));
                }

                if (mob.getHealth() == 0) {
                    game.mobsToRemove.add(mob);
                }
            }
            // End the game
            if (game.gamePlayers.size() - game.getDeadPlayers().size() <= 1) { // If last player is alive
                // Let the game finish it's logic before ending
                if (game.getEndingTicks() < Constants.TICKS_TO_END_GAME) game.addTick(false);
                if (game.getEndingTicks() == Constants.TICKS_TO_END_GAME) {
                    game.endGame(); // End the game
                    gamesToRemove.add(game.gameId); // Remove the game from the sever
                }
            }
            game.update();
        }
    }
}
