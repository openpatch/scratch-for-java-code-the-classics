package bunner;

import org.openpatch.scratch.Random;

public class Dirt extends Row {

  public Dirt(Play play, Row predecessor, int index, double y) {
    super(play, "dirt", index, y);
  }

  @Override
  public void playLandingSound() {
    this.play.playSound("dirt0");
  }

  @Override
  public Row next() {
    if (this.index <= 5) {
      return new Dirt(this.play, this, this.index + 8, this.my - Play.ROW_HEIGHT);
    } else if (this.index == 6) {
      return new Dirt(this.play, this, 7, this.my - Play.ROW_HEIGHT);
    } else if (this.index == 7) {
      return new Dirt(this.play, this, 15, this.my - Play.ROW_HEIGHT);
    } else if (this.index >= 8 && this.index <= 14) {
      return new Dirt(this.play, this, this.index + 1, this.my - Play.ROW_HEIGHT);
    } else if (Random.random() < 0.5) {
      return new Road(this.play, this, 0, this.my - Play.ROW_HEIGHT);
    } else {
      return new Water(this.play, this, 0, this.my - Play.ROW_HEIGHT);
    }
  }
}
