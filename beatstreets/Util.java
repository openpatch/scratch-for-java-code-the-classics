package beatstreets;

import org.openpatch.scratch.extensions.text.BitmapText;

/** The utility functions of the original module. */
public final class Util {

  private Util() {}

  /** The image pattern of the game's bitmap font. */
  public static final String FONT = "beatstreets/assets/images/font%03d.png";

  /** The image that the '%' sign substitutes for in text. */
  public static final String BUTTON_GLYPH = "beatstreets/assets/images/xb_a.png";

  /** A text drawn with the game's bitmap font, with the '%' button glyph. */
  public static BitmapText bitmapText() {
    BitmapText text = new BitmapText(FONT);
    text.addGlyph('%', BUTTON_GLYPH);
    return text;
  }

  /** Returns 1, 0 or -1 depending on whether the number is positive, zero or negative. */
  public static int sign(double x) {
    if (x == 0) {
      return 0;
    }
    return x < 0 ? -1 : 1;
  }

  /**
   * Moves towards a target value at the given speed. Returns the new value and
   * the direction of travel (-1, 0 or 1).
   */
  public static double[] moveTowards(double n, double target, double speed) {
    if (n < target) {
      return new double[] {Math.min(n + speed, target), 1};
    } else if (n > target) {
      return new double[] {Math.max(n - speed, target), -1};
    }
    return new double[] {n, 0};
  }

}
