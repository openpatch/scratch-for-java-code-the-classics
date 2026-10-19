package cavern;

import java.util.ArrayList;
import java.util.List;
import org.openpatch.scratch.Random;

public class Robot extends GravityActor {

  public static final int TYPE_NORMAL = 0;
  public static final int TYPE_AGGRESSIVE = 1;

  public boolean alive = true;

  private int type;
  private int speed;
  private int directionX = 1;
  private int changeDirTimer = 0;
  private int fireTimer = 100;

  public Robot(Play play, int x, int y, int type) {
    super(play, x, y);

    this.type = type;
    this.speed = Random.randomInt(1, 3);

    for (int t = 0; t < 2; t++) {
      for (int d = 0; d < 2; d++) {
        for (int f = 0; f < 8; f++) {
          this.addCostume(
              String.format("robot%d%d%d", t, d, f),
              String.format("cavern/assets/images/robot%d%d%d.png", t, d, f));
        }
      }
    }
  }

  @Override
  public void run() {
    if (this.frozen()) {
      return;
    }

    this.updateGravity(true);

    this.changeDirTimer -= 1;
    this.fireTimer += 1;

    // Move in current direction - turn around if we hit a wall
    if (this.move(this.directionX, 0, this.speed)) {
      this.changeDirTimer = 0;
    }

    if (this.changeDirTimer <= 0) {
      // Randomly choose a direction to move in.
      // If there's a player, there's a two thirds chance that we'll move towards them.
      List<Integer> directions = new ArrayList<>(List.of(-1, 1));
      if (this.play.player != null) {
        directions.add(sign(this.play.player.mx - this.mx));
      }
      this.directionX = directions.get(Random.randomInt(0, directions.size() - 1));
      this.changeDirTimer = Random.randomInt(100, 250);
    }

    // The more powerful type of robot can deliberately shoot at orbs
    if (this.type == TYPE_AGGRESSIVE && this.fireTimer >= 24) {
      for (Orb orb : this.play.find(Orb.class)) {
        if (orb.my >= this.top() && orb.my < this.bottom() && Math.abs(orb.mx - this.mx) < 200) {
          this.directionX = sign(orb.mx - this.mx);
          this.fireTimer = 0;
          break;
        }
      }
    }

    // Check to see if we can fire at the player
    if (this.fireTimer >= 12) {
      double fireProbability = this.play.fireProbability();
      if (this.play.player != null
          && this.top() < this.play.player.bottom()
          && this.bottom() > this.play.player.top()) {
        fireProbability *= 10;
      }
      if (Random.random() < fireProbability) {
        this.fireTimer = 0;
        this.play.playRandom("laser", 4);
      }
    } else if (this.fireTimer == 8) {
      // Frame 8 of the fire animation is when the actual bolt is fired
      this.play.add(new Bolt(this.play, this.mx + this.directionX * 20, this.my - 38, this.directionX));
    }

    // Am I colliding with an orb? If so, become trapped by it
    for (Orb orb : this.play.find(Orb.class)) {
      if (orb.trappedEnemyType == null && this.containsPoint(orb.mx, orb.my)) {
        this.alive = false;
        orb.floating = true;
        orb.trappedEnemyType = this.type;
        this.play.playRandom("trap", 4);
        this.remove();
        return;
      }
    }

    // Choose and set sprite image
    String dirIndex = this.directionX > 0 ? "1" : "0";
    int frame;
    if (this.fireTimer < 12) {
      frame = 5 + this.fireTimer / 4;
    } else {
      frame = 1 + (this.play.timer / 4 % 4);
    }
    this.switchCostume("robot" + this.type + dirIndex + frame);
    this.sync();
  }

  public int getType() {
    return this.type;
  }
}
