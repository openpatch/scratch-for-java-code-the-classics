package kinetix;

import java.util.ArrayList;
import java.util.List;
import org.openpatch.scratch.Random;
import org.openpatch.scratch.Sprite;
import org.openpatch.scratch.Vector2;

/**
 * The ball. Movement and collisions are handled one pixel at a time, first on
 * the X axis, then on the Y axis, mirroring the original implementation.
 */
public class Ball extends KxActor {

  /** Result of {@link #getBatBounceVector()}. */
  public record BatBounce(boolean collided, Vector2 vector) {}

  private final Play play;

  private Vector2 dir;
  private boolean stuckToBat;
  private double batOffset;
  int speed;

  private int speedUpTimer = 0;
  private int timeSinceTouchedBat = 0;
  private int timeSinceDamagedBrick = 0;

  public final Sprite shadow;

  public Ball(Play play) {
    this(play, 0, 0, new Vector2(0, 0), true, Play.BALL_START_SPEED);
  }

  public Ball(Play play, double x, double y, Vector2 dir, boolean stuckToBat, int speed) {
    this.play = play;
    this.x = x;
    this.y = y;
    this.dir = new Vector2(dir);
    this.stuckToBat = stuckToBat;
    this.batOffset = Play.BALL_INITIAL_OFFSET;
    this.speed = speed;

    this.addCostume("ball0", "kinetix/assets/images/ball0.png");
    this.switchCostume("ball0");
    this.sync();

    this.shadow = new Sprite();
    this.shadow.addCostume("balls", "kinetix/assets/images/balls.png");
    this.shadow.setPosition(this.x + 16 - Play.WIDTH / 2.0, Play.HEIGHT / 2.0 - (this.y + 16));
  }

  /**
   * Has this ball neither damaged a brick nor touched the bat in the last 30
   * seconds? Used to detect balls stuck between indestructible bricks.
   */
  boolean isStuckForAges() {
    return this.timeSinceDamagedBrick >= 30 * 60 && this.timeSinceTouchedBat >= 30 * 60;
  }

  /** Pretends that the ball has just touched the bat. */
  void pretendTouchedBat() {
    this.timeSinceTouchedBat = 0;
  }

  @Override
  public void run() {
    if (!this.isPlaying()) {
      return;
    }
    this.timeSinceDamagedBrick += 1;

    if (this.stuckToBat) {
      this.x = this.play.bat.x + this.batOffset;
      this.y = this.play.bat.y - Play.BALL_RADIUS;

      // Launch ball from the bat if fire is pressed
      if (this.play.controls.firePressed()) {
        this.stuckToBat = false;
        this.dir = this.getBatBounceVector().vector();
      }
    } else {
      // Normal ball movement
      this.timeSinceTouchedBat += 1;

      // Speed up every so often. If the ball hasn't touched the bat in a
      // while, speed up more frequently.
      this.speedUpTimer += 1;
      if (this.timeSinceTouchedBat > 5 * 60) {
        this.speedUpTimer += 1;
      }
      double interval =
          this.speed < Play.BALL_FAST_SPEED_THRESHOLD
              ? Play.BALL_SPEED_UP_INTERVAL
              : Play.BALL_SPEED_UP_INTERVAL_FAST;
      double interval2 = interval * 0.75;
      if (this.speedUpTimer > interval
          || (this.speedUpTimer > interval2 && this.timeSinceTouchedBat > interval2)) {
        this.incrementSpeed();
        this.speedUpTimer = 0;
      }

      // Move one pixel at a time, speed times
      for (int i = 0; i < this.speed; i++) {
        // Move and collide on the X axis
        this.x += this.dir.getX();

        var c = this.play.collide(this.x, this.y, this.dir.getX(), this.dir.getY(), Play.BALL_RADIUS);

        if (c != null) {
          // Invert X direction and move back to the previous position
          this.dir = new Vector2(-this.dir.getX(), this.dir.getY());
          this.x += this.dir.getX();

          if (c.showImpact()) {
            this.play.addImpact(new Impact(c.x(), c.y(), 0xc));
          }

          if (c.type() == CollisionType.BRICK) {
            this.timeSinceDamagedBrick = 0;
          }

          collisionSound(c.type());
        }

        // Original Y position before movement
        double oy = this.y;

        // Move and collide on the Y axis
        this.y += this.dir.getY();

        c = this.play.collide(this.x, this.y, this.dir.getX(), this.dir.getY(), Play.BALL_RADIUS);

        if (c != null) {
          // Invert Y direction and move back to the previous position
          this.dir = new Vector2(this.dir.getX(), -this.dir.getY());
          this.y += this.dir.getY();

          if (c.showImpact()) {
            this.play.addImpact(new Impact(c.x(), c.y(), 0xc));
          }

          if (c.type() == CollisionType.BRICK) {
            this.timeSinceDamagedBrick = 0;
          }

          collisionSound(c.type());
        } else if (this.dir.getY() > 0) {
          // Check for collision with the bat, only if we're moving down.
          // If the bottom of the ball was previously above or at the top edge
          // of the bat, but is now below it:
          if (oy + Play.BALL_RADIUS <= Play.BAT_TOP_EDGE
              && this.y + Play.BALL_RADIUS > Play.BAT_TOP_EDGE) {
            var bounce = this.getBatBounceVector();
            if (bounce.collided()) {
              if (this.play.bat.currentType == BatType.MAGNET) {
                this.stuckToBat = true;
                this.batOffset = this.x - this.play.bat.x;
                this.dir = new Vector2(0, 0);
              } else {
                this.dir = bounce.vector();
              }

              this.timeSinceTouchedBat = 0;

              this.play.addImpact(new Impact(this.x, this.y, 0xc));
              collisionSound(CollisionType.BAT);

              // If we became stuck to the bat, stop moving for this frame
              if (this.stuckToBat) {
                break;
              }
            }
          } else if (this.y + Play.BALL_RADIUS > Play.BAT_TOP_EDGE
              && this.y < Play.BAT_TOP_EDGE + 15) {
            // The ball must have hit the side of the bat. Send it off at an
            // extreme angle and increase speed.
            var bounce = this.getBatBounceVector();
            if (bounce.collided()) {
              double dx = this.x > this.play.bat.x ? 1 : -1;
              this.dir = new Vector2(dx, Random.random(-0.3, -0.1)).unitVector();

              this.timeSinceTouchedBat = 0;

              this.play.addImpact(new Impact(this.x, Play.BAT_TOP_EDGE, 0xc));
              this.speed = Math.min(this.speed + 4, Play.BALL_MAX_SPEED);

              collisionSound(CollisionType.BAT_EDGE);
            }
          }
        }
      }
    }

    this.sync();

    this.shadow.setPosition(this.x + 16 - Play.WIDTH / 2.0, Play.HEIGHT / 2.0 - (this.y + 16));
  }

  private void incrementSpeed() {
    this.speed = Math.min(this.speed + 1, Play.BALL_MAX_SPEED);
  }

  /**
   * Determines the direction vector for the ball bouncing off the bat. If the
   * ball is not within range of the bat on the X axis, returns that there was
   * no collision and a vector pointing straight up.
   */
  private BatBounce getBatBounceVector() {
    double dx = this.x - this.play.bat.x;
    double w = this.play.bat.getWidth() / 2.0 + Play.BALL_RADIUS;

    if (Math.abs(dx) < w) {
      return new BatBounce(true, new Vector2(dx / w, -0.5).unitVector());
    }
    return new BatBounce(false, new Vector2(0, -1));
  }

  /**
   * Returns three new balls for this one, with direction vectors rotated 120
   * degrees from each other.
   */
  public List<Ball> generateMultiballs() {
    List<Ball> newBalls = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      Vector2 vec = this.dir.rotateBy(Math.toRadians(i * 120));
      if (Math.abs(vec.getY()) < 0.15) {
        // The ball could be stuck to the bat or moving almost horizontally,
        // which could lead to it bouncing left and right for ages. Generate a
        // random upward vector instead.
        vec = new Vector2(Random.random(-1, 1), -1).unitVector();
      }
      newBalls.add(new Ball(this.play, this.x, this.y, vec, false, this.speed));
    }
    return newBalls;
  }

  private void collisionSound(CollisionType collisionType) {
    switch (collisionType) {
      case BRICK, INDESTRUCTIBLE_BRICK -> this.play.playSound("hit_brick0");
      case WALL -> this.play.playSound("hit_wall0");
      case BAT -> {
        if (this.play.bat.currentType == BatType.MAGNET) {
          this.play.playSound("ball_stick0");
        } else {
          this.play.playSound("hit_fast0");
        }
      }
      case BAT_EDGE -> {
        if (this.play.bat.currentType == BatType.MAGNET) {
          this.play.playSound("ball_stick0");
        } else {
          this.play.playSound("hit_veryfast0");
        }
      }
    }
  }
}
