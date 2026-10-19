package bunner;

public class Water extends ActiveRow {

  public Water(Play play, Row predecessor, int index, double y) {
    super(
        play,
        Log::new,
        // Logs on alternate rows move in opposite directions
        predecessor.dx == 0
            ? new int[] {-2, -1, 1, 2}
            : predecessor.dx > 0 ? new int[] {-2, -1} : new int[] {1, 2},
        "water",
        index,
        y);
  }

  @Override
  public void run() {
    if (!this.isPlaying()) {
      return;
    }
    super.run();

    for (BunActor child : this.children) {
      if (child instanceof Log log) {
        // Make the log dip down into the water if the player is standing on it
        if (this.play.bunner != null
            && this.my == this.play.bunner.my
            && log == this.collide(this.play.bunner.mx, -4)) {
          log.relY = 2;
        } else {
          log.relY = 0;
        }
      }
    }
  }

  @Override
  public int push() {
    return this.dx;
  }

  @Override
  public int[] checkCollision(double x) {
    // If we're colliding with a log, that's a good thing!
    if (this.collide(x, -4) != null) {
      return new int[] {Bunner.STATE_ALIVE, 0};
    }
    this.play.playSound("splash0");
    return new int[] {Bunner.STATE_SPLASH, 0};
  }

  @Override
  public void playLandingSound() {
    this.play.playSound("log0");
  }

  @Override
  public Row next() {
    // After 2 water rows, there's a 50-50 chance of the next row being
    // either another water row, or a dirt row
    if (this.index == 7 || (this.index >= 1 && org.openpatch.scratch.Random.random() < 0.5)) {
      return new Dirt(this.play, this, org.openpatch.scratch.Random.randomInt(4, 6), this.my - Play.ROW_HEIGHT);
    }
    return new Water(this.play, this, this.index + 1, this.my - Play.ROW_HEIGHT);
  }
}
