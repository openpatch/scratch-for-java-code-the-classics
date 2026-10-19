package cavern;

import org.openpatch.scratch.KeyCode;
import org.openpatch.scratch.Operators;

public class Player extends GravityActor {

  public int lives = 2;
  public int score = 0;
  public int health = 3;

  private int directionX = 1;
  private int fireTimer = 0;
  private int hurtTimer = 100;
  private Orb blowingOrb = null;

  public Player(Play play) {
    super(play, 0, 0);

    this.addCostume("blank", "cavern/assets/images/blank.png");
    this.addCostume("still", "cavern/assets/images/still.png");
    this.addCostume("recoil0", "cavern/assets/images/recoil0.png");
    this.addCostume("recoil1", "cavern/assets/images/recoil1.png");
    this.addCostume("fall0", "cavern/assets/images/fall0.png");
    this.addCostume("fall1", "cavern/assets/images/fall1.png");
    this.addCostume("blow0", "cavern/assets/images/blow0.png");
    this.addCostume("blow1", "cavern/assets/images/blow1.png");

    for (int i = 0; i < 4; i++) {
      this.addCostume("run0" + i, String.format("cavern/assets/images/run0%d.png", i));
      this.addCostume("run1" + i, String.format("cavern/assets/images/run1%d.png", i));
    }

    this.reset();
  }

  public void reset() {
    this.mx = Play.WIDTH / 2;
    this.my = 100;
    this.velY = 0;
    this.directionX = 1;
    this.fireTimer = 0;
    this.hurtTimer = 100;
    this.health = 3;
    this.blowingOrb = null;
    this.switchCostume("still");
    this.sync();
  }

  /** Check for collision between player and bolt. */
  public boolean hitTest(Bolt bolt) {
    if (this.containsPoint(bolt.mx, bolt.my) && this.hurtTimer < 0) {
      this.hurtTimer = 200;
      this.health -= 1;
      this.velY = -12;
      this.landed = false;
      this.directionX = bolt.directionX;

      if (this.health > 0) {
        this.play.playRandom("ouch", 4);
      } else {
        this.play.playSound("die0");
      }
      return true;
    }
    return false;
  }

  @Override
  public void run() {
    if (this.frozen()) {
      return;
    }

    boolean space = this.isKeyPressed(KeyCode.SPACE);
    boolean spacePressed = this.play.spaceJustPressed();

    this.updateGravity(this.health > 0);

    this.fireTimer -= 1;
    this.hurtTimer -= 1;

    if (this.landed) {
      this.hurtTimer = Math.min(this.hurtTimer, 100);
    }

    int dx = 0;

    if (this.hurtTimer > 100) {
      // We've just been hurt
      if (this.health > 0) {
        this.move(this.directionX, 0, 4);
      } else {
        if (this.top() >= Play.HEIGHT * 1.5) {
          this.lives -= 1;
          this.reset();
          return;
        }
      }
    } else {
      // We're not hurt
      if (this.isKeyPressed(KeyCode.LEFT)) {
        dx = -1;
      } else if (this.isKeyPressed(KeyCode.RIGHT)) {
        dx = 1;
      }

      if (dx != 0) {
        this.directionX = dx;

        if (this.fireTimer < 10) {
          this.move(dx, 0, 4);
        }
      }

      if (spacePressed && this.fireTimer <= 0 && this.play.count(Orb.class) < 5) {
        int x = Operators.constrain(this.mx + this.directionX * 38, 70, 730);
        int y = this.my - 35;
        this.blowingOrb = new Orb(this.play, x, y, this.directionX);
        this.play.add(this.blowingOrb);
        this.play.playRandom("blow", 4);
        this.fireTimer = 20;
      }

      if (this.isKeyPressed(KeyCode.UP) && this.velY == 0 && this.landed) {
        this.velY = -16;
        this.landed = false;
        this.play.playSound("jump0");
      }
    }

    // Holding down space causes the current orb (if there is one) to be blown further
    if (space) {
      if (this.blowingOrb != null) {
        this.blowingOrb.blownFrames += 4;
        if (this.blowingOrb.blownFrames >= 120) {
          this.blowingOrb = null;
        }
      }
    } else {
      this.blowingOrb = null;
    }

    // Set sprite image. If we're currently hurt, the sprite flashes on and off
    this.switchCostume("blank");
    if (this.hurtTimer <= 0 || this.hurtTimer % 2 == 1) {
      String dirIndex = this.directionX > 0 ? "1" : "0";
      if (this.hurtTimer > 100) {
        if (this.health > 0) {
          this.switchCostume("recoil" + dirIndex);
        } else {
          this.switchCostume("fall" + (this.play.timer / 4 % 2));
        }
      } else if (this.fireTimer > 0) {
        this.switchCostume("blow" + dirIndex);
      } else if (dx == 0) {
        this.switchCostume("still");
      } else {
        this.switchCostume("run" + dirIndex + (this.play.timer / 8 % 4));
      }
    }

    this.sync();
  }
}
