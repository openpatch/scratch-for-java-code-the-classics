package eggzy;

import java.util.ArrayList;
import java.util.List;
import org.openpatch.scratch.Vector2;

/**
 * The player. Runs, jumps, wall jumps and dashes around the level, collects
 * gems and stomps enemies, and records its position every frame as replay
 * data for the ghost players of the next game.
 */
public class Player extends GravityActor {

  public static final int DASH_TIME = 18;
  public static final int DASH_SPEED = 10;
  public static final int DASH_PAUSE_TIME = 5;
  public static final int DASH_TRAIL_INTERVAL = 3;
  public static final int DASH_TIMER_TRAIL_CUTOFF = -10;
  public static final int MAX_X_RUN_SPEED = 5;

  private final Controls controls;

  /** Actor for the flame on the character's head. */
  public final EzActor flame = new EzActor(0, 0, Play.ANCHOR_FLAME);

  private int velX = 0;
  private int facingX = 1;
  private boolean hurt = false;
  private int dashTimer = DASH_TIMER_TRAIL_CUTOFF;
  private int dashAnimationTimer = 0;
  private boolean dashAllowed = false;
  private int grabbedWall = 0;
  private int coyoteTime = 0;
  private int fallTimer = 0;
  private int wallJumpCoyoteTime = 0;
  private int cachedJumpInputTimer = 0;
  private int enemyStompedTimer = 0;
  private int changeDirectionTimer = 0;
  private String lastDashSprite = "dash_horizontal_0_0";
  private boolean stompedLastFrame = false;
  private int previousGrabbedWall = 0;

  private double startX = 0;
  private double startY = 0;

  /** One entry per frame: position, level and sprite, for the replay file. */
  public final List<Replays.Entry> replayData = new ArrayList<>();

  public Player(Play game, Controls controls) {
    // Initial pos is 0,0 but Game.nextLevel sets the actual starting position
    super(game, 0, 0, true, Play.ANCHOR_PLAYER);
    this.controls = controls;
    this.flame.setImage("flame_stand_0");
  }

  public void newLevel(Vector2 startPos) {
    this.startX = startPos.getX();
    this.startY = startPos.getY();
    this.reset();
  }

  public void reset() {
    this.x = this.startX;
    this.y = this.startY;
    this.velX = 0;
    this.velY = 0;
    this.facingX = 1;
    this.hurt = false;
    this.dashTimer = DASH_TIMER_TRAIL_CUTOFF;
    this.gravityEnabled = true;
    this.grabbedWall = 0;
    this.coyoteTime = 0;
    this.wallJumpCoyoteTime = 0;
    this.cachedJumpInputTimer = 0;
    this.enemyStompedTimer = 0;

    // Ensure that no enemies are at or near the spawn point - treat it as if
    // we'd stomped on their heads
    for (Enemy enemy : new ArrayList<>(this.game.enemies)) {
      if (this.distanceTo(enemy) < 150) {
        enemy.destroy();
        this.game.playSound("enemy_death", 5);
      }
    }
  }

  /** Distance between the anchor positions, like pgzero's distance_to. */
  private double distanceTo(EzActor other) {
    return Math.hypot(other.x - this.x, other.y - this.y);
  }

  /** Check for collision between the player and an enemy. */
  private boolean hitTest(Enemy other) {
    return !this.hurt && this.getRect(this.x, this.y).colliderect(other.getRect());
  }

  private List<Enemy> getCollidingEnemies() {
    List<Enemy> result = new ArrayList<>();
    for (Enemy enemy : this.game.enemies) {
      if (!enemy.dying && this.hitTest(enemy)) {
        result.add(enemy);
      }
    }
    return result;
  }

  /** Is the given point within the player's sprite? Used by the gems. */
  public boolean collidePoint(double px, double py) {
    return this.collidepoint(px, py);
  }

  @Override
  public void run() {
    if (!this.isPlaying()) {
      return;
    }
    boolean wasLanded = this.landed();
    // While hurt, fall out of the level without collision detection
    this.applyGravity(!this.hurt);

    if (wasLanded && !this.landed()) {
      // We must have walked off a platform. Set coyote time timer
      this.coyoteTime = Play.COYOTE_TIME;
      this.fallTimer = 0;
    }

    if (this.imageTop() >= Play.HEIGHT) {
      this.reset();
    }

    // Check for collisions with enemies, including landing on their heads
    boolean stompedAny = false;
    for (Enemy enemy : this.getCollidingEnemies()) {
      // Die or stomp? Are we within the top 20% of the enemy collision
      // rectangle, or the top 50% if we're moving downward?
      Rect enemyRect = enemy.getRect();
      double threshold =
          enemyRect.top()
              + (enemyRect.bottom() - enemyRect.top()) * (this.velY > 0 ? 0.5 : 0.2);
      // Unless we're within the top 20% of the sprite, a stomp due to
      // downward motion would hit us on the next frame, so forgive any
      // stomp from the previous frame as well
      if (this.y < threshold || this.stompedLastFrame) {
        enemy.stomped();
        stompedAny = true;
        this.velY = -6;
        this.enemyStompedTimer = 3;
        this.dashAllowed = true;
      } else {
        // Die and respawn
        this.hurt = true;
        this.velY = -12;
        this.fallState = FallState.FALLING;
        this.fallTimer = 0;
        this.dashTimer = DASH_TIMER_TRAIL_CUTOFF;
        this.game.playSound("player_death0");
        this.game.addAnimation(new Animation(this.x, this.y, "loselife_{0}", 8, 4,
            Play.ANCHOR_CENTRE, 0, -1));
        break;
      }
    }

    this.stompedLastFrame = stompedAny;

    if (this.landed()) {
      this.dashAllowed = true;
    }

    this.dashTimer -= 1;
    this.dashAnimationTimer += 1;
    this.cachedJumpInputTimer -= 1;
    this.coyoteTime -= 1;
    this.wallJumpCoyoteTime -= 1;

    if (this.dashTimer > DASH_TIMER_TRAIL_CUTOFF
        && this.dashTimer % DASH_TRAIL_INTERVAL == 0) {
      this.game.addAnimation(new DashTrail(this.x, this.y, this.lastDashSprite));
    }

    int dx = 0;

    boolean jumpPressed = this.controls.buttonPressed(0);

    if (this.hurt) {
      // We're dropping out of the level; the reset above respawns us once we
      // are fully below the bottom edge
      this.gravityEnabled = true;
      if (this.imageTop() >= Play.HEIGHT) {
        this.hurt = false;
      }
    } else if (this.dashTimer > 0) {
      // Update dash. For the first few frames (dashTimer above DASH_TIME),
      // the player doesn't move
      if (this.dashTimer < DASH_TIME) {
        if (this.dashTimer % DASH_TRAIL_INTERVAL == 0) {
          this.game.addAnimation(new DashTrail(this.x, this.y, this.lastDashSprite));
        }

        // The vertical and horizontal components are applied separately, to
        // improve how collision detection works
        this.move(0, sign(this.velY), Math.abs(this.velY));

        if (this.move(sign(this.velX), 0, Math.abs(this.velX)) && this.velY >= 0) {
          // If we hit a wall, and are not travelling up, end the dash
          this.dashTimer = 0;
          this.grabbedWall = this.facingX;
        }
      }
    } else {
      // We're either on a wall, jumping/falling or walking
      dx = this.controls.getX();

      if (this.grabbedWall != 0) {
        // Wall slide
        this.gravityEnabled = false;

        // Check for wall jump
        if (jumpPressed || this.cachedJumpInputTimer > 0) {
          this.wallJump(this.grabbedWall);
        } else if (dx == -this.grabbedWall) {
          // Player is pushing away from the wall
          this.previousGrabbedWall = this.grabbedWall;
          this.wallJumpCoyoteTime = Play.WALL_JUMP_COYOTE_TIME;
          this.grabbedWall = 0;
        } else {
          // Slowly slide down wall, stop grabbing if we hit the floor or the
          // wall is no longer there
          Rect rect = this.getRect(this.x + this.grabbedWall, this.y);

          if (this.move(0, 1, 1) || !this.game.positionBlocked(rect)) {
            this.grabbedWall = 0;
          }
        }
      } else {
        // Not grabbing a wall - check for coyote time wall jump
        if (jumpPressed && this.wallJumpCoyoteTime > 0) {
          this.wallJump(this.previousGrabbedWall);
        } else {
          // Normal movement
          this.gravityEnabled = true;
          if (dx == 0) {
            // No horizontal input - come to a halt over several frames
            this.velX = (int) moveTowards(this.velX, 0, 1);
          } else {
            // Horizontal input - apply to x velocity
            this.facingX = dx;
            this.velX = (int) moveTowards(this.velX, MAX_X_RUN_SPEED * dx, 1);
          }

          // Start grabbing the wall if we hit a wall while moving down.
          // The move may change our position, so it must come first.
          if (this.velX != 0
              && this.move(sign(this.velX), 0, Math.abs(this.velX))
              && this.velY > 0) {
            this.grabbedWall = sign(this.velX);
            this.velX = 0;
          }

          if ((jumpPressed || this.cachedJumpInputTimer > 0)
              && (this.landed() || this.coyoteTime > 0)) {
            // Jump
            this.jump();
          } else if (jumpPressed && !this.landed()) {
            // Cache jump input for a few frames
            this.cachedJumpInputTimer = Play.CACHE_JUMP_INPUT_TIME;
          } else if (!this.landed()
              && this.velY < 0
              && !this.controls.buttonDown(0)
              && this.dashTimer < -10
              && this.enemyStompedTimer <= 0) {
            // In the air and moving up: the upward velocity drops off faster
            // if the player has let go of the jump button
            this.velY = Math.min(this.velY + 1, 0);
          }

          if (this.dashAllowed && this.controls.buttonPressed(1)) {
            // Dash
            int dy = this.controls.getY();
            if (dx != 0 || dy != 0) {
              double len = Math.hypot(dx, dy);
              this.velX = (int) (dx / len * DASH_SPEED);
              this.velY = (int) (dy / len * DASH_SPEED);
              this.gravityEnabled = false;
              this.dashAllowed = false;
              this.dashTimer = DASH_TIME + DASH_PAUSE_TIME;
              this.dashAnimationTimer = 0;
              this.fallState = FallState.FALLING;
              this.wallJumpCoyoteTime = 0;
              this.game.playSound("jump_long", 5);
            }
          }
        }
      }
    }

    // If we change direction, play the change direction animation for a
    // few frames
    if (sign(dx) != sign(this.velX) && this.dashTimer <= 0) {
      this.changeDirectionTimer = 5;
    } else {
      this.changeDirectionTimer -= 1;
    }

    // Update sprite
    this.determineSprite(dx);

    // Update fall timer after choosing the sprite so we don't skip frame 0
    if (!this.landed() && this.dashTimer <= 0) {
      this.fallTimer += 1;
    }

    // Update replay data
    this.replayData.add(new Replays.Entry(this.x, this.y, this.game.levelIndex, this.getImage()));

    super.run();
  }

  private void jump() {
    this.velY = Play.JUMP_VEL_Y;
    this.fallState = FallState.JUMPING;
    this.coyoteTime = 0;
    this.cachedJumpInputTimer = 0;
    this.lowerGravityTimer = 5;
    this.fallTimer = 0;
    this.game.playSound("jump0");
  }

  private void wallJump(int wallDirection) {
    this.velY = Play.JUMP_VEL_Y;
    this.fallState = FallState.WALL_JUMPING;
    this.velX = -wallDirection * Play.WALL_JUMP_X_VEL;
    this.facingX = -wallDirection;
    this.grabbedWall = 0;
    this.previousGrabbedWall = 0;
    this.wallJumpCoyoteTime = 0;
    this.cachedJumpInputTimer = 0;
    this.fallTimer = 0;
    this.game.playSound("jump0");
  }

  private static double moveTowards(double n, double target, double speed) {
    if (n < target) {
      return Math.min(n + speed, target);
    }
    return Math.max(n - speed, target);
  }

  /**
   * Sets the sprite image. If we're currently hurt, the sprite flashes on and
   * off on alternate frames. dx is the X direction we tried to move this frame,
   * zero if there was no control input.
   */
  private void determineSprite(int dx) {
    this.setImage("blank");
    this.flame.setImage("blank");
    // The flame has a different anchor point while dashing
    this.flame.anchorY = Play.ANCHOR_FLAME;

    if (!this.hurt || this.game.timer % 2 == 1) {
      // First number of a sprite name is the direction (0 right, 1 left)
      String dirIndex = this.facingX < 0 ? "1" : "0";

      if (this.hurt) {
        int frame = Math.min(this.fallTimer / 8, 5);
        this.setImage("die_" + frame);
      } else if (this.grabbedWall != 0 && this.velY >= 0) {
        // No wall slide animation while moving upward
        this.setImage("climb_" + dirIndex + "_1");
        this.flame.setImage("flame_climb_" + dirIndex + "_1");
      } else if (!this.landed()) {
        // In air
        if (this.fallState == FallState.JUMPING) {
          int frame = Math.min(this.fallTimer / 3, 5);
          int flameFrame = Math.min(this.fallTimer / 3, 5) + 1;
          this.setImage("jump_" + dirIndex + "_" + frame);
          this.flame.setImage("flame_jump_" + dirIndex + "_" + flameFrame);
        } else if (this.fallState == FallState.WALL_JUMPING) {
          int frame = Math.min(this.fallTimer / 8, 2);
          int flameFrame = Math.min(this.fallTimer / 4, 6);
          this.setImage("wall_jump_" + dirIndex + "_" + frame);
          this.flame.setImage("flame_wall_jump_" + dirIndex + "_" + flameFrame);
        } else if (this.dashTimer > 0) {
          // All dash directions start with dash_start, before switching to
          // frames for the specific dash direction
          if (this.dashAnimationTimer < 4) {
            int flameFrame = this.dashAnimationTimer / 2;
            this.lastDashSprite = "dash_start_" + dirIndex;
            this.setImage(this.lastDashSprite);
            this.flame.setImage("flame_dash_start_" + dirIndex + "_" + flameFrame);
            this.flame.anchorY = Play.ANCHOR_FLAME;
          } else {
            int timer = this.dashAnimationTimer - 4;
            int frame = Math.min(timer / 3, 2);
            int flameFrame = Math.min(timer / 3, 7);
            String sprite = "dash_";
            if (this.velY < 0) {
              sprite += "up_";
            } else if (this.velY > 0) {
              sprite += "down_";
            }
            if (this.velX != 0) {
              sprite += "horizontal_";
            }
            this.lastDashSprite = sprite + dirIndex + "_" + frame;
            this.setImage(this.lastDashSprite);
            this.flame.setImage("flame_" + sprite + dirIndex + "_" + flameFrame);
            this.flame.anchorY = Play.ANCHOR_FLAME_DASH;
          }
        } else {
          // For the flame, use frames 4 and 5 of the wall jump
          int frame = Math.min(this.fallTimer / 8, 1);
          int flameFrame = Math.min(this.fallTimer / 8, 1) + 4;
          this.setImage("fall_" + dirIndex + "_" + frame);
          this.flame.setImage("flame_wall_jump_" + dirIndex + "_" + flameFrame);
        }
      } else if (dx == 0) {
        this.setImage("stand_front");
        this.flame.setImage("flame_stand_" + (this.game.timer / 4) % 8);
      } else if (this.changeDirectionTimer > 0) {
        this.setImage("change_dir_" + dirIndex + "_0");
        this.flame.setImage("flame_change_dir_" + dirIndex + "_" + (this.game.timer / 4) % 3);
      } else {
        // 8 frames of the run animation, switching every 4 game frames
        int frame = (this.game.timer / 4) % 8;
        this.setImage("run_" + dirIndex + "_" + frame);
        this.flame.setImage("flame_run_" + dirIndex + "_" + (this.game.timer / 4) % 8);
      }
    }
  }

  /** Syncs the player and its flame to the Scratch coordinates. */
  @Override
  public void sync() {
    super.sync();
    this.flame.x = this.x;
    this.flame.y = this.y;
    this.flame.sync();
  }

  @Override
  public int getCollidableWidth() {
    return Play.PLAYER_WIDTH;
  }

  @Override
  public int getCollidableHeight() {
    return Play.PLAYER_HEIGHT;
  }
}
