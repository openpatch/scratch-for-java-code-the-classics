package kinetix;

/** A bullet fired by the gun powerup. */
public class Bullet extends KxActor {

  private final Play play;
  public boolean alive = true;

  public Bullet(Play play, double x, double y, int side) {
    this.play = play;
    this.addCostume("bullet" + side, "kinetix/assets/images/bullet" + side + ".png");
    this.switchCostume("bullet" + side);
    this.x = x;
    this.y = y;
    this.sync();
  }

  @Override
  public void run() {
    if (!this.isPlaying()) {
      return;
    }
    this.y -= Play.BULLET_SPEED;
    this.sync();

    var collision = this.play.collide(this.x, this.y, 0, -1, 2);
    if (collision != null) {
      this.alive = false;
      this.play.addImpact(new Impact(this.x, this.y, 15));
      if (collision.type() == CollisionType.BRICK
          || collision.type() == CollisionType.INDESTRUCTIBLE_BRICK) {
        this.play.playRandom("bullet_hit", 4);
      }
    }
  }
}
