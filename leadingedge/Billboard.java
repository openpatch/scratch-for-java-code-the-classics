package leadingedge;

/** A billboard at the side of the track. */
public class Billboard extends Scenery {

  public Billboard(double x, String imageName) {
    super(
        x,
        imageName,
        0,
        Play.VIEW_DISTANCE / 2,
        2,
        new double[][] {{-Play.imageWidth(imageName), Play.imageWidth(imageName)}});
  }
}
