package beatstreets;

import org.openpatch.scratch.Random;
import org.openpatch.scratch.Vector2;

public class EnemyHoodie extends Enemy {

  public EnemyHoodie(Play game, Vector2 pos, int startTimer) {
    super(game, "blank", pos, 160, 256, "hoodie",
        new String[] {"hoodie_lpunch", "hoodie_rpunch", "hoodie_special"}, startTimer,
        new Vector2(1.2, 1), 12, 500, Game.ENEMY_APPROACH_PLAYER_DISTANCE, new Vector2(25, 20),
        Random.randomInt(0, 2), null, 20);
  }

  @Override
  public void died() {
    super.died();

    // Chance of dropping a stick
    if (Random.randomInt(0, 2) == 0) {
      this.game.addWeapon(new Stick(this.game, new Vector2(this.vpos)));
    }
  }
}
