package myriapod;

import org.openpatch.scratch.Random;

public class Bullet extends MyrActor {

  public boolean done = false;

  public Bullet(Play play, int x, int y) {
    super(play);
    this.mx = x;
    this.my = y;
    this.addCostume("bullet", "myriapod/assets/images/bullet.png");
    this.sync();
  }

  @Override
  public void run() {
    if (this.play.isFrozen() || this.done) {
      return;
    }

    // Move up the screen
    this.my -= 24;

    // Is there a rock at this position?
    int cellX = Math.floorDiv(this.mx - 16, 32);
    int cellY = Math.floorDiv(this.my, 32);
    if (this.play.damage(cellX, cellY, 1, true)) {
      this.done = true;
      this.sync();
      return;
    }

    // Check each segment and the flying enemy for a collision
    for (Segment obj : this.play.segments) {
      if (this.hit(obj)) {
        return;
      }
    }
    if (this.hit(this.play.flyingEnemy)) {
      return;
    }

    this.sync();
  }

  private boolean hit(MyrActor obj) {
    if (obj != null && obj.containsPoint(this.mx, this.my)) {
      this.play.addExplosion(new Explosion(this.play, obj.mx, obj.my, 2));

      if (obj instanceof Segment segment) {
        segment.health -= 1;
        if (segment.health == 0
            && this.play.rockAt(segment.cellX, segment.cellY) == null
            && this.play.allowMovement(
                this.play.player.mx, this.play.player.my, segment.cellX, segment.cellY)) {
          // Create a new rock in the segment's place - 20% chance of a totem
          this.play.setRock(
              segment.cellX,
              segment.cellY,
              new Rock(this.play, segment.cellX, segment.cellY, Random.random() < 0.2));
        }
        this.play.playGameSound("segment_explode0");
        this.play.score += 10;
      } else {
        this.play.playGameSound("meanie_explode0");
        this.play.score += 20;
      }

      this.done = true;
      return true;
    }
    return false;
  }
}
