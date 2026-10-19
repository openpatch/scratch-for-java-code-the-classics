package eggzy;

/**
 * The trail effect left behind by the player while dashing. It uses the trail
 * version of the player's current dash sprite.
 */
public class DashTrail extends Animation {

  public DashTrail(double x, double y, String image) {
    super(x, y, image + "_trail_{0}", 6, 5, Play.ANCHOR_PLAYER, 0, -1);
  }
}
