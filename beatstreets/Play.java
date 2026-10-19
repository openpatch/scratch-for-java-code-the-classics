package beatstreets;

import java.util.ArrayList;
import java.util.List;
import org.openpatch.scratch.Random;
import org.openpatch.scratch.Sprite;
import org.openpatch.scratch.TextAlign;
import org.openpatch.scratch.Vector2;
import org.openpatch.scratch.Window;
import org.openpatch.scratch.extensions.text.BitmapText;

/**
 * The play state of the game: the scrolling level with its stages, the
 * player, enemies, weapons, scooters and powerups. All positions are kept in
 * the original's pixel coordinates, see {@link ScrollHeightActor}.
 *
 * <p>The draw order mirrors the original: the road, then the background
 * tiles, then the game objects sorted by their Y position (lowest on screen
 * first), and the UI on top.
 */
public class Play extends GameStage {

  /** The background tiles, in the order they appear along the level. */
  public static final String[] BACKGROUND_TILES = {
    // 1st row
    "wall_end1", "wall_fill1", "wall_fill5", "wall_fill2", "alley1", "wall_end6", "wall_fill7",
    "wall_fill5", "alley2", "wall_end3", "wall_fill3", "wall_fill4", "wall_fill8", "alley5",
    "wall_end2", "alley3", "wall_end4", "wall_fill6",
    // 2nd row
    "alley6", "wall_end8", "wall_fill4", "alley7", "wall_end5", "alley8", "set_pc_a1", "set_pc_a2",
    "alley9", "set_pc_b1", "set_pc_b2", "set_pc_b3", "wall_end3", "wall_fill3", "alley8",
    "set_pc_a1", "set_pc_a2", "wall_fill2",
    // 3rd row
    "con_start2", "con_end1a", "con_end2", "con_start2", "con_end1", "con_fill1", "con_end2a",
    "con_start2", "con_end1a", "con_fill1a", "con_end2", "set_pc_c1", "set_pc_c2", "set_pc_c3",
    "con_start1", "con_end1", "con_fill1", "con_fill2", "con_fill1a", "con_fill2a",
    // 4th row
    "wall_end1", "alley10", "steps_end1a", "steps_fill1a", "steps_fill2a", "steps_end2a",
    "flats_alley1", "steps_end1", "steps_end2", "flats_alley1", "flats_end1a", "steps_fill2",
    "steps_fill1", "flats_end2a", "flats_alley2", "set_pc_d1", "set_pc_d2", "set_pc_d3",
    "steps_end2a"
  };

  private static final String[] STOLEN_ITEMS = {
    "A SHIPMENT OF RASPBERRY\nPIS",
    "YOUR COPY OF CODE THE\nCLASSICS VOL 2",
    "THE COMPLETE WORKS OF\nSHAKESPEARE",
    "THE BLOCKCHAIN",
    "THE WORLD'S ENTIRE SUPPLY\nOF COVID VACCINES",
    "ALL OF YOUR SAVED GAME\nFILES",
    "YOUR DOG'S FLEA MEDICINE"
  };

  private static final String OUTRO_TEXT =
      "FOLLOWING THE DEFEAT OF\n"
          + "THE EVIL GANG, HUMANITY\n"
          + "ENTERED A NEW GOLDEN AGE\n"
          + "IN WHICH CRIME BECAME A\n"
          + "THING OF THE PAST. THE\n"
          + "WORD ITSELF WAS SOON\n"
          + "FORGOTTEN AND EVERYONE\n"
          + "HAD A BIG PARTY IN YOUR\n"
          + "HONOUR.\n"
          + "\nNICE JOB!";

  private final Controls controls;

  public final Player player;
  public final List<Enemy> enemies = new ArrayList<>();
  public final List<Weapon> weapons = new ArrayList<>();
  public final List<Scooter> scooters = new ArrayList<>();
  public final List<Powerup> powerups = new ArrayList<>();

  private final List<LevelStage> stages;
  private int stageIndex = -1;
  private int timer = 0;
  public int score = 0;

  private double scrollX = 0;
  private double maxScrollOffsetX = 0;
  private boolean scrolling = false;
  private double boundaryLeft = 0;

  private boolean textActive;
  private String currentText;
  private String displayedText = "";

  // The background
  private final ImageSprite road1 = new ImageSprite();
  private final ImageSprite road2 = new ImageSprite();
  private final ImageSprite[] tiles = new ImageSprite[6];

  // The UI
  private final ImageSprite arrow = new ScrollArrow();
  private final ImageSprite healthBar = new HealthBar();
  private final ImageSprite staminaBar = new StaminaBar();
  private final ImageSprite statusBar = new ImageSprite();
  private final ImageSprite[] lifeIcons = new ImageSprite[8];
  private final ImageSprite fade = new IntroFade();
  /** The score, drawn centred at the top of the screen. */
  private final ScoreText scoreText = new ScoreText();
  /** The intro/outro text lines, one text per line of the original. */
  private final List<TextLine> textLines = new ArrayList<>();

  public Play(Controls controls) {
    this.controls = controls;
    // Consume the button press that started the game, like the original,
    // which keeps using the same controls object
    controls.update();

    // The sound effects of the game, one file per entry. These have to be
    // added before the stages are set up, whose weapons and powerups play
    // their sounds when they are created.
    for (String name : new String[] {
        "barrel_hit0", "barrel_throw0", "chain_break0", "chain_drop0",
        "flyingkick_whoosh0", "health0", "portal_appear0", "portal_destroyed0",
        "portal_enemy_spawn0", "portal_hit0", "scooter_fall0", "scooter_hit0",
        "scooter_slow0", "stick_break0", "teletype0"}) {
      this.addSound(name, "beatstreets/assets/sounds/" + name + ".ogg");
    }
    for (int i = 0; i < 2; i++) {
      this.addSound("boss_punch" + i,
          String.format("beatstreets/assets/sounds/boss_punch%d.ogg", i));
      this.addSound("flyingkick_hit" + i,
          String.format("beatstreets/assets/sounds/flyingkick_hit%d.ogg", i));
      this.addSound("stick_hit" + i, String.format("beatstreets/assets/sounds/stick_hit%d.ogg", i));
    }
    for (int i = 0; i < 4; i++) {
      this.addSound("chain_hit" + i, String.format("beatstreets/assets/sounds/chain_hit%d.ogg", i));
      this.addSound("elbow_hit" + i, String.format("beatstreets/assets/sounds/elbow_hit%d.ogg", i));
      this.addSound("elbow_whoosh" + i,
          String.format("beatstreets/assets/sounds/elbow_whoosh%d.ogg", i));
      this.addSound("kick_hit" + i, String.format("beatstreets/assets/sounds/kick_hit%d.ogg", i));
      this.addSound("kick_whoosh" + i,
          String.format("beatstreets/assets/sounds/kick_whoosh%d.ogg", i));
      this.addSound("punch_hit" + i, String.format("beatstreets/assets/sounds/punch_hit%d.ogg", i));
      this.addSound("punch_whoosh" + i,
          String.format("beatstreets/assets/sounds/punch_whoosh%d.ogg", i));
      this.addSound("stick_whoosh" + i,
          String.format("beatstreets/assets/sounds/stick_whoosh%d.ogg", i));
    }
    for (int i = 0; i < 6; i++) {
      this.addSound("scooter_accelerate" + i,
          String.format("beatstreets/assets/sounds/scooter_accelerate%d.ogg", i));
    }

    this.player = new Player(this, controls);
    this.stages = this.setupStages();

    // Set up the intro text, selecting randomly from one of several stolen items
    this.textActive = Game.INTRO_ENABLED;
    this.currentText = "THE NOTORIOUS CRIME BOSS\nEBEN UPTON HAS STOLEN\n"
        + STOLEN_ITEMS[Random.randomInt(0, STOLEN_ITEMS.length - 1)]
        + "\n\n\nFIGHT TO RECLAIM WHAT\nHAS BEEN TAKEN!";

    // The road the fighters walk along, drawn twice to scroll
    for (ImageSprite road : new ImageSprite[] {this.road1, this.road2}) {
      road.showImage("road");
      this.add(road);
    }
    this.road1.layerKey = 0;
    this.road2.layerKey = 1;

    // The background tiles, a fixed sequence along the level of which only a
    // few are visible at a time
    for (int i = 0; i < this.tiles.length; i++) {
      this.tiles[i] = new ImageSprite();
      this.tiles[i].layerKey = 100 + i;
      this.tiles[i].hide();
      this.add(this.tiles[i]);
    }

    // The player and its separate shadow
    this.add(this.player);
    this.add(this.player.shadow);

    // The arrow shown when the level can be scrolled
    this.arrow.showImage("arrow");
    this.arrow.layerKey = 2000;
    this.arrow.hide();
    this.add(this.arrow);

    // The health and stamina bars, the status bar frame and the life icons
    this.healthBar.layerKey = 2001;
    this.staminaBar.layerKey = 2002;
    this.healthBar.hide();
    this.staminaBar.hide();
    this.add(this.healthBar);
    this.add(this.staminaBar);

    this.statusBar.showImage("status");
    this.statusBar.layerKey = 2003;
    this.setBlitPosition(this.statusBar, 0, 0);
    this.add(this.statusBar);

    for (int i = 0; i < this.lifeIcons.length; i++) {
      this.lifeIcons[i] = new LifeIcon(i);
      this.lifeIcons[i].layerKey = 2004 + i;
      this.lifeIcons[i].hide();
      this.add(this.lifeIcons[i]);
    }

    // Tint an opaque pixel black and scale it to cover the window. The blank
    // costume is transparent, so it cannot provide a fade overlay.
    this.fade.addCostume("black", "beatstreets/assets/images/road.png", 0, 0, 1, 1);
    this.fade.setTint(0, 0, 0);
    this.fade.switchCostume("black");
    this.fade.setSize(80000);
    this.fade.layerKey = 3000;
    this.fade.hide();
    this.add(this.fade);

    // The score in the status bar, and the intro/outro text, one text per
    // line, with the lines 35 pixels apart like the original
    this.scoreText.setAlign(TextAlign.CENTER);
    this.scoreText.setPosition(0, Game.HEIGHT / 2.0);
    this.add(this.scoreText);
    this.createTextLines();

    // Draw the game objects sorted by their Y position, above the background
    // and behind the UI
    this.getSorting().by((a, b) -> Double.compare(this.sortKey(a), this.sortKey(b)));
  }

  /** The key a sprite is drawn at: background, game objects, then UI. */
  private double sortKey(Sprite sprite) {
    if (sprite instanceof ShadowSprite shadow) {
      return 1000 + shadow.drawSortKey();
    }
    if (sprite instanceof ScrollHeightActor actor) {
      return 1000 + actor.drawSortKey();
    }
    if (sprite == this.scoreText) {
      return 2100;
    }
    if (sprite instanceof BitmapText) {
      return 4000;
    }
    if (sprite instanceof ImageSprite imageSprite) {
      return imageSprite.layerKey;
    }
    return 0;
  }

  public boolean isPlaying() {
    return Window.getInstance().getStage() == this && !this.textActive;
  }

  public double getScrollX() {
    return this.scrollX;
  }

  /** The boundary the fighters can walk within. */
  public double boundaryLeft() {
    return this.boundaryLeft;
  }

  public double boundaryRight() {
    return this.boundaryLeft + Game.WIDTH - 1;
  }

  public double boundaryTop() {
    return Game.MIN_WALK_Y;
  }

  public double boundaryBottom() {
    return Game.HEIGHT;
  }

  /**
   * Plays a sound, randomly choosing between the given number of varieties
   * (e.g. punch_whoosh0 to punch_whoosh3).
   */
  public void playSound(String name, int count) {
    this.playSound(name + Random.randomInt(0, count - 1));
  }

  @Override
  public void run() {
    super.run();

    this.controls.update();
    this.advanceLevel();

    if (this.player.lives <= 0 || this.checkWon()) {
      // Tell the enemies to die, which is how the scooter engine sound
      // effect gets turned off
      this.shutdown();
      this.switchStage(new GameOverStage(this.checkWon()));
      return;
    }

    this.drawBackground();
  }

  /** Advances dialogue, scrolling and level progress; sprites run themselves. */
  private void advanceLevel() {
    this.timer += 1;

    if (this.textActive) {
      // Every 6 frames, display an extra character of the text, and make a
      // sound if the new character is visible (as opposed to a space or
      // new line)
      if (this.timer % 6 == 0 && this.displayedText.length() < this.currentText.length()) {
        int lengthToDisplay = Math.min(this.timer / 6, this.currentText.length());
        this.displayedText = this.currentText.substring(0, lengthToDisplay);
        if (!Character.isWhitespace(
            this.displayedText.charAt(this.displayedText.length() - 1))) {
          this.playSound("teletype", 1);
        }
      }

      // Allow the player to skip/leave the text
      for (int button = 0; button < Controls.NUM_BUTTONS; button++) {
        if (this.controls.buttonPressed(button)) {
          this.textActive = false;
          this.timer = 0;
        }
      }

      return;
    }

    if (this.scrolling) {
      if (this.scrollX < this.maxScrollOffsetX) {
        // How far are we from reaching the new max scroll offset?
        double diff = this.maxScrollOffsetX - this.scrollX;
        // Scroll at 1-4px per frame depending on the player's distance from
        // the right edge
        double scrollSpeed = this.player.screenX / (Game.WIDTH / 4.0);
        scrollSpeed = Math.min(diff, scrollSpeed);
        this.scrollX += scrollSpeed;
        this.boundaryLeft = this.scrollX; // moves the entire rectangle
      } else {
        // Scrolling is complete
        this.scrolling = false;
      }
    } else {
      // Start scrolling if the player is near the right hand edge of the
      // screen and the level can be scrolled further
      double beginScrollBoundary = Game.WIDTH - 300;
      if (this.player.vpos.getX() - this.scrollX > beginScrollBoundary
          && this.scrollX < this.maxScrollOffsetX) {
        this.scrolling = true;

        // When we start scrolling, create the enemies for the current stage
        if (this.stageIndex < this.stages.size()) {
          this.createStageObjects(this.stages.get(this.stageIndex));
        }
      }
    }

    // Remove expired enemies and gain their score
    List<Enemy> expiredEnemies = new ArrayList<>();
    for (Enemy enemy : this.enemies) {
      if (enemy.lives <= 0) {
        expiredEnemies.add(enemy);
      }
    }
    for (Enemy enemy : expiredEnemies) {
      this.score += enemy.score;
      this.removeObject(enemy);
    }
    this.enemies.removeAll(expiredEnemies);

    // Remove expired scooters
    List<Scooter> expiredScooters = new ArrayList<>();
    for (Scooter scooter : this.scooters) {
      if (scooter.frame >= 200) {
        expiredScooters.add(scooter);
      }
    }
    for (Scooter scooter : expiredScooters) {
      this.removeObject(scooter);
    }
    this.scooters.removeAll(expiredScooters);

    // Remove broken weapons and ones which are off the left of the screen
    List<Weapon> expiredWeapons = new ArrayList<>();
    for (Weapon weapon : this.weapons) {
      if (weapon.isBroken() || weapon.screenX <= -200) {
        expiredWeapons.add(weapon);
      }
    }
    for (Weapon weapon : expiredWeapons) {
      this.removeObject(weapon);
    }
    this.weapons.removeAll(expiredWeapons);

    // Remove collected powerups, and ones off the left of the screen
    List<Powerup> expiredPowerups = new ArrayList<>();
    for (Powerup powerup : this.powerups) {
      if (powerup.collected || powerup.screenX <= -200) {
        expiredPowerups.add(powerup);
      }
    }
    for (Powerup powerup : expiredPowerups) {
      this.removeObject(powerup);
    }
    this.powerups.removeAll(expiredPowerups);

    // If there are no enemies and we've fully scrolled to the current stage's
    // max scroll position, start the next stage
    if (this.enemies.isEmpty() && this.scrollX == this.maxScrollOffsetX) {
      this.nextStage();
    }
  }

  /**
   * A stage is over when we've scrolled to its max scroll position and there
   * are no enemies left.
   */
  private void nextStage() {
    this.stageIndex += 1;
    if (this.stageIndex < this.stages.size()) {
      LevelStage stage = this.stages.get(this.stageIndex);
      this.maxScrollOffsetX = stage.maxScrollX;
      if (this.scrolling || this.maxScrollOffsetX <= this.scrollX) {
        // No scrolling needed (or already scrolling) - create the stage
        // objects right away
        this.createStageObjects(stage);
      }
    } else {
      // We've been through all the stages, show the outro text. After that,
      // checkWon() will return true and the game will be over.
      if (!this.textActive) {
        this.textActive = true;
        this.currentText = OUTRO_TEXT;
        this.displayedText = "";
        this.timer = 0;
        this.createTextLines();
      }
    }
  }

  /** Have we been through all the stages, and has the outro text finished? */
  public boolean checkWon() {
    return this.stageIndex >= this.stages.size() && !this.textActive;
  }

  private void createStageObjects(LevelStage stage) {
    // Copy the enemies list from the stage, and tell them they've been spawned
    this.enemies.clear();
    for (Enemy enemy : stage.enemies) {
      this.enemies.add(enemy);
      this.addObject(enemy);
    }
    for (Enemy enemy : stage.enemies) {
      enemy.spawned();
    }

    // Add the weapons and powerups from the stage to the game
    for (Weapon weapon : stage.weapons) {
      this.weapons.add(weapon);
      this.addObject(weapon);
    }
    for (Powerup powerup : stage.powerups) {
      this.powerups.add(powerup);
      this.addObject(powerup);
    }
  }

  /** Called by the portal when a spawned enemy is ready to appear. */
  public void spawnEnemy(Enemy enemy) {
    this.enemies.add(enemy);
    this.addObject(enemy);
    enemy.spawned();
  }

  /** Called when a scooterboy is knocked off his scooter. */
  public void addScooter(Scooter scooter) {
    this.scooters.add(scooter);
    this.addObject(scooter);
  }

  /** Called when a dying enemy drops a weapon. */
  public void addWeapon(Weapon weapon) {
    this.weapons.add(weapon);
    this.addObject(weapon);
  }

  private void addObject(ScrollHeightActor object) {
    this.add(object);
    if (object.shadow != null) {
      this.add(object.shadow);
    }
  }

  private void removeObject(ScrollHeightActor object) {
    object.remove();
    if (object.shadow != null) {
      object.shadow.remove();
    }
  }

  /** When the game is over, tell the enemies to die. */
  private void shutdown() {
    for (Enemy enemy : new ArrayList<>(this.enemies)) {
      enemy.died();
    }
  }

  /**
   * Creates one self-updating text per line of the current intro/outro text,
   * the lines 35 pixels apart like the original.
   */
  private void createTextLines() {
    for (TextLine line : this.textLines) {
      line.remove();
    }
    this.textLines.clear();
    String[] lines = this.currentText.split("\n", -1);
    for (int i = 0; i < lines.length; i++) {
      TextLine line = new TextLine(i);
      line.setAlign(TextAlign.LEFT);
      line.setPosition(50 - Game.WIDTH / 2.0, Game.HEIGHT / 2.0 - (50 + i * 35.0));
      this.textLines.add(line);
      this.add(line);
    }
  }

  /** The score, showing the current score each frame. */
  private class ScoreText extends BitmapText {
    private ScoreText() {
      super(Util.FONT);
      this.addGlyph('%', Util.BUTTON_GLYPH);
    }

    @Override
    public void run() {
      this.showText(String.format("%04d", Play.this.score));
    }
  }

  /**
   * One line of the intro/outro text, showing the typed progress of its line
   * each frame, and nothing once the text has been skipped.
   */
  private class TextLine extends BitmapText {
    private final int index;

    private TextLine(int index) {
      super(Util.FONT);
      this.addGlyph('%', Util.BUTTON_GLYPH);
      this.index = index;
    }

    @Override
    public void run() {
      if (!Play.this.textActive) {
        this.showText("");
        return;
      }
      String[] lines = Play.this.displayedText.split("\n", -1);
      this.showText(this.index < lines.length ? lines[this.index] : "");
    }
  }

  private void drawBackground() {
    // Draw two copies of the road background
    double road1X = -(this.scrollX % Game.WIDTH);
    this.setBlitPosition(this.road1, road1X, 0);
    this.setBlitPosition(this.road2, road1X + Game.WIDTH, 0);

    // Draw the background tiles. Skip the tiles which are off the left of the
    // screen, and stop once we've reached the right edge.
    int firstTile = (int) Math.ceil((this.scrollX - 417)
        / (double) Game.BACKGROUND_TILE_SPACING) + 1;
    if (firstTile < 0) {
      firstTile = 0;
    }
    for (int i = 0; i < this.tiles.length; i++) {
      int tileIndex = firstTile + i;
      double x = (tileIndex - 1) * (double) Game.BACKGROUND_TILE_SPACING - this.scrollX;
      if (tileIndex < BACKGROUND_TILES.length && x < Game.WIDTH) {
        this.tiles[i].showImage(BACKGROUND_TILES[tileIndex]);
        this.setBlitPosition(this.tiles[i], x, 0);
      } else {
        this.tiles[i].hide();
      }
    }
  }

  private class HealthBar extends ImageSprite {
    @Override
    public void run() {
      int width = (int) ((double) Play.this.player.health / Play.this.player.startHealth
          * Game.HEALTH_STAMINA_BAR_WIDTH);
      Play.this.drawBar(this, "beatstreets/assets/images/health.png", width, 48, 11);
    }
  }

  private class StaminaBar extends ImageSprite {
    @Override
    public void run() {
      int width = (int) (Play.this.player.stamina / Play.this.player.maxStamina
          * Game.HEALTH_STAMINA_BAR_WIDTH);
      Play.this.drawBar(this, "beatstreets/assets/images/stamina.png", width, 517, 11);
    }
  }

  private class LifeIcon extends ImageSprite {
    private final int index;

    private LifeIcon(int index) {
      this.index = index;
    }

    @Override
    public void run() {
      if (this.index < Play.this.player.lives) {
        int frame = Play.this.player.getExtraLifeTimer() <= 0
                || this.index < Play.this.player.lives - 1
            ? 9 : Math.min(9, (30 - Play.this.player.getExtraLifeTimer()) / 3);
        this.showImage("status_life" + frame);
        Play.this.setBlitPosition(this, this.index * 46 - 55, -35);
      } else {
        this.hide();
      }
    }
  }

  private class ScrollArrow extends ImageSprite {
    @Override
    public void run() {
      if (Play.this.scrollX < Play.this.maxScrollOffsetX && (Play.this.timer / 30) % 2 == 0) {
        Play.this.setBlitPosition(this, Game.WIDTH - 450, 120);
        this.show();
      } else {
        this.hide();
      }
    }
  }

  private class IntroFade extends ImageSprite {
    @Override
    public void run() {
      if (Play.this.textActive || Play.this.timer < 255) {
        double alpha = Play.this.textActive ? 255 : Math.max(0, 255 - Play.this.timer);
        this.setTransparency(100 - alpha * 100 / 255);
        this.show();
      } else {
        this.hide();
      }
    }
  }

  /**
   * Draws a horizontal slice of a bar image by switching to a lazily created
   * costume cropped to the given width.
   */
  private void drawBar(ImageSprite bar, String path, int width,
      double x, double y) {
    if (width <= 0) {
      bar.hide();
      return;
    }
    String costume = "bar" + width;
    bar.addCostume(costume, path, 0, 0, width, Game.HEALTH_STAMINA_BAR_HEIGHT);
    bar.switchCostume(costume);
    this.setBlitPosition(bar, x, y);
    bar.show();
  }

  /** The list of all stages, mirroring setup_stages() of the original. */
  private List<LevelStage> setupStages() {
    List<LevelStage> stages = new ArrayList<>();

    stages.add(new LevelStage(300,
        List.of(new EnemyVax(this, new Vector2(1000, 400), 20)),
        List.of(), List.of()));

    stages.add(new LevelStage(600,
        List.of(new EnemyVax(this, new Vector2(1400, 400), 20),
            new EnemyHoodie(this, new Vector2(1500, 500), 20)),
        List.of(new Barrel(this, new Vector2(1600, 400))), List.of()));

    stages.add(new LevelStage(600,
        List.of(new EnemyScooterboy(this, new Vector2(200, 400), 20)),
        List.of(), List.of()));

    stages.add(new LevelStage(900,
        List.of(new EnemyBoss(this, new Vector2(1800, 400), 20),
            new EnemyVax(this, new Vector2(400, 400), 20)),
        List.of(), List.of()));

    stages.add(new LevelStage(1400,
        List.of(new EnemyHoodie(this, new Vector2(2100, 380), 20),
            new EnemyHoodie(this, new Vector2(2100, 480), 20),
            new EnemyHoodie(this, new Vector2(800, 420), 20)),
        List.of(), List.of(new HealthPowerup(this, new Vector2(2300, Game.MIN_WALK_Y)))));

    stages.add(new LevelStage(1900,
        List.of(new EnemyVax(this, new Vector2(2400, 380), 20),
            new EnemyHoodie(this, new Vector2(2500, 480), 20),
            new EnemyScooterboy(this, new Vector2(2800, 400), 20)),
        List.of(), List.of()));

    stages.add(new LevelStage(2500,
        List.of(new EnemyScooterboy(this, new Vector2(3800, 380), 20),
            new EnemyScooterboy(this, new Vector2(3300, 480), 20),
            new EnemyScooterboy(this, new Vector2(1200, 400), 20)),
        List.of(), List.of()));

    stages.add(new LevelStage(3000,
        List.of(new EnemyVax(this, new Vector2(4000, 380), 20),
            new EnemyVax(this, new Vector2(3900, 480), 20),
            new EnemyVax(this, new Vector2(4200, 460), 20),
            new EnemyVax(this, new Vector2(4200, 450), 20),
            new EnemyHoodie(this, new Vector2(3900, 300), 20),
            new EnemyHoodie(this, new Vector2(3950, 320), 20)),
        List.of(), List.of()));

    stages.add(new LevelStage(3600,
        List.of(new EnemyVax(this, new Vector2(4600, 380), 20),
            new EnemyScooterboy(this, new Vector2(1200, 350), 20),
            new EnemyScooterboy(this, new Vector2(1400, 350), 20),
            new EnemyScooterboy(this, new Vector2(1600, 350), 20),
            new EnemyScooterboy(this, new Vector2(1800, 350), 20),
            new EnemyScooterboy(this, new Vector2(2000, 350), 20)),
        List.of(), List.of(new HealthPowerup(this, new Vector2(5100, Game.MIN_WALK_Y)))));

    stages.add(new LevelStage(4600,
        List.of(new EnemyHoodie(this, new Vector2(4800, 380), 20),
            new EnemyHoodie(this, new Vector2(4800, 350), 20),
            new EnemyScooterboy(this, new Vector2(1200, 350), 20),
            new EnemyScooterboy(this, new Vector2(1400, 350), 20),
            new EnemyScooterboy(this, new Vector2(4800, 350), 20),
            new EnemyScooterboy(this, new Vector2(4800, 400), 20),
            new EnemyScooterboy(this, new Vector2(4900, 450), 20)),
        List.of(), List.of()));

    stages.add(new LevelStage(5500,
        List.of(new EnemyBoss(this, new Vector2(6500, 380), 20),
            new EnemyBoss(this, new Vector2(6500, 360), 20)),
        List.of(new Barrel(this, new Vector2(6000, 400)),
            new Barrel(this, new Vector2(5900, 370))),
        List.of()));

    stages.add(new LevelStage(6400,
        List.of(new EnemyBoss(this, new Vector2(7000, 380), 20),
            new EnemyBoss(this, new Vector2(7000, 360), 20),
            new EnemyBoss(this, new Vector2(7000, 390), 20)),
        List.of(new Barrel(this, new Vector2(7000, 380))), List.of()));

    stages.add(new LevelStage(6900,
        List.of(new EnemyVax(this, new Vector2(7500, 380), 20),
            new EnemyScooterboy(this, new Vector2(7500, 350), 20),
            new EnemyScooterboy(this, new Vector2(7500, 360), 20)),
        List.of(), List.of()));

    stages.add(new LevelStage(7550,
        List.of(new EnemyHoodie(this, new Vector2(8000, 380), 50),
            new EnemyVax(this, new Vector2(8200, 340), 100),
            new EnemyHoodie(this, new Vector2(8200, 340), 150),
            new EnemyHoodie(this, new Vector2(7900, 360), 200),
            new EnemyHoodie(this, new Vector2(8300, 390), 250),
            new EnemyVax(this, new Vector2(8700, 400), 300),
            new EnemyHoodie(this, new Vector2(8800, 400), 400),
            new EnemyHoodie(this, new Vector2(8900, 400), 500),
            new EnemyVax(this, new Vector2(9000, 320), 600),
            new EnemyVax(this, new Vector2(9100, 400), 700),
            new EnemyHoodie(this, new Vector2(9100, 450), 800),
            new EnemyVax(this, new Vector2(9100, 420), 900),
            new EnemyBoss(this, new Vector2(9100, 450), 1000)),
        List.of(),
        List.of(new HealthPowerup(this, new Vector2(8000, Game.MIN_WALK_Y)),
            new ExtraLifePowerup(this, new Vector2(8200, Game.MIN_WALK_Y)))));

    stages.add(new LevelStage(8400,
        List.of(new EnemyPortal(this, new Vector2(8900, 400),
            List.of(pos -> new EnemyVax(this, pos, 20),
                pos -> new EnemyHoodie(this, pos, 20)),
            120, 30, 250, 2, 90)),
        List.of(), List.of()));

    stages.add(new LevelStage(8900,
        List.of(new EnemyPortal(this, new Vector2(9500, 400),
                List.of(pos -> new EnemyVax(this, pos, 20),
                    pos -> new EnemyHoodie(this, pos, 20)),
                120, 50, 250, 5, 90),
            new EnemyPortal(this, new Vector2(9500, 400),
                List.of(pos -> new EnemyScooterboy(this, pos, 20)),
                160, 50, 250, 5, 90)),
        List.of(), List.of()));

    stages.add(new LevelStage(9600,
        List.of(new EnemyPortal(this, new Vector2(10000, 420),
                List.of(pos -> new EnemyVax(this, pos, 20),
                    pos -> new EnemyHoodie(this, pos, 20)),
                120, 50, 250, 5, 90),
            new EnemyScooterboy(this, new Vector2(10500, 320), 20),
            new EnemyScooterboy(this, new Vector2(10500, 350), 20),
            new EnemyScooterboy(this, new Vector2(10500, 380), 20)),
        List.of(), List.of()));

    stages.add(new LevelStage(10800,
        List.of(new EnemyPortal(this, new Vector2(11200, 420),
            List.of(pos -> new EnemyHoodie(this, pos, 20)),
            40, 10, 250, 8, 90)),
        List.of(), List.of()));

    stages.add(new LevelStage(11400,
        List.of(new EnemyPortal(this, new Vector2(12100, 340),
                List.of(pos -> new EnemyScooterboy(this, pos, 20)),
                40, 20, 250, 8, 90),
            new EnemyPortal(this, new Vector2(11900, 400),
                List.of(pos -> new EnemyScooterboy(this, pos, 20)),
                50, 25, 250, 8, 90)),
        List.of(new Barrel(this, new Vector2(11800, 380))),
        List.of(new HealthPowerup(this, new Vector2(12000, Game.MIN_WALK_Y)),
            new HealthPowerup(this, new Vector2(12500, Game.MIN_WALK_Y)))));

    stages.add(new LevelStage(12600,
        List.of(new EnemyPortal(this, new Vector2(12900, 340),
                List.of(pos -> new EnemyBoss(this, pos, 20)),
                240, 20, 300, 4, 90),
            new EnemyHoodie(this, new Vector2(13200, 320), 20),
            new EnemyHoodie(this, new Vector2(13200, 330), 20),
            new EnemyVax(this, new Vector2(13400, 360), 20)),
        List.of(), List.of()));

    stages.add(new LevelStage(13400,
        List.of(new EnemyPortal(this, new Vector2(13600, 320),
                List.of(pos -> new EnemyVax(this, pos, 20)),
                230, 20, 300, 10, 90),
            new EnemyPortal(this, new Vector2(13600, 435),
                List.of(pos -> new EnemyHoodie(this, pos, 20)),
                240, 20, 300, 10, 90),
            new EnemyPortal(this, new Vector2(14000, 320),
                List.of(pos -> new EnemyScooterboy(this, pos, 20)),
                250, 30, 300, 10, 90),
            new EnemyPortal(this, new Vector2(14000, 435),
                List.of(pos -> new EnemyBoss(this, pos, 20)),
                260, 30, 300, 10, 90)),
        List.of(), List.of()));

    stages.add(new LevelStage(14700,
        List.of(new EnemyPortal(this, new Vector2(14900, 320),
                List.of(pos -> new EnemyVax(this, pos, 20)),
                220, 20, 300, 8, 90),
            new EnemyPortal(this, new Vector2(14900, 435),
                List.of(pos -> new EnemyHoodie(this, pos, 20)),
                230, 20, 300, 8, 90),
            new EnemyPortal(this, new Vector2(15300, 320),
                List.of(pos -> new EnemyScooterboy(this, pos, 20)),
                240, 20, 300, 8, 90),
            new EnemyPortal(this, new Vector2(15300, 435),
                List.of(pos -> new EnemyBoss(this, pos, 20)),
                250, 20, 300, 8, 90)),
        List.of(), List.of(new HealthPowerup(this, new Vector2(14650, 350)))));

    stages.add(new LevelStage(15400,
        List.of(new EnemyPortal(this, new Vector2(15800, 350),
            List.of(pos -> new EnemyVax(this, pos, 20),
                pos -> new EnemyHoodie(this, pos, 20),
                pos -> new EnemyScooterboy(this, pos, 20)),
            60, 20, 300, 8, 90)),
        List.of(), List.of(new HealthPowerup(this, new Vector2(16000, Game.MIN_WALK_Y)))));

    stages.add(new LevelStage(16600,
        List.of(new EnemyVax(this, new Vector2(17600, 300), 20),
            new EnemyVax(this, new Vector2(17900, 320), 20),
            new EnemyVax(this, new Vector2(17600, 340), 20),
            new EnemyVax(this, new Vector2(17900, 360), 20),
            new EnemyVax(this, new Vector2(17600, 380), 20),
            new EnemyVax(this, new Vector2(17900, 400), 20),
            new EnemyVax(this, new Vector2(17600, 420), 20)),
        List.of(new Barrel(this, new Vector2(17000, 380))),
        List.of(new HealthPowerup(this, new Vector2(17000, Game.MIN_WALK_Y)))));

    stages.add(new LevelStage(17400,
        List.of(new EnemyBoss(this, new Vector2(17800, Game.MIN_WALK_Y), 20),
            new EnemyScooterboy(this, new Vector2(18500, 380), 20),
            new EnemyScooterboy(this, new Vector2(18600, 380), 20),
            new EnemyScooterboy(this, new Vector2(18700, 380), 20),
            new EnemyScooterboy(this, new Vector2(18800, 380), 20),
            new EnemyScooterboy(this, new Vector2(19000, 380), 20)),
        List.of(new Stick(this, new Vector2(18000, 340))), List.of()));

    stages.add(new LevelStage(18500,
        List.of(new EnemyBoss(this, new Vector2(18800, 320), 20),
            new EnemyPortal(this, new Vector2(18900, 390),
                List.of(pos -> new EnemyVax(this, pos, 20),
                    pos -> new EnemyHoodie(this, pos, 20)),
                30, 5, 600, 10, 400)),
        List.of(), List.of()));

    stages.add(new LevelStage(19300,
        List.of(new EnemyScooterboy(this, new Vector2(19900, 340), 20)),
        List.of(new Barrel(this, new Vector2(19400, 340))),
        List.of(new HealthPowerup(this, new Vector2(19600, Game.MIN_WALK_Y)))));

    // Final battles

    stages.add(new LevelStage(20500,
        List.of(new EnemyHoodie(this, new Vector2(20900, 380), 500),
            new EnemyBoss(this, new Vector2(21500, 330), 20),
            new EnemyBoss(this, new Vector2(21500, 350), 20),
            new EnemyBoss(this, new Vector2(21500, 370), 20),
            new EnemyBoss(this, new Vector2(21500, 390), 20),
            new EnemyBoss(this, new Vector2(18200, 320), 20),
            new EnemyBoss(this, new Vector2(17800, 390), 20)),
        List.of(), List.of(new ExtraLifePowerup(this, new Vector2(20900, Game.MIN_WALK_Y)))));

    stages.add(new LevelStage(20500,
        List.of(new EnemyPortal(this, new Vector2(20700, 315),
                List.of(pos -> new EnemyVax(this, pos, 20)),
                60, 5, 600, 20, 600),
            new EnemyPortal(this, new Vector2(20700, 440),
                List.of(pos -> new EnemyHoodie(this, pos, 20)),
                60, 10, 600, 20, 600),
            new EnemyPortal(this, new Vector2(21100, 315),
                List.of(pos -> new EnemyScooterboy(this, pos, 20)),
                60, 15, 600, 20, 600),
            new EnemyPortal(this, new Vector2(21100, 440),
                List.of(pos -> new EnemyBoss(this, pos, 20)),
                60, 20, 600, 20, 600)),
        List.of(), List.of()));

    return stages;
  }
}
