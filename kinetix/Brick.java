package kinetix;

import org.openpatch.scratch.Sprite;

/**
 * A brick of the level grid. The id corresponds to the brick sprite filenames
 * brick0 to brickd: id 12 (brickc) needs one hit to turn into 11, id 13
 * (brickd) is indestructible.
 */
public class Brick extends KxActor {

  public final int gridX;
  public final int gridY;
  public int id;

  /** The shadow sprite drawn behind the bricks. */
  public Sprite shadow;

  public Brick(int gridX, int gridY, int id) {
    this.gridX = gridX;
    this.gridY = gridY;
    this.id = id;
    this.addCostume(id);
    this.switchCostume(costumeName(id));
    this.x = gridX * Play.BRICK_WIDTH + Play.BRICKS_X_START + Play.BRICK_WIDTH / 2.0;
    this.y = gridY * Play.BRICK_HEIGHT + Play.BRICKS_Y_START + Play.BRICK_HEIGHT / 2.0;
    this.sync();
  }

  /**
   * Changes the id of the brick and its sprite. Brick 12 (two-hit) turning
   * into brick 11 keeps its old sprite, just like in the original, because the
   * impact animation covers it.
   */
  public void setId(int id) {
    this.id = id;
    this.addCostume(id);
    this.switchCostume(costumeName(id));
  }

  private void addCostume(int id) {
    String name = costumeName(id);
    this.addCostume(name, "kinetix/assets/images/" + name + ".png");
  }

  private static String costumeName(int id) {
    if (id < 10) {
      return "brick" + id;
    }
    return "brick" + (char) ('a' + id - 10);
  }
}
