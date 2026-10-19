package leadingedge;

import java.util.List;
import org.openpatch.scratch.Random;

/**
 * A CPU-controlled car. It follows the track, changes speed every few seconds
 * (upwards on average, so that slow cars can catch up) and picks a target X
 * position to steer towards, avoiding nearby cars.
 */
public class CPUCar extends Car {

  // CPU cars accelerate faster than the player but have a lower top speed
  public final double accel;
  public double targetSpeed;
  public double targetX;

  // Set based on track curvature, so we can display an angled variant of the
  // car sprite
  public double steering;

  private double changeSpeedTimer;

  private static final char[] CAR_LETTERS = {'b', 'c', 'd', 'e'};

  public CPUCar(Play game, Vector3 pos, double accel, double speed) {
    super(
        game,
        pos,
        CAR_LETTERS[org.openpatch.scratch.Random.randomInt(CAR_LETTERS.length - 1)]);

    this.accel = Play.PLAYER_ACCELERATION_MAX * accel;
    this.targetSpeed = speed;
    this.targetX = pos.x;
    this.steering = 0;
    this.changeSpeedTimer = Random.random(2, 4);
  }

  @Override
  public void update(double deltaTime) {
    if (this.game.raceComplete) {
      this.targetSpeed = this.game.playerCar.speed;
    }

    this.speed = Play.moveTowards(this.speed, this.targetSpeed, this.accel * deltaTime);
    this.pos.x = Play.moveTowards(this.pos.x, this.targetX, 400 * deltaTime);

    super.update(deltaTime);

    Integer trackPieceIdx = this.game.getFirstTrackPieceAheadIndex(this.pos.z);
    if (trackPieceIdx != null) {
      this.steering = this.game.track.get(trackPieceIdx).offsetX;
    }

    // Every few seconds we'll change target speed by a random amount, but
    // upwards on average, so that slow cars have a chance to catch up, and so
    // that we can see CPU cars overtaking each other
    this.changeSpeedTimer -= deltaTime;
    if (this.changeSpeedTimer <= 0 && !this.game.raceComplete) {
      this.targetSpeed += Random.random(-4, 6);
      this.targetSpeed =
          Math.min(
              Math.max(this.targetSpeed, Play.CPU_CAR_MIN_TARGET_SPEED),
              Play.CPU_CAR_MAX_TARGET_SPEED);

      // If we're on a sharp corner and speed is above a certain level, reduce
      // target speed
      if (trackPieceIdx != null) {
        Double targetSpeedOverride =
            this.game.track.get(trackPieceIdx).cpuMaxTargetSpeed;
        if (targetSpeedOverride != null && this.targetSpeed > targetSpeedOverride) {
          // Make it slightly random
          this.targetSpeed = Random.random(targetSpeedOverride - 3, targetSpeedOverride);
        }
      }

      // Also change target X pos to a random value. Ensure not too close to
      // values for nearby cars, to avoid cars driving through each other
      for (int attempt = 0; attempt < 20; attempt++) {
        this.targetX = Random.random(-1000, 1000);
        if (!this.isTargetXTooCloseToNearbyCars()) {
          break;
        }
      }

      // Reset timer
      this.changeSpeedTimer = Random.random(2, 4);
    }
  }

  private boolean isTargetXTooCloseToNearbyCars() {
    List<Car> cars = this.game.cars;
    for (Car car : cars) {
      if (car != this
          && Math.abs(this.pos.z - car.pos.z) < 20
          && Math.abs(this.targetX - car.pos.x) < 300) {
        return true;
      }
    }
    return false;
  }
}
