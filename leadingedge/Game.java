package leadingedge;

import org.openpatch.scratch.Window;

public class Game extends Window {

  public Game() {
    super(Play.WIDTH, Play.HEIGHT, "leadingedge/assets");
    this.setStage(new Menu());
  }

  public static void main(String[] args) {
    new Game();
  }
}
