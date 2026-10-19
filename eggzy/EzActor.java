package eggzy;

import org.openpatch.scratch.Sprite;
import org.openpatch.scratch.Window;
import org.openpatch.scratch.extensions.text.BitmapText;

/**
 * Base class for all actors, standing in for the Pygame Zero Actor. Positions
 * are kept in the original game's pixel coordinates (origin top-left, y
 * growing downwards), and an anchor describes which point of the current
 * costume sits at that position, exactly like pgzero's anchor attribute.
 *
 * <p>Each sprite owns its behavior in {@link #run()} and synchronizes its position.
 * {@link #sync()} recomputes the costume's placement from the
 * anchor, so costumes of different sizes align the same way they do in the
 * original.
 */
public class EzActor extends Sprite {

  /** Y anchor value: the middle of the costume. */
  public static final double ANCHOR_MIDDLE = -1;

  /** Y anchor value: the bottom edge of the costume. */
  public static final double ANCHOR_BOTTOM = -2;

  /** Path prefix of the game's images, relative to the repository root. */
  public static final String IMAGES = "eggzy/assets/images/";

  /**
   * A text drawn with one of the game's bitmap fonts (e.g. "font" or
   * "fontlrg"), as part of the user interface.
   */
  public static BitmapText bitmapText(String font) {
    BitmapText text = new BitmapText(IMAGES + font + "%03d.png");
    // These symbols substitute for the controller button images in text
    text.addGlyph('%', IMAGES + "xb_a.png");
    text.addGlyph('#', IMAGES + "xb_b.png");
    text.setIsUI(true);
    return text;
  }

  /** Position in pixel coordinates. */
  protected double x;
  protected double y;

  /**
   * The Y anchor: a pixel offset from the top edge of the costume, or
   * ANCHOR_MIDDLE or ANCHOR_BOTTOM. The X anchor is the centre, except for
   * actors that use the left edge (the doors).
   */
  protected double anchorY;

  /** Is the anchor on the left edge of the costume rather than the centre? */
  protected boolean anchorLeftEdge;

  public EzActor(double x, double y, double anchorY) {
    this(x, y, anchorY, false);
  }

  public EzActor(double x, double y, double anchorY, boolean anchorLeftEdge) {
    this.x = x;
    this.y = y;
    this.anchorY = anchorY;
    this.anchorLeftEdge = anchorLeftEdge;
    this.addCostume("blank", IMAGES + "blank.png");
    this.switchCostume("blank");
  }

  /**
   * Sets the current image, loading the costume on first use, like assigning
   * to a pgzero Actor's image attribute.
   */
  public void setImage(String name) {
    this.addCostume(name, IMAGES + name + ".png");
    this.switchCostume(name);
  }

  /** The name of the current image, like a pgzero Actor's image attribute. */
  public String getImage() {
    return this.getCurrentCostumeName();
  }

  /** Left edge of the current costume, in pixel coordinates. */
  public double imageLeft() {
    return this.anchorLeftEdge ? this.x : this.x - this.getWidth() / 2.0;
  }

  /** Top edge of the current costume, in pixel coordinates. */
  public double imageTop() {
    if (this.anchorY == ANCHOR_MIDDLE) {
      return this.y - this.getHeight() / 2.0;
    }
    if (this.anchorY == ANCHOR_BOTTOM) {
      return this.y - this.getHeight();
    }
    return this.y - this.anchorY;
  }

  /** Centre of the current costume, in pixel coordinates. */
  public double centerX() {
    return this.imageLeft() + this.getWidth() / 2.0;
  }

  public double centerY() {
    return this.imageTop() + this.getHeight() / 2.0;
  }

  /** Rectangle of the current costume, in pixel coordinates. */
  public Rect imageRect() {
    return new Rect(this.imageLeft(), this.imageTop(), this.getWidth(), this.getHeight());
  }

  /** Does the given point lie within the current costume? */
  public boolean collidepoint(double px, double py) {
    return this.imageRect().collidepoint(px, py);
  }

  /** A stage switch can leave callbacks from the old frame's snapshot pending. */
  protected boolean isPlaying() {
    return this.getStage() != null && Window.getInstance().getStage() == this.getStage();
  }

  /** Positions this sprite; subclasses call it after their own behavior. */
  @Override
  public void run() {
    if (this.isPlaying()) {
      this.sync();
    }
  }

  /** Transfers the pixel position to the sprite position. */
  public void sync() {
    this.setX(this.centerX());
    this.setY(-this.centerY());
  }
}
