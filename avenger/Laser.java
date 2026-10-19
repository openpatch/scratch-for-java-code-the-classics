package avenger;

import java.util.List;

/** A laser fired by the player. */
public class Laser extends WrapActor {

  private final double velX;
  private int animTimer;

  public Laser(Play game, double x, double y, double velX) {
    super(game, velX > 0 ? "laser_0_0" : "laser_1_0");
    this.x = x + velX;
    this.y = y;
    this.velX = velX;
    this.animTimer = 0;
    for (int facing = 0; facing < 2; facing++) {
      this.addCostume("laser_" + facing + "_1", Utils.img("laser_" + facing + "_1"));
    }
    game.playSound("player_shoot0");
  }

  /**
   * Moves the laser and removes it after a hit or when it is too far from
   * the player.
   */
  @Override
  public void run() {
    if (!this.isPlaying()) {
      return;
    }
    this.wrap();

    // Update position
    this.x += this.velX;

    // Update sprite
    this.animTimer += 1;
    int facingIdx = this.velX > 0 ? 0 : 1;
    this.switchCostume("laser_" + facingIdx + "_" + Math.min(1, this.animTimer / 8));

    boolean tooFar = Math.abs(this.x - this.game.player.x) > 800;

    // This kills any enemy or human that collides with the laser. The enemies
    // are iterated over a copy, because killing a pod releases swarmers
    boolean collisions = false;
    for (Enemy enemy : List.copyOf(this.game.enemies)) {
      collisions |= enemy.laserHitTest(this.x, this.y);
    }
    for (Human human : List.copyOf(this.game.humans)) {
      collisions |= human.laserHitTest(this.x, this.y);
    }

    if (tooFar || collisions) {
      this.game.lasers.remove(this);
      this.remove();
      return;
    }
    super.run();
  }
}
