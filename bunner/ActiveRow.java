package bunner;

import org.openpatch.scratch.Random;

/** Base class for rows containing moving child objects (cars or logs). */
public class ActiveRow extends Row {

  /** Creates the moving child objects of an active row. */
  public interface MoverFactory {
    Mover create(Play play, int dx, double x, double y);
  }

  private final MoverFactory factory;
  private double timer = 0;

  public ActiveRow(Play play, MoverFactory factory, int[] dxs, String baseImage, int index, double y) {
    super(play, baseImage, index, y);

    this.factory = factory;
    this.dx = dxs[Random.randomInt(0, dxs.length - 1)];

    // Populate the row with child objects (cars or logs)
    double x = -Play.WIDTH / 2.0 - 70;
    while (x < Play.WIDTH / 2.0 + 70) {
      x += Random.randomInt(240, 480);
      this.children.add(
          factory.create(this.play, this.dx, Play.WIDTH / 2.0 + (this.dx > 0 ? x : -x), 0));
    }
  }

  @Override
  public void run() {
    if (!this.isPlaying()) {
      return;
    }
    super.run();

    // Remove children which are too far off the edge of the screen
    this.children.removeIf(
        c -> {
          if (c.mx <= -70 || c.mx >= Play.WIDTH + 70) {
            c.remove();
            return true;
          }
          return false;
        });

    this.timer -= 1;

    // Create new child objects on a random interval
    if (this.timer < 0) {
      Mover child =
          this.factory.create(this.play, this.dx, this.dx < 0 ? Play.WIDTH + 70 : -70, 0);
      child.row = this;
      child.my = this.my + child.relY;
      child.sync();
      this.children.add(child);
      this.play.add(child);
      this.timer = (1 + Random.random()) * (240 / Math.abs(this.dx));
    }
  }
}
