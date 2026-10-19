package bunner;

import org.openpatch.scratch.Sprite;

/** The eagle catches the rabbit if it goes off the bottom of the screen. */
public class Eagle extends BunActor {

  private final Sprite eagle;

  public Eagle(Play play, double x, double y) {
    super(play, false);

    this.mx = x;
    this.my = y;

    this.addCostume("eagles", "bunner/assets/images/eagles.png");
    this.switchCostume("eagles");

    // The eagle itself is drawn 32 pixels above its shadow
    this.eagle = new Sprite();
    this.eagle.addCostume("eagle", "bunner/assets/images/eagle.png");
    this.eagle.switchCostume("eagle");
    play.add(this.eagle);

    this.sync();
  }

  @Override
  public void run() {
    if (!this.isPlaying()) {
      return;
    }
    this.my += 12;
    this.sync();

    this.eagle.setX(this.getX());
    this.eagle.setY(this.getY() + 32);
    this.eagle.goToFrontLayer();
  }
}
