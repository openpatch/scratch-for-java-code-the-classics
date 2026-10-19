package bunner;

import org.openpatch.scratch.Random;

public class Train extends Mover {

  public Train(Play play, int dx, double x, double y) {
    super(play, dx, x, y);

    String image = "train" + Random.randomInt(0, 2) + (dx < 0 ? "0" : "1");
    this.addCostume("train", "bunner/assets/images/" + image + ".png");
    this.switchCostume("train");
    this.sync();
  }
}
