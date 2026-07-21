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

    this.addSound("bounce0", "boing/assets/sounds/bounce0.wav");
    this.addSound("bounce1", "boing/assets/sounds/bounce1.wav");
    this.addSound("bounce2", "boing/assets/sounds/bounce2.wav");
    this.addSound("bounce3", "boing/assets/sounds/bounce3.wav");
    this.addSound("bounce4", "boing/assets/sounds/bounce4.wav");
    this.addSound("bounce_synth0", "boing/assets/sounds/bounce_synth0.wav");

    this.addSound("hit0", "boing/assets/sounds/hit0.wav");
    this.addSound("hit1", "boing/assets/sounds/hit1.wav");
    this.addSound("hit2", "boing/assets/sounds/hit2.wav");
    this.addSound("hit3", "boing/assets/sounds/hit3.wav");
    this.addSound("hit4", "boing/assets/sounds/hit4.wav");

    this.addSound("hit_fast0", "boing/assets/sounds/hit_fast0.wav");
    this.addSound("hit_medium0", "boing/assets/sounds/hit_medium0.wav");
    this.addSound("hit_slow0", "boing/assets/sounds/hit_slow0.wav");
    this.addSound("hit_veryfast0", "boing/assets/sounds/hit_veryfast0.wav");

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
          dy = Math.min(Math.max(dy, -1), 1);

          this.d = new Vector2(dx, dy).unitVector();

          this.table.add(new Impact(this.getPosition().add(new Vector2(newDirX * 10, 0))));

          this.speed += 1;

          this.table.randomizeAiOffset();

          bat.startGlowFor(10);

          this.playSound(this.hitSounds[(int) (Math.random() * this.hitSounds.length)]);
          if (this.speed <= 10) {
            this.playSound("hit_slow0");
          } else if (this.speed <= 12) {
            this.playSound("hit_medium0");
          } else if (this.speed <= 16) {
            this.playSound("hit_fast0");
          } else {
            this.playSound("hit_veryfast0");
          }
        }

      }

      if (Math.abs(this.getY()) > 220) {
        this.d = new Vector2(this.d.getX(), this.d.getY() * -1);
        this.changeY(this.d.getY());

        this.getStage().add(new Impact(this.getPosition()));

        this.playSound(this.bounceSounds[(int) (Math.random() * this.bounceSounds.length)]);
        this.playSound("bounce_synth0");
      }
    }
  }

}
