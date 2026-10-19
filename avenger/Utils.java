package avenger;

/** The module level utility functions of the original implementation. */
public final class Utils {

  private Utils() {}

  /** Returns 1, 0 or -1 depending on whether the number is positive, zero or negative. */
  public static int sign(double x) {
    if (x == 0) {
      return 0;
    }
    return x < 0 ? -1 : 1;
  }

  /**
   * For animations which should run from the first frame to the last frame
   * and then backwards through those frames before repeating: with 4 frames,
   * the repeating sequence is 0, 1, 2, 3, 2, 1.
   */
  public static int forwardBackwardAnimationFrame(int frame, int numFrames) {
    if (numFrames < 2) {
      return 0;
    }
    frame %= (numFrames * 2) - 2;
    if (frame >= numFrames) {
      frame = (numFrames - 1) * 2 - frame;
    }
    return frame;
  }

  /** The path of an image of the game, relative to the repository root. */
  public static String img(String name) {
    return "avenger/assets/images/" + name + ".png";
  }
}
