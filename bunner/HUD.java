package bunner;

import org.openpatch.scratch.TextAlign;
import org.openpatch.scratch.extensions.text.BitmapText;

/** Displays the score and the high score at the top of the screen. */
public class HUD {

  private static final int DIGIT_DISTANCE = 25;

  private final Play play;
  private final BitmapText score = makeNumber();
  private final BitmapText highScore = makeNumber();

  public HUD(Play play) {
    this.play = play;

    this.score.setPosition(-Play.WIDTH / 2.0, Play.HEIGHT / 2.0);
    // The original positions right-aligned digits by their 25-pixel advance;
    // each glyph is 35 pixels wide, so its painted right edge is 10 pixels later.
    this.highScore.setAlign(TextAlign.RIGHT);
    this.highScore.setPosition(Play.WIDTH / 2.0, Play.HEIGHT / 2.0);
    play.add(this.score);
    play.add(this.highScore);
  }

  private static BitmapText makeNumber() {
    BitmapText number = new BitmapText("bunner/assets/images/digit0%d.png");
    for (int digit = 0; digit < 10; digit++) {
      number.addGlyph((char) ('0' + digit), "bunner/assets/images/digit0" + digit + ".png");
    }
    number.setIsUI(true);
    number.setLetterSpacing(DIGIT_DISTANCE - 35);
    return number;
  }

  public void update() {
    boolean visible = this.play.bunner != null && !this.play.isFrozen();

    if (visible) {
      this.score.showText(Integer.toString(this.play.score()));
      this.highScore.showText(Integer.toString(Play.highScore));
    } else {
      this.score.showText("");
      this.highScore.showText("");
    }
  }
}
