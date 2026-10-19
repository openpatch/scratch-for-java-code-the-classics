package eggzy;

/**
 * The door which prevents the player from leaving a level until all gems have
 * been collected. The exit door opens once the last gem is picked up.
 */
public class Door extends EzActor {

  private final Play game;
  private final String biomeName;
  private final String variant;

  private boolean opening;
  private final int lastFrame;
  private int frame;

  public Door(Play game, double x, double y, String biomeName, String variant,
      boolean alreadyOpen) {
    // The door's position refers to the top-left corner of the sprite
    super(x, y, 0, true);
    this.game = game;
    this.biomeName = biomeName;
    this.variant = variant;
    this.opening = alreadyOpen;
    this.lastFrame = biomeName.equals("castle") ? 15 : 13;
    this.frame = alreadyOpen ? this.lastFrame : 0;
    this.setImage("door_" + this.biomeName + "_" + this.variant + "_" + this.frame);
  }

  @Override
  public void run() {
    if (!this.isPlaying()) {
      return;
    }
    if (this.opening && this.frame < this.lastFrame && this.game.timer % 3 == 0) {
      this.frame += 1;
      this.setImage("door_" + this.biomeName + "_" + this.variant + "_" + this.frame);
    }

    super.run();
  }

  public void open() {
    this.opening = true;
  }

  public boolean isFullyOpen() {
    return this.frame == this.lastFrame;
  }
}
