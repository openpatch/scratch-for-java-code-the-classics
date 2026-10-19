package boing;

import org.openpatch.scratch.*;

public class Impact extends AnimatedSprite {

  public Impact(Vector2 position) {
    this.setPosition(position);
    this.addAnimation("impact", frame -> String.format("boing/assets/images/impact%d.png", frame - 1), 5);
  }

  public void run() {
    this.playAnimation("impact", true);
    if (this.isAnimationPlayed()) {
      this.remove();
    }
  }

}
