package kinetix;

import org.openpatch.scratch.Sprite;

/**
 * A collectable powerup barrel that falls from a destroyed brick. The sprite
 * names have the format barrelXY where X is the powerup type and Y the
 * animation frame.
 */
public class Barrel extends KxActor {

  private final Play play;
  public final Powerup type;
  public final Sprite shadow;

  private int time = 0;

  public Barrel(Play play, double x, double y) {
    this.play = play;
    this.x = x;
    this.y = y;
    this.type = this.play.choosePowerupType();

    for (int frame = 0; frame < 10; frame++) {
      this.addCostume(
          "barrel" + this.type.ordinal() + frame,
          String.format("kinetix/assets/images/barrel%d%d.png", this.type.ordinal(), frame));
    }
    this.switchCostume("barrel" + this.type.ordinal() + 0);
    this.sync();

    this.shadow = new Sprite();
    this.shadow.addCostume("barrels", "kinetix/assets/images/barrels.png");
  }

  @Override
  public void run() {
    if (!this.isPlaying()) {
      return;
    }
    this.time += 1;
    this.y += 1;
    this.sync();

    double w = this.play.bat.getWidth() / 2.0 + Play.BALL_RADIUS;

    // Check for barrel being collected by the bat
    if (this.y >= Play.BAT_TOP_EDGE - 10
        && this.y <= Play.BAT_TOP_EDGE + 30
        && Math.abs(this.x - this.play.bat.x) < w) {
      // Barrel collection animation (type 14, hexadecimal e)
      this.play.addImpact(new Impact(this.x, this.y - 11, 14));

      String sound = this.play.soundForPowerup(this.type);
      if (sound != null) {
        this.play.playSound(sound);
      }

      // Move the barrel off the bottom of the screen, it will then be deleted
      this.y = Play.HEIGHT + 100;
      this.sync();

      switch (this.type) {
        case EXTEND_BAT -> this.play.bat.changeType(BatType.EXTENDED);
        case GUN -> this.play.bat.changeType(BatType.GUN);
        case SMALL_BAT -> this.play.bat.changeType(BatType.SMALL);
        case MAGNET -> this.play.bat.changeType(BatType.MAGNET);
        case MULTI_BALL -> this.play.multiball();
        case FAST_BALLS -> this.play.changeAllBallSpeeds(3);
        case SLOW_BALLS -> this.play.changeAllBallSpeeds(-3);
        case PORTAL -> this.play.activatePortal();
        case EXTRA_LIFE -> this.play.lives += 1;
      }
    }

    this.switchCostume("barrel" + this.type.ordinal() + this.time / 10 % 10);

    this.shadow.setPosition(this.x + Play.SHADOW_OFFSET - Play.WIDTH / 2.0,
        Play.HEIGHT / 2.0 - (this.y + Play.SHADOW_OFFSET));
  }
}
