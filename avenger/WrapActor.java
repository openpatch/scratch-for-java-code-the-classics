package avenger;

import org.openpatch.scratch.Sprite;
import org.openpatch.scratch.Window;

/**
 * An object in a scrolling game world that wraps around at the edges. The
 * position is kept in world pixel coordinates (origin top-left, y growing
 * downwards), mirroring the original implementation; the sprite position is
 * synchronized to the Scratch coordinate system in {@link #sync(double, double)},
 * which also applies the scrolling camera offset.
 */
public class WrapActor extends Sprite {

  protected final Play game;

  protected double x;
  protected double y;

  public WrapActor(Play game, String costume) {
    this.game = game;
    this.addCostume(costume, Utils.img(costume));
    this.switchCostume(costume);
  }

  /**
   * If the actor goes off the left or right edge of the game world, relative
   * to the player, wrap it back round to the other side.
   */
  public void wrap() {
    while (this.x - game.player.x < -Play.LEVEL_WIDTH / 2.0) {
      this.relocate(Play.LEVEL_WIDTH);
    }
    while (this.x - game.player.x > Play.LEVEL_WIDTH / 2.0) {
      this.relocate(-Play.LEVEL_WIDTH);
    }
  }

  /** Places this actor after its behavior, using the stage's shared camera. */
  @Override
  public void run() {
    if (this.isPlaying()) {
      this.sync(this.game.getOffsetX(), this.game.getOffsetY());
    }
  }

  protected boolean isPlaying() {
    return this.getStage() == this.game && Window.getInstance().getStage() == this.game;
  }

  /**
   * Transfers the world position to the sprite position, moving it into
   * screen space with the scrolling offsets.
   */
  public void sync(double offsetX, double offsetY) {
    this.setX(this.x + offsetX - Play.WIDTH / 2.0);
    this.setY(Play.HEIGHT / 2.0 - (this.y + offsetY));
  }

  /** Shifts the actor within the wrapping world. */
  public void relocate(double delta) {
    this.x += delta;
  }

  /** Is the given position within this sprite's rectangle? */
  public boolean collidepoint(double px, double py) {
    return Math.abs(px - this.x) < this.getWidth() / 2.0
        && Math.abs(py - this.y) < this.getHeight() / 2.0;
  }
}
