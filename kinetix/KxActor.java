package kinetix;

import org.openpatch.scratch.Sprite;

/**
 * Base class for all moving objects. Positions are kept in screen pixel
 * coordinates (origin top-left, y growing downwards), mirroring the original
 * implementation. The sprite position is synchronized to the Scratch
 * coordinate system in {@link #sync()}.
 */
public class KxActor extends Sprite {

  protected double x;
  protected double y;

  /** Gameplay sprites stay still while the game-over screen is active. */
  protected boolean isPlaying() {
    return this.getStage() instanceof Play play && play.isPlaying();
  }

  /** Transfers the pixel position to the sprite position. */
  public void sync() {
    this.setX(this.x - Play.WIDTH / 2.0);
    this.setY(Play.HEIGHT / 2.0 - this.y);
  }
}
