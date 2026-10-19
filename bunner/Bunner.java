package bunner;

import java.util.ArrayList;
import java.util.List;
import org.openpatch.scratch.KeyCode;
import org.openpatch.scratch.Operators;

public class Bunner extends BunActor {

  public static final int STATE_ALIVE = 0;
  public static final int STATE_SPLAT = 1;
  public static final int STATE_SPLASH = 2;
  public static final int STATE_EAGLE = 3;

  public static final double MOVE_DISTANCE = 10;

  // Constants representing directions: 0 = up, 1 = right, 2 = down, 3 = left
  public static final double[] DX = {0, 4, 0, -4};
  public static final double[] DY = {-4, 0, 4, 0};

  private static final KeyCode[] DIRECTION_KEYS = {
    KeyCode.UP, KeyCode.RIGHT, KeyCode.DOWN, KeyCode.LEFT
  };

  public int state = STATE_ALIVE;
  public int timer = 0;
  public double minY;

  private int direction = 2;
  private final List<Integer> inputQueue = new ArrayList<>();
  private final boolean[] keyStatus = new boolean[4];

  public Bunner(Play play, double x, double y) {
    super(play, false);

    this.mx = x;
    this.my = y;
    this.minY = y;

    this.addCostume("blank", "bunner/assets/images/blank.png");
    for (int d = 0; d < 4; d++) {
      this.addCostume("jump" + d, String.format("bunner/assets/images/jump%d.png", d));
      this.addCostume("sit" + d, String.format("bunner/assets/images/sit%d.png", d));
    }
    for (int f = 0; f < 8; f++) {
      this.addCostume("splash" + f, String.format("bunner/assets/images/splash%d.png", f));
    }

    this.switchCostume("sit2");
    this.sync();
  }

  private void handleInput(int dir) {
    // Find the row the player is trying to move to
    for (Row row : this.play.rows) {
      if (row.my == this.my + Bunner.MOVE_DISTANCE * DY[dir]) {
        if (row.allowMovement(this.mx + Bunner.MOVE_DISTANCE * DX[dir])) {
          this.direction = dir;
          this.timer = (int) Bunner.MOVE_DISTANCE;
          this.play.playSound("jump0");
        }
        return;
      }
    }
  }

  @Override
  public void run() {
    if (!this.isPlaying()) {
      return;
    }

    // Check each control direction for a key that was just pressed
    for (int d = 0; d < 4; d++) {
      boolean pressed = this.isKeyPressed(DIRECTION_KEYS[d]);
      if (pressed && !this.keyStatus[d]) {
        this.inputQueue.add(d);
      }
      this.keyStatus[d] = pressed;
    }

    if (this.state == STATE_ALIVE) {
      // Are we on the ground, and are there inputs to process?
      if (this.timer == 0 && !this.inputQueue.isEmpty()) {
        this.handleInput(this.inputQueue.remove(0));
      }

      boolean land = false;
      if (this.timer > 0) {
        // Apply movement, one pixel at a time
        this.mx += DX[this.direction];
        this.my += DY[this.direction];
        this.timer -= 1;
        land = this.timer == 0;
      }

      Row currentRow = null;
      for (Row row : this.play.rows) {
        if (row.my == this.my) {
          currentRow = row;
          break;
        }
      }

      if (currentRow != null) {
        int[] result = currentRow.checkCollision(this.mx);
        this.state = result[0];
        if (this.state == STATE_ALIVE) {
          // Water rows move the player along the X axis, if standing on a log
          this.mx += currentRow.push();

          if (land) {
            currentRow.playLandingSound();
          }
        } else {
          if (this.state == STATE_SPLAT) {
            // Add the splat graphic to the current row, underneath other objects
            var splat = new Splat(this.play, this.direction);
            splat.mx = this.mx;
            splat.relY = result[1];
            splat.row = currentRow;
            splat.my = currentRow.my + splat.relY;
            splat.sync();
            currentRow.children.add(0, splat);
            this.play.add(splat);
          }
          this.timer = 100;
        }
      } else {
        // No current row - has the player gone off the bottom of the screen?
        if (this.my > this.play.scrollPos + Play.HEIGHT + 80) {
          this.play.eagle = new Eagle(this.play, this.mx, this.play.scrollPos);
          this.play.add(this.play.eagle);
          this.state = STATE_EAGLE;
          this.timer = 150;
          this.play.playSound("eagle0");
        }
      }

      // Limit x position so the player doesn't go off the screen
      this.mx = Operators.constrain(this.mx, 16, Play.WIDTH - 16);
    } else {
      // Not alive - timer counts down prior to the game over screen
      this.timer -= 1;
    }

    // Keep track of the furthest we've got in the level
    this.minY = Math.min(this.minY, this.my);

    // Choose sprite image
    this.switchCostume("blank");
    if (this.state == STATE_ALIVE) {
      if (this.timer > 0) {
        this.switchCostume("jump" + this.direction);
      } else {
        this.switchCostume("sit" + this.direction);
      }
    } else if (this.state == STATE_SPLASH && this.timer > 84) {
      this.switchCostume("splash" + ((100 - this.timer) / 2));
    }

    this.sync();
  }
}
