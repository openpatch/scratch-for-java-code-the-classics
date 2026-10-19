package cavern;

import org.openpatch.scratch.Sprite;

/**
 * A single level block. Blocks are placed behind all actors.
 */
public class Block extends Sprite {

  public Block(Play play, int x, int y, int level) {
    this.addCostume("block", String.format("cavern/assets/images/block%d.png", level % 4));
    this.setPosition(x - Play.WIDTH / 2.0, Play.HEIGHT / 2.0 - y);
  }
}
