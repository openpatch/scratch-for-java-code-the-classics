package bunner;

public class Pavement extends Row {

  public Pavement(Play play, Row predecessor, int index, double y) {
    super(play, "side", index, y);
  }

  @Override
  public void playLandingSound() {
    this.play.playSound("sidewalk0");
  }

  @Override
  public Row next() {
    if (this.index < 2) {
      return new Pavement(this.play, this, this.index + 1, this.my - Play.ROW_HEIGHT);
    }
    return new Road(this.play, this, 0, this.my - Play.ROW_HEIGHT);
  }
}
