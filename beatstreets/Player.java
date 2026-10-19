package beatstreets;

import java.util.ArrayList;
import java.util.List;
import org.openpatch.scratch.Random;
import org.openpatch.scratch.Vector2;

public class Player extends Fighter {

  private final Controls controls;
  private int extraLifeTimer = 0;

  public Player(Play game, Controls controls) {
    // Anchor point just above the bottom of the sprite
    super(game, "blank", new Vector2(400, 400), 160, 256, new Vector2(3, 2), "hero", 30, 8, 500,
        new Vector2(25, 20), 3, null, true, null);
    this.controls = controls;
  }

  @Override
  public void run() {
    if (!this.isPlaying()) {
      super.run();
      return;
    }
    super.run();

    this.extraLifeTimer -= 1;

    // Check for collecting powerups
    for (Powerup powerup : new ArrayList<>(this.game.powerups)) {
      if (powerup.vpos.sub(this.vpos).length() < 30) {
        powerup.collect(this);
      }
    }
  }

  @Override
  protected Attack determineAttack() {
    // Do we have a weapon?
    if (this.weapon != null) {
      // We cannot attack during the pickup animation
      if (this.pickupAnimation == null && this.controls.buttonPressed(0)) {
        return Attack.get(this.weapon.name);
      }
    } else if (this.controls.buttonPressed(0)) {
      // In a combo?
      if (this.lastAttack != null && this.lastAttack.comboNext != null
          && this.attackTimer >= -30) {
        // The only combo we have is where you press button 0 three times to do
        // a sequence of punches ending in an uppercut
        String nextAttack = this.lastAttack.comboNext.get(0);
        if (nextAttack != null) {
          return Attack.get(nextAttack);
        }
      }

      // Not in a combo, just return the default attack
      return Attack.get("punch");
    } else if (this.controls.buttonPressed(1)) {
      return Random.randomInt(0, 1) == 0 ? Attack.get("kick") : Attack.get("highkick");
    } else if (this.controls.buttonPressed(2)) {
      return Attack.get("elbow");
    } else if (this.controls.buttonPressed(3)) {
      return Attack.get("flyingkick");
    }

    return null;
  }

  @Override
  protected boolean determinePickUpWeapon() {
    return this.controls.buttonPressed(0);
  }

  @Override
  protected boolean determineDropWeapon() {
    return this.weapon != null && this.controls.buttonPressed(1);
  }

  @Override
  protected List<Fighter> getOpponents() {
    return new ArrayList<>(this.game.enemies);
  }

  @Override
  protected Vector2 getMoveTarget() {
    // Our target position is our current position offset based on the control
    // inputs and speed
    return this.vpos.add(new Vector2(this.controls.getX() * this.speed.getX(),
        this.controls.getY() * this.speed.getY()));
  }

  @Override
  protected Integer getDesiredFacing() {
    double dx = this.controls.getX();
    if (dx != 0) {
      this.facingX = Util.sign(dx);
    }
    // Keep facing the same direction as before if there is no X input
    return this.facingX;
  }

  @Override
  public double getDrawOrderOffset() {
    // The player is considered to be in front of another object with the same Y pos
    return 1;
  }

  public void gainExtraLife() {
    this.lives += 1;
    this.extraLifeTimer = 30;
  }

  public int getExtraLifeTimer() {
    return this.extraLifeTimer;
  }
}
