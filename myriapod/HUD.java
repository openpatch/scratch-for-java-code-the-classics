package myriapod;

import java.util.ArrayList;
import java.util.List;
import org.openpatch.scratch.Sprite;
import org.openpatch.scratch.UISprite;

/** Displays the number of lives and the score. */
public class HUD {

  private final Play play;
  private final List<Sprite> lives = new ArrayList<>();
  private final List<Sprite> digits = new ArrayList<>();

  public HUD(Play play) {
    this.play = play;

    for (int i = 0; i < 4; i++) {
      var s = new UISprite();
      s.addCostume("life", "myriapod/assets/images/life.png");
      s.hide();
      play.add(s);
      this.lives.add(s);
    }

    for (int i = 0; i < 8; i++) {
      var s = new UISprite();
      for (int d = 0; d < 10; d++) {
        s.addCostume("digit" + d, String.format("myriapod/assets/images/digit%d.png", d));
      }
      s.hide();
      play.add(s);
      this.digits.add(s);
    }
  }

  public void update() {
    boolean visible = this.play.player != null && !this.play.isFrozen();

    if (!visible) {
      for (Sprite s : this.lives) {
        s.hide();
      }
      for (Sprite s : this.digits) {
        s.hide();
      }
      return;
    }

    // Display the number of lives
    for (int i = 0; i < this.lives.size(); i++) {
      var s = this.lives.get(i);
      if (i < this.play.player.lives) {
        // Blit position in the original: (i*40+8, 4), image size 32 x 32
        s.setPosition(i * 40 + 8 + 16 - Play.WIDTH / 2.0, Play.HEIGHT / 2.0 - (4 + 16));
        s.show();
      } else {
        s.hide();
      }
    }

    // Display the score, right-justified
    String score = String.valueOf(this.play.score);
    for (int i = 0; i < this.digits.size(); i++) {
      var s = this.digits.get(i);
      if (i < score.length()) {
        // Digits are placed from right to left, at 24 pixel intervals
        char digit = score.charAt(score.length() - 1 - i);
        s.switchCostume("digit" + digit);
        s.setPosition(468 - (i + 1) * 24 + 15 - Play.WIDTH / 2.0, Play.HEIGHT / 2.0 - (5 + 15));
        s.show();
      } else {
        s.hide();
      }
    }
  }
}
