package avenger;

import java.util.HashSet;
import java.util.Set;
import org.openpatch.scratch.Random;
import org.openpatch.scratch.Sprite;
import org.openpatch.scratch.Vector2;

/** An enemy. The position is kept in world pixel coordinates. */
public class Enemy extends WrapActor {

  public final EnemyType type;

  private final double maxSpeed;
  private final double acceleration;

  public EnemyState state;
  public int stateTimer;

  public double targetX;
  public double targetY;
  private int updateTargetTimer;

  public double velX;
  public double velY;

  // Enemies will sometimes pick up humans and carry them into the sky,
  // turning them into mutants
  public Human targetHuman = null;
  public boolean carrying = false;

  // Counts down, allowed to shoot when zero or lower
  private int bulletTimer;

  // Only used for baiter enemies, which fire in a fixed pattern of
  // ever-increasing angles
  private double fireAngle = 0;

  /** Our radar blip. */
  public final Sprite blip;

  public int animTimer;

  public Enemy(Play game, int startTimer, EnemyType type, Vector2 pos, Vector2 startVel) {
    super(game, "blank");

    // If no position has been supplied, generate a random position
    if (pos == null) {
      this.x = Random.randomInt(0, Play.LEVEL_WIDTH - 1);
      this.y = Random.randomInt(32, Play.LEVEL_HEIGHT - 32);
    } else {
      this.x = pos.getX();
      this.y = pos.getY();
    }

    this.type = type;

    switch (type) {
      case LANDER -> {
        this.maxSpeed = 5;
        this.acceleration = 0.1;
      }
      case MUTANT -> {
        this.maxSpeed = 9;
        this.acceleration = 0.5;
      }
      case BAITER -> {
        this.maxSpeed = 9;
        this.acceleration = 0.01;
      }
      case POD -> {
        this.maxSpeed = 10;
        this.acceleration = 0.03;
      }
      case SWARMER -> {
        this.maxSpeed = 8;
        this.acceleration = 1;
      }
      default -> {
        this.maxSpeed = 5;
        this.acceleration = 0.1;
      }
    }

    // The appear animation is played by all enemy types
    for (int f = 0; f <= 10; f++) {
      this.addCostume("appear" + f, Utils.img("appear" + f));
    }

    // The sprites of this enemy type
    switch (type) {
      case LANDER -> {
        for (int f = 0; f < 3; f++) {
          this.addCostume("lander" + f, Utils.img("lander" + f));
        }
      }
      case MUTANT -> {
        for (int f = 0; f < 4; f++) {
          this.addCostume("mutant" + f, Utils.img("mutant" + f));
        }
      }
      case BAITER -> {
        for (int f = 0; f < 8; f++) {
          this.addCostume("baiter" + f, Utils.img("baiter" + f));
        }
      }
      case POD -> {
        for (int f = 0; f < 6; f++) {
          this.addCostume("pod" + f, Utils.img("pod" + f));
        }
      }
      case SWARMER -> {
        for (int f = 0; f < 8; f++) {
          this.addCostume("swarmer" + f, Utils.img("swarmer" + f));
        }
      }
    }

    // The explosion animation
    for (int f = 0; f <= 9; f++) {
      this.addCostume("enemy_explode" + f, Utils.img("enemy_explode" + f));
    }

    // Select a target position which the enemy will oscillate around
    this.targetX = this.x + Random.random(-100, 100);
    this.targetY = this.y + Random.random(-100, 100);
    this.updateTargetTimer = 0;

    this.velX = startVel == null ? 0 : startVel.getX();
    this.velY = startVel == null ? 0 : startVel.getY();

    // Most enemies start in the 'start' state where they play an animation
    // to appear. Swarmers just appear immediately
    if (type == EnemyType.SWARMER) {
      this.state = EnemyState.ALIVE;
      this.stateTimer = 0;
    } else {
      this.state = EnemyState.START;
      this.stateTimer = startTimer;
    }

    this.bulletTimer = Random.randomInt(30, 90);

    // Create our radar blip
    this.blip = new Sprite();
    this.blip.addCostume("dot-red", Utils.img("dot-red"));
    this.blip.switchCostume("dot-red");
    this.blip.hide();
    game.add(this.blip);

    this.animTimer = Random.randomInt(0, 47);
  }

  @Override
  public void relocate(double delta) {
    super.relocate(delta);

    this.targetX += delta;
  }

  /**
   * Given a position, see if it falls within this sprite's rectangle (but
   * only if we're in the alive state). Kill the enemy if it is.
   */
  public boolean laserHitTest(double px, double py) {
    if (this.collidepoint(px, py) && this.state == EnemyState.ALIVE) {
      this.state = EnemyState.EXPLODING;
      this.stateTimer = 0;
      this.animTimer = 0;
      if (this.targetHuman != null) {
        if (this.carrying) {
          this.targetHuman.dropped();
        }
        this.targetHuman = null;
        this.carrying = false;
      }
      this.game.playRandom("enemy_explode", 6);

      // If we're a pod, release several swarmers
      if (this.type == EnemyType.POD) {
        for (int i = 0; i < 3; i++) {
          Vector2 startVel = new Vector2(Random.random(-25, 25), Random.random(-25, 25));
          this.game.addEnemy(new Enemy(this.game, 0, EnemyType.SWARMER,
              new Vector2(this.x, this.y), startVel));
        }
      }

      return true;
    }
    return false;
  }

  @Override
  public void run() {
    if (!this.isPlaying()) {
      return;
    }
    this.wrap();

    if (this.state == EnemyState.START) {
      this.stateTimer += 1;
      // When the state timer hits 1, our appear animation has just started
      if (this.stateTimer == 1) {
        switch (this.type) {
          case MUTANT -> this.game.playSound("enemy_appear_mutant0");
          case LANDER -> this.game.playSound("enemy_appear_normal0");
          case BAITER -> this.game.playSound("enemy_appear_ufo0");
          default -> {}
        }
      }

      // When the state timer hits 33, we've finished the appear animation, so
      // we switch to the alive state
      if (this.stateTimer == 33) {
        this.state = EnemyState.ALIVE;
      } else if (this.stateTimer >= 0) {
        // Play the appear animation
        this.switchCostume("appear" + this.stateTimer / 3);
      }
    } else if (this.state == EnemyState.ALIVE) {
      this.updateAlive();
    } else if (this.state == EnemyState.EXPLODING) {
      // There are 10 frames of the explode animation, one every 2 game frames
      this.animTimer += 1;
      int frame = this.animTimer / 2;
      this.switchCostume("enemy_explode" + Math.min(9, frame));

      if (frame == 10) {
        // Animation finished, the enemy is now officially dead
        this.state = EnemyState.DEAD;
      }
    }

    // Update radar blip pos
    this.game.radar.setBlipPosition(this.blip, this.game.radar.radarPos(this.x, this.y));

    super.run();
  }

  private void updateAlive() {
    double maxSpeed = this.maxSpeed;

    // If we're targeting or carrying a human, check to see if they were shot
    // by the player
    if (this.targetHuman != null && this.targetHuman.dead) {
      this.targetHuman = null;
      this.carrying = false;
    }

    // Should we start heading for a human to pick up?
    if (this.targetHuman == null && this.type == EnemyType.LANDER
        && Random.random(0, 1) < 0.001) {
      // Find a human who isn't currently being carried, and isn't being
      // targeted by another enemy
      Set<Human> targetedHumans = new HashSet<>();
      for (Enemy enemy : this.game.enemies) {
        if (enemy.targetHuman != null) {
          targetedHumans.add(enemy.targetHuman);
        }
      }
      // Choose the nearest available human
      Human nearest = null;
      double nearestDistanceSq = Double.MAX_VALUE;
      for (Human human : this.game.humans) {
        if (targetedHumans.contains(human) || !human.canBePickedUpByEnemy()) {
          continue;
        }
        double distanceSq = (human.x - this.x) * (human.x - this.x)
            + (human.y - this.y) * (human.y - this.y);
        if (distanceSq < nearestDistanceSq) {
          nearestDistanceSq = distanceSq;
          nearest = human;
        }
      }
      this.targetHuman = nearest;
    }

    // Try to move towards a target position. This will either be the player
    // position, a human we're about to pick up, the top of the sky (if we're
    // carrying a human), or the previously determined target position
    if (this.targetHuman != null) {
      if (this.carrying) {
        // Carrying a human into the sky - target pos will be our current pos
        // on the X axis and close to the top of the screen on the Y axis
        this.targetX = this.x;
        this.targetY = 64;
        maxSpeed = 0.5;

        // If we reach the top of the screen, turn the captured human into a
        // mutant enemy
        if (Math.abs(this.y - this.targetY) < 10) {
          Vector2 mutantPos = new Vector2(this.targetHuman.x, this.targetHuman.y);
          this.game.addEnemy(new Enemy(this.game, 0, EnemyType.MUTANT, mutantPos, null));
          this.targetHuman.die();
          this.targetHuman = null;
          this.carrying = false;
        }
      } else {
        // If we're going to a human, we initially go to a position above
        // them, then go down to pick them up. As we get closer, we reduce our
        // max speed to ensure we don't overshoot
        double xDistance = Math.abs(this.x - this.targetHuman.x);
        if (xDistance < 80) {
          // Slow down as we approach the human so we don't overshoot
          maxSpeed = 1;
        }
        if (xDistance > 100) {
          // Set target pos to be above our target human's pos
          this.targetX = this.targetHuman.x;
          this.targetY = this.targetHuman.y - 200;
        } else {
          // Set target pos to our target human's pos. Start carrying them
          // when we get within 55 pixels
          this.targetX = this.targetHuman.x;
          this.targetY = this.targetHuman.y;
          double distance = Math.hypot(this.x - this.targetX, this.y - this.targetY);
          if (distance < 55) {
            this.carrying = true;
            this.targetHuman.pickedUp(this);
          }
        }
      }
    } else {
      // No target human - go for our target position, and update the target
      // position every so often
      this.updateTargetTimer -= 1;
      if (this.updateTargetTimer <= 0) {
        // Update target pos
        this.updateTargetTimer = 60;

        // Landers go for the player if they're nearby, other enemies will
        // always go for the player regardless of distance
        double maxPlayerDistance = this.type == EnemyType.LANDER ? 500 : Play.LEVEL_WIDTH;

        double distance = Math.hypot(this.game.player.x - this.x, this.game.player.y - this.y);
        if (distance < maxPlayerDistance) {
          // Go for the player
          this.targetX = this.game.player.x;
          this.targetY = this.game.player.y;
        }

        // In either case, we add a random offset to our target position.
        // Baiter enemies have quite a large random variation
        double xRange = this.type == EnemyType.BAITER ? 800 : 100;
        double yRange = this.type == EnemyType.BAITER ? 300 : 100;
        this.targetX += Random.random(-xRange, xRange);
        this.targetY += Random.random(-yRange, yRange);
      }
    }

    // Get the vector from our pos to the target pos, and apply it to our
    // velocity as this frame's force
    double distance = Math.hypot(this.targetX - this.x, this.targetY - this.y);
    double forceX = 0;
    double forceY = 0;
    if (distance > 0) {
      forceX = (this.targetX - this.x) / distance * this.acceleration;
      forceY = (this.targetY - this.y) / distance * this.acceleration;
    }

    // If we're near the top or bottom of the game world, apply an additional
    // force to push us away from the edge
    if (this.y < 64) {
      forceY += 0.2;
    }
    if (this.y > Play.LEVEL_HEIGHT - 64) {
      forceY -= 0.2;
    }

    // Apply force to velocity
    this.velX += forceX;
    this.velY += forceY;

    // Limit max speed. If we're over our max speed, slow down gradually over
    // several frames, rather than slowing down suddenly
    double speed = Math.hypot(this.velX, this.velY);
    if (speed > maxSpeed) {
      double newSpeed = Math.max(speed * 0.9, maxSpeed);
      this.velX = this.velX / speed * newSpeed;
      this.velY = this.velY / speed * newSpeed;
    }

    // Apply velocity to position
    this.x += this.velX;
    this.y += this.velY;

    // If carrying, update the carried human's pos
    if (this.carrying) {
      this.targetHuman.x = this.x;
      this.targetHuman.y = this.y + 50;
    }

    // Count down the bullet timer. If it's zero or lower and the enemy is near
    // the player (but not too near!), fire a bullet
    this.bulletTimer -= 1;
    if (this.bulletTimer <= 0) {
      if (this.type == EnemyType.BAITER) {
        // Baiters have their own firing pattern and don't care about the
        // position of the player
        Vector2 velocity = new Vector2(Math.cos(this.fireAngle) * 3, Math.sin(this.fireAngle) * 3);
        this.game.addBullet(new Bullet(this.game, this.x, this.y, velocity));
        this.bulletTimer = 8;
        this.fireAngle += 0.3;
      } else if (this.game.player.lives > 0) {
        // Other enemy types only fire if the player is alive
        double playerVecX = this.game.player.x - this.x;
        double playerVecY = this.game.player.y - this.y;
        double playerDistance = Math.hypot(playerVecX, playerVecY);
        if (100 < playerDistance && playerDistance < 300) {
          // Fire a bullet at the player, with a bit of random inaccuracy
          double unitX = playerVecX / playerDistance;
          double unitY = playerVecY / playerDistance;
          Vector2 velocity = new Vector2((unitX + Random.random(-0.5, 0.5)) * 6,
              (unitY + Random.random(-0.5, 0.5)) * 6);
          this.game.addBullet(new Bullet(this.game, this.x, this.y, velocity));

          // Non-baiter enemies fire at a random interval, with mutants
          // firing more often
          int upperLimit = this.type == EnemyType.MUTANT ? 30 : 90;
          this.bulletTimer = Random.randomInt(20, upperLimit);
        }
      }
    }

    // Update sprite/animation
    switch (this.type) {
      case LANDER -> {
        // Frame 0 if not picking up a human, frame 1 if close to picking up a
        // human, frame 2 if we've picked up a human
        int frame = 0;
        if (this.targetHuman != null) {
          if (this.carrying) {
            frame = 2;
          } else if (Math.hypot(this.x - this.targetHuman.x, this.y - this.targetHuman.y) < 90) {
            frame = 1;
          }
        }
        this.switchCostume("lander" + frame);
      }
      case MUTANT -> {
        this.animTimer += 1;
        this.switchCostume("mutant" + this.animTimer / 6 % 4);
      }
      case BAITER -> {
        this.animTimer += 1;
        this.switchCostume("baiter" + this.animTimer / 3 % 8);
      }
      case POD -> {
        // Frames 0 to 2 = left, 3 to 5 = right
        this.animTimer += 1;
        int frame = Utils.forwardBackwardAnimationFrame(this.animTimer / 6, 3);
        if (this.velX > 0) {
          frame += 3;
        }
        this.switchCostume("pod" + frame);
      }
      case SWARMER -> {
        this.animTimer += 1;
        this.switchCostume("swarmer" + this.animTimer / 6 % 8);
      }
    }
  }
}
