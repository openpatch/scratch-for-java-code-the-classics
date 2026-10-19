package beatstreets;

import org.openpatch.scratch.Vector2;

/**
 * Base class for weapons lying around in the level, which can be picked up,
 * carried, thrown and dropped.
 */
public abstract class Weapon extends ScrollHeightActor {

  public final String name;
  public final int endPickupFrame;
  public boolean held = false;
  public Vector2 vel = new Vector2(0, 0);

  private final double bounciness;
  private final double groundFriction;
  private final double airFriction;

  protected Weapon(Play game, String name, String sprite, Vector2 pos, int endPickupFrame,
      double anchorX, double anchorY, double bounciness, double groundFriction,
      double airFriction, boolean separateShadow) {
    super(game, sprite, pos, anchorX, anchorY, separateShadow);
    this.name = name;
    this.endPickupFrame = endPickupFrame;
    this.bounciness = bounciness;
    this.groundFriction = groundFriction;
    this.airFriction = airFriction;
  }

  @Override
  public void run() {
    if (!this.isPlaying()) {
      super.run();
      return;
    }
    if (!this.held) {
      // If not held, check whether we're above the ground, or if we're moving
      if (this.heightAboveGround > 0 || this.vel.getY() != 0) {
        // Fall to the ground
        this.vel = new Vector2(this.vel.getX(), this.vel.getY() + Game.WEAPON_GRAVITY);
        if (this.vel.getY() > this.heightAboveGround) {
          // Bounce if we have bounciness, but stop bouncing if the Y velocity is low
          if (this.bounciness > 0 && this.vel.getY() > 1) {
            this.heightAboveGround =
                Math.abs(this.heightAboveGround - this.vel.getY()) * this.bounciness;
            this.vel = new Vector2(this.vel.getX(), -this.vel.getY() * this.bounciness);
          } else {
            this.heightAboveGround = 0;
            this.vel = new Vector2(this.vel.getX(), 0);
          }
        } else {
          // Didn't bounce - apply the velocity to the Y position
          this.heightAboveGround -= this.vel.getY();
        }
      }

      this.vpos = new Vector2(this.vpos.getX() + this.vel.getX(), this.vpos.getY());

      // Friction on the X axis, varies depending on whether we're on the
      // ground or in the air
      double friction = this.heightAboveGround == 0 ? this.groundFriction : this.airFriction;
      this.vel = new Vector2(this.vel.getX() * friction, this.vel.getY());
      if (Math.abs(this.vel.getX()) < 0.05) {
        this.vel = new Vector2(0, this.vel.getY());
      }
    }

    super.run();
  }

  /** Can the weapon currently be picked up? */
  public boolean canBePickedUp() {
    return !this.held && this.heightAboveGround == 0;
  }

  public void pickUp(int holdHeight) {
    this.held = true;
    this.heightAboveGround = holdHeight; // for when we are dropped
    this.vel = new Vector2(0, 0);
    this.imageName = "blank";
  }

  /** The subclass has the responsibility of setting the image when dropped. */
  public void dropped() {
    this.held = false;
  }

  /** Called when the weapon is used to hit an opponent. */
  public void used() {}

  /** Has the weapon broken? */
  public boolean isBroken() {
    return false;
  }

  /** Throws the weapon in the given direction. */
  public void throwWeapon(int dirX, Fighter thrower) {
    // Default behaviour: subclasses override
  }
}
