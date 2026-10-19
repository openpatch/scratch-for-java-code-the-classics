package myriapod;

import org.openpatch.scratch.Sprite;

/**
 * Base class for all myriapod actors. Positions are kept in screen pixel
 * coordinates (origin top-left, y growing downwards), mirroring the original
 * implementation. The sprite position is synchronized to the Scratch
 * coordinate system in {@link #sync()}.
 */
public class MyrActor extends Sprite {

  protected Play play;
  protected int mx;
  protected int my;

  public MyrActor(Play play) {
    this.play = play;
  }

  public void sync() {
    this.setX(this.mx - Play.WIDTH / 2.0);
    this.setY(Play.HEIGHT / 2.0 - this.my);
  }

  /** Returns true, if the given point (screen coordinates) is inside the actor. */
  public boolean containsPoint(double x, double y) {
    var width = this.getWidth();
    var height = this.getHeight();
    return x >= this.mx - width / 2.0
        && x < this.mx + width / 2.0
        && y >= this.my - height / 2.0
        && y < this.my + height / 2.0;
  }
}
