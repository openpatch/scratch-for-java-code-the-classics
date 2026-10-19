package eggzy;

/**
 * A rectangle in the original game's pixel coordinates (origin top-left, y
 * growing downwards), like the pygame Rect the Python code uses. Edge cases
 * match pygame: rectangles that only touch do not overlap, and a point on the
 * right or bottom edge is outside.
 */
public class Rect {

  public double x;
  public double y;
  public double w;
  public double h;

  public Rect(double x, double y, double w, double h) {
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
  }

  public double left() {
    return this.x;
  }

  public double top() {
    return this.y;
  }

  public double right() {
    return this.x + this.w;
  }

  public double bottom() {
    return this.y + this.h;
  }

  /** Do the two rectangles overlap by at least one pixel? */
  public boolean colliderect(Rect other) {
    return this.left() < other.right()
        && other.left() < this.right()
        && this.top() < other.bottom()
        && other.top() < this.bottom();
  }

  /** Is the point inside the rectangle (left/top inclusive, right/bottom not)? */
  public boolean collidepoint(double px, double py) {
    return this.left() <= px
        && px < this.right()
        && this.top() <= py
        && py < this.bottom();
  }
}
