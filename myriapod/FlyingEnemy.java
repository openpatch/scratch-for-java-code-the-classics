package myriapod;

import org.openpatch.scratch.Random;

public class FlyingEnemy extends MyrActor {

  public int health = 1;

  private int movingX = 1;
  private int dx;
  private int dy;
  private int type;
  private int timer = 0;

  public FlyingEnemy(Play play, int playerX) {
    super(play);

    // Start on the side of the screen away from the player
    int side;
    if (playerX < 160) {
      side = 1;
    } else if (playerX > 320) {
      side = 0;
    } else {
      side = Random.randomInt(0, 1);
    }

    this.mx = 550 * side - 35;
    this.my = 688;
    this.dx = 1 - 2 * side;
    this.dy = Random.random() < 0.5 ? -1 : 1;
    this.type = Random.randomInt(0, 2);

    for (int t = 0; t < 3; t++) {
      for (int f = 0; f < 3; f++) {
        this.addCostume(
            String.format("meanie%d%d", t, f),
            String.format("myriapod/assets/images/meanie%d%d.png", t, f));
      }
    }

    this.switchCostume("meanie" + this.type + "0");
    this.sync();
  }

  @Override
  public void run() {
    if (this.play.isFrozen()) {
      return;
    }
    this.timer += 1;

    this.mx += this.dx * this.movingX * (3 - Math.abs(this.dy));
    this.my += this.dy * (3 - Math.abs(this.dx * this.movingX));

    if (this.my < 592 || this.my > 784) {
      // Gone too high or low - reverse y direction
      this.movingX = Random.randomInt(0, 1);
      this.dy = -this.dy;
    }

    int animFrame = new int[] {0, 2, 1, 2}[this.timer / 4 % 4];
    this.switchCostume("meanie" + this.type + animFrame);
    this.sync();
  }
}
