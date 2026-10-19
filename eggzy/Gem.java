package eggzy;

/**
 * A gem pickup. Collecting a gem gains bonus time. The gems cycle through four
 * types so that a run of gems shows a variety of colours.
 */
public class Gem extends EzActor {

  /** The type of the next gem to be created, cycling 1 to 4. */
  private static int nextType = 1;

  private final Play game;
  public final int type;
  public boolean collected = false;

  public Gem(Play game, double x, double y) {
    super(x, y, Play.ANCHOR_CENTRE_BOTTOM);
    this.game = game;

    // Choose which type of gem we're going to be
    this.type = nextType;
    nextType += 1;
    if (nextType >= 5) {
      nextType = 1;
    }
  }

  @Override
  public void run() {
    if (!this.isPlaying()) {
      return;
    }
    // Does the player exist, and is it colliding with us?
    if (this.game.player != null
        && this.game.player.collidePoint(this.centerX(), this.centerY())) {
      this.game.gainTime(this.game.timePickupBonus, this.centerX(), this.centerY());
      this.game.playSound("collect0");
      this.collected = true;
    }

    this.setImage("gem" + this.type + "_" + (this.game.timer / 6) % 4);

    super.run();
  }

  /** Called when a new game starts, to reset the next gem type. */
  public static void newGame() {
    nextType = 1;
  }
}
