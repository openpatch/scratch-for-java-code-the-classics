package myriapod;

import org.openpatch.scratch.KeyCode;

public class Player extends MyrActor {

  public static final int INVULNERABILITY_TIME = 100;
  public static final int RESPAWN_TIME = 100;
  public static final int RELOAD_TIME = 10;

  public int lives = 3;
  public boolean alive = true;
  public int timer = 0;

  private int direction = 0;
  private int frame = 0;
  private int fireTimer = 0;

  public Player(Play play, int x, int y) {
    super(play);

    this.mx = x;
    this.my = y;

    this.addCostume("blank", "myriapod/assets/images/blank.png");
    for (int d = 0; d < 4; d++) {
      for (int f = 0; f < 3; f++) {
        this.addCostume(
            String.format("player%d%d", d, f),
            String.format("myriapod/assets/images/player%d%d.png", d, f));
      }
    }

    this.switchCostume("player00");
    this.sync();
  }

  /** Moves pixel by pixel, only to positions allowed by the grid. */
  public void move(int dx, int dy, int speed) {
    for (int i = 0; i < speed; i++) {
      if (this.play.allowMovement(this.mx + dx, this.my + dy, -1, -1)) {
        this.mx += dx;
        this.my += dy;
      }
    }
  }

  @Override
  public void run() {
    if (this.play.isFrozen()) {
      return;
    }

    this.timer += 1;

    if (this.alive) {
      int dx = 0;
      if (this.isKeyPressed(KeyCode.LEFT)) {
        dx = -1;
      } else if (this.isKeyPressed(KeyCode.RIGHT)) {
        dx = 1;
      }

      int dy = 0;
      if (this.isKeyPressed(KeyCode.UP)) {
        dy = -1;
      } else if (this.isKeyPressed(KeyCode.DOWN)) {
        dy = 1;
      }

      this.move(dx, 0, 3 - Math.abs(dy));
      this.move(0, dy, 3 - Math.abs(dx));

      // Turn the vehicle towards the new direction over several frames
      int[] directions = {7, 0, 1, 6, -1, 2, 5, 4, 3};
      int dir = directions[dx + 3 * dy + 4];

      if (this.timer % 2 == 0 && dir >= 0) {
        int difference = dir - this.direction;
        int rotation = new int[] {0, 1, 1, -1}[Math.floorMod(difference, 4)];
        this.direction = Math.floorMod(this.direction + rotation, 4);
      }

      this.fireTimer -= 1;

      // Fire cannon (or allow firing animation to finish)
      if (this.fireTimer < 0 && (this.frame > 0 || this.isKeyPressed(KeyCode.SPACE))) {
        if (this.frame == 0) {
          this.play.playSound("laser0");
          this.play.addBullet(new Bullet(this.play, this.mx, this.my - 8));
        }
        this.frame = (this.frame + 1) % 3;
        this.fireTimer = Player.RELOAD_TIME;
      }

      // Check for collisions with enemy segments and the flying enemy
      for (Segment enemy : this.play.segments) {
        this.hit(enemy);
      }
      this.hit(this.play.flyingEnemy);
    } else {
      // Not alive - wait a while before respawning
      if (this.timer > Player.RESPAWN_TIME) {
        this.alive = true;
        this.timer = 0;
        this.mx = 240;
        this.my = 768;
        this.play.clearRocksForRespawn(this.mx, this.my);
      }
    }

    // Display the player sprite, blinking while invulnerable
    boolean invulnerable = this.timer > Player.INVULNERABILITY_TIME;
    if (this.alive && (invulnerable || this.timer % 2 == 0)) {
      this.switchCostume(String.format("player%d%d", this.direction, this.frame));
    } else {
      this.switchCostume("blank");
    }
    this.sync();
  }

  private void hit(MyrActor enemy) {
    if (enemy != null && enemy.containsPoint(this.mx, this.my)) {
      if (this.timer > Player.INVULNERABILITY_TIME) {
        this.play.playSound("player_explode0");
        this.play.addExplosion(new Explosion(this.play, this.mx, this.my, 1));
        this.alive = false;
        this.timer = 0;
        this.lives -= 1;
      }
    }
  }
}
