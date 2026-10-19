package beatstreets;

import org.openpatch.scratch.Vector2;

/**
 * Base class for all objects of the scrolling level. Positions are kept in the
 * original's world pixel coordinates in {@link #vpos} (origin top-left, y
 * growing downwards); the sprite position is derived from them in
 * {@link #drawFrame(double, double)}, taking the scroll offset and the height
 * above ground into account.
 */
public class ScrollHeightActor extends ImageSprite {

  protected final Play game;

  /** The object's position in the scrolling level. */
  public Vector2 vpos;

  /** How far above the ground the object currently is. */
  public double heightAboveGround;

  /** The separate shadow sprite, or null if the shadow is part of the image. */
  public final ShadowSprite shadow;

  /** The object's on-screen X position, updated when the object is drawn. */
  public double screenX;

  /** The image currently shown, or "blank" if the object is invisible. */
  protected String imageName;

  public ScrollHeightActor(Play game, String image, Vector2 pos, double anchorX, double anchorY,
      boolean separateShadow) {
    this.game = game;
    this.imageName = image;
    this.vpos = pos;
    this.screenX = pos.getX();
    this.setRotationCenter(anchorX, anchorY);
    this.shadow = separateShadow ? new ShadowSprite(anchorX, anchorY, this) : null;
    this.hide(); // nothing to draw until the first drawFrame
  }

  /** Returns the image that should be shown this frame. */
  public String determineImage() {
    return this.imageName;
  }

  /**
   * Sets the costumes and the sprite positions for this frame, with the given
   * scroll offset. Mirrors the original's draw() method.
   */
  public void drawFrame(double offsetX, double offsetY) {
    String image = this.determineImage();

    if (this.shadow != null) {
      this.shadow.showImage(image.equals("blank") ? "blank" : image + "_shadow");
      this.shadow.setPosition(this.vpos.getX() - offsetX - Game.WIDTH / 2.0,
          Game.HEIGHT / 2.0 - (this.vpos.getY() - offsetY));
    }

    this.showImage(image);
    this.setPosition(this.vpos.getX() - offsetX - Game.WIDTH / 2.0,
        Game.HEIGHT / 2.0 - (this.vpos.getY() - offsetY - this.heightAboveGround));
    this.screenX = this.vpos.getX() - offsetX;
  }

  /** Is the object currently visible on the screen? */
  public boolean onScreen() {
    return this.screenX > 0 && this.screenX < Game.WIDTH;
  }

  /**
   * Extra offset applied to the Y position when deciding the draw order. See
   * the subclasses for the special cases.
   */
  public double getDrawOrderOffset() {
    return 0;
  }

  /** The key the play stage sorts the objects by. */
  public double drawSortKey() {
    return this.vpos.getY() + this.getDrawOrderOffset();
  }

  protected boolean isPlaying() {
    return this.getStage() != null && this.game.isPlaying();
  }

  /** Subclasses call this after their behavior to place the costume and shadow. */
  @Override
  public void run() {
    if (this.getStage() != null) {
      this.drawFrame(this.game.getScrollX(), 0);
    }
  }
}
