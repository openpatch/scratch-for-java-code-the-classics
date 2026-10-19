package cavern;

import org.openpatch.scratch.AnimatedSprite;

/**
 * The burst shown when a fruit is collected (type 0) or an orb pops (type 1).
 * Like the original, it shows the first six of its seven images.
 */
public class Pop extends AnimatedSprite {

  private Play play;

  public Pop(Play play, int x, int y, int type) {
    this.play = play;

    this.addAnimation("pop", frame -> "cavern/assets/images/pop" + type + (frame - 1) + ".png", 6);
    // The original shows each image for two frames at 60 frames per second
    this.setAnimationInterval(33);
    this.setPosition(x - Play.WIDTH / 2.0, Play.HEIGHT / 2.0 - y);
  }

  @Override
  public void run() {
    if (this.play.isFrozen()) {
      return;
    }
    this.playAnimation("pop", true);
    if (this.isAnimationPlayed()) {
      this.remove();
    }
  }
}
