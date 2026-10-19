package leadingedge;

import org.openpatch.scratch.Operators;

/**
 * The start/finish gantry. Which image is shown depends on the game's start
 * timer: the countdown from start0 to start4, then alternating between start4
 * and start5.
 */
public class StartGantry extends Scenery {

  private final Play game;

  public StartGantry(Play game) {
    super(
        0,
        "start0",
        1,
        Play.VIEW_DISTANCE,
        4,
        new double[][] {{-3000, -2400}, {2400, 3000}});
    this.game = game;
  }

  @Override
  public String getImageName() {
    int index;
    if (this.game.startTimer > 0) {
      index = (int) Operators.map(this.game.startTimer, 4, 0, 0, 4);
    } else {
      index = (int) (this.game.timer * 2) % 2 == 0 ? 4 : 5;
    }
    this.imageName = "start" + index;
    return this.imageName;
  }
}
