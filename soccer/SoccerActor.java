package soccer;

import org.openpatch.scratch.Sprite;
import org.openpatch.scratch.Vector2;

/**
 * Base class for all soccer actors. Positions are kept as a vector in level
 * pixel coordinates (origin top-left of the 1000 x 1400 level, y growing
 * downwards), mirroring the original implementation. The sprite position is
 * synchronized to the Scratch world coordinate system in {@link #sync()}.
 */
public class SoccerActor extends Sprite {

  protected Play play;
  protected Vector2 vpos = new Vector2(0, 0);
  protected Sprite shadow;
  public int team;

  public SoccerActor(Play play) {
    this.play = play;
  }

  public void sync() {
    this.setX(this.vpos.getX() - Play.HALF_LEVEL_W);
    this.setY(Play.HALF_LEVEL_H - this.vpos.getY());
  }

  /** Is this actor close enough to the ball to take part in the game? */
  public boolean active() {
    return false;
  }

  public void syncShadow() {
    if (this.shadow != null) {
      this.shadow.setX(this.getX());
      this.shadow.setY(this.getY());
    }
  }
}
