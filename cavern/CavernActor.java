package cavern;

import org.openpatch.scratch.Sprite;

/**
 * Base class for all actors that collide with the level grid. Positions are
 * kept in screen pixel coordinates (origin top-left, y growing downwards),
 * mirroring the original implementation. The sprite position is synchronized
 * to the Scratch coordinate system in {@link #sync()}.
 */
public class CavernActor extends Sprite {

  protected Play play;
  protected int mx;
  protected int my;
  protected boolean bottomAnchor;

  public CavernActor(Play play, boolean bottomAnchor) {
    this.play = play;
    this.bottomAnchor = bottomAnchor;
  }

  /**
   * Moves the actor one pixel at a time, stopping when colliding with a block
   * or the edge of the level. Returns true on collision.
   */
  public boolean move(int dx, int dy, int speed) {
    int newX = mx;
    int newY = my;

    for (int i = 0; i < speed; i++) {
      newX = newX + dx;
      newY = newY + dy;

      if (newX < 70 || newX > 730) {
        return true;
      }

      if (((dy > 0 && Math.floorMod(newY, Play.GRID_BLOCK_SIZE) == 0)
          || (dx > 0 && Math.floorMod(newX, Play.GRID_BLOCK_SIZE) == 0)
          || (dx < 0 && Math.floorMod(newX, Play.GRID_BLOCK_SIZE) == Play.GRID_BLOCK_SIZE - 1))
          && this.play.block(newX, newY)) {
        return true;
      }

      mx = newX;
      my = newY;
    }

    return false;
  }

  /** Returns true, if the given point (screen coordinates) is inside the actor. */
  public boolean containsPoint(double x, double y) {
    var width = this.getWidth();
    var height = this.getHeight();

    if (x >= mx - width / 2.0 && x < mx + width / 2.0) {
      if (bottomAnchor) {
        return y >= my - height && y < my;
      }
      return y >= my - height / 2.0 && y < my + height / 2.0;
    }
    return false;
  }

  /** Transfers the model position to the sprite position. */
  public void sync() {
    this.setX(mx - Play.WIDTH / 2.0);
    if (bottomAnchor) {
      this.setY(Play.HEIGHT / 2.0 - (my - this.getHeight() / 2.0));
    } else {
      this.setY(Play.HEIGHT / 2.0 - my);
    }
  }

  protected boolean frozen() {
    return play.isFrozen();
  }
}
