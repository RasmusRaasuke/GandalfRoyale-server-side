package ee.taltech.server.entities;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import ee.taltech.server.components.Constants;
import ee.taltech.server.components.ItemTypes;
import ee.taltech.server.entities.collision.CollisionBodyTypes;
import ee.taltech.server.network.messages.game.KeyPress;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerCharacter implements Entity {

    public static final Integer WIDTH = 12;
    public static final Integer HEIGHT = 24;
    private static final float HEAL_PER_TICK = 0.17f; // 10hp per sec
    private static final Integer FULL_HEALING_TICKS = 600; // 10 seconds

    private Body body;
    public float xPosition;
    public float yPosition;
    public int mouseXPosition;
    public int mouseYPosition;
    private boolean mouseLeftClick;

    public final int playerID;

    private Vector2 movement;

    public float health;
    public float mana;
    private final Map<Integer, Item> inventory;
    private Integer coins;
    private Integer healingTicks;
    public ItemTypes type;
    private boolean collidingWithMob;
    private boolean loaded;

    /**
     * Construct PlayerCharacter.
     *
     * @param playerID player's ID
     */
    public PlayerCharacter(Integer playerID) {
        // Here should be the semi-random spawn points for a PlayerCharacter
        this.loaded = false;
        this.xPosition = 8200 / Constants.PPM;
        this.yPosition = 2960 / Constants.PPM;

        this.movement = new Vector2(0f, 0f);
        this.playerID = playerID;
        health = 100;
        mana = 100;
        coins = 0;

        inventory = new HashMap<>();

        healingTicks = 0;
        collidingWithMob = false;
    }

    /**
     * Get player's ID.
     *
     * @return playerID
     */
    public int getPlayerID() {
        return playerID;
    }

    /**
     * @return Player x position
     */
    public float getXPosition() {
        if (body == null) {
            return xPosition;
        }
        return body.getPosition().x;
    }

    /**
     * @return Player y position
     */
    public float getYPosition() {
        if (body == null) {
            return yPosition;
        }
        return body.getPosition().y;
    }

    public void setSpawn(int[] coord) {
        body.setTransform(coord[0], coord[1], body.getAngle());
    }

    /**
     * Get player's left mouse click.
     *
     * @return true if left mouse click is pressed else false
     */
    public boolean getMouseLeftClick() {
        return mouseLeftClick;
    }

    /**
     * Set health value.
     *
     * @param newHealth new health value
     */
    public void setHealth(float newHealth) {
        health = newHealth;
    }

    /**
     * Set mana value.
     *
     * @param newMana new mana value
     */
    public void setMana(float newMana) {
        mana = newMana;
    }

    /**
     * Get player's healing ticks.
     *
     * @return healingTicks
     */
    public Integer getHealingTicks() {
        return healingTicks;
    }

    /**
     * Start player's healing.
     */
    public void startHealing() {
        this.healingTicks = FULL_HEALING_TICKS;
    }

    /**
     * Method sets the heading action for the player, but doesn't update the position coordinates.
     * Only use if's, because multiple buttons can be pressed simultaneously.
     *
     * @param keyPress Incoming from a client. Contains if and what button is pressed.
     */
    public void setMovement(KeyPress keyPress) {
        // Set an action where player should be headed.
        if (body != null) {
            if (keyPress.pressed) {
                if (keyPress.action == KeyPress.Action.LEFT) {
                    movement.x = -1;
                } else if (keyPress.action == KeyPress.Action.RIGHT) {
                    movement.x = 1;
                } else if (keyPress.action == KeyPress.Action.UP) {
                    movement.y = 1;
                } else if (keyPress.action == KeyPress.Action.DOWN) {
                    movement.y = -1;
                }
            } else {
                // Only cancel the movement if one key is pressed down.
                if (keyPress.action == KeyPress.Action.LEFT && movement.x < 0) {
                    movement.x = 0;
                } else if (keyPress.action == KeyPress.Action.RIGHT && movement.x > 0) {
                    movement.x = 0;
                } else if (keyPress.action == KeyPress.Action.UP && movement.y > 0) {
                    movement.y = 0;
                } else if (keyPress.action == KeyPress.Action.DOWN && movement.y < 0) {
                    movement.y = 0;
                }
            }
        }
    }

    /**
     * Update players position.
     */
    public void updatePosition() {
        // updatePosition is activated every TPS.
        if (body != null) {
            // One key press distance that a character travels.
            Vector2 scaledMovement = movement.cpy().scl(Constants.PLAYER_MOVEMENT_SPEED);
            float maxSpeed = Constants.PLAYER_MOVEMENT_SPEED * (float) Math.sqrt(2);
            scaledMovement.clamp(maxSpeed, maxSpeed);
            body.setLinearVelocity(scaledMovement);

            xPosition = body.getPosition().x;
            yPosition = body.getPosition().y;
        }
    }

    /**
     * Set new values for mouse.
     *
     * @param leftMouse is left mouse clicked
     * @param mouseXPosition mouse x coordinate
     * @param mouseYPosition mouse y coordinate
     */
    public void setMouseControl(boolean leftMouse, int mouseXPosition, int mouseYPosition, ItemTypes type){
        this.mouseXPosition = mouseXPosition;
        this.mouseYPosition = mouseYPosition;
        this.mouseLeftClick = leftMouse;
        this.type = type;
    }

    /**
     * Get player's coins.
     *
     * @return integer value of coin amount that player has
     */
    public Integer getCoins() {
        return coins;
    }

    /**
     * Add one coin to player's coins.
     */
    public void addCoin() {
        coins++;
    }

    /**
     * Change the colliding with mob value.
     *
     * @param bool new bool value
     */
    public void changeCollidingWithMob(boolean bool) {
        collidingWithMob = bool;
    }

    /**
     * Get if player is colliding with a mob.
     */
    public boolean getCollidingWithMob() {
        return collidingWithMob;
    }

    /**
     * Pick up item to inventory.
     *
     * @param item item that is picked up
     */
    public void pickUpItem(Item item) {
        inventory.put(item.getId(), item);
    }

    /**
     * Remove item from inventory.
     *
     * @param itemId item's id that is removed
     * @return item that is removed
     */
    public Item removeItem(Integer itemId) {
        Item droppedItem = inventory.get(itemId);
        inventory.remove(itemId);
        return droppedItem;
    }

    /**
     * Get player's inventory.
     *
     * @return list of all items in players inventory
     */
    public List<Item> getInventory() {
        return inventory.values().stream().toList();
    }

    /**
     * Create player's hit box.
     *
     * @param world world, where hit boxes are in
     */
    public void createBody(World world) {
        // Create a dynamic body for the player
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(xPosition, yPosition);
        bodyDef.fixedRotation = true;
        Body playerBody = world.createBody(bodyDef);

        // Create a fixture defining the hit box shape
        PolygonShape hitBoxShape = new PolygonShape();
        hitBoxShape.setAsBox(PlayerCharacter.WIDTH / Constants.PPM, PlayerCharacter.HEIGHT / Constants.PPM);

        CircleShape collisionCircle = new CircleShape();
        float circleRadius = 10 / Constants.PPM;
        collisionCircle.setRadius(circleRadius);
        collisionCircle.setPosition(new Vector2(0f, -(PlayerCharacter.HEIGHT / Constants.PPM) + circleRadius));

        FixtureDef fixtureDefCollisionCircle = new FixtureDef();
        fixtureDefCollisionCircle.shape = collisionCircle;
        fixtureDefCollisionCircle.density = 0.0f;

        // Attach the fixture to the body
        FixtureDef fixtureDefHitbox = new FixtureDef();
        fixtureDefHitbox.shape = hitBoxShape;
        fixtureDefHitbox.density = 1.0f;
        fixtureDefHitbox.isSensor = true;


        playerBody.createFixture(fixtureDefHitbox).setUserData(List.of(this, CollisionBodyTypes.HIT_BOX));
        playerBody.createFixture(fixtureDefCollisionCircle).setUserData(List.of(this, CollisionBodyTypes.WORLD_COLLISION_BOX));

        // Clean up
        hitBoxShape.dispose();
        collisionCircle.dispose();

        body = playerBody;
    }

    /**
     * Remove body.
     */
    public void removeBody(World world) {
        if (body != null) {
            world.destroyBody(body); // Destroy the player's body
            body = null;
        }
    }


    /**
     * Regenerate mana.
     */
    public void regenerateMana() {
        if (mana < 100){
            // Add mana every tick so that one second regenerates around 5 mana
            mana = (float) Math.min(mana + Constants.MANA_REGEN, 100f); // Mana can not be over 100
        }
    }

    /**
     * Regenerate health.
     */
    public void regenerateHealth() {
        if (healingTicks > 0) {
            setHealth(Math.min(health + HEAL_PER_TICK, 100));
            healingTicks--;
        }
    }

    public void setReady() {
        this.loaded = true;
    }

    public boolean isReady() {
        return this.loaded;
    }
}
