package eggzy;

/**
 * An enemy. There are four types per biome; some fly (no gravity), some have
 * directional sprites, and their sizes, anchors, health and speed differ per
 * type - see the ENEMY_TYPES_* tables in Play.
 */
public class Enemy extends GravityActor {

  public final int type;
  public final Biome biome;

  private int directionX;
  private int directionY;
  private int health;
  private final int speed;
  private final boolean useDirectionalSprites;
  private int stompedTimer = 0;

  public boolean dying = false;

  public Enemy(
      Play game, double x, double y, int type, Biome biome, int directionX,
      int appearanceCount) {
    super(game, x, y, !Play.ENEMY_TYPES_FLYING[biome.ordinal()][type],
        Play.ENEMY_TYPES_ANCHOR_Y[biome.ordinal()][type]);
    this.type = type;
    this.biome = biome;
    this.directionX = directionX;
    this.health = Play.ENEMY_TYPES_HEALTH[type];
    this.speed = Play.ENEMY_TYPES_SPEED[type];

    // Flying enemies which are on their third appearance move diagonally
    this.directionY = appearanceCount >= 3 && !this.gravityEnabled ? 1 : 0;

    // Castle types 2 and 3, and fly/ghost, have different sprites for facing
    // left/right
    this.useDirectionalSprites =
        (this.biome == Biome.CASTLE && this.type >= 2)
            || (this.biome == Biome.FOREST && this.type < 2);
  }

  @Override
  public void run() {
    if (!this.isPlaying()) {
      return;
    }
    this.applyGravity(!this.dying);

    if (!this.dying) {
      this.stompedTimer -= 1;

      // Don't move on the x axis while falling. Flying enemies have gravity
      // disabled, so they always move.
      if (!this.gravityEnabled || this.fallState != FallState.FALLING) {
        // Move in the current direction - turn around if we hit a wall
        if (this.move(this.directionX, 0, this.speed)) {
          this.directionX = -this.directionX;
        }
        if (this.directionY != 0 && this.move(0, this.directionY, this.speed)) {
          this.directionY = -this.directionY;
        }
      }
    }

    // Choose and set sprite image, 8 frames of animation
    String image = Play.ENEMY_SPRITE_NAMES[this.biome.ordinal()][this.type];
    if (this.useDirectionalSprites) {
      image += "_" + (this.directionX > 0 ? "1" : "0");
    }
    image += "_" + (this.game.timer / 4) % 8;
    if (this.stompedTimer > 0 || this.dying) {
      image += "_hit";
    }
    this.setImage(image);

    super.run();
  }

  public void stomped() {
    // Don't lose health if we're being stomped several frames in a row
    if (this.stompedTimer <= 0) {
      this.health -= 1;
      if (this.health <= 0) {
        this.destroy();
        this.game.playSound("enemy_death", 5);
      } else {
        this.game.playSound("enemy_take_damage", 5);
      }
    }
    this.stompedTimer = 2;
  }

  public void destroy() {
    this.dying = true;
    this.gravityEnabled = true;

    // Create the explosion animation before gaining time, so it appears
    // underneath the gain time animation
    String explosionSprite = this.type > 1 ? "explosion" : "air_explosion";
    this.game.addAnimation(new Animation(this.x, this.y, explosionSprite + "_{0}", 12, 4,
        Play.ANCHOR_CENTRE_BOTTOM, 0, -1));

    // Destroying an enemy always gains 3 seconds of time
    this.game.gainTime(Play.STOMP_ENEMY_TIME_BONUS, this.centerX(), this.centerY());
  }

  @Override
  public int getCollidableWidth() {
    return Play.ENEMY_TYPES_WIDTH_OVERRIDES[this.biome.ordinal()][this.type];
  }

  @Override
  public int getCollidableHeight() {
    return Play.ENEMY_TYPES_HEIGHT_OVERRIDES[this.biome.ordinal()][this.type];
  }
}
