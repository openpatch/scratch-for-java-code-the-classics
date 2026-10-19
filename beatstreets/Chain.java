package beatstreets;

import org.openpatch.scratch.Random;
import org.openpatch.scratch.Vector2;

public class Chain extends BreakableWeapon {

  public Chain(Play game, Vector2 pos) {
    super(game, pos, "chain", Random.randomInt(18, 25));
  }

  @Override
  protected void onBreak() {
    this.game.playSound("chain_break", 1);
  }
}
