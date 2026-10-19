package bunner;

/** Base class for Car, Log and Train. */
public class Mover extends BunActor {

  public int dx;

  public Mover(Play play, int dx, double x, double y) {
    super(play, false);
    this.dx = dx;
    this.mx = x;
    this.my = y;
  }

  @Override
  public void run() {
    if (!this.isPlaying()) {
      return;
    }
    this.mx += this.dx;
    super.run();
  }
}
