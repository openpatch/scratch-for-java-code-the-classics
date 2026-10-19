package cavern;

import org.openpatch.scratch.Random;

/** Pickups including fruit, extra health and extra life. */
public class Fruit extends GravityActor {

  public static final int APPLE = 0;
  public static final int RASPBERRY = 1;
  public static final int LEMON = 2;
  public static final int EXTRA_HEALTH = 3;
  public static final int EXTRA_LIFE = 4;

  private int type;
  private int timeToLive = 500;

  public Fruit(Play play, int x, int y, int trappedEnemyType) {
    super(play, x, y);

    // Choose which type of fruit we're going to be
    if (trappedEnemyType == Robot.TYPE_NORMAL) {
      this.type = Random.randomInt(Fruit.APPLE, Fruit.LEMON);
    } else {
      int[] types = new int[40];
      int i = 0;
      for (int j = 0; j < 10; j++) {
        types[i++] = Fruit.APPLE;
        types[i++] = Fruit.RASPBERRY;
        types[i++] = Fruit.LEMON;
      }
      for (int j = 0; j < 9; j++) {
        types[i++] = Fruit.EXTRA_HEALTH;
      }
      types[i] = Fruit.EXTRA_LIFE;
      this.type = types[Random.randomInt(0, types.length - 1)];
    }

    for (int t = 0; t < 5; t++) {
      for (int f = 0; f < 3; f++) {
        this.addCostume(
            String.format("fruit%d%d", t, f),
            String.format("cavern/assets/images/fruit%d%d.png", t, f));
      }
    }

    this.switchCostume("fruit" + this.type + "0");
    this.sync();
  }

  @Override
  public void run() {
    if (this.frozen()) {
      return;
    }

    this.updateGravity(true);

    // Does the player exist, and are they colliding with us?
    if (this.play.player != null
        && this.play.player.containsPoint(this.mx, this.my - this.getHeight() / 2.0)) {
      if (this.type == Fruit.EXTRA_HEALTH) {
        this.play.player.health = Math.min(3, this.play.player.health + 1);
        this.play.playSound("bonus0");
      } else if (this.type == Fruit.EXTRA_LIFE) {
        this.play.player.lives += 1;
        this.play.playSound("bonus0");
      } else {
        this.play.player.score += (this.type + 1) * 100;
        this.play.playSound("score0");
      }
      this.timeToLive = 0;
    } else {
      this.timeToLive -= 1;
    }

    if (this.timeToLive <= 0) {
      this.play.add(new Pop(this.play, this.mx, this.my - 27, 0));
      this.remove();
      return;
    }

    int animFrame = new int[] {0, 1, 2, 1}[this.play.timer / 6 % 4];
    this.switchCostume("fruit" + this.type + animFrame);
    this.sync();
  }
}
