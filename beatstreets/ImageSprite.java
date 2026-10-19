package beatstreets;

import org.openpatch.scratch.Sprite;

/**
 * A sprite that can show images by name, adding the costumes lazily, and that
 * carries a fixed sort key for the draw order of the play stage.
 */
public class ImageSprite extends Sprite {

  /** The draw order key used by the play stage's sprite sorting. */
  public double layerKey;

  /**
   * Switches to the image of the given name, adding its costume on first use.
   * The name "blank" hides the sprite.
   */
  public void showImage(String name) {
    if (name.equals("blank")) {
      this.hide();
      return;
    }
    this.addCostume(name, "beatstreets/assets/images/" + name + ".png");
    this.switchCostume(name);
    this.show();
  }
}
