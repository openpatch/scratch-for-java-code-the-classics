package cavern;

/**
 * Base class for all actors affected by gravity and collision with blocks.
 */
public class GravityActor extends CavernActor {

  public static final int MAX_FALL_SPEED = 10;

  protected int velY = 0;
  protected boolean landed = false;

  public GravityActor(Play play, int x, int y) {
    super(play, true);
    this.mx = x;
    this.my = y;
  }

  /**
   * Applies gravity, without going over the maximum fall speed. If detect is
   * false, no collision detection is performed as the actor falls.
   */
  public void updateGravity(boolean detect) {
    velY = Math.min(velY + 1, MAX_FALL_SPEED);

    if (detect) {
      if (this.move(0, sign(velY), Math.abs(velY))) {
        velY = 0;
        landed = true;
      }

      if (this.top() >= Play.HEIGHT) {
        this.my = 1;
      }
    } else {
      this.my += velY;
    }
  }

  public double top() {
    return this.my - this.getHeight();
  }

  public double bottom() {
    return this.my;
  }

  public static int sign(double x) {
    return x < 0 ? -1 : 1;
  }
}
