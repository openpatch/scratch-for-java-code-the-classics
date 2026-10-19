package boing;

import org.openpatch.scratch.*;

public class GameOver extends Stage {

  public GameOver() {
    this.addBackdrop("gameover", "boing/assets/images/over.png");
  }

  @Override
  public void whenKeyPressed(KeyCode key) {

    if (key == KeyCode.SPACE) {
      Window.getInstance().transitionToStage(new Menu(), 300);
    }
  }
}
