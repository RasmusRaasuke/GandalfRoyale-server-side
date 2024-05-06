package ee.taltech.server;

import com.esotericsoftware.kryonet.Server;
import ee.taltech.server.components.Constants;
import ee.taltech.server.components.ItemTypes;
import ee.taltech.server.entities.Item;
import ee.taltech.server.entities.Mob;
import ee.taltech.server.entities.Spell;
import ee.taltech.server.network.messages.game.*;
import ee.taltech.server.entities.PlayerCharacter;

import ee.taltech.server.components.Game;

public class TickRateLoop implements Runnable {
    private volatile boolean running = true;
    private Server server;
    private GameServer gameServer;

    /**
     * @param server The whole gameServer instance to access the servers contents.
     */
    public TickRateLoop(Server server, GameServer gameServer) {
        this.server = server;
        this.gameServer = gameServer;
        // Whole list for the players in the list.
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
        // This function activates, according to ticks per second.
        // If 1 TPS, then every second.
        // Update player positions for clients that are in the same game with player
        for (Game game : this.gameServer.games.values()) {
            if (game.getTicks() < 1501) game.addTick(); // Used only to demonstrate item generation by server
            if (game.getTicks() == 1500) { // Trigger only once after 1000 ticks
                Item item1 = new Item(ItemTypes.FIREBALL, 7640 / Constants.PPM, 2940 / Constants.PPM);
                Item item2 = new Item(ItemTypes.FIREBALL, 7640 / Constants.PPM, 2910 / Constants.PPM);
                Item potion = new Item(ItemTypes.HEALING_POTION, 7640 / Constants.PPM, 2880 / Constants.PPM);
                Mob mob = new Mob(7640, 3020);

                game.addItem(item1, null);
                game.addItem(item2, null);
                game.addItem(potion, null);

                game.sendPlayZoneCoordinates();
                game.addMob(mob);
            }

            for (PlayerCharacter player : game.gamePlayers.values()) {
                if (!game.deadPlayers.containsValue(player)) {
                    player.updatePosition();
                }
                if (player.mana != 100) {
                    player.regenerateMana();
                }
                if (player.getHealingTicks() > 0) {
                    player.regenerateHealth();
                }
                if (!game.getPlayZone().areCoordinatesInZone((int) player.getXPosition(), (int) player.getYPosition())) {
                    player.receiveZoneDamage();
                }
                for (Integer playerId : game.gamePlayers.keySet()) {
                    server.sendToUDP(playerId, new Position(player.playerID, player.getXPosition(), player.getYPosition()));
                    server.sendToUDP(playerId, new UpdateHealth(player.playerID, (int) player.health));
                    server.sendToUDP(playerId, new UpdateMana(player.playerID, player.mana));
                    server.sendToUDP(playerId, new PlayZoneUpdate(game.getPlayZone().getTimer(), game.getPlayZone().stage()));
                    server.sendToUDP(playerId, new ActionTaken(player.playerID, player.getMouseLeftClick(),
                            game.gamePlayers.get(player.playerID).mouseXPosition,
                            game.gamePlayers.get(player.playerID).mouseYPosition));
                    if (game.getKilledPlayerId() != 0) {
                        server.sendToUDP(playerId, new KilledPlayer(game.getKilledPlayerId()));
                    }
                }
            }
            for (Spell spell : game.spells.values()) {
                spell.updatePosition();
                for (Integer playerId : game.gamePlayers.keySet()) {
                    server.sendToUDP(playerId, new SpellPosition(spell.getPlayerId(), spell.getSpellId(),
                                spell.getSpellXPosition(), spell.getSpellYPosition(), spell.getType()));
                }
            }
            for (Mob mob : game.mobs.values()) {
                mob.updatePosition();
                for (Integer playerId : game.gamePlayers.keySet()) {
                    server.sendToUDP(playerId, new MobPosition(mob.getId(), mob.getXPosition(), mob.getYPosition()));
                    server.sendToUDP(playerId, new UpdateMobHealth(mob.getId(), mob.getHealth()));
                }

                if (mob.getHealth() == 0) {
                    game.mobsToRemove.add(mob);
                }
            }
            game.update();
        }
    }
}
