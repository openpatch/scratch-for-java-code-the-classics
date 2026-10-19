package avenger;

import org.openpatch.scratch.Random;
import org.openpatch.scratch.Sprite;

/** A human to rescue. The position is kept in world pixel coordinates. */
public class Human extends WrapActor {

  public double yVelocity = 0;

  /** Our radar blip. */
  public final Sprite blip;

  public int animTimer = 0;
  public boolean waving = false;

  public boolean dead = false;
  public boolean exploding = false;

  /** The player or the enemy carrying us, or null. */
  public WrapActor carrier = null;
  public boolean falling = false;

  public Human(Play game, double x, double y) {
    super(game, "blank");
    this.x = x;
    this.y = y;

    this.addCostume("human_stand0", Utils.img("human_stand0"));
    for (int f = 0; f < 3; f++) {
      this.addCostume("human_wave" + f, Utils.img("human_wave" + f));
    }
    for (int f = 0; f < 2; f++) {
      this.addCostume("human_fall" + f, Utils.img("human_fall" + f));
    }
    for (int f = 0; f < 4; f++) {
      this.addCostume("human_abducted" + f, Utils.img("human_abducted" + f));
    }
    this.addCostume("human_saved0", Utils.img("human_saved0"));
    for (int f = 0; f < 10; f++) {
      this.addCostume("human_explode" + f, Utils.img("human_explode" + f));
    }

    // Create our radar blip
    this.blip = new Sprite();
    this.blip.addCostume("dot-green", Utils.img("dot-green"));
    this.blip.switchCostume("dot-green");
    this.blip.hide();
    game.add(this.blip);
  }

  /**
   * Given a position, see if it falls within this sprite's rectangle. Kill
   * the human if it does.
   */
  public boolean laserHitTest(double px, double py) {
    if (!this.exploding && this.collidepoint(px, py)) {
      this.die();
      return true;
    }
    return false;
  }

  @Override
  public void run() {
    if (!this.isPlaying()) {
      return;
    }
    this.wrap();

    this.animTimer += 1;

    if (this.exploding) {
      // Play the explode animation
      int frame = this.animTimer / 2;
      if (frame >= 10) {
        this.dead = true;
      } else {
        this.switchCostume("human_explode" + frame);
      }
      super.run();
      return;
    }

    // If not being carried, check to see if we're on the ground. If not, fall.
    if (this.carrier == null) {
      this.falling = !this.terrainCheck();
      if (!this.falling && this.yVelocity > 3) {
        this.die();
      }

      if (this.falling) {
        this.yVelocity += 0.05;
        this.yVelocity = Math.min(this.yVelocity, 4);
        this.y += this.yVelocity;
      }
    }

    // Update radar blip pos
    this.game.radar.setBlipPosition(this.blip, this.game.radar.radarPos(this.x, this.y));

    // Set sprite image. Animations run forwards and backwards (at least stand)
    int frame = this.animTimer / 7;
    String sprite;
    int numFrames = 4;
    if (this.carrier == this.game.player) {
      sprite = "saved";
      numFrames = 1;
    } else if (this.carrier != null) {
      sprite = "abducted";
    } else if (this.falling) {
      sprite = "fall";
      numFrames = 2;
    } else if (this.waving) {
      sprite = "wave";
      numFrames = 3;
      if (this.animTimer > 100) {
        this.waving = false;
      }
    } else {
      sprite = "stand";
      numFrames = 1;
      // Sometimes start the wave animation
      if (Random.randomInt(0, 200) == 0) {
        this.waving = true;
        this.animTimer = 0;
      }
    }

    this.switchCostume("human_" + sprite
        + Utils.forwardBackwardAnimationFrame(frame, numFrames));

    super.run();
  }

  /** The player can only pick up a human if they're falling. */
  public boolean canBePickedUpByPlayer() {
    return this.carrier == null && this.falling && !this.dead;
  }

  /** Enemies won't pick up a falling human. */
  public boolean canBePickedUpByEnemy() {
    return this.carrier == null && !this.falling && !this.dead;
  }

  public void pickedUp(WrapActor carrier) {
    this.carrier = carrier;
    this.falling = false;
  }

  public void dropped() {
    this.carrier = null;
    this.falling = !this.terrainCheck();
    this.yVelocity = 0;
  }

  /** Are we standing on the terrain? */
  public boolean terrainCheck() {
    // Convert the world position to a pixel position on the terrain image
    int terrainX = (int) (this.x - Play.LEVEL_WIDTH * Math.floor(this.x / Play.LEVEL_WIDTH));
    int terrainY = (int) (this.y - Play.TERRAIN_OFFSET_Y);
    return this.game.terrainAt(terrainX, terrainY);
  }

  /** Start the explode animation; dead is set to True when it's done. */
  public void die() {
    this.exploding = true;
    this.animTimer = 0;
    this.game.playSound("prisoner_die0");
  }

  /**
   * The explode sprites are anchored at pixel (175, 172) rather than at their
   * centre, so the sprite is shifted accordingly while exploding.
   */
  @Override
  public void sync(double offsetX, double offsetY) {
    if (this.exploding) {
      double centreX = this.x + offsetX - 175 + this.getWidth() / 2.0;
      double centreY = this.y + offsetY - 172 + this.getHeight() / 2.0;
      this.setX(centreX - Play.WIDTH / 2.0);
      this.setY(Play.HEIGHT / 2.0 - centreY);
    } else {
      super.sync(offsetX, offsetY);
    }
  }
}
