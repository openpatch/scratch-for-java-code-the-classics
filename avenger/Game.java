package avenger;

import org.openpatch.scratch.Window;

public class Game extends Window {

  public Game() {
    super(960, 540, "avenger/assets");
    this.setStage(new Menu());
  }

  public static void main(String[] args) {
    new Game();
  }
}
