package eggzy;

/**
 * An actor which is subject to gravity: the player and the non-flying
 * enemies. The flying enemies use this class too, but disable gravity.
 */
public class GravityActor extends CollideActor {

  public static final int MAX_FALL_SPEED = 7;

  public enum FallState {
    LANDED,
    FALLING,
    JUMPING,
    WALL_JUMPING
  }

  protected boolean gravityEnabled;
  protected int velY;
  protected FallState fallState = FallState.FALLING;
  protected int lowerGravityTimer = 0;

  public GravityActor(
      Play game, double x, double y, boolean gravityEnabled, double anchorY) {
    super(game, x, y, anchorY);
    this.gravityEnabled = gravityEnabled;
  }

  /**
   * Applies gravity. The detect parameter indicates whether to check for
   * collisions with blocks as we fall - false while the player is losing a
   * life and just falls out of the level.
   */
  protected void applyGravity(boolean detect) {
    if (!this.gravityEnabled) {
      return;
    }

    this.lowerGravityTimer -= 1;

    // Apply change to Y velocity
    if (this.game.timer % (this.lowerGravityTimer > 0 ? 3 : 2) == 0) {
      this.velY = Math.min(this.velY + 1, MAX_FALL_SPEED);
    }

    if (detect && this.velY != 0) {
      if (this.fallState == FallState.LANDED) {
        this.fallState = FallState.FALLING;
      }
      if (this.move(0, sign(this.velY), Math.abs(this.velY))) {
        // We must have either landed or hit our head on the ceiling
        if (this.velY > 0) {
          this.velY = 0;
          this.fallState = FallState.LANDED;
        }
      }
    } else {
      // Collision detection disabled - just move without any checks
      this.y += this.velY;
    }
  }

  public boolean landed() {
    return this.fallState == FallState.LANDED;
  }

  /** Returns 1, 0 or -1 depending on whether the value is positive, zero or negative. */
  public static int sign(double v) {
    if (v == 0) {
      return 0;
    }
    return v < 0 ? -1 : 1;
  }
}
