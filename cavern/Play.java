package cavern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.openpatch.scratch.KeyCode;
import org.openpatch.scratch.Random;
import org.openpatch.scratch.Sprite;
import org.openpatch.scratch.Stage;
import org.openpatch.scratch.Window;

public class Play extends Stage {

  public static final int WIDTH = 800;
  public static final int HEIGHT = 480;
  public static final int NUM_ROWS = 18;
  public static final int NUM_COLUMNS = 28;
  public static final int LEVEL_X_OFFSET = 50;
  public static final int GRID_BLOCK_SIZE = 25;

  private static final String[][] LEVELS = {
    {
      "XXXXX     XXXXXXXX     XXXXX",
      "",
      "",
      "",
      "",
      "   XXXXXXX        XXXXXXX   ",
      "",
      "",
      "   XXXXXXXXXXXXXXXXXXXXXX   ",
      "",
      "",
      "XXXXXXXXX          XXXXXXXXX",
      "",
      ""
    },
    {
      "XXXX    XXXXXXXXXXXX    XXXX",
      "",
      "",
      "",
      "    XXXXXXXXXXXXXXXXXXXX    ",
      "",
      "",
      "XXXXXX                XXXXXX",
      "      X              X      ",
      "       X            X       ",
      "        X          X        ",
      "         X        X         ",
      "",
      ""
    },
    {
      "XXXX    XXXX    XXXX    XXXX",
      "",
      "",
      "",
      "  XXXXXXXX        XXXXXXXX  ",
      "",
      "",
      "XXXX      XXXXXXXX      XXXX",
      "",
      "",
      "    XXXXXX        XXXXXX    ",
      "",
      ""
    }
  };

  public int timer = 0;
  public int level = -1;

  public Player player;

  private final List<Integer> pendingEnemies = new ArrayList<>();

  private int levelColour = -1;
  private String[] grid = new String[NUM_ROWS];
  private Status status;
  private boolean frozen = false;
  private boolean soundOn;

  /** A sprite owns the music so its volume is independent of sound effects. */
  protected final Sprite theme = new Sprite();

  private static boolean spaceDown = false;
  private boolean spaceJustPressed = false;

  public Play() {
    this(true);
  }

  public Play(boolean withPlayer) {
    this.theme.addSound("theme", "cavern/assets/music/theme.wav");
    this.soundOn = withPlayer;

    for (int i = 0; i < 4; i++) {
      this.addBackdrop("bg" + i, String.format("cavern/assets/images/bg%d.png", i));
    }

    this.addSound("level0", "cavern/assets/sounds/level0.wav");
    this.addSound("over0", "cavern/assets/sounds/over0.wav");
    this.addSound("die0", "cavern/assets/sounds/die0.wav");
    this.addSound("jump0", "cavern/assets/sounds/jump0.wav");
    this.addSound("bonus0", "cavern/assets/sounds/bonus0.wav");
    this.addSound("score0", "cavern/assets/sounds/score0.wav");
    for (int i = 0; i < 4; i++) {
      this.addSound("blow" + i, String.format("cavern/assets/sounds/blow%d.wav", i));
      this.addSound("laser" + i, String.format("cavern/assets/sounds/laser%d.wav", i));
      this.addSound("ouch" + i, String.format("cavern/assets/sounds/ouch%d.wav", i));
      this.addSound("pop" + i, String.format("cavern/assets/sounds/pop%d.wav", i));
      this.addSound("trap" + i, String.format("cavern/assets/sounds/trap%d.wav", i));
    }

    if (withPlayer) {
      this.player = new Player(this);
      this.add(this.player);
    }

    this.status = new Status(this);

    this.nextLevel();
  }

  /** Is there a level grid block at these coordinates? */
  public boolean block(int x, int y) {
    int gridX = Math.floorDiv(x - LEVEL_X_OFFSET, GRID_BLOCK_SIZE);
    int gridY = Math.floorDiv(y, GRID_BLOCK_SIZE);
    if (gridY > 0 && gridY < NUM_ROWS) {
      String row = this.grid[gridY];
      return gridX >= 0 && gridX < NUM_COLUMNS && row.length() > 0 && row.charAt(gridX) != ' ';
    }
    return false;
  }

  public boolean isFrozen() {
    return this.frozen;
  }

  public boolean spaceJustPressed() {
    return this.spaceJustPressed;
  }

  /** Plays a random sound variation, e.g. blow0 to blow3. */
  public void playRandom(String name, int count) {
    if (this.player != null) {
      this.playSound(name + Random.randomInt(0, count - 1));
    }
  }

  /** Likelihood per frame of each robot firing a bolt. */
  public double fireProbability() {
    return 0.001 + 0.0001 * Math.min(100, this.level);
  }

  /** Maximum number of enemies on-screen at once. */
  public int maxEnemies() {
    return Math.min((this.level + 6) / 2, 8);
  }

  public void nextLevel() {
    this.levelColour = (this.levelColour + 1) % 4;
    this.level += 1;
    this.switchBackdrop("bg" + this.levelColour);

    // Clear the level
    this.remove(Block.class);
    this.remove(Fruit.class);
    this.remove(Bolt.class);
    this.remove(Robot.class);
    this.remove(Pop.class);
    this.remove(Orb.class);

    // Set up grid. The last row is a copy of the first row.
    String[] level = LEVELS[this.level % LEVELS.length];
    this.grid = new String[NUM_ROWS];
    for (int i = 0; i < NUM_ROWS; i++) {
      if (i < level.length) {
        this.grid[i] = level[i];
      } else if (i == level.length) {
        this.grid[i] = level[0];
      } else {
        this.grid[i] = "";
      }
    }

    this.timer = -1;

    if (this.player != null) {
      this.player.reset();
    }

    // Set up the list of pending enemies
    int numEnemies = 10 + this.level;
    int numStrongEnemies = 1 + (int) (this.level / 1.5);
    int numWeakEnemies = numEnemies - numStrongEnemies;
    this.pendingEnemies.clear();
    for (int i = 0; i < numStrongEnemies; i++) {
      this.pendingEnemies.add(Robot.TYPE_AGGRESSIVE);
    }
    for (int i = 0; i < numWeakEnemies; i++) {
      this.pendingEnemies.add(Robot.TYPE_NORMAL);
    }
    Collections.shuffle(this.pendingEnemies);

    if (this.soundOn) {
      this.playSound("level0");
    }

    this.drawBlocks();
  }

  private void drawBlocks() {
    for (int rowY = 0; rowY < NUM_ROWS; rowY++) {
      String row = this.grid[rowY];
      if (row.length() > 0) {
        int x = LEVEL_X_OFFSET;
        for (int col = 0; col < row.length(); col++) {
          if (row.charAt(col) != ' ') {
            var block =
                new Block(
                    this,
                    x + GRID_BLOCK_SIZE / 2,
                    rowY * GRID_BLOCK_SIZE + GRID_BLOCK_SIZE / 2,
                    this.level);
            this.add(block);
            block.goToBackLayer();
          }
          x += GRID_BLOCK_SIZE;
        }
      }
    }
  }

  /** Find a spawn location for a robot by checking the top row for empty spots. */
  private int getRobotSpawnX() {
    int r = Random.randomInt(0, NUM_COLUMNS - 1);
    for (int i = 0; i < NUM_COLUMNS; i++) {
      int gridX = (r + i) % NUM_COLUMNS;
      if (this.grid[0].charAt(gridX) == ' ') {
        return GRID_BLOCK_SIZE * gridX + LEVEL_X_OFFSET + 12;
      }
    }
    return WIDTH / 2;
  }

  @Override
  public void run() {
    boolean space = this.isKeyPressed(KeyCode.SPACE);
    this.spaceJustPressed = space && !spaceDown;
    spaceDown = space;

    // The theme music keeps playing, like the music channel of the original
    this.theme.playSound("theme");
    this.theme.setVolume(30);

    this.status.update();

    if (this.frozen) {
      return;
    }

    this.timer += 1;

    if (this.player != null && this.player.lives < 0) {
      this.frozen = true;
      this.playSound("over0");
      var over = new Sprite();
      over.addCostume("over", "cavern/assets/images/over.png");
      over.setPosition(0, 0);
      over.goToFrontLayer();
      this.add(over);
      return;
    }

    // Every 100 frames, create a random fruit
    if (this.timer % 100 == 0 && (!this.pendingEnemies.isEmpty() || this.count(Robot.class) > 0)) {
      this.add(new Fruit(this, Random.randomInt(70, 730), Random.randomInt(75, 400), Robot.TYPE_NORMAL));
    }

    // Every 81 frames, create a robot if there are pending enemies
    if (this.timer % 81 == 0
        && !this.pendingEnemies.isEmpty()
        && this.count(Robot.class) < this.maxEnemies()) {
      int robotType = this.pendingEnemies.remove(this.pendingEnemies.size() - 1);
      var robot = new Robot(this, this.getRobotSpawnX(), -30, robotType);
      this.add(robot);
    }

    // End level if there is nothing left to do
    if (this.pendingEnemies.isEmpty()
        && this.count(Fruit.class) == 0
        && this.count(Robot.class) == 0
        && this.count(Pop.class) == 0) {
      boolean trappedOrb = false;
      for (Orb orb : this.find(Orb.class)) {
        if (orb.trappedEnemyType != null) {
          trappedOrb = true;
          break;
        }
      }
      if (!trappedOrb) {
        this.nextLevel();
      }
    }
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
