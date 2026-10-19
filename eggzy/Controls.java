package eggzy;

import org.openpatch.scratch.KeyCode;
import org.openpatch.scratch.Stage;

/** Keyboard controls: arrows to move, SPACE to jump and Z to dash. */
public class Controls {

  public static final int NUM_BUTTONS = 2;

  private final Stage stage;
  private final boolean[] buttonPreviouslyDown = new boolean[NUM_BUTTONS];
  private final boolean[] isButtonPressed = new boolean[NUM_BUTTONS];

  public Controls(Stage stage) {
    this.stage = stage;
  }

  /** Call each frame to update the button status. */
  public void update() {
    for (int button = 0; button < NUM_BUTTONS; button++) {
      boolean buttonDown = this.buttonDown(button);
      this.isButtonPressed[button] = buttonDown && !this.buttonPreviouslyDown[button];
      this.buttonPreviouslyDown[button] = buttonDown;
    }
  }

  public int getX() {
    if (this.stage.isKeyPressed(KeyCode.LEFT)) {
      return -1;
    } else if (this.stage.isKeyPressed(KeyCode.RIGHT)) {
      return 1;
    }
    return 0;
  }

  public int getY() {
    if (this.stage.isKeyPressed(KeyCode.UP)) {
      return -1;
    } else if (this.stage.isKeyPressed(KeyCode.DOWN)) {
      return 1;
    }
    return 0;
  }

  public boolean buttonDown(int button) {
    if (button == 0) {
      return this.stage.isKeyPressed(KeyCode.SPACE);
    }
    return this.stage.isKeyPressed(KeyCode.Z);
  }

  public String buttonName(String button) {
    if (button.equals("dash")) {
      return "Z";
    } else if (button.equals("jump")) {
      return "SPACE";
    }
    return "?";
  }

  /** Was the given button pressed since the last frame? */
  public boolean buttonPressed(int button) {
    return this.isButtonPressed[button];
  }
}
