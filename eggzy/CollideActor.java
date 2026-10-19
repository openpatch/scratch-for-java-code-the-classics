package eggzy;

/**
 * Base class for objects which move around the level and collide with walls,
 * such as the player and enemies. Movement is done one pixel at a time, so an
 * actor never ends up embedded in the wall it is moving towards.
 */
public class CollideActor extends EzActor {

  /** The game the actor belongs to, like the Python module's global game. */
  protected final Play game;

  public CollideActor(Play game, double x, double y, double anchorY) {
    super(x, y, anchorY);
    this.game = game;
  }

  /**
   * Moves by (dx, dy) at the given speed, one pixel at a time. Returns true if
   * the move was blocked.
   */
  public boolean move(int dx, int dy, int speed) {
    double newX = this.x;
    double newY = this.y;

    for (int i = 0; i < speed; i++) {
      newX += dx;
      newY += dy;

      // Does this proposed new position overlap a collidable tile or the door?
      if (this.game.positionBlocked(this.getRect(newX, newY))) {
        return true;
      }

      this.x = newX;
      this.y = newY;
    }

    return false;
  }

  /**
   * Returns the collision rectangle this actor would have at the given
   * position. The collidable size is smaller than the sprite for the player
   * and enemies, so it is not just the costume bounds.
   */
  public Rect getRect() {
    return this.getRect(this.x, this.y);
  }

  public Rect getRect(double centreX, double bottomY) {
    double w = this.getCollidableWidth();
    double h = this.getCollidableHeight();
    return new Rect(centreX - w / 2, bottomY - h, w, h);
  }

  /** Overridden by Player and Enemy. */
  public int getCollidableWidth() {
    return this.getWidth();
  }

  /** Overridden by Player and Enemy. */
  public int getCollidableHeight() {
    return this.getHeight();
  }
}
