package soccer;

import org.openpatch.scratch.KeyCode;

/**
 * The keyboard controls for one team. Player 1 uses the arrow keys and space,
 * player 2 uses WASD and shift. A team without controls is computer
 * controlled.
 */
public class Controls {

  private final KeyCode keyUp;
  private final KeyCode keyDown;
  private final KeyCode keyLeft;
  private final KeyCode keyRight;
  private final KeyCode keyShoot;

  private final boolean[] keyStatus = new boolean[5];

  private double moveX;
  private double moveY;
  private boolean shootJustPressed;
  private boolean shootConsumed;
  private boolean shootPressedPrev;
  private boolean shootConsumedPrev;

  public Controls(int playerNum) {
    if (playerNum == 0) {
      this.keyUp = KeyCode.UP;
      this.keyDown = KeyCode.DOWN;
      this.keyLeft = KeyCode.LEFT;
      this.keyRight = KeyCode.RIGHT;
      this.keyShoot = KeyCode.SPACE;
    } else {
      this.keyUp = KeyCode.W;
      this.keyDown = KeyCode.S;
      this.keyLeft = KeyCode.A;
      this.keyRight = KeyCode.D;
      this.keyShoot = KeyCode.SHIFT;
    }
  }

  /** Polls the keyboard. Must be called once per frame. */
  public void poll(Play play) {
    // Keep the previous frame's state for the deferred player switching
    this.shootPressedPrev = this.shootJustPressed;
    this.shootConsumedPrev = this.shootConsumed;

    boolean[] status = {
      play.isKeyPressed(this.keyLeft),
      play.isKeyPressed(this.keyRight),
      play.isKeyPressed(this.keyUp),
      play.isKeyPressed(this.keyDown),
      play.isKeyPressed(this.keyShoot)
    };

    this.shootJustPressed = status[4] && !this.keyStatus[4];
    this.shootConsumed = false;

    double dx = 0;
    if (status[0]) {
      dx = -1;
    } else if (status[1]) {
      dx = 1;
    }
    double dy = 0;
    if (status[2]) {
      dy = -1;
    } else if (status[3]) {
      dy = 1;
    }

    this.moveX = dx;
    this.moveY = dy;

    System.arraycopy(status, 0, this.keyStatus, 0, status.length);
  }

  /** Returns a vector representing the requested movement at the given speed. */
  public org.openpatch.scratch.Vector2 move(double speed) {
    return new org.openpatch.scratch.Vector2(this.moveX, this.moveY).multiply(speed);
  }

  /** Returns true, if the shoot key was just pressed this frame. */
  public boolean shoot() {
    if (this.shootJustPressed && !this.shootConsumed) {
      this.shootConsumed = true;
      return true;
    }
    return false;
  }

  /**
   * Returns true, if the shoot key was pressed on the previous frame, but not
   * consumed by kicking the ball. Used for manual player switching.
   */
  public boolean shootDeferred() {
    return this.shootPressedPrev && !this.shootConsumedPrev;
  }
}
