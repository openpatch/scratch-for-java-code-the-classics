package beatstreets;

import org.openpatch.scratch.Operators;
import org.openpatch.scratch.Random;
import org.openpatch.scratch.Vector2;
import org.openpatch.scratch.extensions.sound.SoundPlayback;

/**
 * The scooter-riding enemy. The engine sound is a looping sound copy whose
 * volume depends on the distance to the player - the original pans it between
 * the left and right speakers, which this port approximates with the average
 * of the two volumes.
 */
public class EnemyScooterboy extends Enemy {

  public static final double SCOOTER_SPEED_SLOW = 4;
  public static final double SCOOTER_SPEED_FAST = 12;
  public static final double SCOOTER_ACCELERATION = 0.2;

  private double scooterSpeed = SCOOTER_SPEED_SLOW;
  private double scooterTargetSpeed = SCOOTER_SPEED_SLOW;

  /** The looping engine sound copy, a private channel of the original. */
  private SoundPlayback engineSound;
  /** Is the engine sound currently looping? */
  private boolean engineLooping = false;

  public EnemyScooterboy(Play game, Vector2 pos, int startTimer) {
    super(game, "blank", pos, 160, 256, "scooterboy",
        new String[] {"scooterboy_attack1"}, startTimer,
        new Vector2(1, 1), 15, 500, Game.ENEMY_APPROACH_PLAYER_DISTANCE_SCOOTERBOY,
        new Vector2(25, 20), Random.randomInt(0, 2), null, 30);
    this.state = State.RIDING_SCOOTER;
    this.scooterTargetSpeed = this.scooterSpeed;
  }

  @Override
  public void spawned() {
    super.spawned();
    this.engineSound = this.game.playSoundCopy("scooter_slow0", 100);
    this.engineLooping = true;
  }

  @Override
  protected void makeDecision() {
    // Scooterboy stays on his scooter until knocked off
    if (this.state != State.RIDING_SCOOTER) {
      super.makeDecision();
    }
  }

  @Override
  public String determineImage() {
    // Riding the scooter is a state unique to Scooterboy
    if (this.state == State.RIDING_SCOOTER) {
      int facingId = this.facingX == 1 ? 1 : 0;
      int frame = 0;
      if (this.scooterSpeed < this.scooterTargetSpeed) {
        // Currently speeding up
        frame = Math.min(this.frame / 5, 2);
      }
      return this.sprite + "_ride_" + facingId + "_" + frame + "_" + this.colourVariant;
    }
    return super.determineImage();
  }

  @Override
  public void run() {
    if (!this.isPlaying()) {
      super.run();
      return;
    }
    if (this.state == State.RIDING_SCOOTER) {
      Player player = this.game.player;

      if (this.engineSound != null) {
        // The original changes the volume independently on the left and right
        // speakers - approximate with the average of the two volumes
        double leftVolume = Operators.constrain(Operators.map(
            Math.abs(this.vpos.getX() - player.vpos.getX() + 500), 0, 1000, 1, 0), 0, 1);
        double rightVolume = Operators.constrain(Operators.map(
            Math.abs(this.vpos.getX() - player.vpos.getX() - 500), 0, 1000, 1, 0), 0, 1);
        double volume = (leftVolume + rightVolume) / 2 * 100;
        if (this.engineLooping) {
          if (!this.engineSound.isPlaying()) {
            this.engineSound = this.game.playSoundCopy("scooter_slow0", volume);
          } else {
            this.engineSound.setVolume(volume);
          }
        }
      }

      // Currently accelerating/decelerating?
      if (this.scooterSpeed != this.scooterTargetSpeed) {
        this.scooterSpeed = Util.moveTowards(this.scooterSpeed, this.scooterTargetSpeed,
            SCOOTER_ACCELERATION)[0];
        this.frame += 1;
      } else if (this.onScreen() && Random.randomInt(0, 30) == 0) {
        // If on screen, random chance of accelerating
        this.scooterTargetSpeed = SCOOTER_SPEED_FAST;
        if (this.engineSound != null) {
          // The original replaces the looping engine sound with a one-shot
          // accelerate sound on the same channel
          this.engineSound.stop();
          this.engineLooping = false;
        }
        this.game.playSound("scooter_accelerate", 6);
        this.frame = 0;
      }

      // Move forward
      this.target = new Vector2(this.vpos.getX() + this.facingX * this.scooterSpeed,
          this.target.getY());
      this.vpos = new Vector2(this.target.getX(), this.vpos.getY());

      // Turn around if we've gone off the edge of the screen. We check
      // screenX, the actual screen position, as opposed to the position in
      // the scrolling level.
      if ((this.facingX > 0 && this.screenX > Game.WIDTH + 200)
          || (this.facingX < 0 && this.screenX < -200)) {
        this.facingX = -this.facingX;
        this.target = new Vector2(this.target.getX(), player.vpos.getY());

        // If the player is standing, move to the same Y position as them,
        // otherwise choose a random Y position which is not close to the
        // player Y position (to avoid the player getting stunlocked)
        if (player.fallingState == FallingState.STANDING) {
          this.vpos = new Vector2(this.vpos.getX(), this.target.getY());
        } else {
          while (Math.abs(this.vpos.getY() - this.target.getY()) < 40) {
            this.vpos = new Vector2(this.vpos.getX(),
                Random.randomInt(Game.MIN_WALK_Y, Game.HEIGHT - 1));
          }
        }

        // Also slow down if at high speed
        this.scooterTargetSpeed = SCOOTER_SPEED_SLOW;
        this.scooterSpeed = this.scooterTargetSpeed;

        // Go back to the slow engine sound
        if (this.engineSound != null) {
          this.engineSound = this.game.playSoundCopy("scooter_slow0", 100);
          this.engineLooping = true;
        }
      }

      // Check to see if we hit the player
      if (player.fallingState == FallingState.STANDING
          && Math.abs(player.vpos.getY() - this.vpos.getY()) < 30
          && Math.abs(this.vpos.getX() - player.vpos.getX()) < 60
          && player.heightAboveGround < 20) {
        player.hit(this, Attack.get("scooter_hit"));
      }
    } else if (this.justKnockedOffScooter && this.engineSound != null
        && this.engineSound.isPlaying()) {
      this.engineSound.stop();
    }

    super.run();
  }

  @Override
  public boolean overrideWalking() {
    return this.state == State.RIDING_SCOOTER;
  }

  @Override
  public void died() {
    super.died();

    // Low chance of dropping a chain
    if (Random.randomInt(0, 19) == 0) {
      this.game.addWeapon(new Chain(this.game, new Vector2(this.vpos)));
    }

    // Stop the engine sound
    if (this.engineSound != null && this.engineSound.isPlaying()) {
      this.engineSound.stop();
    }
    this.engineLooping = false;
  }
}
