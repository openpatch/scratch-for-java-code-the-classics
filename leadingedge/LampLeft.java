package leadingedge;

/** A street lamp on the left side of the track. */
public class LampLeft extends Scenery {

  public LampLeft() {
    super(
        Play.LAMP_X,
        "left_light",
        0,
        Play.VIEW_DISTANCE / 2,
        2,
        new double[][] {{350, 1200}});
  }
}
