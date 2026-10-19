package bunner;

/** A hedge on a grass row. */
public class Hedge extends BunActor {

  public Hedge(Play play, int x, int y, double px, double py) {
    super(play, false);

    this.mx = px;
    this.my = py;

    this.addCostume("bush", String.format("bunner/assets/images/bush%d%d.png", x, y));
    this.switchCostume("bush");
    this.sync();
  }
}
