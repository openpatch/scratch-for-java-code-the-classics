package bunner;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for all row types. Each row corresponds to one of the 40 pixel
 * high images which make up sections of grass, road, etc. Rows are anchored
 * at the bottom-left. Children (cars, logs, hedges, ...) are positioned
 * relative to the row.
 */
public class Row extends BunActor {

  public final List<BunActor> children = new ArrayList<>();
  public int dx = 0;
  public int index;

  public Row(Play play, String baseImage, int index, double y) {
    super(play, true);

    this.index = index;
    this.mx = 0;
    this.my = y;

    this.addCostume("row", String.format("bunner/assets/images/%s%d.png", baseImage, index));
    this.switchCostume("row");
    this.sync();
  }

  /** Creates the row that follows this one. */
  public Row next() {
    return null;
  }

  /**
   * Checks if the given x coordinate collides with any of this row's child
   * objects. A negative margin makes the collidable area narrower.
   */
  public BunActor collide(double x, double margin) {
    for (BunActor child : this.children) {
      if (x >= child.mx - child.getWidth() / 2.0 - margin
          && x < child.mx + child.getWidth() / 2.0 + margin) {
        return child;
      }
    }
    return null;
  }

  /** Horizontal movement applied to the player standing on this row. */
  public int push() {
    return 0;
  }

  /**
   * Returns the new state the player should be in, and a Y offset used when
   * creating a splat graphic.
   */
  public int[] checkCollision(double x) {
    return new int[] {Bunner.STATE_ALIVE, 0};
  }

  /** Ensure the player can't walk off the left or right sides of the screen. */
  public boolean allowMovement(double x) {
    return x >= 16 && x <= Play.WIDTH - 16;
  }

  /** Sound effect played when the player lands on this row. */
  public void playLandingSound() {}

  /** Removes this row and all its children from the stage. */
  public void removeChildren() {
    for (BunActor child : this.children) {
      child.remove();
    }
  }
}
