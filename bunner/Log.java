package bunner;

import org.openpatch.scratch.Random;

public class Log extends Mover {

  public Log(Play play, int dx, double x, double y) {
    super(play, dx, x, y);

    String image = "log" + Random.randomInt(0, 1);
    this.addCostume("log", "bunner/assets/images/" + image + ".png");
    this.switchCostume("log");
    this.sync();
  }
}
