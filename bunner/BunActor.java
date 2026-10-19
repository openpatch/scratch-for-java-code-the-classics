package bunner;

import org.openpatch.scratch.Sprite;
import org.openpatch.scratch.Window;

/**
 * Base class for all bunner actors. Positions are kept in screen pixel
 * coordinates (origin top-left, y growing downwards), mirroring the original
 * implementation. The sprite position is synchronized to the Scratch world
 * coordinate system in {@link #sync()}.
 */
public class BunActor extends Sprite {

  protected Play play;
  protected double mx;
  protected double my;
  protected boolean leftBottomAnchor;

  /** Offset from the parent row's y position, for child objects of a row. */
  public double relY;

  /** The row this child is positioned relative to, or null for independent actors. */
  protected Row row;

  public BunActor(Play play, boolean leftBottomAnchor) {
    this.play = play;
    this.leftBottomAnchor = leftBottomAnchor;
  }

  @Override
  public void run() {
    if (!this.isPlaying()) {
      return;
    }
    if (this.row != null) {
      this.my = this.row.my + this.relY;
    }
    this.sync();
  }

  protected boolean isPlaying() {
    return this.getStage() == this.play && Window.getInstance().getStage() == this.play
        && !this.play.isFrozen();
  }

  public void sync() {
    if (this.leftBottomAnchor) {
      this.setX(this.mx + this.getWidth() / 2.0 - Play.WIDTH / 2.0);
    } else {
      this.setX(this.mx - Play.WIDTH / 2.0);
    }
    this.setY(Play.HEIGHT / 2.0 - (this.my - this.getHeight() / 2.0));
  }
}
