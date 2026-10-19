package beatstreets;

import org.openpatch.scratch.Random;
import org.openpatch.scratch.Vector2;

public class Stick extends BreakableWeapon {

  public Stick(Play game, Vector2 pos) {
    super(game, pos, "stick", Random.randomInt(12, 16));
  }

  @Override
  protected void onBreak() {
    this.game.playSound("stick_break", 1);
  }
}
