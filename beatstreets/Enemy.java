package beatstreets;

import java.util.ArrayList;
import java.util.List;
import org.openpatch.scratch.Operators;
import org.openpatch.scratch.Random;
import org.openpatch.scratch.Vector2;

/**
 * Base class for all enemies. Implements the AI: approaching the player,
 * flanking, going for weapons and deciding when to attack.
 */
public abstract class Enemy extends Fighter {

  /** The AI states. A class within a class, so the name doesn't clash. */
  public enum State {
    APPROACH_PLAYER, GO_TO_POS, GO_TO_WEAPON, PAUSE, KNOCKED_DOWN, RIDING_SCOOTER,
    PORTAL, PORTAL_EXPLODE
  }

  /** The position we are walking towards. */
  protected Vector2 target;

  protected Weapon targetWeapon = null;

  /**
   * Enemies don't try to target the player until their start timer drops to
   * zero - e.g. so they can scroll onto the screen first.
   */
  protected State state = State.PAUSE;
  protected int stateTimer;

  protected final String[] attacks;
  protected final double approachPlayerDistance;
  protected final int score;

  protected Enemy(Play game, String image, Vector2 pos, double anchorX, double anchorY,
      String name, String[] attacks, int startTimer, Vector2 speed, int health, double stamina,
      double approachPlayerDistance, Vector2 halfHitArea, Integer colourVariant,
      String hitSound, int score) {
    // Slower animation speed than the hero
    super(game, image, pos, anchorX, anchorY, speed, name, health, 14, stamina, halfHitArea, 1,
        colourVariant, false, hitSound);

    // Must make a copy of the value, not a copy of the reference
    this.target = new Vector2(pos);

    this.stateTimer = startTimer;
    this.attacks = attacks;
    this.approachPlayerDistance = approachPlayerDistance;
    this.score = score;
  }

  /** Called when the enemy is added into the game (when its stage is reached). */
  public void spawned() {}

  @Override
  public void run() {
    if (!this.isPlaying()) {
      super.run();
      return;
    }
    if (this.state == State.APPROACH_PLAYER) {
      Player player = this.game.player;

      // If the player is attacking and we are quite close, chance (each
      // frame) of backing up a little
      if (player.attackTimer > 0
          && Math.abs(this.vpos.getY() - player.vpos.getY()) < 20
          && Math.abs(this.vpos.getX() - player.vpos.getX()) < 200
          && Random.randomInt(0, 500) == 0) {
        this.target = new Vector2(this.vpos.getX() - this.facingX * 90, this.target.getY());
        this.state = State.GO_TO_POS;
      } else {
        // Head towards the player. If we are holding a barrel, use a larger
        // X offset so we throw from a distance.
        double xOffset = this.weapon instanceof Barrel
            ? Game.ENEMY_APPROACH_PLAYER_DISTANCE_BARREL
            : this.approachPlayerDistance;
        this.target = new Vector2(
            player.vpos.getX() + xOffset * Util.sign(this.vpos.getX() - player.vpos.getX()),
            player.vpos.getY());
      }
    } else if (this.state == State.GO_TO_POS) {
      // Have we reached the target position? If so, make a new decision
      if (this.target.equals(this.vpos)) {
        this.makeDecision();
      }
    } else if (this.state == State.GO_TO_WEAPON) {
      if (!this.targetWeapon.canBePickedUp() || !this.targetWeapon.onScreen()) {
        // Weapon no longer available, make a new decision
        this.targetWeapon = null;
        this.makeDecision();
      } else {
        this.target = new Vector2(this.targetWeapon.vpos);
        if (this.target.equals(this.vpos)) {
          // Arrived - pick up the weapon and make a new decision
          this.pickupAnimation = this.targetWeapon.name;
          this.frame = 0;
          this.targetWeapon.pickUp(Fighter.WEAPON_HOLD_HEIGHT);
          this.weapon = this.targetWeapon;
          this.targetWeapon = null;
          this.makeDecision();
        }
      }
    } else if (this.state == State.PAUSE) {
      this.stateTimer -= 1;
      if (this.stateTimer < 0) {
        this.makeDecision();
      }
    } else if (this.state == State.KNOCKED_DOWN) {
      // Have we got up again? If so, switch state
      if (this.fallingState == FallingState.STANDING) {
        this.makeDecision();
      }
    }

    // The RIDING_SCOOTER state is updated in the EnemyScooterboy class

    if (this.state == State.APPROACH_PLAYER
        || this.state == State.GO_TO_POS
        || this.state == State.GO_TO_WEAPON) {
      // Ensure that the target position is within the level boundary
      this.target = new Vector2(
          Operators.constrain(this.target.getX(), this.game.boundaryLeft(), this.game.boundaryRight()),
          Operators.constrain(this.target.getY(), this.game.boundaryTop(), this.game.boundaryBottom()));

      // Check to see if another enemy is already heading for the target pos,
      // or one very close to it. If so, make a new decision.
      for (Enemy enemy : this.game.enemies) {
        if (enemy != this && enemy.target.sub(this.target).length() < 20) {
          this.makeDecision();
          break;
        }
      }
    }

    // Apply the shared fighter behavior
    super.run();
  }

  @Override
  protected Attack determineAttack() {
    // Allow attacking if we're in the APPROACH_PLAYER state, aligned with the
    // player on the Y axis, both we and the player are standing up, we're
    // within the right range of distances on the X axis, and finally we must
    // pass a random chance check of 1 in 20. If we're holding a barrel, we can
    // be within any distance on the X axis.
    double px = this.game.player.vpos.getX();
    double py = this.game.player.vpos.getY();

    boolean holdingBarrel = this.weapon instanceof Barrel;

    if (this.state == State.APPROACH_PLAYER
        && this.game.player.fallingState == FallingState.STANDING
        && this.vpos.getY() == py
        && ((this.approachPlayerDistance * 0.9 < Math.abs(this.vpos.getX() - px)
              && Math.abs(this.vpos.getX() - px) <= this.approachPlayerDistance * 1.1)
            || holdingBarrel)
        && Random.randomInt(0, 19) == 0) {
      if (this.weapon != null) {
        return Attack.get(this.weapon.name);
      }
      Attack chosenAttack = Attack.get(this.attacks[Random.randomInt(0, this.attacks.length - 1)]);

      // If the chosen attack is a grab, don't allow it if the player is
      // currently doing a flying kick
      if (chosenAttack.grab
          && this.game.player.lastAttack != null
          && this.game.player.lastAttack.flyingKick) {
        return null;
      }

      return chosenAttack;
    }
    return null;
  }

  @Override
  protected boolean determinePickUpWeapon() {
    return false;
  }

  @Override
  protected boolean determineDropWeapon() {
    return false;
  }

  @Override
  protected List<Fighter> getOpponents() {
    List<Fighter> opponents = new ArrayList<>();
    opponents.add(this.game.player);
    return opponents;
  }

  @Override
  protected Vector2 getMoveTarget() {
    // Move towards the target position
    return this.target;
  }

  @Override
  protected Integer getDesiredFacing() {
    // Always face towards the player, unless we're on a scooter
    if (this.state == State.RIDING_SCOOTER) {
      return this.facingX;
    }
    return this.vpos.getX() < this.game.player.vpos.getX() ? 1 : -1;
  }

  @Override
  public void hit(ScrollHeightActor hitter, Attack attack) {
    if (this.state == State.KNOCKED_DOWN) {
      // Already knocked down
      return;
    }

    super.hit(hitter, attack);

    // If we're riding a scooter, then getting hit will always cause us to
    // fall, regardless of stamina
    if (this.state == State.RIDING_SCOOTER) {
      this.fallingState = FallingState.FALLING;
      this.frame = 0;
      this.hitTimer = 0;
      this.justKnockedOffScooter = true;
    }

    if (this.fallingState == FallingState.FALLING) {
      // Set the state as knocked down
      this.state = State.KNOCKED_DOWN;
    }
  }

  protected void makeDecision() {
    Player player = this.game.player;

    if (this.game.enemies.size() == 1) {
      // If we're the only enemy, always move in to attack
      this.state = State.APPROACH_PLAYER;
    } else {
      // 7/10 chance of going directly to a point where we can attack the
      // player, unless there's another enemy already heading there in which
      // case flank. 3/10 chance of going to a random point slightly further
      // from the player. 1/10 chance of pausing for a short time.
      int r = Random.randomInt(0, 9);
      if (r < 7) {
        // Check to see if another enemy on the same X side of the player is
        // already heading to attack them. If so, flank instead.
        boolean otherEnemiesOnSameSideAttacking = false;
        for (Enemy enemy : this.game.enemies) {
          if (enemy != this
              && enemy.state == State.APPROACH_PLAYER
              && Util.sign(enemy.vpos.getX() - player.vpos.getX())
                  == Util.sign(this.vpos.getX() - player.vpos.getX())) {
            otherEnemiesOnSameSideAttacking = true;
            break;
          }
        }
        if (otherEnemiesOnSameSideAttacking) {
          // Go to the opposite side of the player, at a Y position offset from
          // them but on the same Y side that we're on now. If the Y position
          // is the same, choose a Y side randomly.
          this.state = State.GO_TO_POS;
          this.target = new Vector2(player.vpos.getX()
              - Util.sign(this.vpos.getX() - player.vpos.getX()) * 50,
              player.vpos.getY()
                  + Util.sign(this.vpos.getY() - player.vpos.getY()) * 50);
          if (this.target.getY() == player.vpos.getY()) {
            this.target = new Vector2(this.target.getX(), player.vpos.getY()
                + (Random.randomInt(0, 1) == 0 ? 1 : -1) * 50);
          }
        } else {
          // Go to the player
          this.state = State.APPROACH_PLAYER;
        }
      } else if (r < 9) {
        // Go to a random point at a moderate distance from the player,
        // sticking to the same half of the screen on the X axis
        int xSide = Util.sign(this.vpos.getX() - player.vpos.getX());
        if (xSide == 0) {
          xSide = Random.randomInt(0, 1) == 0 ? 1 : -1;
        }
        int x1 = (int) (player.vpos.getX() + 150 * xSide);
        int x2 = (int) (player.vpos.getX() + 400 * xSide);
        int x = Random.randomInt(Math.min(x1, x2), Math.max(x1, x2));
        int y = Random.randomInt((int) this.game.boundaryTop(), (int) this.game.boundaryBottom());
        this.target = new Vector2(x, y);
        this.state = State.GO_TO_POS;
      } else {
        // Pause
        this.stateTimer = Random.randomInt(50, 100);
        this.state = State.PAUSE;
      }
    }
  }
}
