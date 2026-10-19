package boing;

import org.openpatch.scratch.*;

public class Ball extends Sprite {

  private double speed = 5;
  private Vector2 d;
  private Table table;

  private String[] bounceSounds = { "bounce0", "bounce1", "bounce2", "bounce3", "bounce4" };
  private String[] hitSounds = { "hit0", "hit1", "hit2", "hit3", "hit4" };

  public Ball(Table table, double dx) {
    this.table = table;

    this.addCostume("ball", "boing/assets/images/ball.png");
    this.d = new Vector2(dx, 0);
  }

  public boolean out() {
    return this.getX() < -360 || this.getX() > 360;
  }

  public void run() {

    for (int i = 0; i < this.speed; i++) {
      var originalX = this.getX();

      this.changePosition(this.d);

      if (Math.abs(this.getX()) >= 344 && Math.abs(originalX) < 344) {
        var newDirX = 1;
        var bat = table.getBatLeft();

        if (this.getX() > 0) {
          newDirX = -1;
          bat = table.getBatRight();
        }

        var differenceY = this.getY() - bat.getY();

        if (differenceY > -64 && differenceY < 64) {
          var dx = this.d.getX() * -1;
          var dy = this.d.getY();
          dy += differenceY / 128;
          dy = Operators.constrain(dy, -1, 1);

          this.d = new Vector2(dx, dy).unitVector();

          this.table.add(new Impact(this.getPosition().add(new Vector2(newDirX * 10, 0))));

          this.speed += 1;

          this.table.randomizeAiOffset();

          bat.startGlowFor(10);

          this.table.playSound(this.hitSounds[this.pickRandom(0, this.hitSounds.length - 1)]);
          if (this.speed <= 10) {
            this.table.playSound("hit_slow0");
          } else if (this.speed <= 12) {
            this.table.playSound("hit_medium0");
          } else if (this.speed <= 16) {
            this.table.playSound("hit_fast0");
          } else {
            this.table.playSound("hit_veryfast0");
          }
        }

      }

      if (Math.abs(this.getY()) > 220) {
        this.d = new Vector2(this.d.getX(), this.d.getY() * -1);
        this.changeY(this.d.getY());

        this.getStage().add(new Impact(this.getPosition()));

        this.table.playSound(this.bounceSounds[this.pickRandom(0, this.bounceSounds.length - 1)]);
        this.table.playSound("bounce_synth0");
      }
    }
  }

}
