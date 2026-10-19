package kinetix;

import org.openpatch.scratch.KeyCode;
import org.openpatch.scratch.Stage;

public class KeyboardControls extends Controls {

  private final Stage stage;

  public KeyboardControls(Stage stage) {
    this.stage = stage;
  }

  @Override
  public double getX() {
    if (this.stage.isKeyPressed(KeyCode.LEFT)) {
      return -Play.BAT_SPEED;
    } else if (this.stage.isKeyPressed(KeyCode.RIGHT)) {
      return Play.BAT_SPEED;
    }
    return 0;
  }

  @Override
  public boolean fireDown() {
    return this.stage.isKeyPressed(KeyCode.SPACE);
  }
}
