package beatstreets;

import org.openpatch.scratch.Random;
import org.openpatch.scratch.Vector2;

public class EnemyBoss extends Enemy {

  public EnemyBoss(Play game, Vector2 pos, int startTimer) {
    super(game, "blank", pos, 160, 280, "boss",
        new String[] {"boss_lpunch", "boss_rpunch", "boss_kick", "boss_grab_player"},
        startTimer, new Vector2(0.9, 0.8), 25, 1000, Game.ENEMY_APPROACH_PLAYER_DISTANCE,
        new Vector2(30, 20), Random.randomInt(0, 2), null, 75);
  }

  @Override
  protected void makeDecision() {
    // The boss can pick up a barrel, if he's not currently holding one. Look
    // for a barrel we can walk to - it must not be held by anyone else and
    // must be on the screen.
    if (this.weapon == null) {
      for (Weapon weapon : this.game.weapons) {
        if (weapon instanceof Barrel && weapon.canBePickedUp() && weapon.onScreen()) {
          // Don't go to a barrel if another enemy is already going to it
          boolean otherEnemiesSameTarget = false;
          for (Enemy enemy : this.game.enemies) {
            if (enemy != this && enemy.targetWeapon == weapon) {
              otherEnemiesSameTarget = true;
              break;
            }
          }
          if (!otherEnemiesSameTarget) {
            // This weapon is OK to go for
            this.state = State.GO_TO_WEAPON;
            this.targetWeapon = weapon;
            return;
          }
        }
      }
    }

    // If we didn't enter the GO_TO_WEAPON state, call the parent method
    super.makeDecision();
  }
}
