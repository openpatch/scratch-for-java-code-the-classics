package boing;

import org.openpatch.scratch.Window;

public class Game extends Window {

  public Game() {
    super(800, 480, "boing/assets");
    this.setStage(new Menu());
  }

  public static void main(String[] args) {
    new Game();
  }
}
