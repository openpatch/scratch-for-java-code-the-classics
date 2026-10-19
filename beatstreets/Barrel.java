package beatstreets;

import java.util.ArrayList;
import java.util.List;
import org.openpatch.scratch.Vector2;

public class Barrel extends Weapon {

  private Fighter lastThrower = null;
  private int frame = 0;

  public Barrel(Play game, Vector2 pos) {
    super(game, "barrel", "barrel_upright", pos, 2, 160, 190, 0.75, 0.96, 0.996, true);
  }

  @Override
  public void run() {
    if (!this.isPlaying()) {
      super.run();
      return;
    }
    // Apply weapon physics before checking barrel collisions
    super.run();

    // If moving, look for people to bash into. Won't collide if it can be
    // picked up (if it is moving slowly enough).
    if (!this.held && !this.canBePickedUp() && this.vel.getX() != 0) {
      List<Fighter> fighters = new ArrayList<>();
      fighters.add(this.game.player);
      fighters.addAll(this.game.enemies);
      for (Fighter fighter : fighters) {
        // Won't collide with the person who threw it, or with a fighter who is
        // falling (including lying on the ground). Must be within 30 pixels on
        // the X axis and 30 pixels on the Y axis (the Y position doesn't take
        // the height above ground into account, so this is effectively the
        // character's depth in the level). Must also hit within the height of
        // the character, taking the height above ground into account for both
        // the barrel and the fighter.
        int BARREL_HEIGHT = 40;
        double fighterBottomHeight = fighter.heightAboveGround;
        double barrelBottomHeight = this.heightAboveGround - (BARREL_HEIGHT / 2);
        double barrelTopHeight = barrelBottomHeight + BARREL_HEIGHT;

        if (fighter != this.lastThrower
            && fighter.fallingState == Fighter.FallingState.STANDING
            && Math.abs(fighter.vpos.getY() - this.vpos.getY()) < 30
            && Math.abs(this.vpos.getX() - fighter.vpos.getX()) < 30
            && fighterBottomHeight < barrelTopHeight) {
          fighter.hit(this, Attack.get("barrel"));
        }
      }

      // Update the rolling animation
      int facingId = this.vel.getX() > 0 ? 1 : 0;
      this.frame += 1;
      this.imageName = "barrel_roll_" + facingId + "_" + (this.frame / 14 % 4);
    }

    this.drawFrame(this.game.getScrollX(), 0);
  }

  @Override
  public void throwWeapon(int dirX, Fighter thrower) {
    this.dropped();
    this.vel = new Vector2(dirX * Game.BARREL_THROW_VEL_X, Game.BARREL_THROW_VEL_Y);
    this.lastThrower = thrower;

    // Shift position for the throw animation
    this.vpos = new Vector2(this.vpos.getX() + dirX * 104, this.vpos.getY());
  }

  @Override
  public void dropped() {
    super.dropped();
    this.imageName = "barrel_roll_0_0";
  }

  @Override
  public boolean canBePickedUp() {
    return super.canBePickedUp() && this.vel.length() < 1;
  }

  @Override
  public double getDrawOrderOffset() {
    // Consider the barrel to be in front of another object with the same Y
    // pos (including the player, which has a draw offset of 1)
    return 2;
  }
}
