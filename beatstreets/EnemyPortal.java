package beatstreets;

import java.util.List;
import org.openpatch.scratch.Random;
import org.openpatch.scratch.Vector2;

/**
 * An enemy portal. Spawns enemies of the given types until destroyed.
 */
public class EnemyPortal extends Enemy {

  public static final int GENERATE_ANIMATION_FRAMES = 6;
  public static final int GENERATE_ANIMATION_DIVISOR = 16;
  public static final int GENERATE_ANIMATION_TIME =
      GENERATE_ANIMATION_FRAMES * GENERATE_ANIMATION_DIVISOR;

  private final List<EnemyFactory> enemies;
  private int spawnInterval;
  private int spawnTimer;
  private final int spawnIntervalChange;
  private final int maxSpawnInterval;
  private final int maxEnemies;
  private Enemy spawningEnemy = null;
  private int spawnFacing = 0;

  public EnemyPortal(Play game, Vector2 pos, List<EnemyFactory> enemies, int spawnInterval,
      int spawnIntervalChange, int maxSpawnInterval, int maxEnemies, int startTimer) {
    // The hittable area is larger for portals
    super(game, "blank", pos, 240, 340, "portal", new String[0], startTimer,
        new Vector2(1, 1), 15, 500, Game.ENEMY_APPROACH_PLAYER_DISTANCE, new Vector2(50, 50),
        null, "portal_hit", 10);
    this.enemies = enemies;
    this.spawnInterval = spawnInterval;
    this.spawnTimer = spawnInterval;
    this.spawnIntervalChange = spawnIntervalChange;
    this.maxSpawnInterval = maxSpawnInterval;
    this.maxEnemies = maxEnemies;
  }

  @Override
  public void spawned() {
    super.spawned();
    this.game.playSound("portal_appear", 1);
  }

  @Override
  protected void makeDecision() {
    // Like all enemies, portals start in the PAUSE state until their start
    // timer expires
    this.state = State.PORTAL;
  }

  @Override
  public String determineImage() {
    if (this.state == State.PAUSE && this.frame / 8 < 4) {
      return "portal_grow_" + Math.min(this.frame / 8, 3);
    } else if (this.state == State.PORTAL_EXPLODE) {
      return "portal_destroyed_" + Math.min(this.frame / 6, 7);
    } else if (this.spawningEnemy != null) {
      // 3 frames of neutral generate animation, then 3 frames of animation
      // for generating the specific enemy
      int frame = this.frame / EnemyPortal.GENERATE_ANIMATION_DIVISOR;
      if (frame < 3) {
        return "portal_generate_" + frame;
      }
      frame = Math.min(frame - 3, 2);
      return "portal_generate_" + this.spawningEnemy.sprite + "_" + this.spawnFacing + "_"
          + frame + "_" + this.spawningEnemy.colourVariant;
    } else if (this.hitTimer > 0) {
      return "portal_hit_0";
    }
    return "portal_idle_" + (this.frame / 8) % 8;
  }

  @Override
  public void run() {
    if (!this.isPlaying()) {
      super.run();
      return;
    }
    this.frame += 1;

    if (this.state == State.PORTAL) {
      if (this.health <= 0) {
        this.state = State.PORTAL_EXPLODE;
        this.frame = 0;
        this.game.playSound("portal_destroyed", 1);
      } else {
        this.spawnTimer -= 1;
        if (this.spawnTimer <= 0 && this.spawningEnemy != null) {
          // Animation complete, actually put the enemy in the level
          this.game.spawnEnemy(this.spawningEnemy);

          this.spawningEnemy = null;

          // Reset the spawn timer; depending on spawnIntervalChange we may
          // spawn less frequently as time goes on
          this.spawnInterval += this.spawnIntervalChange;
          this.spawnInterval = Math.min(this.spawnInterval, this.maxSpawnInterval);
          this.spawnTimer = this.spawnInterval;
        } else if (this.spawningEnemy == null
            && this.spawnTimer <= EnemyPortal.GENERATE_ANIMATION_TIME) {
          if (this.game.enemies.size() >= this.maxEnemies) {
            // Too many enemies to spawn at the moment, try again in one second
            this.spawnTimer = 60;
          } else {
            // Randomly choose an enemy to spawn from our list
            EnemyFactory chosenEnemy =
                this.enemies.get(Random.randomInt(0, this.enemies.size() - 1));

            // Choose the direction for the spawned enemy to face (0/1 = left/right)
            this.spawnFacing = this.vpos.getX() > this.game.player.vpos.getX() ? 0 : 1;

            // Instantiate the enemy, but it won't appear in the level until
            // the animation is complete
            this.spawningEnemy = chosenEnemy.create(new Vector2(this.vpos));

            // Reset the frame for the spawning animation
            this.frame = 0;

            this.game.playSound("portal_enemy_spawn", 1);
          }
        }
      }
    } else if (this.state == State.PORTAL_EXPLODE) {
      if (this.frame > 50) {
        this.lives -= 1;
        this.died();
      }
    }

    super.run();
  }

  @Override
  public boolean overrideWalking() {
    // A portal never walks
    return true;
  }
}
