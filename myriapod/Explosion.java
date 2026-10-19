package myriapod;

public class Explosion extends MyrActor {

  public int timer = 0;

  private int type;

  public Explosion(Play play, int x, int y, int type) {
    super(play);

    this.mx = x;
    this.my = y;
    this.type = type;

    for (int t = 0; t < 3; t++) {
      for (int f = 0; f < 8; f++) {
        this.addCostume(
            String.format("exp%d%d", t, f),
            String.format("myriapod/assets/images/exp%d%d.png", t, f));
      }
    }

    this.switchCostume("exp" + type + "0");
    this.sync();
  }

  @Override
  public void run() {
    if (this.play.isFrozen()) {
      return;
    }
    this.timer += 1;
    this.switchCostume("exp" + this.type + (this.timer / 4));
    this.sync();
  }
}
