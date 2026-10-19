package myriapod;

import org.openpatch.scratch.Window;

public class Game extends Window {

  public Game() {
    super(480, 800, "myriapod/assets");
    this.setStage(new Menu());
  }

  public static void main(String[] args) {
    new Game();
  }
}
