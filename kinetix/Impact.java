package kinetix;

import org.openpatch.scratch.AnimatedSprite;

/**
 * A short animation played when the ball hits a wall or a brick, or when a
 * powerup is collected. The image names have the format impactXY where X is
 * the type as a hexadecimal digit and Y the animation frame.
 */
public class Impact extends AnimatedSprite {

  public Impact(double x, double y, int type) {
    String hex = Integer.toHexString(type);
    this.addAnimation("impact", frame -> "kinetix/assets/images/impact" + hex + (frame - 1) + ".png", 4);
    // The original shows each image for four frames at 60 frames per second
    this.setAnimationInterval(67);
    this.setPosition(x - Play.WIDTH / 2.0, Play.HEIGHT / 2.0 - y);
  }

  @Override
  public void run() {
    // Impacts stay still while the game-over screen is active
    if (!(this.getStage() instanceof Play play && play.isPlaying())) {
      return;
    }
    this.playAnimation("impact", true);
    if (this.isAnimationPlayed()) {
      this.remove();
    }
  }
}
