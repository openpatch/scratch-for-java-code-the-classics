package avenger;

import org.openpatch.scratch.Operators;
import org.openpatch.scratch.Vector2;

/** A bullet fired by an enemy. */
public class Bullet extends WrapActor {

  private final double velX;
  private final double velY;

  public Bullet(Play game, double x, double y, Vector2 velocity) {
    super(game, "blank");
    this.x = x;
    this.y = y;
    this.velX = velocity.getX();
    this.velY = velocity.getY();
    this.addCostume("bullet0", Utils.img("bullet0"));
    this.addCostume("bullet1", Utils.img("bullet1"));

    // The laser sound is played at a volume which depends on how far away
    // from the player the bullet was fired
    double distance = Math.hypot(game.player.x - x, game.player.y - y);
    double volume = Operators.constrain(Operators.map(distance, 400, 2500, 1, 0), 0, 1);
    game.playSound("enemy_laser0", volume);
  }

  /**
   * Moves the bullet and removes it after hitting the player or travelling
   * too far from them.
   */
  @Override
  public void run() {
    if (!this.isPlaying()) {
      return;
    }
    this.wrap();

    this.x += this.velX;
    this.y += this.velY;

    // Update sprite animation
    this.switchCostume("bullet" + this.game.timer / 4 % 2);

    boolean tooFar = this.x < this.game.player.x - Play.WIDTH
        || this.x > this.game.player.x + Play.WIDTH;
    if (this.game.player.hitTest(this.x, this.y) || tooFar) {
      this.game.bullets.remove(this);
      this.remove();
      return;
    }
    super.run();
  }
}
