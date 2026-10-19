package cavern;

import org.openpatch.scratch.*;

public class Game extends Window {

  public Game() {
    super(800, 480, "cavern/assets");

    this.setStage(new Menu());
  }

  public static void main(String[] args) {
    new Game();
  }
}
