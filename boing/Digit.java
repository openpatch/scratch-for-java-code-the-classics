package boing;

import org.openpatch.scratch.TextAlign;
import org.openpatch.scratch.extensions.text.BitmapText;

/** One score digit, using the normal or goal-highlighted image glyph. */
public class Digit extends BitmapText {

  public Digit() {
    super("boing/assets/images/digit0%d.png");
    this.setAlign(TextAlign.CENTER);
    this.showScore(0, 0);
  }

  public void showScore(int score, int colour) {
    // The tenth point ends the game; leave the last digit visible during the fade.
    if (score > 9) {
      return;
    }
    this.addGlyph((char) ('0' + score), "boing/assets/images/digit" + colour + score + ".png");
    this.showText(Integer.toString(score));
  }
}
