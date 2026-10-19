package kinetix;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openpatch.scratch.Operators;
import org.openpatch.scratch.Random;
import org.openpatch.scratch.Sprite;
import org.openpatch.scratch.Stage;
import org.openpatch.scratch.UISprite;
import org.openpatch.scratch.Window;
import org.openpatch.scratch.extensions.text.BitmapText;

/**
 * The play area: bat, ball, bricks, barrels, bullets and impacts, with the
 * level progress and the score. All positions are kept in pixel coordinates,
 * see {@link KxActor}.
 *
 * <p>The draw order mirrors the original: shadows behind the bricks, the
 * moving objects in front of the bricks, impact animations above the moving
 * objects, and the score and the menu on top.
 */
public class Play extends Stage {

  public static final int WIDTH = 640;
  public static final int HEIGHT = 640;

  public static final int BAT_SPEED = 8;
  public static final int BAT_MIN_X = 35;
  public static final int BAT_MAX_X = 605;
  public static final int TOP_EDGE = 50;
  public static final int RIGHT_EDGE = 617;
  public static final int LEFT_EDGE = 23;
  public static final int BAT_TOP_EDGE = 590;
  public static final int BALL_INITIAL_OFFSET = 10;
  public static final int BALL_START_SPEED = 5;
  public static final int BALL_MIN_SPEED = 4;
  public static final int BALL_MAX_SPEED = 11;
  public static final int BALL_SPEED_UP_INTERVAL = 10 * 60;
  public static final int BALL_SPEED_UP_INTERVAL_FAST = 15 * 60;
  public static final int BALL_FAST_SPEED_THRESHOLD = 7;
  public static final int BALL_RADIUS = 7;
  public static final int BULLET_SPEED = 8;
  public static final int BRICKS_X_START = 20;
  public static final int BRICKS_Y_START = 100;
  public static final int BRICK_WIDTH = 40;
  public static final int BRICK_HEIGHT = 20;
  public static final int SHADOW_OFFSET = 10;
  public static final double POWERUP_CHANCE = 0.2;
  public static final int FIRE_INTERVAL = 30;
  public static final int PORTAL_ANIMATION_SPEED = 5;

  /** A collision returned by {@link #collide(double, double, double, double, int)}. */
  public record Collision(double x, double y, boolean showImpact, CollisionType type) {}

  private static final String[][] LEVELS = {
    {
      "        ",
      "        ",
      "        ",
      "     a  ",
      "    a7a ",
      "     a  ",
      "     a55",
      "    444 ",
      "   333a ",
      "  222a  ",
      " 111a   ",
      "   11aa ",
      "    111 ",
      "    6   ",
      "     6  "
    },
    {
      "        ",
      "        ",
      "    3   ",
      "    3   ",
      "    3   ",
      "    3000",
      "    3000",
      "   53000",
      "   53000",
      "  35a555",
      " 3 5aa55",
      "3  5aaa5",
      "  355555",
      "  333333",
      "   333  ",
      "    33  ",
      "     3  "
    },
    {
      "   7    ",
      "  77    ",
      " 7777   ",
      " 7777   ",
      " 77777  ",
      " 77777  ",
      " 77 777 ",
      " 7  7777",
      " 7   717",
      "     777",
      "      77",
      "      7 ",
      "     c7 ",
      "      c ",
      "      c "
    },
    {
      "   03   ",
      "   30   ",
      "    03  ",
      "    30  ",
      "     0  ",
      " 8   0  ",
      " 88 8033",
      "  883333",
      "   8333d",
      "   33733",
      "  33373d",
      " 3333333",
      " 3c 333d",
      " cc 3333",
      " c   3 3",
      "     3 3",
      "    3 3 ",
      "    c 3 ",
      "    cc3c",
      "    cccc",
      "      d "
    },
    {
      "5   9  0",
      "0   4  3",
      "08  4  4",
      "53  47 2",
      " 39 92 1",
      " 84  2  ",
      "  47 26 ",
      "5 92 71 ",
      "08 26 1 ",
      "53971 1 ",
      " 8471c6 ",
      "  926acc",
      "   71aad",
      "039 6aac",
      "dc421ac ",
      "  dccc  ",
      "    d   "
    },
    {
      "  dccccd",
      "  c89765",
      "  c34210",
      "  c34210",
      "  c34210",
      "  c34210",
      "  c3421d",
      "  c34210",
      "  c34210",
      "  c34210",
      "  c34210",
      "  c89765",
      "  dccccd"
    }
  };

  protected Controls controls;
  protected int lives = 3;
  protected int score = 0;

  protected final List<Ball> balls = new ArrayList<>();
  protected Bat bat;
  protected final List<Bullet> bullets = new ArrayList<>();
  protected final List<Barrel> barrels = new ArrayList<>();

  /** All shadows share one draw layer, behind the bricks and moving objects. */
  private final Set<Sprite> shadowSprites = new HashSet<>();

  private Brick[][] bricks;
  private int numRows;
  private int numCols;
  private int bricksRemaining;
  private int levelNum;

  protected boolean portalActive = false;
  private int portalFrame = 0;
  private int portalTimer = 0;

  private boolean gameOver = false;
  protected int totalFrames = 0;

  private final Sprite portalExit;
  private final BitmapText scoreText = new BitmapText("kinetix/assets/images/digit%d.png");
  private final List<Sprite> lifeSprites = new ArrayList<>();
  private Sprite gameOverSprite;

  private final KeyboardControls keyboardControls;

  /** The sound effects of the game, one file per entry. */
  private static final String[] SOUNDS = {
    "ball_stick0", "bat_extend0", "bat_gun0", "bat_small0", "extra_life0",
    "game_over0", "hit_brick0", "hit_fast0", "hit_veryfast0", "hit_wall0",
    "laser0", "lose_life0", "magnet0", "multiball0", "portal_exit0",
    "powerup0", "speed_up0", "start_game0"
  };

  public Play(Controls controls) {
    this.getSorting().by(Comparator.comparingInt(this::drawLayer));
    this.controls = controls;
    if (controls instanceof AIControls aiControls) {
      aiControls.setGame(this);
    }
    this.keyboardControls = new KeyboardControls(this);

    for (String sound : SOUNDS) {
      this.addSound(sound, "kinetix/assets/sounds/" + sound + ".ogg");
    }
    for (int i = 0; i < 4; i++) {
      this.addSound("bullet_hit" + i, String.format("kinetix/assets/sounds/bullet_hit%d.ogg", i));
    }

    for (int i = 0; i < LEVELS.length; i++) {
      this.addBackdrop("arena" + i, String.format("kinetix/assets/images/arena%d.png", i));
    }

    // Exit portal at the bottom right, and the two enemy doors
    this.portalExit = new Sprite();
    for (int i = 0; i < 4; i++) {
      this.portalExit.addCostume(
          "portal_exit" + i, String.format("kinetix/assets/images/portal_exit%d.png", i));
    }
    this.portalExit.switchCostume("portal_exit0");
    this.setPixelPosition(this.portalExit, WIDTH - 70 - 20 + 35, HEIGHT - 70 + 35);
    this.add(this.portalExit);

    var doorLeft = new Sprite();
    doorLeft.addCostume("door0", "kinetix/assets/images/portal_meanie00.png");
    doorLeft.switchCostume("door0");
    this.setPixelPosition(doorLeft, 110 + 45, 40 + 35);
    this.add(doorLeft);

    var doorRight = new Sprite();
    doorRight.addCostume("door1", "kinetix/assets/images/portal_meanie10.png");
    doorRight.switchCostume("door1");
    this.setPixelPosition(doorRight, 440 + 45, 40 + 35);
    this.add(doorRight);

    // The score and life icons stay fixed on screen, above the game objects.
    for (int digit = 0; digit < 10; digit++) {
      this.scoreText.addGlyph((char) ('0' + digit), "kinetix/assets/images/digit" + digit + ".png");
    }
    this.scoreText.setIsUI(true);
    this.scoreText.setLetterSpacing(55 - 75);
    this.scoreText.setPosition(15 - WIDTH / 2.0, HEIGHT / 2.0 - 50);
    this.add(this.scoreText);
    for (int i = 0; i < 6; i++) {
      var life = new UISprite();
      life.addCostume("life", "kinetix/assets/images/life.png");
      life.hide();
      this.add(life);
      this.lifeSprites.add(life);
    }

    this.newLevel(0);
  }

  private boolean inDemoMode() {
    return this.controls instanceof AIControls;
  }

  public boolean isPlaying() {
    return Window.getInstance().getStage() == this
        && !this.gameOver && (this.inDemoMode() || this.lives > 0);
  }

  @Override
  public void playSound(String name) {
    // No in-game sound effects while the AI player shows the game on the menu
    if (!this.inDemoMode()) {
      super.playSound(name);
    }
  }

  /** Plays a random sound variation, e.g. bullet_hit0 to bullet_hit3. */
  public void playRandom(String name, int count) {
    this.playSound(name + Random.randomInt(0, count - 1));
  }

  @Override
  public void run() {
    this.totalFrames += 1;
    this.controls.update();
    this.keyboardControls.update();

    if (this.gameOver) {
      // Wait for fire to return to the title screen
      this.gameOverSprite.switchCostume("gameover" + this.totalFrames / 4 % 15);
      if (this.keyboardControls.firePressed()) {
        this.stopAllSounds();
        Window.getInstance().setStage(new Menu());
      }
      return;
    }

    // In demo mode the game keeps running no matter how many lives the AI
    // player has left
    if (this.inDemoMode() || this.lives > 0) {
      this.advanceLevel();
    } else {
      this.playSound("game_over0");
      this.gameOver = true;
      this.gameOverSprite = new UISprite();
      for (int i = 0; i < 15; i++) {
        this.gameOverSprite.addCostume(
            "gameover" + i, String.format("kinetix/assets/images/gameover%d.png", i));
      }
      this.gameOverSprite.switchCostume("gameover0");
      this.setPixelPosition(this.gameOverSprite, WIDTH / 2.0, 450 + 25);
      this.add(this.gameOverSprite);
    }
  }

  private void advanceLevel() {
    // Remove any balls which are off the bottom of the screen
    this.balls.removeIf(
        ball -> {
          if (ball.y >= HEIGHT) {
            ball.remove();
            this.removeShadow(ball.shadow);
            return true;
          }
          return false;
        });

    // Lose a life if there are no balls
    if (this.balls.isEmpty()) {
      // We don't care about how many lives the AI player has in demo mode
      if (this.lives > 0 || this.inDemoMode()) {
        this.lives -= 1;
        this.addBall(new Ball(this));
        this.bat.changeType(BatType.NORMAL);
      }
      this.playSound("lose_life0");
    }

    // Remove barrels which have gone off the bottom of the screen and bullets
    // which are no longer alive (impacts remove themselves)
    this.barrels.removeIf(
        barrel -> {
          if (barrel.y >= HEIGHT) {
            barrel.remove();
            this.removeShadow(barrel.shadow);
            return true;
          }
          return false;
        });
    this.bullets.removeIf(
        bullet -> {
          if (!bullet.alive) {
            bullet.remove();
            return true;
          }
          return false;
        });

    // Update the portal that allows you to leave the level
    if (this.portalActive) {
      if (this.portalFrame < 3) {
        this.portalTimer -= 1;
        if (this.portalTimer <= 0) {
          this.portalTimer = PORTAL_ANIMATION_SPEED;
          this.portalFrame += 1;
          this.portalExit.switchCostume("portal_exit" + this.portalFrame);
        }
      } else if (this.bat.isPortalTransitionComplete()) {
        this.newLevel(this.levelNum + 1);
        return;
      }
    }

    // If no balls have damaged bricks or touched the bat in the last 30
    // seconds, change all indestructible bricks to two-hit bricks, to avoid a
    // situation where the ball can get stuck bouncing between them
    if (this.detectStuckBalls()) {
      boolean changedAny = false;
      for (int row = 0; row < this.numRows; row++) {
        for (int col = 0; col < this.numCols; col++) {
          // 13 is the indestructible brick, 12 the two-hit brick
          Brick brick = this.bricks[row][col];
          if (brick != null && brick.id == 13) {
            brick.setId(12);
            changedAny = true;
          }
        }
      }

      if (changedAny) {
        this.playSound("bat_small0");
      }

      // To prevent this triggering again next frame, pretend that one of the
      // balls has touched the bat in the last 30 seconds
      if (!this.balls.isEmpty()) {
        this.balls.get(0).pretendTouchedBat();
      }
    }

    this.drawScore();
    this.drawLives();
  }

  private void drawScore() {
    if (this.inDemoMode()) {
      return;
    }
    this.scoreText.showText(Integer.toString(this.score));
  }

  private void drawLives() {
    if (this.inDemoMode()) {
      return;
    }
    for (int i = 0; i < this.lifeSprites.size(); i++) {
      var life = this.lifeSprites.get(i);
      if (i < this.lives) {
        // Blit position of the original: (0, HEIGHT - 20), images are 45 x 22
        this.setPixelPosition(life, 22.5 + i * 50, HEIGHT - 20 + 11);
        life.show();
      } else {
        life.hide();
      }
    }
  }

  /** Detects whether all balls are stuck bouncing between indestructible bricks. */
  private boolean detectStuckBalls() {
    // Having no balls in play doesn't count as all balls being stuck
    if (this.balls.isEmpty()) {
      return false;
    }
    for (Ball ball : this.balls) {
      if (!ball.isStuckForAges()) {
        return false;
      }
    }
    return true;
  }

  public void newLevel(int levelNumber) {
    this.playSound("start_game0");

    // Go back to the first level if we've finished the last level
    if (levelNumber >= LEVELS.length) {
      levelNumber = 0;
    }
    this.levelNum = levelNumber;
    this.switchBackdrop("arena" + levelNumber % LEVELS.length);

    this.clearLevel();

    // For each row, build a new row which includes the existing row plus a
    // mirrored version
    String[] level = new String[LEVELS[levelNumber].length];
    for (int y = 0; y < level.length; y++) {
      String row = LEVELS[levelNumber][y];
      level[y] = row + new StringBuilder(row.substring(0, row.length() - 1)).reverse();
    }

    this.numRows = level.length;
    this.numCols = level[0].length();
    this.bricks = new Brick[this.numRows][this.numCols];

    // The numbers in the level data are in hexadecimal
    for (int y = 0; y < this.numRows; y++) {
      for (int x = 0; x < this.numCols; x++) {
        char c = level[y].charAt(x);
        if (c != ' ') {
          this.bricks[y][x] = new Brick(x, y, Character.digit(c, 16));
        }
      }
    }

    // Count the destructible bricks, not counting brick id 13 which is
    // indestructible
    this.bricksRemaining = 0;
    for (Brick[] row : this.bricks) {
      for (Brick brick : row) {
        if (brick != null && brick.id != 13) {
          this.bricksRemaining += 1;
        }
      }
    }

    // Draw the brick shadows, then the bricks, both above the doors
    for (Brick[] row : this.bricks) {
      for (Brick brick : row) {
        if (brick != null) {
          var shadow = new Sprite();
          shadow.addCostume("bricks", "kinetix/assets/images/bricks.png");
          this.setPixelPosition(
              shadow,
              brick.gridX * BRICK_WIDTH + BRICKS_X_START + SHADOW_OFFSET + BRICK_WIDTH / 2.0,
              brick.gridY * BRICK_HEIGHT + BRICKS_Y_START + SHADOW_OFFSET + BRICK_HEIGHT / 2.0);
          brick.shadow = shadow;
          this.addShadow(shadow);
        }
      }
    }
    for (Brick[] row : this.bricks) {
      for (Brick brick : row) {
        if (brick != null) {
          this.add(brick);
        }
      }
    }

    this.addBall(new Ball(this));
    this.bat = new Bat(this, this.controls);
    this.addShadow(this.bat.shadow);
    this.add(this.bat);

    this.portalActive = false;
    this.portalFrame = 0;
    this.portalTimer = 0;
    this.portalExit.switchCostume("portal_exit0");
  }

  /** Removes the bricks, balls, bat, barrels, bullets and impacts of the current level. */
  private void clearLevel() {
    if (this.bricks != null) {
      for (Brick[] row : this.bricks) {
        for (Brick brick : row) {
          if (brick != null) {
            brick.remove();
            this.removeShadow(brick.shadow);
          }
        }
      }
    }
    for (Ball ball : this.balls) {
      ball.remove();
      this.removeShadow(ball.shadow);
    }
    this.balls.clear();
    if (this.bat != null) {
      this.bat.remove();
      this.removeShadow(this.bat.shadow);
      this.bat = null;
    }
    for (Barrel barrel : this.barrels) {
      barrel.remove();
      this.removeShadow(barrel.shadow);
    }
    this.barrels.clear();
    for (Bullet bullet : this.bullets) {
      bullet.remove();
    }
    this.bullets.clear();
    this.remove(Impact.class);
  }

  public void addBall(Ball ball) {
    this.balls.add(ball);
    this.addShadow(ball.shadow);
    this.add(ball);
  }

  public void addBullet(Bullet bullet) {
    this.bullets.add(bullet);
    this.add(bullet);
  }

  public void addBarrel(Barrel barrel) {
    this.barrels.add(barrel);
    this.addShadow(barrel.shadow);
    this.add(barrel);
  }

  public void addImpact(Impact impact) {
    this.add(impact);
  }

  private void addShadow(Sprite shadow) {
    this.shadowSprites.add(shadow);
    this.add(shadow);
  }

  private void removeShadow(Sprite shadow) {
    shadow.remove();
    this.shadowSprites.remove(shadow);
  }

  /** Called by collide when a brick is destroyed. */
  private void destroyBrick(int x, int y) {
    Brick brick = this.bricks[y][x];
    brick.remove();
    this.removeShadow(brick.shadow);
    this.bricks[y][x] = null;
  }

  /** Chooses the type of a new powerup barrel, weighted by probability. */
  public Powerup choosePowerupType() {
    // The portal to the next level can't be generated unless there are only
    // a few bricks remaining, at which point it becomes very likely
    int portalWeight = this.bricksRemaining > 20 || this.portalActive ? 0 : 20;
    int[] weights = {6, 6, 6, 6, 6, 6, 6, 2, portalWeight};

    int total = 0;
    for (int weight : weights) {
      total += weight;
    }
    int pick = Random.randomInt(0, total - 1);
    for (int type = 0; type < weights.length; type++) {
      pick -= weights[type];
      if (pick < 0) {
        return Powerup.values()[type];
      }
    }
    return Powerup.EXTEND_BAT;
  }

  /** The sound effect played when a powerup is collected, if it has one. */
  public String soundForPowerup(Powerup type) {
    return switch (type) {
      case EXTEND_BAT -> "bat_extend0";
      case GUN -> "bat_gun0";
      case MAGNET -> "magnet0";
      case SMALL_BAT -> "bat_small0";
      case EXTRA_LIFE -> "extra_life0";
      case FAST_BALLS -> "speed_up0";
      case SLOW_BALLS -> "powerup0";
      case MULTI_BALL -> "multiball0";
      case PORTAL -> null;
    };
  }

  /** Replaces every ball with three new balls. */
  public void multiball() {
    List<Ball> newBalls = new ArrayList<>();
    for (Ball ball : this.balls) {
      newBalls.addAll(ball.generateMultiballs());
      ball.remove();
      this.removeShadow(ball.shadow);
    }
    this.balls.clear();
    for (Ball ball : newBalls) {
      this.addBall(ball);
    }
  }

  public void changeAllBallSpeeds(int change) {
    for (Ball ball : this.balls) {
      ball.speed = Operators.constrain(ball.speed + change, BALL_MIN_SPEED, BALL_MAX_SPEED);
    }
  }

  public void activatePortal() {
    this.portalActive = true;
    this.playSound("portal_exit0");
  }

  /**
   * Checks whether a ball or a bullet moving in the given direction collides
   * with a wall or a brick. Destructible bricks take damage, and the method
   * returns where the collision happened, or null if there was none.
   */
  public Collision collide(double x, double y, double dx, double dy, int r) {
    if (dx < 0 && x < LEFT_EDGE + r) {
      return new Collision(LEFT_EDGE, y, true, CollisionType.WALL);
    }
    if (dx > 0 && x > RIGHT_EDGE - r) {
      return new Collision(RIGHT_EDGE, y, true, CollisionType.WALL);
    }
    if (dy < 0 && y < TOP_EDGE + r) {
      return new Collision(x, TOP_EDGE, true, CollisionType.WALL);
    }

    // Work out the range of brick rows and columns that the ball overlaps
    int x0 = Math.max(0, (int) Math.floor((x - BRICKS_X_START - r) / (double) BRICK_WIDTH));
    int y0 = Math.max(0, (int) Math.floor((y - BRICKS_Y_START - r) / (double) BRICK_HEIGHT));
    int x1 = Math.min(this.numCols - 1, (int) Math.floor((x - BRICKS_X_START + r) / (double) BRICK_WIDTH));
    int y1 = Math.min(this.numRows - 1, (int) Math.floor((y - BRICKS_Y_START + r) / (double) BRICK_HEIGHT));

    for (int yb = y0; yb <= y1; yb++) {
      for (int xb = x0; xb <= x1; xb++) {
        Brick brick = this.bricks[yb][xb];
        if (brick != null) {
          double[] c = brickCollide(x, y, xb, yb, r);

          if (c != null) {
            double centreX = xb * BRICK_WIDTH + BRICKS_X_START + BRICK_WIDTH / 2.0;
            double centreY = yb * BRICK_HEIGHT + BRICKS_Y_START + BRICK_HEIGHT / 2.0;

            CollisionType collisionType = CollisionType.BRICK;

            // Brick 12 (brickc) requires a hit to turn into brick 11
            // (brickb), brick 13 (brickd) is indestructible
            if (brick.id >= 12) {
              if (brick.id == 13) {
                collisionType = CollisionType.INDESTRUCTIBLE_BRICK;
              }
              this.addImpact(new Impact(centreX, centreY, 13));
              if (brick.id == 12) {
                // Just like in the original, the sprite is not changed here:
                // the impact animation covers the brick
                brick.id = 11;
              }
            } else {
              this.addImpact(new Impact(centreX, centreY, brick.id));

              if (Random.random() < POWERUP_CHANCE) {
                this.addBarrel(new Barrel(this, centreX, centreY));
              }

              this.destroyBrick(xb, yb);

              this.bricksRemaining -= 1;
              if (this.bricksRemaining == 0) {
                this.activatePortal();
              }

              this.score += 10;
            }

            return new Collision(c[0], c[1], false, collisionType);
          }
        }
      }
    }

    return null;
  }

  /**
   * Does the ball (x, y, radius) collide with the brick at the given grid
   * position? Returns the point at which the collision occurred.
   */
  private static double[] brickCollide(double x, double y, int gridX, int gridY, int r) {
    // Get the ball extent as a square
    double x0 = x - r;
    double y0 = y - r;
    double x1 = x + r;
    double y1 = y + r;

    // Get the brick's left, top, right and bottom coordinates
    double xb0 = gridX * BRICK_WIDTH + BRICKS_X_START;
    double yb0 = gridY * BRICK_HEIGHT + BRICKS_Y_START;
    double xb1 = xb0 + BRICK_WIDTH;
    double yb1 = yb0 + BRICK_HEIGHT;

    double xbc = (xb0 + xb1) / 2;
    double ybc = (yb0 + yb1) / 2;

    // Detecting a bounce off the side of the brick
    if (x1 > xb0 && x0 < xb1 && y > yb0 && y < yb1) {
      return x < xbc ? new double[] {xb0, y} : new double[] {xb1, y};
    }

    // Detecting a bounce off the top or bottom of the brick
    if (x > xb0 && x < xb1 && y1 > yb0 && y0 < yb1) {
      return y < ybc ? new double[] {x, yb0} : new double[] {x, yb1};
    }

    // Get the closest brick corner
    double[][] corners = {{xb0, yb0}, {xb1, yb0}, {xb0, yb1}, {xb1, yb1}};
    double[] closest = corners[0];
    double closestDistanceSq = Double.MAX_VALUE;
    for (double[] corner : corners) {
      double distanceSq = (x - corner[0]) * (x - corner[0]) + (y - corner[1]) * (y - corner[1]);
      if (distanceSq < closestDistanceSq) {
        closestDistanceSq = distanceSq;
        closest = corner;
      }
    }

    // Check if we are actually overlapping with the nearest corner
    if (Math.sqrt(closestDistanceSq) < r) {
      return closest;
    }
    return null;
  }

  /** Sets a sprite position from pixel coordinates (top-left origin). */
  protected void setPixelPosition(Sprite sprite, double x, double y) {
    sprite.setPosition(x - WIDTH / 2.0, HEIGHT / 2.0 - y);
  }

  /** Doors, shadows, bricks, moving objects, impacts, then the UI. */
  private int drawLayer(Sprite sprite) {
    if (sprite instanceof UISprite || sprite == this.scoreText) return 5;
    if (sprite instanceof Impact) return 4;
    if (sprite instanceof Brick) return 2;
    if (sprite instanceof KxActor) return 3;
    return this.shadowSprites.contains(sprite) ? 1 : 0;
  }
}
