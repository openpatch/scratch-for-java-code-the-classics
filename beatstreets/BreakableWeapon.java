package beatstreets;

import org.openpatch.scratch.Vector2;

/**
 * A weapon which breaks after being used a number of times, such as the stick
 * and the chain.
 */
public abstract class BreakableWeapon extends Weapon {

  private int breakCounter;

  protected BreakableWeapon(Play game, Vector2 pos, String name, int durability) {
    super(game, name, name, pos, 1, 160, 160, 0, 0.5, 0.996, false);
    this.breakCounter = durability;
  }

  @Override
  public void dropped() {
    super.dropped();
    this.imageName = this.name;
  }

  @Override
  public double getDrawOrderOffset() {
    // Used for a stick or chain on the ground. The default draw order means
    // it is sometimes drawn on top of a character standing on it, but changing
    // the Y anchor point also has some undesirable effects
    return -50;
  }

  @Override
  public void used() {
    this.breakCounter -= 1;
    if (this.breakCounter == 0) {
      this.onBreak();
    }
  }

  @Override
  public boolean isBroken() {
    return this.breakCounter <= 0;
  }

  /** Called when the weapon breaks. */
  protected abstract void onBreak();
}
