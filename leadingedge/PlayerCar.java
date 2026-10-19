package leadingedge;

import org.openpatch.scratch.Operators;
import org.openpatch.scratch.extensions.sound.SoundPlayback;

/**
 * The player's car: accelerate, brake and steer. Corners shift the car's X
 * position so that failing to steer will take it off the track, driving on
 * grass slows it down, and hitting scenery makes it explode and reset.
 */
public class PlayerCar extends Car {

  private final Controls controls;
  private double offsetXChange = 0;
  private boolean resetting = false;
  private Integer explodeTimer = null;
  private Integer lastCheckpointIdx = null;
  public int lap = 1;
  public double lapTime = 0;
  public double raceTime = 0;
  public Double fastestLap = null;
  public boolean lastLapWasFastest = false;
  private boolean braking = false;

  // Engine and skid sounds are not played with Play.playSound as they require
  // custom behaviour. They are played as copies with their own volume.
  private SoundPlayback currentEngineSound = null;
  private int currentEngineSoundIdx = -1;

  private SoundPlayback skidSound = null;

  private double grassSoundRepeatTimer = 0;
  private boolean onGrass = false;

  // Last known position in the race, indexed from 0 - used to decide when to
  // play overtaking sounds
  private int prevPosition = Play.NUM_CARS - 1;

  public PlayerCar(Play game, Vector3 pos, Controls controls) {
    super(game, pos, 'a');
    this.controls = controls;
    this.updateEngineSound();
  }

  /** Stops the engine and skid sounds of the car. */
  public void stopEngineSound() {
    if (this.currentEngineSound != null) {
      this.currentEngineSound.stop();
    }
    if (this.skidSound != null) {
      this.skidSound.stop();
      this.skidSound = null;
    }
  }

  @Override
  public void update(double deltaTime) {
    if (!this.game.raceComplete) {
      this.lapTime += deltaTime;
      this.raceTime += deltaTime;
    }

    this.grassSoundRepeatTimer -= deltaTime;

    this.updateEngineSound();

    // Play overtaking sounds? See if our position in the race has changed
    // since last frame
    int currentPosition = this.game.cars.indexOf(this);
    if (currentPosition != this.prevPosition) {
      // Only play sound if speed difference is high enough
      if (Math.abs(this.speed - this.game.cars.get(this.prevPosition).speed) > 4) {
        this.game.playSound("overtake", 6);
      }
      this.prevPosition = currentPosition;
    }

    if (this.resetting) {
      if (this.explodeTimer != null) {
        this.explodeTimer += 1;
        if (this.explodeTimer > 31) {
          this.explodeTimer = null;
        }
      } else {
        // Reset player to centre of track over about 2 seconds
        this.pos.x = Play.moveTowards(this.pos.x, 0, 2000 * deltaTime);
        this.resetting = this.pos.x != 0;
      }
    }

    double xMove = 0;
    double accel = 0;

    // The index of the track piece ahead, used for scenery collisions,
    // checkpoints and the skid sound
    Integer trackPieceIdx = null;

    if (!this.resetting) {
      // Not resetting - do normal movement & controls

      this.braking = false;

      // Only get control inputs if race is not complete
      if (!this.game.raceComplete) {
        if (this.controls.buttonDown(0)) {
          accel =
              this.speed < Play.HIGH_ACCEL_THRESHOLD
                  ? Play.PLAYER_ACCELERATION_MAX
                  : Play.PLAYER_ACCELERATION_MIN;
          this.speed += accel * deltaTime;
        } else if (this.controls.buttonDown(1)) {
          // Brake
          this.braking = true;
          this.speed = Math.max(0, this.speed - deltaTime * 10);
        }
      }

      // Apply drag in a frame-rate independent way
      double dragFactor = 0.9975;
      if (this.onGrass) {
        // More drag on grass
        dragFactor -= 0.0025;
      }
      this.speed *= Math.pow(dragFactor, deltaTime / (1.0 / 60));

      // If we're going round a corner, shift X pos so that failing to steer
      // will take you off the track. This is necessary because in this game,
      // the corners are just illusions!
      if (this.offsetXChange != 0) {
        // We also set grip to less than 1 if we're cornering at high speed
        // (but only if we're steering in the same direction as the corner)
        if (this.speed > Play.LOSE_GRIP_SPEED
            && Play.sign(this.getXInput()) == -Play.sign(this.offsetXChange)) {
          this.grip = Operators.constrain(
              Operators.map(this.speed, Play.LOSE_GRIP_SPEED, Play.ZERO_GRIP_SPEED, 1, 0), 0, 1);
        } else {
          this.grip = 1;
        }

        // Apply corner offset - grip will be used to alter steering movement.
        // We don't multiply by deltaTime here as offsetXChange is partly based
        // on the total amount of forward motion that has taken place since the
        // previous frame, which already takes deltaTime into account
        if (!this.game.raceComplete) {
          this.pos.x -= this.offsetXChange * Play.CORNER_OFFSET_MULTIPLIER;
        }
      } else {
        // Not going around a corner
        this.grip = 1;
      }

      // Get track piece we were on before forward motion was applied
      Integer previousTrackPieceIdx = this.game.getFirstTrackPieceAheadIndex(this.pos.z);

      // Apply steering
      if (this.speed > 0 && !this.game.raceComplete) {
        xMove =
            this.getXInput() * this.speed * Play.STEERING_STRENGTH * this.grip * deltaTime;
        this.pos.x -= xMove;
      }

      // Call parent (Car) update method, which includes applying motion
      super.update(deltaTime);

      // Check for collisions with other cars. Note - axes are not uniform in
      // scale (1 unit in X axis is much smaller than 1 unit in Z axis), so we
      // just check X and Z differences separately
      for (Car car : this.game.cars) {
        if (car != this) {
          Vector3 vec = this.pos.minus(car.pos);
          double collideFrontDistanceZ = 0.6;
          double collideBackDistanceZ = 1.2;
          if (Math.abs(vec.x) < 260
              && vec.z < collideFrontDistanceZ
              && vec.z > -collideBackDistanceZ) {
            double midpoint = (this.pos.z - car.pos.z) / 2 + car.pos.z;

            if (Math.abs(vec.z) < 0.2) {
              // Side collision
              this.pos.x += Play.sign(vec.x) * 50;
              car.pos.x -= Play.sign(vec.x) * 50;
            } else if (vec.z > 0) {
              // Colliding with the back of the car in front
              this.speed = Math.max(car.speed - 3, 0);
              car.speed = Math.max(car.speed, this.speed + 3);
              if (car instanceof CPUCar cpuCar) {
                cpuCar.targetSpeed = car.speed;
              }

              // Shift us back and other car forward so we're no longer
              // overlapping
              this.pos.z = midpoint + collideFrontDistanceZ * 0.6;
              car.pos.z = midpoint - collideFrontDistanceZ * 0.6;

              this.game.playSound("bump", 6);
            } else {
              // Car behind collided with us - get a speed boost
              this.speed = Math.max(this.speed, car.speed + 3);
              car.speed = Math.max(this.speed - 3, 0);

              // Shift other car back and us forward so we're no longer
              // overlapping
              this.pos.z = midpoint - collideBackDistanceZ * 0.6;
              car.pos.z = midpoint + collideBackDistanceZ * 0.6;

              this.game.playSound("bump_behind", 1);
            }
          }
        }
      }

      // Check for collisions with scenery, driving on grass and passing a
      // checkpoint
      trackPieceIdx = this.game.getFirstTrackPieceAheadIndex(this.pos.z);
      if (trackPieceIdx != null) {
        TrackPiece trackPiece = this.game.track.get(trackPieceIdx);

        for (Scenery scenery : trackPiece.scenery) {
          for (double[] collisionZone : scenery.collisionZones) {
            double zoneLeft = scenery.x + collisionZone[0];
            double zoneRight = scenery.x + collisionZone[1];
            if (zoneLeft < this.pos.x && this.pos.x < zoneRight) {
              this.speed = 0;
              this.resetting = true;
              this.explodeTimer = 0; // Start explosion animation
              this.game.playSound("explosion", 1);
            }
          }
        }

        // Are we on, or have we passed, a checkpoint?
        if (previousTrackPieceIdx != null) {
          for (int i = previousTrackPieceIdx; i <= trackPieceIdx; i++) {
            if (this.game.track.get(i) instanceof TrackPieceStartLine) {
              // It's a checkpoint. If it's the first one, ignore it (passing
              // the start line at the start of the race is not of interest).
              // If we've already dealt with this checkpoint, ignore it.
              // Otherwise update lap count and lap time
              if (this.lastCheckpointIdx != null && this.lastCheckpointIdx != i) {
                this.lap += 1;

                // Was this the fastest lap?
                if (this.fastestLap == null || this.lapTime < this.fastestLap) {
                  this.fastestLap = this.lapTime;
                  this.lastLapWasFastest = true;
                  this.game.playSound("fastlap", 1);
                } else {
                  this.lastLapWasFastest = false;
                }

                // Play final lap sound effect?
                if (this.lap == Play.NUM_LAPS) {
                  this.game.playSound("final_lap", 1);
                }

                // Set lap time back to 0 for new lap
                this.lapTime = 0;
              }

              this.lastCheckpointIdx = i;
            }
          }
        }

        // Are we on the grass?
        if (Math.abs(this.pos.x) + 100 > trackPiece.width / 2) {
          this.onGrass = true;
          if (this.grassSoundRepeatTimer <= 0) {
            this.game.playSound("hit_grass", 1);
            this.grassSoundRepeatTimer = 0.15;
          }

          // Are we way too far off the track? Reset if so
          if (Math.abs(this.pos.x) > 6000) {
            this.speed = 0;
            this.resetting = true;
          }
        } else {
          this.onGrass = false;
        }
      }
    }

    // Depending on grip, turn skid sound on/off or vary volume
    double volume;
    if (this.resetting || this.grip >= Play.SKID_SOUND_START_GRIP || this.getXInput() == 0) {
      volume = 0;
    } else {
      volume = Operators.constrain(Operators.map(this.grip, Play.SKID_SOUND_START_GRIP, 0.5, 0, 1), 0, 1);

      // Scale volume based on track curvature - higher volume for tighter
      // corners
      if (trackPieceIdx != null) {
        TrackPiece trackPiece = this.game.track.get(trackPieceIdx);
        volume *= Operators.constrain(Operators.map(Math.abs(trackPiece.offsetX), 0, 15, 0, 1), 0, 1);
      }
    }

    if (volume > 0) {
      // The original loops the skid sound indefinitely, so a finished copy is
      // simply replaced by a new one
      if (this.skidSound == null || !this.skidSound.isPlaying()) {
        this.skidSound = this.game.playSoundCopy("skid_loop0", volume * 100);
      } else {
        this.skidSound.setVolume(volume * 100);
      }
    } else if (this.skidSound != null) {
      this.skidSound.stop();
      this.skidSound = null;
    }

    // Set sprite
    if (this.explodeTimer != null) {
      this.image = String.format("explode%02d", this.explodeTimer / 2);
    } else {
      int direction = 0;
      if (xMove < 0) {
        direction = -1;
      } else if (xMove > 0) {
        direction = 1;
      }
      boolean boost = accel > 0 && this.speed < Play.HIGH_ACCEL_THRESHOLD && this.speed > 0;
      this.updateSprite(direction, this.braking, boost);
    }
  }

  private void updateEngineSound() {
    int soundIndex = Math.min((int) (this.speed * 0.6), 40 - 1);
    boolean newIndex = soundIndex != this.currentEngineSoundIdx;
    this.currentEngineSoundIdx = soundIndex;

    // The original loops the engine sounds indefinitely, so a finished copy
    // is simply replaced by a new one
    if (newIndex || this.currentEngineSound == null || !this.currentEngineSound.isPlaying()) {
      if (newIndex && this.currentEngineSound != null) {
        this.currentEngineSound.stop();
      }
      this.currentEngineSound = this.game.playSoundCopy("engine_short" + soundIndex, 30);
    }
  }

  private double getXInput() {
    return this.controls.getX();
  }

  public void setOffsetXChange(double value) {
    this.offsetXChange = value;
  }
}
