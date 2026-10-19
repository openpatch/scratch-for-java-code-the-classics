package cavern;

import org.openpatch.scratch.Random;

public class Orb extends CavernActor {

  public static final int MAX_TIMER = 250;

  public boolean floating = false;
  public Integer trappedEnemyType = null;
  public int blownFrames;
  public int timer = -1;

  private int directionX;

  public Orb(Play play, int x, int y, int dirX) {
    super(play, false);

    this.mx = x;
    this.my = y;
    this.directionX = dirX;
    this.blownFrames = 6;

    for (int i = 0; i < 7; i++) {
      this.addCostume("orb" + i, String.format("cavern/assets/images/orb%d.png", i));
    }
    for (int t = 0; t < 2; t++) {
      for (int i = 0; i < 8; i++) {
        this.addCostume(
            String.format("trap%d%d", t, i),
            String.format("cavern/assets/images/trap%d%d.png", t, i));
      }
    }

    this.switchCostume("orb0");
    this.sync();
  }

  /** Check for collision with a bolt. */
  public boolean hitTest(Bolt bolt) {
    if (this.containsPoint(bolt.mx, bolt.my)) {
      this.timer = Orb.MAX_TIMER - 1;
      return true;
    }
    return false;
  }

  @Override
  public void run() {
    if (this.frozen()) {
      return;
    }

    this.timer += 1;

    if (this.floating) {
      // Float upwards
      this.move(0, -1, Random.randomInt(1, 2));
    } else {
      // Move horizontally. If we hit a block, start floating.
      if (this.move(this.directionX, 0, 4)) {
        this.floating = true;
      }
    }

    if (this.timer == this.blownFrames) {
      this.floating = true;
    } else if (this.timer >= Orb.MAX_TIMER || this.my <= -40) {
      // Pop
      this.play.add(new Pop(this.play, this.mx, this.my, 1));
      if (this.trappedEnemyType != null) {
        this.play.add(new Fruit(this.play, this.mx, this.my, this.trappedEnemyType));
      }
      this.play.playRandom("pop", 4);
      this.remove();
      return;
    }

    if (this.timer < 9) {
      // Orb grows to full size over the course of 9 frames
      this.switchCostume("orb" + (this.timer / 3));
    } else if (this.trappedEnemyType != null) {
      this.switchCostume("trap" + this.trappedEnemyType + (this.timer / 4 % 8));
    } else {
      this.switchCostume("orb" + (3 + ((this.timer - 9) / 8 % 4)));
    }

    this.sync();
  }
}
