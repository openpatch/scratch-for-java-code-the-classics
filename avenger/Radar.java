package avenger;

import org.openpatch.scratch.Sprite;
import org.openpatch.scratch.Stage;
import org.openpatch.scratch.Vector2;

/**
 * The radar at the top of the screen, which shows the position of the player,
 * the enemies and the humans in the whole wrapping world.
 */
public class Radar {

  private final Sprite sprite;
  private final double left;
  private final double top;

  public Radar(Stage stage) {
    this.sprite = new Sprite();
    this.sprite.addCostume("radar", Utils.img("radar"));
    this.sprite.switchCostume("radar");
    // The original places the actor at (WIDTH/2, 4) with the anchor at the
    // top centre, i.e. the top-left corner of the image is at (WIDTH/2 - w/2, 4)
    this.left = Play.WIDTH / 2.0 - this.sprite.getWidth() / 2.0;
    this.top = 4;
    this.sprite.setPosition(Play.WIDTH / 2.0 - Play.WIDTH / 2.0,
        Play.HEIGHT / 2.0 - (this.top + this.sprite.getHeight() / 2.0));
    stage.add(this.sprite);
  }

  /**
   * Converts a position in world space into a position on the radar in screen
   * space.
   */
  public Vector2 radarPos(double wx, double wy) {
    int px = Math.floorMod((int) wx, Play.LEVEL_WIDTH);
    return new Vector2(this.left + px / 11.5,
        this.top + Math.floorDiv((int) wy, 11));
  }

  /** Sets a blip sprite to the given radar position, in screen pixel coordinates. */
  public void setBlipPosition(Sprite blip, Vector2 pos) {
    blip.setPosition(pos.getX() - Play.WIDTH / 2.0, Play.HEIGHT / 2.0 - pos.getY());
  }
}
