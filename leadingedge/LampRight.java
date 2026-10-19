package leadingedge;

/** A street lamp on the right side of the track. */
public class LampRight extends Scenery {

  public LampRight() {
    super(
        -Play.LAMP_X,
        "right_light",
        0,
        Play.VIEW_DISTANCE / 2,
        2,
        new double[][] {{-1200, -350}});
  }
}
