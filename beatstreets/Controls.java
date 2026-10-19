package beatstreets;

import org.openpatch.scratch.KeyCode;
import org.openpatch.scratch.Stage;

/** Keyboard input, including buttons pressed since the previous frame. */
public class Controls {

  public static final int NUM_BUTTONS = 4;

  private final Stage stage;
  private final boolean[] buttonPreviouslyDown = new boolean[NUM_BUTTONS];
  private final boolean[] isButtonPressed = new boolean[NUM_BUTTONS];

  public Controls(Stage stage) {
    this.stage = stage;
  }

  /** Call each frame to update the button status. */
  public void update() {
    for (int button = 0; button < Controls.NUM_BUTTONS; button++) {
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

  public double getY() {
    if (this.stage.isKeyPressed(KeyCode.UP)) {
      return -1;
    } else if (this.stage.isKeyPressed(KeyCode.DOWN)) {
      return 1;
    }
    return 0;
  }

  public boolean buttonDown(int button) {
    if (button == 0) {
      return this.stage.isKeyPressed(KeyCode.SPACE)
          || this.stage.isKeyPressed(KeyCode.Z)
          || this.stage.isKeyPressed(KeyCode.CTRL); // punch
    } else if (button == 1) {
      return this.stage.isKeyPressed(KeyCode.X) || this.stage.isKeyPressed(KeyCode.ALT); // kick
    } else if (button == 2) {
      return this.stage.isKeyPressed(KeyCode.C) || this.stage.isKeyPressed(KeyCode.SHIFT); // elbow
    } else if (button == 3) {
      return this.stage.isKeyPressed(KeyCode.A); // flying kick
    }
    return false;
  }

  /** Was the given button pressed since the last frame? */
  public boolean buttonPressed(int button) {
    return this.isButtonPressed[button];
  }
}
