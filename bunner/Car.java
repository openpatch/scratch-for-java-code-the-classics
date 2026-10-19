package bunner;

import org.openpatch.scratch.Random;

public class Car extends Mover {

  public static final int SOUND_ZOOM = 0;
  public static final int SOUND_HONK = 1;

  private final boolean[] played = new boolean[2];

  public Car(Play play, int dx, double x, double y) {
    super(play, dx, x, y);

    String image = "car" + Random.randomInt(0, 3) + (dx < 0 ? "0" : "1");
    this.addCostume("car", "bunner/assets/images/" + image + ".png");
    this.switchCostume("car");
    this.sync();
  }

  public void playCarSound(int num) {
    if (!this.played[num]) {
      if (num == Car.SOUND_ZOOM) {
        this.play.playRandom("zoom", 6);
      } else {
        this.play.playRandom("honk", 4);
      }
      this.played[num] = true;
    }
  }
}
