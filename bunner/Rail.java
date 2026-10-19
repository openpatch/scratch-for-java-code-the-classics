package bunner;

import org.openpatch.scratch.Random;

public class Rail extends Row {

  private final Row predecessor;

  public Rail(Play play, Row predecessor, int index, double y) {
    super(play, "rail", index, y);
    this.predecessor = predecessor;
  }

  @Override
  public void run() {
    if (!this.isPlaying()) {
      return;
    }
    super.run();

    // Only Rail rows with index 1 have trains on them
    if (this.index == 1) {
      // Remove trains which are too far off the edge of the screen
      this.children.removeIf(
          c -> {
            if (c.mx <= -1000 || c.mx >= Play.WIDTH + 1000) {
              c.remove();
              return true;
            }
            return false;
          });

      // If on-screen, no train exists, and with a 1% chance every frame,
      // create a train
      if (this.my < this.play.scrollPos + Play.HEIGHT
          && this.children.isEmpty()
          && Random.random() < 0.01) {
        int dx = Random.random() < 0.5 ? -20 : 20;
        Train train = new Train(this.play, dx, dx < 0 ? Play.WIDTH + 1000 : -1000, -13);
        train.row = this;
        train.my = this.my + train.relY;
        train.sync();
        this.children.add(train);
        this.play.add(train);
        this.play.playRandom("bell", 1);
        this.play.playRandom("train", 2);
      }
    }
  }

  @Override
  public int[] checkCollision(double x) {
    if (this.index == 2 && this.predecessor.collide(x, 0) != null) {
      this.play.playSound("splat0");
      return new int[] {Bunner.STATE_SPLAT, 8};
    }
    return new int[] {Bunner.STATE_ALIVE, 0};
  }

  @Override
  public void playLandingSound() {
    this.play.playSound("grass0");
  }

  @Override
  public Row next() {
    if (this.index < 3) {
      return new Rail(this.play, this, this.index + 1, this.my - Play.ROW_HEIGHT);
    }
    if (Random.random() < 0.5) {
      return new Road(this.play, this, 0, this.my - Play.ROW_HEIGHT);
    }
    return new Water(this.play, this, 0, this.my - Play.ROW_HEIGHT);
  }
}
