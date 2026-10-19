package leadingedge;

/**
 * A three-dimensional vector, used for car positions and the camera, like
 * pygame.math.Vector3 in the original.
 */
public class Vector3 {

  public double x;
  public double y;
  public double z;

  public Vector3(double x, double y, double z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public Vector3(Vector3 v) {
    this(v.x, v.y, v.z);
  }

  /** Adds the given vector to this one, in place. */
  public Vector3 add(Vector3 o) {
    this.x += o.x;
    this.y += o.y;
    this.z += o.z;
    return this;
  }

  /** Adds the given offsets to this one, in place. */
  public Vector3 add(double x, double y, double z) {
    this.x += x;
    this.y += y;
    this.z += z;
    return this;
  }

  /** Returns a new vector that is this one plus the given one. */
  public Vector3 plus(Vector3 o) {
    return new Vector3(this.x + o.x, this.y + o.y, this.z + o.z);
  }

  /** Returns a new vector that is this one minus the given one. */
  public Vector3 minus(Vector3 o) {
    return new Vector3(this.x - o.x, this.y - o.y, this.z - o.z);
  }

  /** Returns a new vector scaled by the given factor. */
  public Vector3 times(double f) {
    return new Vector3(this.x * f, this.y * f, this.z * f);
  }
}
