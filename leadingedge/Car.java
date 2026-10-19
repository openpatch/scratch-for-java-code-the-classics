package leadingedge;

/**
 * A racing car. The position is in track coordinates: z is the distance along
 * the track (decreasing as the car moves forwards, because the camera points
 * along the negative z axis) and x is the offset from the centre of the track.
 */
public class Car {

  protected final Play game;
  public final Vector3 pos;
  public String image;
  public double speed;
  public double grip = 1;
  public final char carLetter;
  public TrackPiece trackPiece;
  public double tyreRotation;

  public Car(Play game, Vector3 pos, char carLetter) {
    this.game = game;
    this.pos = pos;
    this.carLetter = carLetter;
    this.image = "car_" + carLetter + "_0_0";
  }

  public void update(double deltaTime) {
    this.pos.z -= this.speed * deltaTime;
    this.updateCurrentTrackPiece();
    this.tyreRotation += deltaTime * this.speed * 0.75;
  }

  public void updateCurrentTrackPiece() {
    TrackPiece currentTrackPiece = this.trackPiece;
    Integer idx = this.game.getTrackPieceForZ(this.pos.z);
    if (idx != null) {
      this.trackPiece = this.game.track.get(idx);
      if (this.trackPiece != currentTrackPiece) {
        if (currentTrackPiece != null) {
          currentTrackPiece.cars.remove(this);
        }
        this.trackPiece.cars.add(this);
      }
    }
  }

  /**
   * Sets the sprite to use. The angle is how much the car is turning (-4 to 4),
   * the frame depends on speed, braking and boost.
   */
  public void updateSprite(int angle, boolean braking, boolean boost) {
    int frame;
    if (this.speed == 0) {
      frame = 0;
    } else if (braking) {
      frame = 3;
    } else if (boost) {
      frame = (int) (this.tyreRotation % 2) + 4;
    } else {
      frame = (int) (this.tyreRotation % 2) + 1;
    }
    this.image = "car_" + this.carLetter + "_" + angle + "_" + frame;
  }
}
