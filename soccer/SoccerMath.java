package soccer;

import org.openpatch.scratch.Vector2;

/** Helper functions for angles, vectors and costs. */
public final class SoccerMath {

  private SoccerMath() {}

  // Custom sine/cosine functions for angles of 0 to 7, where 0 is up
  public static double sin(int x) {
    return Math.sin(x * Math.PI / 4);
  }

  public static double cos(int x) {
    return sin(x + 2);
  }

  /** Converts a vector to an angle in the range 0 to 7. */
  public static int vecToAngle(Vector2 vec) {
    return (int) (4 * Math.atan2(vec.getX(), -vec.getY()) / Math.PI + 8.5) % 8;
  }

  /** Converts an angle in the range 0 to 7 to a direction vector. */
  public static Vector2 angleToVec(int angle) {
    return new Vector2(sin(angle), -cos(angle));
  }

  /**
   * Turns a vector into a unit vector. Returns the x and y components of the
   * unit vector, followed by the original length. A zero length vector stays
   * a zero length vector.
   */
  public static double[] safeNormalise(Vector2 vec) {
    double length = vec.length();
    if (length == 0) {
      return new double[] {0, 0, 0};
    }
    return new double[] {vec.getX() / length, vec.getY() / length, length};
  }

  public static Vector2 unitVector(double[] normalised) {
    return new Vector2(normalised[0], normalised[1]);
  }

  /** Generates a score for a given position, where lower numbers are better. */
  public static double cost(Play play, Vector2 pos, int team, double handicap) {
    Vector2 ownGoalPos = new Vector2(Play.HALF_LEVEL_W, team == 1 ? 78 : Play.LEVEL_H - 78);
    double result = 3500 / pos.sub(ownGoalPos).length();

    for (Player p : play.players) {
      if (p.team != team) {
        result += 4000 / Math.max(24, p.vpos.sub(pos).length());
      }
    }

    result += Math.pow(pos.getX() - Play.HALF_LEVEL_W, 2) / 200;
    result -= pos.getY() * (4 * team - 2);
    result += handicap;

    return result;
  }

  /** Average of two numbers, snapping if the difference is less than 1. */
  public static double avg(double a, double b) {
    return Math.abs(b - a) < 1 ? b : (a + b) / 2;
  }

  /**
   * Returns true, if target is a good target for a pass from source. Target
   * can also be a goal.
   */
  public static boolean targetable(Play play, SoccerActor target, Player source) {
    double[] n0 = safeNormalise(target.vpos.sub(source.vpos));
    Vector2 v0 = unitVector(n0);
    double d0 = n0[2];

    // Computer-controlled teams avoid passes which are likely to be intercepted
    if (!play.teams[source.team].human()) {
      for (Player p : play.players) {
        double[] n1 = safeNormalise(p.vpos.sub(source.vpos));
        Vector2 v1 = unitVector(n1);
        double d1 = n1[2];

        if (p.team != target.team && d1 > 0 && d1 < d0 && v0.dot(v1) > 0.8) {
          return false;
        }
      }
    }

    return target.team == source.team && d0 > 0 && d0 < 300 && v0.dot(angleToVec(source.dir)) > 0.8;
  }
}
