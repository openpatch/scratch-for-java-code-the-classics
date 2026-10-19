package leadingedge;

/**
 * A piece of scenery at the side of the track, e.g. a billboard or a lamp.
 */
public class Scenery {

  public double x;
  protected String imageName;
  public double minDrawDistance;
  public double maxDrawDistance;
  public double scale;
  public double[][] collisionZones;

  public Scenery(
      double x,
      String imageName,
      double minDrawDistance,
      double maxDrawDistance,
      double scale,
      double[][] collisionZones) {
    this.x = x;
    this.imageName = imageName;
    this.minDrawDistance = minDrawDistance;
    this.maxDrawDistance = maxDrawDistance;
    this.scale = scale;
    this.collisionZones = collisionZones;
  }

  /** The image to draw, which subclasses may vary per frame. */
  public String getImageName() {
    return this.imageName;
  }
}
