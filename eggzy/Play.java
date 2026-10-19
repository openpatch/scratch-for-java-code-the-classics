package eggzy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import org.openpatch.scratch.Layer;
import org.openpatch.scratch.Random;
import org.openpatch.scratch.Sprite;
import org.openpatch.scratch.Stage;
import org.openpatch.scratch.TextAlign;
import org.openpatch.scratch.UISprite;
import org.openpatch.scratch.Vector2;
import org.openpatch.scratch.Window;
import org.openpatch.scratch.extensions.text.BitmapText;
import org.openpatch.scratch.extensions.tiled.MapObject;
import org.openpatch.scratch.extensions.tiled.TiledMap;

/**
 * The play state: the level with its tiles, doors, gems and enemies, the
 * player with its ghost replays, and the status bar. All positions are kept
 * in the original game's pixel coordinates, see {@link EzActor}.
 *
 * <p>The level background and tiles are stamped onto the stage's background
 * layer once per level, so they cost nothing per frame. The dynamic objects
 * are sprites, kept in the original game's draw order: ghosts, doors,
 * animations, the player with its flame, gems and enemies, with the status
 * bar on top as user interface sprites.
 */
public class Play extends Stage {

  public static final int WIDTH = 825;
  public static final int HEIGHT = 550;

  public static final String[] LEVEL_SEQUENCE = {
    "starter1.tmx", "starter2.tmx", "starter3.tmx", "starter4.tmx",
    "forest1.tmx", "forest2.tmx", "forest3.tmx", "forest4.tmx", "forest9.tmx",
    "castle1.tmx", "castle2.tmx", "castle3.tmx", "castle4.tmx",
    "castle5.tmx", "castle6.tmx", "castle7.tmx", "castle8.tmx",
    "forest5.tmx", "forest6.tmx", "forest7.tmx", "forest8.tmx"
  };

  public static final int GRID_BLOCK_SIZE = 25;
  public static final int LEVEL_Y_BOUNDARY = -100;

  /** Change to 1, 2 or 3 to start with enemies/more enemies and less bonus time for gems. */
  public static final int INITIAL_LEVEL_CYCLE = 0;

  public static final int INITIAL_TIME_REMAINING = 15;
  public static final double INITIAL_PICKUP_TIME_BONUS = 2;
  public static final int STOMP_ENEMY_TIME_BONUS = 3;

  // Constants affecting player movement
  public static final int COYOTE_TIME = 6;
  public static final int JUMP_VEL_Y = -10;
  public static final int WALL_JUMP_X_VEL = 8;
  public static final int WALL_JUMP_COYOTE_TIME = 15;
  public static final int CACHE_JUMP_INPUT_TIME = 5;
  public static final int PLAYER_WIDTH = 20;
  public static final int PLAYER_HEIGHT = 40;

  // Anchors: the Y value is a pixel offset from the top of the costume, or
  // EzActor.ANCHOR_MIDDLE / EzActor.ANCHOR_BOTTOM
  public static final double ANCHOR_CENTRE = EzActor.ANCHOR_MIDDLE;
  public static final double ANCHOR_CENTRE_BOTTOM = EzActor.ANCHOR_BOTTOM;
  public static final double ANCHOR_PLAYER = 60;
  public static final double ANCHOR_FLAME = 78;
  public static final double ANCHOR_FLAME_DASH = 130;

  // There are four types of enemy per biome. Some properties are the same
  // between the forest/castle equivalents, some are different.
  public static final String[][] ENEMY_SPRITE_NAMES = {
    {"fly", "mghost", "triffid", "bigbloom"}, {"robot0", "robot1", "robot2", "robot3"}
  };
  public static final boolean[][] ENEMY_TYPES_FLYING = {
    {true, true, false, true}, {true, true, false, false}
  };
  public static final int[][] ENEMY_TYPES_WIDTH_OVERRIDES = {
    {30, 50, 50, 50}, {30, 50, 48, 50}
  };
  public static final int[][] ENEMY_TYPES_HEIGHT_OVERRIDES = {
    {30, 65, 70, 90}, {40, 40, 60, 120}
  };
  public static final double[][] ENEMY_TYPES_ANCHOR_Y = {
    {60, EzActor.ANCHOR_BOTTOM, EzActor.ANCHOR_BOTTOM, EzActor.ANCHOR_BOTTOM},
    {40, 40, 95, EzActor.ANCHOR_BOTTOM}
  };
  public static final int[] ENEMY_TYPES_HEALTH = {1, 3, 1, 3};
  public static final int[] ENEMY_TYPES_SPEED = {2, 1, 2, 1};

  public static final String TILEMAPS = "eggzy/assets/tilemaps/";
  public static final String TILE_LAYER = "Tile Layer 1";
  public static final String OBJECT_LAYER = "Object Layer 1";

  public final Controls controls;
  public final Player player;

  public final List<Door> doors = new ArrayList<>();
  public final List<Animation> animations = new ArrayList<>();
  public final List<Gem> gems = new ArrayList<>();
  public final List<Enemy> enemies = new ArrayList<>();
  public final List<GhostPlayer> ghostPlayers = new ArrayList<>();

  public int timer = 0;
  public int timeRemaining = INITIAL_TIME_REMAINING * 60;
  public double timePickupBonus = INITIAL_PICKUP_TIME_BONUS;
  public int gainedTimeTimer = 0;

  public int levelIndex = INITIAL_LEVEL_CYCLE * LEVEL_SEQUENCE.length - 1;
  public String levelText = "";
  public boolean exitOpen = false;

  private TiledMap levelMap;
  private final List<Rect> blockRects = new ArrayList<>();
  private String backgroundImagePath;
  private int backgroundYOffset;

  private final BitmapText levelTextDisplay;
  private final BitmapText timeTextDisplay;

  /** A sprite owns the in-game music and its volume. */
  private final Sprite ingameTheme = new Sprite();

  public Play(Controls controls) {
    this.ingameTheme.addSound("ingame_theme", "eggzy/assets/music/ingame_theme.ogg");
    this.controls = controls;
    this.setColor(0, 0, 0);

    // The Tiled map and its objects live in map coordinates, with the map's
    // top-left at the stage's origin (x right, y up). The camera shows the
    // whole level-sized map, so a pixel of the original game is at the same
    // place on screen
    this.getCamera().setPosition(WIDTH / 2.0, -HEIGHT / 2.0);

    this.addSound("collect0", "eggzy/assets/sounds/collect0.ogg");
    this.addSound("gameover0", "eggzy/assets/sounds/gameover0.ogg");
    this.addSound("jump0", "eggzy/assets/sounds/jump0.ogg");
    this.addSound("new_wave0", "eggzy/assets/sounds/new_wave0.ogg");
    this.addSound("player_death0", "eggzy/assets/sounds/player_death0.ogg");
    for (int i = 0; i < 5; i++) {
      this.addSound(
          "enemy_death" + i, String.format("eggzy/assets/sounds/enemy_death%d.ogg", i));
      this.addSound(
          "enemy_take_damage" + i,
          String.format("eggzy/assets/sounds/enemy_take_damage%d.ogg", i));
      this.addSound(
          "jump_long" + i, String.format("eggzy/assets/sounds/jump_long%d.ogg", i));
    }

    // The gem class is told that a new game has started, so it can reset the
    // next gem type variable
    Gem.newGame();

    this.player = new Player(this, controls);
    this.getSorting().by(Comparator.comparingInt(this::drawLayer));

    for (List<Replays.Entry> replay : Replays.all) {
      this.ghostPlayers.add(new GhostPlayer(this, replay));
    }
    for (GhostPlayer ghost : this.ghostPlayers) {
      this.add(ghost);
    }

    // The blue bar behind the text area, the frame around it and the time
    // remaining display, drawn above everything else
    this.addUiSprite("uibar", WIDTH / 2.0, 500 + 25);
    this.addUiSprite("text_area_frame", WIDTH / 2.0, 500 + 25);
    this.addUiSprite("status_back", WIDTH / 2.0 - 297 / 2.0 + 297 / 2.0, 77 / 2.0);
    // The level text, centred in the text area, and the time remaining,
    // centred in the status bar, both on top of everything else
    this.levelTextDisplay = EzActor.bitmapText("font");
    this.levelTextDisplay.setAlign(TextAlign.CENTER);
    this.levelTextDisplay.setPosition(0, HEIGHT / 2.0 - 508);
    this.levelTextDisplay.showText("");
    this.add(this.levelTextDisplay);
    this.timeTextDisplay = EzActor.bitmapText("font");
    this.timeTextDisplay.setAlign(TextAlign.CENTER);
    this.timeTextDisplay.setPosition(0, HEIGHT / 2.0 - 10);
    this.timeTextDisplay.showText("");
    this.add(this.timeTextDisplay);

    // The in-game music starts when the play state is entered. It keeps
    // playing on the game over screen, which receives the same music sprite.
    this.ingameTheme.playSound("ingame_theme");

    this.nextLevel();
  }

  private void addUiSprite(String image, double centreX, double centreY) {
    UISprite sprite = new UISprite();
    sprite.addCostume(image, EzActor.IMAGES + image + ".png");
    sprite.switchCostume(image);
    setPixelPosition(sprite, centreX, centreY);
    this.add(sprite);
  }

  @Override
  public void run() {
    Game.totalFrames += 1;
    this.controls.update();

    // The in-game music loops forever in the original
    this.ingameTheme.playSound("ingame_theme");
    this.ingameTheme.setVolume(20);

    if (this.timeRemaining <= 0) {
      this.playSound("gameover0");

      // Add the replay data for this game, keeping only the ten longest
      Replays.add(this.player.replayData);

      Window.getInstance().setStage(new GameOver(this.timer, this.ingameTheme));
      return;
    }

    this.timer += 1;
    this.gainedTimeTimer -= 1;

    if (this.timeRemaining > 0) {
      this.timeRemaining -= 1;
    }

    // Remove expired enemies, dash trails, gems and animations
    this.enemies.removeIf(
        enemy -> {
          if (enemy.imageTop() >= HEIGHT) {
            enemy.remove();
            return true;
          }
          return false;
        });
    this.animations.removeIf(
        animation -> {
          if (animation.finished()) {
            animation.remove();
            return true;
          }
          return false;
        });
    this.gems.removeIf(
        gem -> {
          if (gem.collected) {
            gem.remove();
            return true;
          }
          return false;
        });

    // Check whether to open the exit door and whether the player has left
    if (this.exitOpen) {
      if (this.player.centerX() >= WIDTH) {
        this.nextLevel();
      }
    } else if (this.gems.isEmpty()) {
      // All gems collected, open the exit door
      this.exitOpen = true;
      for (Door door : this.doors) {
        door.open();
      }
    }
    this.updateStatus();
  }

  /** Updates the status bar; actor behavior runs in the sprite callbacks. */
  private void updateStatus() {
    // Show the level text in the text area
    this.levelTextDisplay.showText(this.levelText);

    // Show the time remaining, using the bright font if the player has just
    // gained time
    this.timeTextDisplay.setFont(
        EzActor.IMAGES + (this.gainedTimeTimer < 0 ? "font" : "fontbr") + "%03d.png");
    this.timeTextDisplay.showText(
        String.format(Locale.ROOT, "%.1f", this.timeRemaining / 60.0));
  }

  public void nextLevel() {
    this.levelIndex += 1;

    // If the new level is a repeat of the first level, reduce the gem time
    // bonus (to a minimum of half a second)
    if (this.levelIndex != 0 && this.levelIndex % LEVEL_SEQUENCE.length == 0) {
      if (this.timePickupBonus > 1) {
        this.timePickupBonus -= 1;
      } else if (this.timePickupBonus == 1) {
        this.timePickupBonus = 0.5;
      }
    }

    for (Door door : this.doors) {
      door.remove();
    }
    this.doors.clear();
    for (Gem gem : this.gems) {
      gem.remove();
    }
    this.gems.clear();
    for (Enemy enemy : this.enemies) {
      enemy.remove();
    }
    this.enemies.clear();
    for (Animation animation : this.animations) {
      animation.remove();
    }
    this.animations.clear();
    this.levelText = "";

    // Set up level
    Vector2 playerStartPos =
        this.loadLevel(LEVEL_SEQUENCE[this.levelIndex % LEVEL_SEQUENCE.length]);

    this.exitOpen = false;

    // The doors go behind the animations, which go behind the player, which
    // goes behind the gems and the enemies - see the original draw order
    for (Door door : this.doors) {
      this.add(door);
    }
    this.player.remove();
    this.add(this.player);
    this.player.flame.remove();
    this.add(this.player.flame);
    for (Gem gem : this.gems) {
      this.add(gem);
    }
    for (Enemy enemy : this.enemies) {
      this.add(enemy);
    }

    if (this.player != null) {
      this.player.newLevel(playerStartPos);
    }

    // Generate collidable areas
    this.generateBlockRects();

    if (this.player != null) {
      this.player.reset();
    }

    this.playSound("new_wave0");
  }

  /** Loads a Tiled level file, returning the player start position. */
  private Vector2 loadLevel(String filename) {
    Vector2 playerStartPos = new Vector2(0, 0);

    // 0 for the first time through the levels, 1 for the second, etc
    int levelCycle = this.levelIndex / LEVEL_SEQUENCE.length;

    // The level's Tiled map provides its properties, its tile data and its
    // objects, and stamps its tiles. Its top-left sits at the stage's
    // origin, like the coordinates of the map objects, and the stage's
    // camera (see the constructor) shows the whole level
    this.levelMap = new TiledMap(TILEMAPS + filename, this);

    // Load background
    this.backgroundImagePath =
        EzActor.IMAGES + this.levelMap.getProperty("Background") + ".png";
    this.backgroundYOffset =
        Integer.parseInt(this.levelMap.getProperty("Background Offset Y", "0"));

    // Load biome (used for determining which types of enemies and doors to
    // generate)
    String biomeName = this.levelMap.getProperty("biome");
    Biome biome = Biome.valueOf(biomeName.toUpperCase(Locale.ROOT));

    // Default level name text - may be replaced by tutorial text below
    this.levelText = "LEVEL " + (this.levelIndex + 1);

    // Set up level tutorial text - only the first time we go round the levels
    String tutorialText = this.levelMap.getProperty("TutorialText", "");
    if (levelCycle == 0 && !tutorialText.isEmpty()) {
      this.levelText =
          tutorialText
              .replace("{DASH}", this.controls.buttonName("dash"))
              .replace("{JUMP}", this.controls.buttonName("jump"));
    }

    // Read the object layer: the player start position, gems, enemies and
    // doors
    for (MapObject object : this.levelMap.getObjectsFromLayer(OBJECT_LAYER)) {
      String objectName = object.name;
      int objectX = (int) object.x;
      // The map object's y is in stage coordinates (upwards), the game uses
      // the original's pixel coordinates (downwards)
      int objectY = (int) -object.y;

      if (objectName.equals("PlayerStart")) {
        playerStartPos = new Vector2(objectX, objectY);
      } else if (objectName.equals("Gem")) {
        this.gems.add(new Gem(this, objectX, objectY));
      } else if (objectName.contains("Enemy")) {
        // Enemies have names such as "EnemyR00": L/R indicate the facing
        // direction, the first number the enemy type, and the last number
        // the level cycle during which they first show up
        int enemyLevelCycle =
            Integer.parseInt(objectName.substring(objectName.length() - 1));
        int appearanceCount = levelCycle - enemyLevelCycle + 1;
        if (appearanceCount >= 1) {
          int facing =
              objectName.charAt(objectName.length() - 3) == 'R' ? 1 : -1;
          int enemyType =
              Integer.parseInt(objectName.substring(objectName.length() - 2,
                  objectName.length() - 1));
          this.enemies.add(new Enemy(this, objectX, objectY,
              enemyType, biome, facing, appearanceCount));
        }
      } else if (objectName.contains("Door")) {
        String variant = propertyValue(object, "Variant");
        String doorBiomeName = propertyValue(object, "Biome");
        if (variant == null) {
          variant = "0";
        }
        if (doorBiomeName == null) {
          doorBiomeName = biomeName;
        }
        boolean entrance = objectName.contains("Entrance");
        this.doors.add(new Door(this, objectX, objectY,
            doorBiomeName, variant, entrance));
      }
    }

    // Draw the background and the level tiles onto the background layer
    this.stampLevel();

    return playerStartPos;
  }

  /**
   * Builds the collidable rectangles: horizontal runs of solid tiles are
   * merged, then rectangles with an equal-width rectangle directly below are
   * joined. Blocks at the top of the level are extended upwards above the
   * level, so nothing can stand on top of the trees off screen.
   */
  private void generateBlockRects() {
    this.blockRects.clear();
    Rect current = null;

    // Horizontal rows
    for (int gy = 0; gy < this.levelMap.getHeight(); gy++) {
      for (int gx = 0; gx < this.levelMap.getWidth(); gx++) {
        if (this.isSolidTile(gx, gy)) {
          double posX = gx * GRID_BLOCK_SIZE;
          double posY = gy * GRID_BLOCK_SIZE;
          if (current == null) {
            current = new Rect(posX, posY, GRID_BLOCK_SIZE, GRID_BLOCK_SIZE);
          } else {
            // Continue existing rectangle
            current.w += GRID_BLOCK_SIZE;
          }
        } else if (current != null) {
          this.blockRects.add(current);
          current = null;
        }
      }
      if (current != null) {
        this.blockRects.add(current);
        current = null;
      }
    }

    // Consolidate vertically: keep joining rectangles with rectangles of
    // equal width directly below, until there are no more such matches
    boolean anyFound = true;
    while (anyFound) {
      anyFound = false;
      search:
      for (Rect currentRect : this.blockRects) {
        for (Rect other : this.blockRects) {
          if (other.x == currentRect.x
              && other.w == currentRect.w
              && other.y == currentRect.y + currentRect.h) {
            currentRect.h += other.h;
            this.blockRects.remove(other);
            anyFound = true;
            break search;
          }
        }
      }
    }

    // Any rectangle aligning with the top of the level is extended above it
    for (Rect rect : this.blockRects) {
      if (rect.top() == 0) {
        double height = rect.h;
        rect.y = LEVEL_Y_BOUNDARY;
        rect.h = height + -LEVEL_Y_BOUNDARY;
      }
    }
  }

  /** Is the tile at the given cell solid, i.e. does it have collision shapes? */
  private boolean isSolidTile(int column, int row) {
    return this.levelMap.getTileCollisionShapes(TILE_LAYER, column, row).length > 0;
  }

  /** Stamps the background image and the level tiles onto the background. */
  private void stampLevel() {
    // Clears the background stamps of the previous level
    this.eraseAll();

    Sprite stamper = new Sprite();
    this.add(stamper);

    // Draw the background at (0, backgroundYOffset), the top-left corner,
    // in the coordinates of the map (x right from its top-left, y up)
    stamper.addCostume("bg", this.backgroundImagePath);
    stamper.switchCostume("bg");
    stamper.setPosition(stamper.getWidth() / 2.0,
        -(this.backgroundYOffset + stamper.getHeight() / 2.0));
    stamper.stamp(Layer.BACKGROUND);

    this.remove(stamper);

    // Draw the level tiles, from the map's tile layer
    this.levelMap.stampLayerToBackground(TILE_LAYER);
  }

  /** Is the given rectangle blocked by tiles, a closed door or the level edge? */
  public boolean positionBlocked(Rect rect) {
    for (Rect blockRect : this.blockRects) {
      if (rect.colliderect(blockRect)) {
        return true;
      }
    }

    for (Door door : this.doors) {
      if (!door.isFullyOpen() && door.imageRect().colliderect(rect)) {
        return true;
      }
    }

    // Don't allow going off the left side of the screen, or above the level.
    // Going off the right side is allowed, to reach the exit door.
    if (rect.left() <= 0 || rect.top() < LEVEL_Y_BOUNDARY) {
      return true;
    }

    return false;
  }

  /** Gains time, with an animation showing how much was gained. */
  public void gainTime(double time, double x, double y) {
    this.timeRemaining += (int) (time * 60);
    String timeAddedId = time == 0.5 ? "half" : Integer.toString((int) time);
    this.addAnimation(new Animation(x, y, "timer_plus_" + timeAddedId + "_{0}",
        14, 4, ANCHOR_CENTRE, 5, 34));
    this.addAnimation(new Animation(x, y, "pickup_{0}", 8, 4, ANCHOR_CENTRE, 0, -1));
    this.gainedTimeTimer = 20;
  }

  /**
   * Adds an animation to the stage, between the doors and the player, above
   * the previously added animations - the position it has in the original's
   * draw order.
   */
  public void addAnimation(Animation animation) {
    this.animations.add(animation);
    this.add(animation);
  }

  /**
   * Plays a sound effect, choosing randomly between count varieties named
   * name0 to name(count-1).
   */
  public void playSound(String name, int count) {
    this.playSound(name + Random.randomInt(0, count - 1));
  }

  /** Ghosts, doors, effects, player and flame, collectibles, then enemies. */
  private int drawLayer(Sprite sprite) {
    if (sprite instanceof GhostPlayer) return 0;
    if (sprite instanceof Door) return 1;
    if (sprite instanceof Animation) return 2;
    if (sprite == this.player) return 3;
    if (sprite == this.player.flame) return 4;
    if (sprite instanceof Gem) return 5;
    if (sprite instanceof Enemy) return 6;
    return 7;
  }

  /** Sets a sprite position from pixel coordinates (top-left origin). */
  public static void setPixelPosition(Sprite sprite, double x, double y) {
    sprite.setPosition(x - WIDTH / 2.0, HEIGHT / 2.0 - y);
  }

  /** Returns the value of a property of a map object, or null when absent. */
  private static String propertyValue(MapObject object, String name) {
    try {
      return object.getProperty(name);
    } catch (NoSuchElementException absent) {
      return null;
    }
  }
}
