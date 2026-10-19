package leadingedge;

import java.util.ArrayList;
import java.util.List;

/**
 * One piece of the track: technically just a line, but a polygon is drawn to
 * connect it to the next piece. The X and Y offsets define how its position
 * differs from the previous piece, so a series of pieces with X offsets leads
 * to a curve.
 */
public class TrackPiece {

  public final List<Scenery> scenery;
  public final double offsetX;
  public final double offsetY;
  public final Double cpuMaxTargetSpeed;
  public final int[] col;
  public final double width;

  /** The cars currently on this track piece. */
  public final List<Car> cars = new ArrayList<>();

  public TrackPiece() {
    this(new ArrayList<>(), 0, 0, null);
  }

  public TrackPiece(List<Scenery> scenery, double offsetX, double offsetY, Double cpuMaxTargetSpeed) {
    this(scenery, offsetX, offsetY, cpuMaxTargetSpeed, Play.TRACK_COLOUR, Play.TRACK_W);
  }

  public TrackPiece(
      List<Scenery> scenery,
      double offsetX,
      double offsetY,
      Double cpuMaxTargetSpeed,
      int[] col,
      double width) {
    this.scenery = scenery;
    this.offsetX = offsetX;
    this.offsetY = offsetY;
    this.cpuMaxTargetSpeed = cpuMaxTargetSpeed;
    this.col = col;
    this.width = width;
  }
}
