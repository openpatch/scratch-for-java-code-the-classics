package eggzy;

import org.openpatch.scratch.Window;

public class Game extends Window {

  /**
   * The total number of frames since the program started, shared by all
   * stages like the original's total_frames global.
   */
  public static int totalFrames = 0;

  public Game() {
    super(825, 550, "eggzy/assets");
    this.setStage(new Title());
  }

  public static void main(String[] args) {
    Replays.load();
    new Game();
  }
}
