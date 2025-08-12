package boing;

import org.openpatch.scratch.KeyCode;
import org.openpatch.scratch.Stage;

public class GameOver extends Stage {

  public GameOver() {
    this.addBackdrop("gameover", "boing/assets/images/over.png");
  }

  @Override
  public void whenKeyPressed(int key) {

    if (key == KeyCode.VK_SPACE) {
      this.getWindow().transitionToStage(new Menu(), 300);
    }
  }
}
