package myriapod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import org.openpatch.scratch.KeyCode;
import org.openpatch.scratch.Random;
import org.openpatch.scratch.Sprite;
import org.openpatch.scratch.Stage;
import org.openpatch.scratch.UISprite;
import org.openpatch.scratch.Window;

public class Play extends Stage {

  public static final int WIDTH = 480;
  public static final int HEIGHT = 800;
  public static final int NUM_GRID_ROWS = 25;
  public static final int NUM_GRID_COLS = 14;

  public int wave = -1;
  public int time = 0;
  public int score = 0;

  public Player player;
  public FlyingEnemy flyingEnemy = null;

  public final List<Bullet> bullets = new ArrayList<>();
  public final List<Explosion> explosions = new ArrayList<>();
  public final List<Segment> segments = new ArrayList<>();
  public final HashSet<String> occupied = new HashSet<>();

  private final Rock[][] grid = new Rock[NUM_GRID_ROWS][NUM_GRID_COLS];
  private HUD hud;
  private Sprite overSprite;
  private boolean frozen = false;

  /** A sprite owns the music so its volume is independent of sound effects. */
  protected final Sprite theme = new Sprite();

  public Play() {
    this(true);
  }

  public Play(boolean withPlayer) {
    this.theme.addSound("theme", "myriapod/assets/music/theme.wav");
    this.getSorting().by(Comparator.comparingInt(Play::drawLayer)
        .thenComparingInt(sprite -> sprite instanceof MyrActor actor ? actor.my : 0));

    for (int i = 0; i < 3; i++) {
      this.addBackdrop("bg" + i, String.format("myriapod/assets/images/bg%d.png", i));
    }

    this.addSound("gameover", "myriapod/assets/sounds/gameover.wav");
    this.addSound("wave0", "myriapod/assets/sounds/wave0.wav");
    this.addSound("laser0", "myriapod/assets/sounds/laser0.wav");
    this.addSound("rock_destroy0", "myriapod/assets/sounds/rock_destroy0.wav");
    this.addSound("totem_create0", "myriapod/assets/sounds/totem_create0.wav");
    this.addSound("totem_destroy0", "myriapod/assets/sounds/totem_destroy0.wav");
    this.addSound("segment_explode0", "myriapod/assets/sounds/segment_explode0.wav");
    this.addSound("player_explode0", "myriapod/assets/sounds/player_explode0.wav");
    this.addSound("meanie_explode0", "myriapod/assets/sounds/meanie_explode0.wav");
    for (int i = 0; i < 4; i++) {
      this.addSound("hit" + i, String.format("myriapod/assets/sounds/hit%d.wav", i));
    }

    if (withPlayer) {
      this.player = new Player(this, 240, 768);
      this.add(this.player);
    }

    this.hud = new HUD(this);
  }

  public boolean isFrozen() {
    return this.frozen;
  }

  public Rock rockAt(int cellX, int cellY) {
    if (cellX < 0 || cellX >= NUM_GRID_COLS || cellY < 0 || cellY >= NUM_GRID_ROWS) {
      return null;
    }
    return this.grid[cellY][cellX];
  }

  public void setRock(int cellX, int cellY, Rock rock) {
    this.grid[cellY][cellX] = rock;
    this.add(rock);
  }

  /** Damages the rock at the given cell. Returns true, if there was a rock. */
  public boolean damage(int cellX, int cellY, int amount, boolean fromBullet) {
    if (cellX < 0 || cellX >= NUM_GRID_COLS || cellY < 0 || cellY >= NUM_GRID_ROWS) {
      return false;
    }
    Rock rock = this.grid[cellY][cellX];
    boolean hit = rock != null;
    if (rock != null && rock.damage(amount, fromBullet)) {
      this.grid[cellY][cellX] = null;
      rock.remove();
    }
    return hit;
  }

  /** Checks whether the given position is free of rocks and on the playfield. */
  public boolean allowMovement(int x, int y, int ax, int ay) {
    if (x < 40 || x > 440 || y < 592 || y > 784) {
      return false;
    }

    int x0 = Math.floorDiv(x - 18 - 16, 32);
    int y0 = Math.floorDiv(y - 10, 32);
    int x1 = Math.floorDiv(x + 18 - 16, 32);
    int y1 = Math.floorDiv(y + 10, 32);

    for (int yi = y0; yi <= y1; yi++) {
      for (int xi = x0; xi <= x1; xi++) {
        if (this.rockAt(xi, yi) != null || (xi == ax && yi == ay)) {
          return false;
        }
      }
    }
    return true;
  }

  /** Destroys any rocks that might be overlapping the player on respawn. */
  public void clearRocksForRespawn(int x, int y) {
    int x0 = Math.floorDiv(x - 18 - 16, 32);
    int y0 = Math.floorDiv(y - 10, 32);
    int x1 = Math.floorDiv(x + 18 - 16, 32);
    int y1 = Math.floorDiv(y + 10, 32);

    for (int yi = y0; yi <= y1; yi++) {
      for (int xi = x0; xi <= x1; xi++) {
        this.damage(xi, yi, 5, false);
      }
    }
  }

  public void addBullet(Bullet bullet) {
    this.bullets.add(bullet);
    this.add(bullet);
  }

  public void addExplosion(Explosion explosion) {
    this.explosions.add(explosion);
    this.add(explosion);
  }

  /** Plays a random sound variation. Only played while playing. */
  public void playRandom(String name, int count) {
    if (this.player != null) {
      this.playSound(name + Random.randomInt(0, count - 1));
    }
  }

  /** Plays a game sound. No sounds are played on the menu. */
  public void playGameSound(String name) {
    if (this.player != null) {
      this.playSound(name);
    }
  }

  @Override
  public void run() {
    // The theme music keeps playing, like the music channel of the original
    this.theme.playSound("theme");
    this.theme.setVolume(40);

    if (!this.frozen) {
      // Time moves twice as fast every fourth wave
      this.time += Math.floorMod(this.wave, 4) == 3 ? 2 : 1;
      this.occupied.clear();

      // Remove objects which are no longer wanted
      this.bullets.removeIf(
          b -> {
            if (b.my <= 0 || b.done) {
              b.remove();
              return true;
            }
            return false;
          });
      this.explosions.removeIf(
          e -> {
            if (e.timer == 31) {
              e.remove();
              return true;
            }
            return false;
          });
      this.segments.removeIf(
          s -> {
            if (s.health <= 0) {
              s.remove();
              return true;
            }
            return false;
          });

      // Flying enemy
      if (this.flyingEnemy != null) {
        if (this.flyingEnemy.health <= 0
            || this.flyingEnemy.mx < -35
            || this.flyingEnemy.mx > 515) {
          this.flyingEnemy.remove();
          this.flyingEnemy = null;
        }
      } else if (Random.random() < 0.01) {
        this.flyingEnemy = new FlyingEnemy(this, this.player != null ? this.player.mx : 240);
        this.add(this.flyingEnemy);
      }

      // Start a new wave if there are no segments left
      if (this.segments.isEmpty()) {
        int numRocks = 0;
        for (Rock[] row : this.grid) {
          for (Rock rock : row) {
            if (rock != null) {
              numRocks += 1;
            }
          }
        }
        if (numRocks < 31 + this.wave) {
          // Create one new rock per frame until there are enough
          while (true) {
            int x = Random.randomInt(0, NUM_GRID_COLS - 1);
            int y = Random.randomInt(1, NUM_GRID_ROWS - 3);
            if (this.grid[y][x] == null) {
              this.setRock(x, y, new Rock(this, x, y, false));
              break;
            }
          }
        } else {
          this.playGameSound("wave0");
          this.wave += 1;
          this.time = 0;

          int numSegments = 8 + this.wave / 4 * 2;
          for (int i = 0; i < numSegments; i++) {
            int cellX = -1 - i;
            int cellY = 0;
            int health = new int[][] {{1, 1}, {1, 2}, {2, 2}, {1, 1}}[Math.floorMod(this.wave, 4)][i % 2];
            boolean fast = Math.floorMod(this.wave, 4) == 3;
            boolean head = i == 0;
            var segment = new Segment(this, cellX, cellY, health, fast, head);
            this.segments.add(segment);
            this.add(segment);
          }
        }
      }

      // Game over
      if (this.player != null && this.player.lives == 0 && this.player.timer == 100) {
        this.frozen = true;
        this.playSound("gameover");
        this.overSprite = new UISprite();
        this.overSprite.addCostume("over", "myriapod/assets/images/over.png");
        this.overSprite.setPosition(0, 0);
        this.add(this.overSprite);
      }
    }

    this.switchBackdrop("bg" + Math.max(this.wave, 0) % 3);
    // The HUD is drawn on top of everything
    this.hud.update();
  }

  /** World objects sort by y; explosions, the flying enemy and UI draw above them. */
  private static int drawLayer(Sprite sprite) {
    if (sprite instanceof UISprite) {
      return 3;
    }
    if (sprite instanceof FlyingEnemy) {
      return 2;
    }
    return sprite instanceof Explosion ? 1 : 0;
  }

  @Override
  public void whenKeyPressed(KeyCode key) {
    if (this.frozen && key == KeyCode.SPACE) {
      this.stopAllSounds();
      this.theme.stopAllSounds();
      Window.getInstance().setStage(new Menu());
    }
  }
}
