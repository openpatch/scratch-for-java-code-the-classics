package avenger;

import org.openpatch.scratch.Operators;
import org.openpatch.scratch.Random;
import org.openpatch.scratch.Sprite;
import org.openpatch.scratch.extensions.sound.SoundPlayback;

/** The player ship. The position is kept in world pixel coordinates. */
public class Player extends WrapActor {

  // Drag for X and Y axes - closer to 1 = less drag, higher top speed
  private static final double DRAG_X = 0.98;
  private static final double DRAG_Y = 0.9;

  // Force for X and Y axes - higher numbers = more acceleration, higher top speed
  private static final double FORCE_X = 0.2;
  private static final double FORCE_Y = 0.5;

  // Number of frames for which the player ship plays its explode animation
  private static final int EXPLODE_ANIM_SPEED = 4;
  private static final int EXPLODE_FRAMES = 18 * EXPLODE_ANIM_SPEED;

  public static final int TIMER_HURT = 0;
  public static final int TIMER_FIRE = 1;
  public static final int TIMER_ANIM = 2;
  public static final int TIMER_EXPLODE = 3;

  private final Controls controls;

  public double velX = 0;
  public double velY = 0;

  public int lives = 5;
  public int shields = 5;
  public int extraLifeTokens = 0;
  public int facingX = 1;
  public int tiltY = 0;

  // The timers, indexed by the TIMER_* constants
  public final int[] timers = new int[4];

  public int frame = 0;

  public Human carriedHuman = null;

  /** Our radar blip. */
  public final Sprite blip;

  /** Thrust sprite, with its own world position. */
  public final Sprite thrustSprite;
  private double thrustX;
  private double thrustY;

  /** The laser firing flash, in screen space. */
  public final Sprite flashSprite;

  private boolean thrustSoundPlaying = false;

  /** The thrust sound copy that is looping, with its own volume. */
  private SoundPlayback thrustSound;

  public Player(Play game, Controls controls) {
    super(game, "blank");
    this.x = Play.WIDTH / 2.0;
    this.y = Play.LEVEL_HEIGHT / 2.0;
    this.controls = controls;

    // Ship sprites, with the tilted up/down variants of the two frames which
    // show the ship facing fully left or right
    for (int f = 0; f < 16; f++) {
      this.addCostume("ship" + f, Utils.img("ship" + f));
      this.addCostume("hurt" + f, Utils.img("hurt" + f));
      if (f % 8 == 0) {
        this.addCostume("ship" + f + "u", Utils.img("ship" + f + "u"));
        this.addCostume("ship" + f + "d", Utils.img("ship" + f + "d"));
        this.addCostume("hurt" + f + "u", Utils.img("hurt" + f + "u"));
        this.addCostume("hurt" + f + "d", Utils.img("hurt" + f + "d"));
      }
    }

    // Explosion animation
    for (int f = 0; f < 18; f++) {
      this.addCostume("ship_explode" + f, Utils.img("ship_explode" + f));
    }

    // Thrust and laser firing flash
    for (int direction = 0; direction < 2; direction++) {
      for (int f = 0; f < 2; f++) {
        this.addCostume("boost_" + direction + "_" + f, Utils.img("boost_" + direction + "_" + f));
      }
    }
    this.addCostume("flash0", Utils.img("flash0"));
    this.addCostume("flash1", Utils.img("flash1"));

    // Our radar blip
    this.blip = new Sprite();
    this.blip.addCostume("dot-white", Utils.img("dot-white"));
    this.blip.switchCostume("dot-white");
    this.blip.hide();
    game.add(this.blip);

    // Thrust sprite
    this.thrustSprite = new Sprite();
    this.thrustSprite.addCostume("blank", Utils.img("blank"));
    this.thrustSprite.switchCostume("blank");
    for (int direction = 0; direction < 2; direction++) {
      for (int f = 0; f < 2; f++) {
        this.thrustSprite.addCostume("boost_" + direction + "_" + f,
            Utils.img("boost_" + direction + "_" + f));
      }
    }
    this.thrustX = 0;
    this.thrustY = 0;
    game.add(this.thrustSprite);

    // Laser firing flash sprite
    this.flashSprite = new Sprite();
    this.flashSprite.addCostume("flash0", Utils.img("flash0"));
    this.flashSprite.addCostume("flash1", Utils.img("flash1"));
    this.flashSprite.switchCostume("flash0");
    this.flashSprite.hide();
    game.add(this.flashSprite);
  }

  /**
   * Checks if the given position falls within the bounds of the player ship.
   * If it does, the player is hurt.
   */
  public boolean hitTest(double px, double py) {
    // If we're dead or in the explode animation, always return false
    if (this.lives == 0 || this.timers[TIMER_EXPLODE] > 0) {
      return false;
    }

    // As the sprite's rectangle is bigger than the actual visible part of the
    // sprite, we just check whether the given position is within 40 pixels of
    // the centre of the sprite on the X axis, and within 15 pixels of the
    // centre on the Y axis
    if (Math.abs(px - this.x) < 40 && Math.abs(py - this.y) < 15) {
      // If there's a collision, set the 'hurt' timer so that we glow to
      // indicate damage, and decrease shields by 1
      this.timers[TIMER_HURT] = 60;
      this.shields -= 1;

      this.game.playSound("player_hit0");

      if (this.shields == 0) {
        // Lose a life
        this.lives -= 1;

        // If it's game over and we're playing the thrust sound, stop it
        if (this.lives == 0 && this.thrustSoundPlaying) {
          this.thrustSound.stop();
          this.thrustSoundPlaying = false;
        }

        // Explode, later we will respawn in a random position
        this.game.playSound("player_explode0");
        this.timers[TIMER_EXPLODE] = EXPLODE_FRAMES;

        // Any human we're carrying when we lose a life will be dropped
        if (this.carriedHuman != null) {
          this.carriedHuman.dropped();
          this.carriedHuman = null;
        }
      }

      return true;
    }
    return false;
  }

  @Override
  public void run() {
    if (!this.isPlaying()) {
      return;
    }
    // Decrease all timer values by 1
    for (int i = 0; i < this.timers.length; i++) {
      this.timers[i] -= 1;
    }

    if (this.timers[TIMER_EXPLODE] > 0) {
      // If we're currently exploding, don't do any of the normal behaviour,
      // just set our sprite to the appropriate frame, then randomise our
      // position when the timer runs out
      int frame = (EXPLODE_FRAMES - this.timers[TIMER_EXPLODE]) / EXPLODE_ANIM_SPEED;
      this.switchCostume("ship_explode" + frame);

      // No thrust sprite while exploding
      this.thrustSprite.switchCostume("blank");

      // Respawn in a new location if the timer is about to run out, unless
      // we're out of lives
      if (this.timers[TIMER_EXPLODE] == 1 && this.lives > 0) {
        this.respawn();
      }
    } else if (this.lives == 0) {
      // If we're not exploding but out of lives, hide the sprite and don't do
      // anything else
      this.switchCostume("blank");
      this.thrustSprite.switchCostume("blank");
    } else {
      this.updateAlive();
    }

    this.updateFlash(this.game.getOffsetX(), this.game.getOffsetY());
    this.syncThrust(this.game.getOffsetX(), this.game.getOffsetY());
    super.run();
  }

  private void updateAlive() {
    double xInput = this.controls.getX();
    double yInput = this.controls.getY();

    double moveX = xInput;
    double moveY = yInput;

    this.tiltY = (int) yInput;

    if (xInput != 0) {
      this.facingX = Utils.sign(xInput);
    }

    // Only apply movement force on X axis if the player is facing the same
    // direction they're trying to accelerate in, and the ship has fully
    // animated to that facing direction
    if (this.frame % 8 != 0 || Utils.sign(this.facingX) != Utils.sign(moveX)) {
      moveX = 0;
    }

    this.velX = this.velX * DRAG_X + moveX * FORCE_X;
    this.velY = this.velY * DRAG_Y + moveY * FORCE_Y;

    // Apply velocity to position
    this.x += this.velX;
    this.y += this.velY;

    // Limit Y position
    this.y = Operators.constrain(this.y, 0, Play.LEVEL_HEIGHT);

    // Update radar blip position
    this.game.radar.setBlipPosition(this.blip, this.game.radar.radarPos(this.x, this.y));

    // Check to see if we can pick up a falling human
    if (this.carriedHuman == null) {
      for (Human human : this.game.humans) {
        if (human.canBePickedUpByPlayer()
            && Math.hypot(human.x - this.x, human.y - this.y) < 40) {
          human.pickedUp(this);
          this.carriedHuman = human;
          break;
        }
      }
    } else {
      // If we're carrying a human, update their position and check whether
      // they are in a place where they can be safely deposited on the ground
      this.carriedHuman.x = this.x;
      this.carriedHuman.y = this.y + 50;
      if (this.carriedHuman.terrainCheck()) {
        this.carriedHuman.dropped();
        this.carriedHuman = null;
        this.game.playSound("rescue_prisoner0");
      }
    }

    // Decide which sprite to display, and if we're on an appropriate
    // animation frame, also check to see if the player wants to fire
    int target = this.facingX < 0 ? 8 : 0;

    if (this.frame == target) {
      // If we're on our target frame, and we haven't fired too recently,
      // we're allowed to fire
      if (this.controls.buttonDown(0) && this.timers[TIMER_FIRE] <= 0) {
        this.timers[TIMER_FIRE] = 10;
        // Create a laser with the appropriate offset from the player
        double laserVelX = this.velX + 20 * this.facingX;
        double laserX = this.x + 40 * this.facingX;
        double laserY = this.y + this.getLaserFireYOffset();
        this.game.addLaser(new Laser(this.game, laserX, laserY, laserVelX));
      }
    } else {
      // If we're not on our target frame, animate towards it every three
      // game frames
      if (this.timers[TIMER_ANIM] <= 0) {
        this.timers[TIMER_ANIM] = 3;
        // We always animate forward through the frames, wrapping back to
        // zero when we hit 16
        this.frame = (this.frame + 1) % 16;
      }
    }

    // Play the thrust sound while we're thrusting. The original loops the
    // sound and fades it in and out; a played copy lasts only once, so a new
    // one is started when the previous one ends and stopped when the thrust
    // ends
    if (this.thrustSoundPlaying && !this.thrustSound.isPlaying()) {
      this.thrustSound = this.game.playSoundCopy("thrust0", 30);
    }
    // Ship must be fully facing in the direction the player is trying to move
    // in, for the thrust to occur
    if (moveX != 0 && this.frame == target && !this.thrustSoundPlaying) {
      this.thrustSound = this.game.playSoundCopy("thrust0", 30);
      this.thrustSoundPlaying = true;
    } else if ((moveX == 0 || this.frame != target) && this.thrustSoundPlaying) {
      this.thrustSound.stop();
      this.thrustSoundPlaying = false;
    }

    String animType = this.timers[TIMER_HURT] <= 0 ? "ship" : "hurt";
    String tilt = "";
    if (this.frame % 8 == 0 && this.tiltY != 0) {
      tilt = this.tiltY < 0 ? "u" : "d";
    }

    // Set sprite
    this.switchCostume(animType + this.frame + tilt);

    // Set thrust sprite
    if (this.frame % 8 != 0 || moveX == 0) {
      this.thrustSprite.switchCostume("blank");
    } else {
      int direction = moveX > 0 ? 0 : 1;
      int frame = this.game.timer / 3 % 2;
      this.thrustSprite.switchCostume("boost_" + direction + "_" + frame);
      this.thrustX = this.x + 66 * -moveX;
      this.thrustY = this.y - 3;
    }
  }

  public void respawn() {
    // Restore shields
    this.shields = 5;

    // Try several random positions and assign a score to each one, choosing
    // the one which is furthest from any enemy on the X axis
    double bestScore = 0;
    for (int i = 0; i < 20; i++) {
      double randomX = Random.random(0, Play.LEVEL_WIDTH - 1);
      double randomY = Random.random(150, 300);
      if (this.game.enemies.isEmpty()) {
        // If there are no enemies, just go with the first random position
        this.x = randomX;
        this.y = randomY;
        break;
      }
      // Score the random position based on how far away the closest enemy is
      // on the X axis - the further the better
      double score = Double.MAX_VALUE;
      for (Enemy enemy : this.game.enemies) {
        score = Math.min(score, wrapDistance(enemy.x, randomX));
      }
      if (score >= bestScore) {
        this.x = randomX;
        this.y = randomY;
        bestScore = score;
      }
    }
  }

  /** The distance between two X positions in the wrapping level. */
  private static double wrapDistance(double x1, double x2) {
    x1 = x1 % Play.LEVEL_WIDTH;
    x2 = x2 % Play.LEVEL_WIDTH;
    double dist = Math.abs(x1 - x2);
    if (dist < Play.LEVEL_WIDTH / 2.0) {
      return dist;
    }
    return Play.LEVEL_WIDTH - dist;
  }

  /**
   * Displays a flash sprite at the point where a laser turret fires. Only
   * allowed if we're on an appropriate animation frame, and if we've just
   * fired within the last few frames. The flash is drawn in screen space.
   */
  public void updateFlash(double offsetX, double offsetY) {
    if (this.frame % 8 == 0 && this.timers[TIMER_FIRE] > 5) {
      // flash0 is for when the ship is facing right (frame 0), flash1 for
      // facing left (frame 8)
      this.flashSprite.switchCostume("flash" + this.frame / 8);
      double x = this.x + offsetX - 25 + this.flashSprite.getWidth() / 2.0;
      double y = this.y + offsetY - 13 + this.getLaserFireYOffset()
          + this.flashSprite.getHeight() / 2.0;
      this.flashSprite.setPosition(x - Play.WIDTH / 2.0, Play.HEIGHT / 2.0 - y);
      this.flashSprite.show();
    } else {
      this.flashSprite.hide();
    }
  }

  /**
   * The starting Y position of the laser should vary by a few pixels
   * depending on how the ship is tilted.
   */
  public int getLaserFireYOffset() {
    int[] offsets = {-1, 3, 2};
    return offsets[this.tiltY + 1];
  }

  /** Draws the thrust sprite, applying the scrolling offset. */
  public void syncThrust(double offsetX, double offsetY) {
    this.thrustSprite.setX(this.thrustX + offsetX - Play.WIDTH / 2.0);
    this.thrustSprite.setY(Play.HEIGHT / 2.0 - (this.thrustY + offsetY));
  }

  public boolean isCarryingHuman() {
    return this.carriedHuman != null;
  }

  public void levelEnded(int shieldRestoreAmount, int humansSaved) {
    this.shields = Math.min(this.shields + shieldRestoreAmount, 5);

    // Earn an extra life token if all humans were saved
    if (humansSaved == 10) {
      this.extraLifeTokens += 1;
      // Get an extra life if we get 3 life tokens
      if (this.extraLifeTokens >= 3) {
        this.lives += 1;
        this.extraLifeTokens -= 3;
      }
    }
  }
}
