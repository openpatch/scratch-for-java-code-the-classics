package leadingedge;

import org.openpatch.scratch.KeyCode;
import org.openpatch.scratch.Stage;

/**
 * Keyboard controls: left/right arrows to steer, Z or left control to
 * accelerate, X or left shift to brake.
 */
public class Controls {

  public static final int NUM_BUTTONS = 2;

  private final Stage stage;
  private final boolean[] buttonPreviouslyDown = new boolean[NUM_BUTTONS];
  private final boolean[] isButtonPressed = new boolean[NUM_BUTTONS];

  public Controls(Stage stage) {
    this.stage = stage;
  }

  /** Call each frame to update button status. */
  public void update() {
    for (int button = 0; button < NUM_BUTTONS; button++) {
      boolean buttonDown = this.buttonDown(button);
      this.isButtonPressed[button] = buttonDown && !this.buttonPreviouslyDown[button];
      this.buttonPreviouslyDown[button] = buttonDown;
    }
  }

  public double getX() {
    if (this.stage.isKeyPressed(KeyCode.LEFT)) {
      return -1;
    } else if (this.stage.isKeyPressed(KeyCode.RIGHT)) {
      return 1;
    }
    return 0;
  }

  public boolean buttonDown(int button) {
    if (button == 0) {
      return this.stage.isKeyPressed(KeyCode.CTRL) || this.stage.isKeyPressed(KeyCode.Z);
    } else if (button == 1) {
      return this.stage.isKeyPressed(KeyCode.SHIFT) || this.stage.isKeyPressed(KeyCode.X);
    }
    return false;
  }

  /** Was the given button pressed since the last frame? */
  public boolean buttonPressed(int button) {
    return this.isButtonPressed[button];
  }
}
