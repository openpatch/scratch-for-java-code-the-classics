package cavern;

import java.util.ArrayList;
import java.util.List;
import org.openpatch.scratch.Sprite;

/**
 * Displays the score, level, lives and health at the bottom of the screen,
 * using the original font images.
 */
public class Status {

  // Widths of the letters A to Z in the font images
  private static final int[] CHAR_WIDTH = {
    27, 26, 25, 26, 25, 25, 26, 25, 12, 26, 26, 25, 33, 25, 26,
    25, 27, 26, 26, 25, 26, 26, 38, 25, 25, 25
  };

  private static final int TEXT_Y = 451;
  private static final int ICON_Y = 450;

  private final Play play;
  private final List<Sprite> chars = new ArrayList<>();
  private final List<Sprite> icons = new ArrayList<>();

  public Status(Play play) {
    this.play = play;

    for (int i = 0; i < 20; i++) {
      var s = new Sprite();
      s.addCostume("font032", "cavern/assets/images/font032.png");
      for (int c = 48; c <= 57; c++) {
        s.addCostume("font0" + c, String.format("cavern/assets/images/font0%d.png", c));
      }
      for (int c = 65; c <= 90; c++) {
        s.addCostume("font0" + c, String.format("cavern/assets/images/font0%d.png", c));
      }
      s.hide();
      play.add(s);
      this.chars.add(s);
    }

    for (int i = 0; i < 6; i++) {
      var s = new Sprite();
      s.addCostume("life", "cavern/assets/images/life.png");
      s.addCostume("plus", "cavern/assets/images/plus.png");
      s.addCostume("health", "cavern/assets/images/health.png");
      s.hide();
      play.add(s);
      this.icons.add(s);
    }
  }

  public void update() {
    if (this.play.player == null) {
      return;
    }

    int n = 0;

    // Display score, right-justified at the edge of the screen
    String score = String.valueOf(this.play.player.score);
    n = this.drawText(score, Play.WIDTH - 2 - CHAR_WIDTH[0] * score.length(), n);

    // Display level number
    String level = "LEVEL " + (this.play.level + 1);
    int totalWidth = 0;
    for (int i = 0; i < level.length(); i++) {
      totalWidth += this.charWidth(level.charAt(i));
    }
    n = this.drawText(level, (Play.WIDTH - totalWidth) / 2, n);

    for (int i = n; i < this.chars.size(); i++) {
      this.chars.get(i).hide();
    }

    // Display lives and health
    int iconIndex = 0;
    int x = 0;
    int lives = this.play.player.lives;
    for (int i = 0; i < Math.min(2, lives); i++) {
      x = this.drawIcon(iconIndex++, "life", x);
    }
    if (lives > 2) {
      x = this.drawIcon(iconIndex++, "plus", x);
    }
    for (int i = 0; i < this.play.player.health; i++) {
      x = this.drawIcon(iconIndex++, "health", x);
    }
    for (int i = iconIndex; i < this.icons.size(); i++) {
      this.icons.get(i).hide();
    }
  }

  private int drawText(String text, int x, int startIndex) {
    for (int i = 0; i < text.length() && startIndex + i < this.chars.size(); i++) {
      char c = text.charAt(i);
      var s = this.chars.get(startIndex + i);
      s.switchCostume("font0" + (int) c);
      s.setPosition(
          x + s.getWidth() / 2.0 - Play.WIDTH / 2.0,
          Play.HEIGHT / 2.0 - (TEXT_Y + s.getHeight() / 2.0));
      s.show();
      x += this.charWidth(c);
    }
    return startIndex + text.length();
  }

  private int drawIcon(int index, String name, int x) {
    var s = this.icons.get(index);
    s.switchCostume(name);
    s.setPosition(
        x + s.getWidth() / 2.0 - Play.WIDTH / 2.0,
        Play.HEIGHT / 2.0 - (ICON_Y + s.getHeight() / 2.0));
    s.show();
    return x + s.getWidth();
  }

  private int charWidth(char c) {
    int index = Math.max(0, c - 65);
    return CHAR_WIDTH[index];
  }
}
