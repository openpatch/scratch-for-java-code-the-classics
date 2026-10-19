package beatstreets;

import org.openpatch.scratch.Random;
import org.openpatch.scratch.Vector2;

public class EnemyVax extends Enemy {

  public EnemyVax(Play game, Vector2 pos, int startTimer) {
    super(game, "blank", pos, 160, 256, "vax",
        new String[] {"vax_lpunch", "vax_rpunch", "vax_pound"}, startTimer,
        new Vector2(1, 1), 15, 500, Game.ENEMY_APPROACH_PLAYER_DISTANCE, new Vector2(25, 20),
        Random.randomInt(0, 2), null, 20);
  }
}
