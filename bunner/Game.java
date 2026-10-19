package bunner;

import org.openpatch.scratch.Window;

public class Game extends Window {

  public Game() {
    super(480, 800, "bunner/assets");
    this.setStage(new Menu());
  }

  public static void main(String[] args) {
    new Game();
  }
}
