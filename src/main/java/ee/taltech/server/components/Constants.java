package ee.taltech.server.components;

import ee.taltech.server.entities.spawner.EntitySpawner;

import java.util.Map;

public class Constants {
    public static final float PPM = 32;

    public static final float PLAYER_MOVEMENT_SPEED = 5;
    public static final float MOB_MOVEMENT_SPEED = 4;

    public static final float STATUS_BAR_WIDTH_PX = 60;

    public static final int FIRST_ZONE_RADIUS = 140;
    public static final int SECOND_ZONE_RADIUS = 76;
    public static final int THIRD_ZONE_RADIUS = 29;

    public static final float BOOK_HIT_BOX_WIDTH = 0.7f;
    public static final float BOOK_HIT_BOX_HEIGHT = 0.7f;
    public static final float COIN_HIT_BOX_WIDTH = 0.2f; // This size is chosen randomly, SHOULD NOT BE FINAL
    public static final float COIN_HIT_BOX_HEIGHT = 0.2f; // This size is chosen randomly, SHOULD NOT BE FINAL
    public static final float POTION_HIT_BOX_WIDTH = 0.5f;
    public static final float POTION_HIT_BOX_HEIGHT = 0.5f;

    public static final float HIT_BOX_RADIUS = 1f; // One cell size

    public static final float TRIGGERING_RANGE_RADIUS = 15f; // Should be changed
    public static final int MOB_HEALTH = 50;
    public static final float MOD_SPIN_ATTACK_DAMAGE = 10;
    public static final int MAX_PATH_RANGE = 150;

    public static final float COIN_DROP_RANGE = 1;
    public static final float ITEM_DROP_RANGE = 0.5f;
    public static final float FIREBALL_SPEED = 7;
    public static final float PLASMA_SPEED = 10;
    public static final float KUNAI_SPEED = 10;
    public static final float METEOR_SPEED = 5;

    public static final int TICKS_TO_START_GAME = 300;
    public static final int TICKS_TO_END_GAME = 60;

    public static final float MOB_DMG_PER_TIC = 0.15f;
    public static final float ZONE_DMG_PER_TIC = 0.03f;

    public static final Map<EntitySpawner.EntityType, Float> SPAWN_PROBABILITIES = Map.of(
            EntitySpawner.EntityType.MOB, 0.08f, // Probability of spawning a mob
            EntitySpawner.EntityType.ITEM, 0.92f // Probability of spawning a book
    );

    public static final Map<ItemTypes, Float> SPAWN_PROBABILITIES_ITEMS = Map.of(
            ItemTypes.FIREBALL, 0.2f,
            ItemTypes.COIN, 0f,
            ItemTypes.METEOR, 0.1f,
            ItemTypes.ICE_SHARD, 0.1f,
            ItemTypes.KUNAI, 0.1f,
            ItemTypes.PLASMA, 0.2f,
            ItemTypes.HEALING_POTION, 0.2f,
            ItemTypes.POISONBALL, 0.1f
    );

    public static final float MANA_REGEN = 0.2f;
}

