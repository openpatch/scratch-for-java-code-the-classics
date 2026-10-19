package myriapod;

import org.openpatch.scratch.Random;

public class Rock extends MyrActor {

  public int cellX;
  public int cellY;
  public int health;
  public int showHealth;
  public int timer = 1;

  private int type;
  private boolean centerAnchor;

  public Rock(Play play, int x, int y, boolean totem) {
    super(play);

    this.cellX = x;
    this.cellY = y;

    // Centre of the grid cell
    this.mx = x * 32 + 32;
    this.my = y * 32 + 16;

    this.type = Random.randomInt(0, 3);

    for (int c = 0; c < 3; c++) {
      for (int t = 0; t < 4; t++) {
        for (int h = 0; h < 5; h++) {
          this.addCostume(
              String.format("rock%d%d%d", c, t, h),
              String.format("myriapod/assets/images/rock%d%d%d.png", c, t, h));
        }
      }
    }

    if (totem) {
      // Totem rocks take five hits and give bonus points
      this.play.playGameSound("totem_create0");
      this.health = 5;
      this.showHealth = 5;
      this.centerAnchor = false;
    } else {
      this.health = Random.randomInt(3, 4);
      this.showHealth = 1;
      this.centerAnchor = true;
    }

    this.switchCostume("rock0" + this.type + Math.max(this.showHealth - 1, 0));
    this.sync();
  }

  /**
   * Damages the rock. Returns true, if the rock has lost all its health.
   */
  public boolean damage(int amount, boolean damagedByBullet) {
    if (damagedByBullet && this.health == 5) {
      this.play.playGameSound("totem_destroy0");
      this.play.score += 100;
    } else {
      if (amount > this.health - 1) {
        this.play.playGameSound("rock_destroy0");
      } else {
        this.play.playRandom("hit", 4);
      }
    }

    this.play.addExplosion(new Explosion(this.play, this.mx, this.my, this.health == 5 ? 2 : 0));
    this.health -= amount;
    this.showHealth = this.health;
    this.centerAnchor = true;

    return this.health < 1;
  }

  @Override
  public void run() {
    if (this.play.isFrozen()) {
      return;
    }
    this.timer += 1;

    // Every other frame, update the growing animation
    if (this.timer % 2 == 1 && this.showHealth < this.health) {
      this.showHealth += 1;
    }

    if (this.health == 5 && this.timer > 200) {
      // Totem rocks turn into normal rocks if not shot within 200 frames
      this.damage(1, false);
      return;
    }

    int colour = Math.max(this.play.wave, 0) % 3;
    this.switchCostume(
        String.format("rock%d%d%d", colour, this.type, Math.max(this.showHealth - 1, 0)));

    this.sync();
  }

  /** Totem rocks use a lower anchor point until they are damaged. */
  @Override
  public void sync() {
    this.setX(this.mx - Play.WIDTH / 2.0);
    if (this.centerAnchor) {
      this.setY(Play.HEIGHT / 2.0 - this.my);
    } else {
      this.setY(Play.HEIGHT / 2.0 - (this.my - 14));
    }
  }
}
