package cavern;

public class Bolt extends CavernActor {

  public static final int SPEED = 7;

  public boolean active = true;
  public int directionX;

  public Bolt(Play play, int x, int y, int dirX) {
    super(play, false);

    this.mx = x;
    this.my = y;
    this.directionX = dirX;

    for (int d = 0; d < 2; d++) {
      for (int f = 0; f < 2; f++) {
        this.addCostume(
            String.format("bolt%d%d", d, f),
            String.format("cavern/assets/images/bolt%d%d.png", d, f));
      }
    }

    this.switchCostume("bolt" + (dirX > 0 ? "1" : "0") + "0");
    this.sync();
  }

  @Override
  public void run() {
    if (this.frozen()) {
      return;
    }

    // Move horizontally and check to see if we've collided with a block
    if (this.move(this.directionX, 0, Bolt.SPEED)) {
      this.remove();
      return;
    }

    // Check to see if we collided with an orb or the player
    for (Orb orb : this.play.find(Orb.class)) {
      if (orb.hitTest(this)) {
        this.remove();
        return;
      }
    }
    if (this.play.player != null && this.play.player.hitTest(this)) {
      this.remove();
      return;
    }

    String dirIndex = this.directionX > 0 ? "1" : "0";
    this.switchCostume("bolt" + dirIndex + (this.play.timer / 4 % 2));
    this.sync();
  }
}
