package beatstreets;

import java.util.ArrayList;
import java.util.List;
import org.openpatch.scratch.Random;
import org.openpatch.scratch.Vector2;

/**
 * Base class for the player and all enemies. Implements health and stamina,
 * attacks and the walking/idle/attack/hit/death animations. All positions are
 * kept in the original's pixel coordinates, see {@link ScrollHeightActor}.
 */
public abstract class Fighter extends ScrollHeightActor {

  public static final int WEAPON_HOLD_HEIGHT = 100;

  public enum FallingState {
    STANDING, FALLING, GETTING_UP, GRABBED, THROWN
  }

  /** Speed as a Vector2 containing x and y speed. */
  protected final Vector2 speed;

  /** e.g. "hero" or "boss". */
  protected final String sprite;

  protected final int animUpdateRate;

  protected int facingX = 1;

  /**
   * Updated each game frame, then translated into an animation frame number
   * depending on the animation being played.
   */
  protected int frame = 0;

  /** The current attack if attackTimer is above zero. */
  protected Attack lastAttack = null;

  /** Above zero = currently attacking, zero or below = time since last attack. */
  protected int attackTimer = 0;

  /** Are we knocked down or in the process of being knocked down? */
  protected FallingState fallingState = FallingState.STANDING;

  /** Are we currently walking? Used to pick the standing or walking animation. */
  protected boolean walking = false;

  /** Velocity used when falling, being pushed backwards or doing a flying kick. */
  protected Vector2 vel = new Vector2(0, 0);

  protected String pickupAnimation = null;

  /** If above zero, we've just been hit and are doing the recoil animation. */
  protected int hitTimer = 0;
  protected int hitFrame = 0;

  protected double stamina;
  protected final double maxStamina;

  /**
   * Half the size of the area an opponent's attack must touch to hit us. A
   * larger number for the portal, which is physically bigger.
   */
  protected final Vector2 halfHitArea;

  protected int health;
  protected final int startHealth;
  protected int lives;

  /** For enemies with multiple colour variants - appended to the sprite name. */
  protected final Integer colourVariant;

  /** Sound played when we are hit (only used by the portal). */
  protected final String hitSound;

  protected Weapon weapon = null;

  /** Used for the animation where a Scooterboy is knocked off his scooter. */
  protected boolean justKnockedOffScooter = false;

  protected boolean useDieAnimation = false;

  protected Fighter(Play game, String image, Vector2 pos, double anchorX, double anchorY,
      Vector2 speed, String sprite, int health, int animUpdateRate, double stamina,
      Vector2 halfHitArea, int lives, Integer colourVariant, boolean separateShadow,
      String hitSound) {
    super(game, image, pos, anchorX, anchorY, separateShadow);
    this.speed = speed;
    this.sprite = sprite;
    this.animUpdateRate = animUpdateRate;
    this.stamina = stamina;
    this.maxStamina = stamina;
    this.halfHitArea = halfHitArea;
    this.health = health;
    this.startHealth = health;
    this.lives = lives;
    this.colourVariant = colourVariant;
    this.hitSound = hitSound;
  }

  @Override
  public void run() {
    if (!this.isPlaying()) {
      super.run();
      return;
    }
    this.attackTimer -= 1;

    // Apply gravity and velocity if in the air
    if (this.heightAboveGround > 0 || this.vel.getY() != 0) {
      this.vpos = new Vector2(this.vpos.getX() + this.vel.getX(), this.vpos.getY());
      this.vel = new Vector2(this.vel.getX(),
          this.vel.getY() + (this.fallingState == FallingState.THROWN
              ? Game.THROWN_GRAVITY : Game.JUMP_GRAVITY));
      this.heightAboveGround -= this.vel.getY();
      this.applyMovementBoundaries(this.vel.getX(), 0);
      if (this.heightAboveGround < 0) {
        this.heightAboveGround = 0;
        this.vel = new Vector2(0, 0);

        // Don't do the been hit animation after landing (from a flying kick or
        // from being thrown)
        this.hitTimer = 0;
      }
    }

    // Update logic and animation based on the current situation - falling,
    // getting up, hit, pickup animation, or normal walking/standing/attacking

    if (this.fallingState == FallingState.FALLING) {
      // Get pushed backwards
      this.vpos = new Vector2(this.vpos.getX() + this.vel.getX(), this.vpos.getY());
      this.vel = new Vector2(Util.moveTowards(this.vel.getX(), 0, 0.5)[0], this.vel.getY());
      this.applyMovementBoundaries(this.vel.getX(), 0);

      this.frame += 1;

      if (this.frame > 120) {
        // If we're not yet out of health, get up and reset stamina
        if (this.health > 0) {
          this.fallingState = FallingState.GETTING_UP;
          this.frame = 0;
          this.stamina = this.maxStamina;
        } else if (this.frame > 240) {
          // If we're out of health, flash on and off for a short while, then
          // lose a life
          this.lives -= 1;

          // If we still have lives left, get up
          if (this.lives > 0) {
            this.health = this.startHealth;
            this.fallingState = FallingState.GETTING_UP;
            this.frame = 0;
            this.stamina = this.maxStamina;
            this.useDieAnimation = false;
          } else {
            this.died();
          }
        }
      }
    } else if (this.fallingState == FallingState.GETTING_UP) {
      this.frame += 1;
      this.vpos = new Vector2(this.vpos.getX() + 0.1 * this.facingX, this.vpos.getY()); // Move forward slightly as we get up
      if (this.frame > 20) {
        this.fallingState = FallingState.STANDING;
        this.frame = 0;
      }
    } else if (this.fallingState == FallingState.THROWN) {
      this.frame += 1;
      if (this.heightAboveGround <= 0) {
        this.fallingState = FallingState.FALLING;
        this.frame = 80;
      }
    } else if (this.hitTimer > 0) {
      // Playing the 'hit' animation, briefly stunned
      this.hitTimer -= 1;
    } else if (this.pickupAnimation != null) {
      // Doing the animation for picking up a weapon
      this.frame += 1;
      if (this.frame > 30) {
        this.pickupAnimation = null;
      }
    } else if (this.overrideWalking()) {
      // A special state managed by a subclass means we shouldn't do the usual
      // walking/attacking behaviour - e.g. Scooterboy riding his scooter
    } else if (this.fallingState == FallingState.STANDING) {
      // Standing, walking or attacking

      // Recover stamina over time
      if (this.stamina < this.maxStamina) {
        this.stamina += 1;
      }

      // Update the position of the held weapon. The weapon actor is invisible
      // while being held, but its position is kept up to date so that it
      // reappears in the right place when dropped.
      if (this.weapon != null) {
        this.weapon.vpos = new Vector2(this.vpos.getX() + this.facingX * 20, this.vpos.getY());
      }

      // Are we ready to attack or pick up/drop a weapon? If we're out of
      // stamina, the recovery time is longer.
      int lastAttackRecoveryTime =
          this.lastAttack != null ? this.lastAttack.recoveryTime : 0;
      if (this.stamina <= 0) {
        lastAttackRecoveryTime *= 3;
      }
      if (this.attackTimer <= -lastAttackRecoveryTime) {
        // Not currently attacking - do we want to pick up or drop a weapon?
        if (this.weapon == null) {
          List<Weapon> nearbyWeapons = new ArrayList<>();
          for (Weapon weapon : this.game.weapons) {
            if (weapon.vpos.sub(this.vpos).length() < 50) {
              nearbyWeapons.add(weapon);
            }
          }
          if (!nearbyWeapons.isEmpty() && this.determinePickUpWeapon()) {
            // Sort the nearby weapons by distance
            nearbyWeapons.sort((a, b) -> Double.compare(
                a.vpos.sub(this.vpos).lengthSq(), b.vpos.sub(this.vpos).lengthSq()));
            for (Weapon weapon : nearbyWeapons) {
              if (weapon.canBePickedUp()) {
                this.pickupAnimation = weapon.name;
                this.frame = 0;
                this.weapon = weapon;
                weapon.pickUp(Fighter.WEAPON_HOLD_HEIGHT);
                break;
              }
            }
          }
        } else if (this.determineDropWeapon()) {
          this.dropWeapon();
        }

        // Attack? Only allowed if we didn't just start picking up a weapon!
        if (this.pickupAnimation == null) {
          Attack attack = this.determineAttack();
          if (attack != null) {
            this.lastAttack = attack;
            this.attackTimer = attack.animTime;
            this.stamina -= attack.staminaCost;
            this.stamina = Math.max(this.stamina, Game.MIN_STAMINA);
            this.frame = 0;

            if (attack.initialSound != null) {
              this.game.playSound(attack.initialSound[0],
                  Integer.parseInt(attack.initialSound[1]));
            }

            // Is this a flying kick?
            if (attack.flyingKick) {
              this.vel = new Vector2(Game.FLYING_KICK_VEL_X * this.facingX,
                  Game.FLYING_KICK_VEL_Y);
            }

            // Grab the player?
            if (attack.grab) {
              this.game.player.grabbed();
            }
          }
        }
      }

      // Update movement and animation. Must check attackTimer again as an
      // attack may only just have started.
      if (this.attackTimer <= 0) {
        // Not attacking
        Integer desiredFacing = this.getDesiredFacing();
        if (desiredFacing != null) {
          this.facingX = desiredFacing;
        }

        Vector2 target = this.getMoveTarget();
        if (!target.equals(this.vpos)) {
          this.walking = true;

          double[] rx = Util.moveTowards(this.vpos.getX(), target.getX(), this.speed.getX());
          double[] ry = Util.moveTowards(this.vpos.getY(), target.getY(), this.speed.getY());
          this.vpos = new Vector2(rx[0], ry[0]);

          this.applyMovementBoundaries(rx[1], ry[1]);

          this.frame += 1;
        } else {
          // No movement, reset the frame to standing
          this.walking = false;
          this.frame = 7;
        }
      } else {
        // Currently attacking
        this.frame += 1;

        int attackFrame = this.getAttackFrame();

        // If the current frame of the attack is a hit frame, inflict damage
        for (int hitFrame : this.lastAttack.hitFrames) {
          if (hitFrame == attackFrame) {
            // Is this a throw attack?
            if (this.lastAttack.throwAttack) {
              // A grab attack means we're the boss throwing the player
              if (this.lastAttack.grab) {
                // Throw the player, if we haven't already done that on a
                // previous frame
                if (this.game.player.fallingState == FallingState.GRABBED) {
                  this.game.player.hit(this, this.lastAttack);
                  this.game.player.thrown(this.facingX);
                }
              } else if (this.weapon != null) {
                // A normal throw of a barrel - make sure we still have the
                // weapon, it might have been released on a previous frame!
                this.weapon.throwWeapon(this.facingX, this);
                this.weapon = null;
              }
            }

            // Attack regardless of whether this is a throw attack, so that a
            // thrown barrel hits opponents it is released past
            this.attack(this.lastAttack);
            break;
          }
        }
      }
    }

    super.run();
  }

  /**
   * Sees if there is an opponent directly in front of us who we can hit (or
   * behind us if it's a rear attack such as the elbow).
   */
  public void attack(Attack attack) {
    if (attack.strength > 0) {
      for (Fighter opponent : this.getOpponents()) {
        Vector2 vec = opponent.vpos.sub(this.vpos);
        boolean facingCorrect = Util.sign(this.facingX) == Util.sign(vec.getX());
        if (attack.rearAttack) {
          facingCorrect = !facingCorrect;
        }

        // Should the attack hit this opponent?
        if (Math.abs(vec.getY()) < opponent.halfHitArea.getY()
            && facingCorrect
            && Math.abs(vec.getX()) < attack.reach + opponent.halfHitArea.getX()) {
          opponent.hit(this, attack);

          // If we're using a weapon, it may have broken as a result
          if (this.weapon != null && this.weapon.isBroken()) {
            this.dropWeapon();
          }
        }
      }
    }
  }

  /**
   * We have been hit by the given attack. The hitter can be another fighter or
   * a weapon such as a barrel.
   */
  public void hit(ScrollHeightActor hitter, Attack attack) {
    // Can't be hit if we're falling or getting up
    if (this.fallingState == FallingState.STANDING
        || this.fallingState == FallingState.GRABBED) {
      // Can't be hit if we're already in the hit animation
      if (this.hitTimer <= 0) {
        this.stamina -= attack.strength * Game.BASE_STAMINA_DAMAGE_MULTIPLIER
            * attack.staminaDamageMultiplier;
        this.stamina = Math.max(this.stamina, Game.MIN_STAMINA);
        this.health -= attack.strength;

        // The hit timer ensures we can't receive damage again until it has
        // counted down, and stuns the fighter. Stronger attacks stun for longer.
        this.hitTimer = (int) (attack.strength * 8 * attack.stunTimeMultiplier);
        this.hitFrame = Random.randomInt(0, 1);

        // Stop our attack if we're in the middle of one - unless it's a flying
        // kick, in which case continue
        if (this.attackTimer > 0 && this.lastAttack != null && !this.lastAttack.flyingKick) {
          this.attackTimer = 0;
        }

        // Drop our weapon
        if (this.weapon != null) {
          this.dropWeapon();
        }

        if (attack.hitSound != null) {
          this.game.playSound(attack.hitSound[0], Integer.parseInt(attack.hitSound[1]));
        }

        if (this.hitSound != null) {
          // Sound for me being hit (only used by the portal)
          this.game.playSound(this.hitSound, 1);
        }

        // Check for being knocked down due to being out of health or stamina.
        // Portals can't fall.
        if ((this.stamina <= 0 || this.health <= 0) && !(this instanceof EnemyPortal)) {
          this.fallingState = FallingState.FALLING;
          this.frame = 0;
          this.hitTimer = 0;

          // If we're knocked down due to being out of stamina, and we're close
          // to death, just die already
          if (this.health < 3) {
            this.health = 0;
            this.useDieAnimation = Random.randomInt(0, 1) == 0; // 50% of the time
          }
        }

        // If the attacker was using a weapon, tell the weapon that it was used
        if (hitter instanceof Fighter fighterHitter && fighterHitter.weapon != null) {
          fighterHitter.weapon.used();
        }
      }

      // Always face towards the hitter, unless we're at the same X position
      if (hitter.vpos.getX() != this.vpos.getX()) {
        this.facingX = Util.sign(hitter.vpos.getX() - this.vpos.getX());

        if (this.fallingState == FallingState.FALLING && !this.useDieAnimation) {
          // Get knocked backwards
          this.vel = new Vector2(this.vel.getX() + -this.facingX * 10, this.vel.getY());
        }
      }
    }
  }

  /** Called when out of lives. Overridden where subclasses need to know. */
  public void died() {}

  @Override
  public String determineImage() {
    boolean show = true;
    String animType = null;
    int frame;

    if (this.fallingState == FallingState.FALLING) {
      if (this.frame > 60 && this.health <= 0 && (this.frame / 10) % 2 == 0) {
        // If we're out of health, flash on and off for a short while
        show = false;
      }

      if (show) {
        // If we've only just fallen off a scooter, play the knocked_off frame
        // before continuing with the knockdown animation
        if (this.justKnockedOffScooter) {
          if (this.frame > 10) {
            this.justKnockedOffScooter = false;

            // Create the scooter as an independent object
            this.game.addScooter(new Scooter(this.game, new Vector2(this.vpos), this.facingX,
                this.colourVariant));
          }
        }

        // Now choose the frame of the animation to use
        if (this.justKnockedOffScooter) {
          animType = "knocked_off";
          frame = 0;
        } else if (this.useDieAnimation) {
          animType = "die";
          frame = Math.min(this.frame / 20, 2);
        } else {
          int lastFrame = this instanceof EnemyScooterboy ? 3 : 2;
          animType = "knockdown";
          frame = Math.min(this.frame / 10, lastFrame);
        }
      } else {
        frame = 0;
      }
    } else if (this.fallingState == FallingState.GETTING_UP) {
      animType = "getup";
      frame = Math.min(this.frame / 10, 1);
    } else if (this.fallingState == FallingState.GRABBED) {
      show = false;
      frame = 0;
    } else if (this.fallingState == FallingState.THROWN) {
      animType = "thrown";
      frame = Math.min(this.frame / 12, 3);
    } else if (this.hitTimer > 0) {
      frame = this.hitFrame;
      animType = "hit";
    } else if (this.pickupAnimation != null) {
      // Doing the animation for picking up a weapon
      frame = Math.min(this.frame / 12, this.weapon.endPickupFrame);
      animType = "pickup_" + this.pickupAnimation;
    } else if (this.attackTimer > 0) {
      // Currently attacking
      animType = this.lastAttack.sprite;
      frame = this.getAttackFrame();
    } else {
      // Walking or standing
      if (this.walking) {
        // There are four walk animation frames, we take self.frame (an
        // unbounded number incrementing by 1 each game frame) and divide it
        // by the animation update rate, then MOD 4 to reduce it to the actual
        // animation frame in the range 0-3
        animType = "walk";
        frame = (this.frame / this.animUpdateRate) % 4;
      } else {
        // Standing. We only have 'walk' sprites for weapons
        frame = 0;
        animType = this.weapon != null ? "walk" : "stand";
      }

      // Add the weapon name to the walking/standing animation
      if (this.weapon != null) {
        animType += "_" + this.weapon.name;
      }
    }

    if (show) {
      // In the sprite filenames, 0 = facing left, 1 = facing right
      int facingId = this.facingX == 1 ? 1 : 0;
      String image = this.sprite + "_" + animType + "_" + facingId + "_" + frame;
      if (this.colourVariant != null) {
        image += "_" + this.colourVariant;
      }
      return image;
    }
    return "blank";
  }

  /**
   * Returns the current frame of the attack animation, based on the game frame
   * counter and the attack's frame time.
   */
  public int getAttackFrame() {
    return Math.min(this.frame / this.lastAttack.frameTime, this.lastAttack.frames - 1);
  }

  /** Used by subclasses to prevent the usual walking/attacking behaviour. */
  public boolean overrideWalking() {
    return false;
  }

  public void dropWeapon() {
    this.pickupAnimation = null; // Stop the pickup animation if we're in one
    this.weapon.dropped();
    this.weapon = null;
  }

  public void grabbed() {
    this.fallingState = FallingState.GRABBED;
    if (this.weapon != null) {
      this.dropWeapon();
    }
  }

  public void thrown(int dirX) {
    this.fallingState = FallingState.THROWN;
    this.vel = new Vector2(dirX * Game.PLAYER_THROW_VEL_X, Game.PLAYER_THROW_VEL_Y);
    this.facingX = -dirX;

    // Shift position for the throw animation
    this.vpos = new Vector2(this.vpos.getX() + dirX * 50, this.vpos.getY());
    this.heightAboveGround = 45;
  }

  /**
   * A fighter outside the boundary can walk in a direction which helps them get
   * inside the boundary, but not further out of it.
   */
  public void applyMovementBoundaries(double dx, double dy) {
    if (dx < 0 && this.vpos.getX() < this.game.boundaryLeft()) {
      this.vpos = new Vector2(this.game.boundaryLeft(), this.vpos.getY());
    } else if (dx > 0 && this.vpos.getX() > this.game.boundaryRight()) {
      this.vpos = new Vector2(this.game.boundaryRight(), this.vpos.getY());
    }
    if (dy < 0 && this.vpos.getY() < this.game.boundaryTop()) {
      this.vpos = new Vector2(this.vpos.getX(), this.game.boundaryTop());
    } else if (dy > 0 && this.vpos.getY() > this.game.boundaryBottom()) {
      this.vpos = new Vector2(this.vpos.getX(), this.game.boundaryBottom());
    }
  }

  /** The attack we want to start this frame, or null. */
  protected abstract Attack determineAttack();

  /** Do we want to pick up a weapon? */
  protected abstract boolean determinePickUpWeapon();

  /** Do we want to drop our weapon? */
  protected abstract boolean determineDropWeapon();

  /** The fighters our attacks can hit. */
  protected abstract List<Fighter> getOpponents();

  /** The position we want to walk to this frame. */
  protected abstract Vector2 getMoveTarget();

  /** The direction we want to face, or null to keep the current direction. */
  protected abstract Integer getDesiredFacing();
}
