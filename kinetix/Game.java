package kinetix;

import org.openpatch.scratch.Window;

public class Game extends Window {

  public Game() {
    super(640, 640, "kinetix/assets");
    this.setStage(new Menu());
  }

  public static void main(String[] args) {
    new Game();
  }
}
