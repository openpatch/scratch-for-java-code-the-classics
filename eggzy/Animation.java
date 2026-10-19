package eggzy;

/**
 * A frame-based animation, such as the one that appears when picking up a gem
 * or losing a life. Some animations rise up after a while.
 */
public class Animation extends EzActor {

  private final String imageFormatStr;
  private final int numFrames;
  private final int frameInterval;
  private final int riseTime;

  private int timer;

  public Animation(double x, double y, String imageFormatStr, int numFrames,
      int frameInterval, double anchorY, int initialDelay, int riseTime) {
    super(x, y, anchorY);
    this.imageFormatStr = imageFormatStr;
    this.numFrames = numFrames;
    this.frameInterval = frameInterval;
    this.riseTime = riseTime;
    this.timer = -initialDelay;
    this.updateImage();
  }

  @Override
  public void run() {
    if (!this.isPlaying()) {
      return;
    }
    this.timer += 1;
    this.updateImage();

    // Some animations start rising up after a certain time
    if (this.riseTime > -1 && this.timer > this.riseTime) {
      this.y -= 1;
    }

    super.run();
  }

  private void updateImage() {
    if (this.timer < 0) {
      this.setImage("blank");
    } else {
      int frame = Math.min(this.timer / this.frameInterval, this.numFrames - 1);
      this.setImage(this.imageFormatStr.replace("{0}", Integer.toString(frame)));
    }
  }

  public boolean finished() {
    return this.timer / this.frameInterval >= this.numFrames;
  }
}
