package beatstreets;

import org.openpatch.scratch.Vector2;

/** A collectible powerup, such as health or an extra life. */
public abstract class Powerup extends ScrollHeightActor {

  public boolean collected = false;

  protected Powerup(Play game, String image, Vector2 pos, double anchorX, double anchorY) {
    super(game, image, pos, anchorX, anchorY, false);
  }

  /** Called when the player collects this powerup. */
  public abstract void collect(Player collector);
}
