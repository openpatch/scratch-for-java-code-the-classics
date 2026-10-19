package beatstreets;

import org.openpatch.scratch.Vector2;

/** The scooter on its own, with the rider having been knocked off. */
public class Scooter extends ScrollHeightActor {

  private final int facingX;
  private final Integer colourVariant;
  private double velX;
  public int frame = 0;

  public Scooter(Play game, Vector2 pos, int facingX, Integer colourVariant) {
    super(game, "blank", pos, 160, 256, false);
    this.facingX = facingX;
    this.colourVariant = colourVariant;
    this.velX = -facingX * 8;
    this.game.playSound("scooter_fall", 1);
  }

  @Override
  public void run() {
    if (!this.isPlaying()) {
      super.run();
      return;
    }
    this.frame += 1;
    this.vpos = new Vector2(this.vpos.getX() + this.velX, this.vpos.getY());
    this.velX *= 0.94;
    int facingId = this.facingX > 0 ? 1 : 0;
    this.imageName = "scooterboy_bike_" + facingId + "_" + Math.min(this.frame / 30, 2) + "_"
        + this.colourVariant;

    super.run();
  }

  @Override
  public double getDrawOrderOffset() {
    return -1;
  }
}
