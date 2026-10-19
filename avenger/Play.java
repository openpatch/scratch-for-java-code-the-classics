package avenger;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.openpatch.scratch.Random;
import org.openpatch.scratch.TextAlign;
import org.openpatch.scratch.extensions.text.BitmapText;
import org.openpatch.scratch.Sprite;
import org.openpatch.scratch.Stage;
import org.openpatch.scratch.Window;

/**
 * The game world: the player, the enemies, the humans, the lasers and the
 * bullets, with the wave progress and the score. All positions are kept in
 * world pixel coordinates (origin top-left, y growing downwards), see
 * {@link WrapActor}; each sprite applies the shared scrolling camera offset
 * in its own {@code run()} callback.
 *
 * <p>The draw order mirrors the original: background and terrain at the back,
 * then the bullets, humans, enemies and the player with their lasers, then
 * the radar with its blips, the lives, shields and tokens, and the text on
 * top. Just like the original, the game carries on running behind the game
 * over screen.
 */
public class Play extends Stage {

  public static final int WIDTH = 960;
  public static final int HEIGHT = 540;

  public static final int LEVEL_WIDTH = 4096;
  public static final int LEVEL_HEIGHT = 640;

  public static final int WAVE_COMPLETE_SCREEN_DURATION = 320;

  // The positions of the humans are relative to the terrain image, which is
  // displayed with an offset from the top of the game world
  public static final int TERRAIN_OFFSET_Y = 160;

  /** The sound effects of the game, one file per entry. */
  private static final String[] SOUND_EFFECTS = {
    "enemy_appear_mutant0", "enemy_appear_normal0", "enemy_appear_ufo0",
    "enemy_explode0", "enemy_explode1", "enemy_explode2", "enemy_explode3",
    "enemy_explode4", "enemy_explode5", "enemy_laser0", "new_wave0",
    "player_explode0", "player_hit0", "player_shoot0", "prisoner_die0",
    "rescue_prisoner0", "thrust0", "wave_complete0"
  };

  public static final int[][] HUMAN_START_POS = {
    {204, 410}, {489, 209}, {865, 374}, {1262, 405}, {1937, 263},
    {2193, 278}, {2601, 405}, {2846, 347}, {3317, 193}, {3646, 233}
  };

  protected final Controls controls;
  public final Player player;
  public final Radar radar;

  public final List<Enemy> enemies = new ArrayList<>();
  public final List<Human> humans = new ArrayList<>();
  public final List<Laser> lasers = new ArrayList<>();
  public final List<Bullet> bullets = new ArrayList<>();
  public int score = 0;

  // Wave 1 is the first wave; this starts at zero because newWave() increments it
  public int wave = 0;
  public int waveTimer = 0;

  public int timer = 0;

  // Defines the point on the screen at which the player appears
  private int playerCameraOffsetX = WIDTH / 3;
  private double offsetX;
  private double offsetY;

  // The terrain mask: true where the terrain image is opaque
  private boolean[] terrainMask;
  private int maskWidth;
  private int maskHeight;

  private boolean gameOver = false;
  private int stateTimer = 0;

  private final List<Sprite> bgSprites = new ArrayList<>();
  private final List<Sprite> terrainSprites = new ArrayList<>();
  private final List<Sprite> lifeSprites = new ArrayList<>();
  private final List<Sprite> shieldSprites = new ArrayList<>();
  private final List<Sprite> tokenSprites = new ArrayList<>();
  private final BitmapText scoreText;
  private final BitmapText waveText;
  private final BitmapText gameOverText;

  /** The ambience that loops for as long as the game is running. */
  private final Sprite ambience = new Sprite();

  public Play() {
    this.ambience.addSound("ambience", "avenger/assets/music/ambience.ogg");
    this.controls = new Controls(this);

    for (String sound : SOUND_EFFECTS) {
      this.addSound(sound, "avenger/assets/sounds/" + sound + ".ogg");
    }

    // Background, drawn five times: four because the level is four times
    // wider than the background, and another for when we're near the
    // right-hand side of the level, just before it wraps around
    for (int i = 0; i < 5; i++) {
      var background = new Sprite();
      background.addCostume("background", Utils.img("background"));
      background.switchCostume("background");
      this.add(background);
      this.bgSprites.add(background);
    }

    // Terrain, drawn twice; the second one is for when we're near the
    // right-hand side of the level, just before it wraps back around
    for (int i = 0; i < 2; i++) {
      var terrain = new Sprite();
      terrain.addCostume("terrain", Utils.img("terrain"));
      terrain.switchCostume("terrain");
      this.add(terrain);
      this.terrainSprites.add(terrain);
    }

    // The radar at the top of the screen
    this.radar = new Radar(this);

    // Lives, shields and extra life token sprites
    for (int i = 0; i < 20; i++) {
      var life = new Sprite();
      life.addCostume("life", Utils.img("life"));
      life.switchCostume("life");
      life.hide();
      this.add(life);
      this.lifeSprites.add(life);
    }
    for (int i = 0; i < 5; i++) {
      var shield = new Sprite();
      shield.addCostume("armor", Utils.img("armor"));
      shield.switchCostume("armor");
      shield.hide();
      this.add(shield);
      this.shieldSprites.add(shield);
    }
    for (int i = 0; i < 3; i++) {
      var token = new Sprite();
      for (int f = 0; f < 8; f++) {
        token.addCostume("token" + f, Utils.img("token" + f));
      }
      token.switchCostume("token0");
      token.hide();
      this.add(token);
      this.tokenSprites.add(token);
    }

    // The interface text, drawn with the game's bitmap fonts. The status
    // font (the score) is drawn behind the wave end text and the game over
    // text, like the blits of the original
    this.scoreText = new BitmapText(Utils.img("font_status%03d")) {
      @Override
      public void run() {
        this.showText(Integer.toString(Play.this.score));
      }
    };
    this.scoreText.setAlign(TextAlign.RIGHT);
    this.scoreText.setPosition(WIDTH - 20 - WIDTH / 2.0, HEIGHT / 2.0 - 28);
    this.scoreText.setIsUI(true);
    this.add(this.scoreText);

    this.waveText = new BitmapText(Utils.img("font%03d")) {
      @Override
      public void run() {
        this.showText(Play.this.waveTimer < 0
            ? String.join("\n", Play.this.getWaveEndText()) : "");
      }
    };
    this.waveText.setAlign(TextAlign.CENTER);
    this.waveText.setPosition(0, 140);
    this.waveText.showText("WAVE");
    // One line is that many pixels apart in the original
    this.waveText.setLineSpacing(65 - this.waveText.getHeight());
    this.waveText.showText("");
    this.waveText.setIsUI(true);
    this.add(this.waveText);

    this.gameOverText = new BitmapText(Utils.img("font%03d")) {
      @Override
      public void run() {
        this.showText(Play.this.gameOver ? "GAME OVER" : "");
      }
    };
    this.gameOverText.setAlign(TextAlign.CENTER);
    this.gameOverText.setPosition(0, 100);
    this.gameOverText.setIsUI(true);
    this.add(this.gameOverText);

    // Load the terrain mask, used to check whether a human is on the ground
    this.loadTerrainMask();

    // The player; its blip, thrust and flash sprites are added to the stage
    // by its constructor
    this.player = new Player(this, this.controls);
    this.add(this.player);

    this.newWave();
  }

  /** True where the terrain image is opaque, like the terrain mask of the original. */
  public boolean terrainAt(int x, int y) {
    if (0 <= x && x < this.maskWidth && 0 <= y && y < this.maskHeight) {
      return this.terrainMask[y * this.maskWidth + x];
    } else if (y >= this.maskHeight) {
      // If we're somehow off the bottom of the terrain, treat that as being
      // on the terrain, otherwise we'd fall off the bottom of the game world
      return true;
    }
    return false;
  }

  private void loadTerrainMask() {
    try {
      BufferedImage terrain = ImageIO.read(new File(Utils.img("terrain")));
      this.maskWidth = terrain.getWidth();
      this.maskHeight = terrain.getHeight();
      this.terrainMask = new boolean[this.maskWidth * this.maskHeight];
      for (int y = 0; y < this.maskHeight; y++) {
        for (int x = 0; x < this.maskWidth; x++) {
          this.terrainMask[y * this.maskWidth + x] = (terrain.getRGB(x, y) >>> 24) > 127;
        }
      }
    } catch (IOException e) {
      this.maskWidth = 0;
      this.maskHeight = 0;
      this.terrainMask = new boolean[0];
    }
  }

  /**
   * Plays a sound of the game at the given volume. Sounds at a volume below 1
   * are played as overlapping copies with their own volume, so that changing
   * the volume doesn't change the volume of the instances that are already
   * playing, just like the original.
   */
  public void playSound(String name, double volume) {
    // Don't bother playing the sound if the volume is 0 or less. Also don't
    // play it if it's game over and the player has been dead for a while
    if (volume <= 0
        || (this.player.lives == 0 && this.player.timers[Player.TIMER_HURT] < -1000)) {
      return;
    }
    if (volume < 1) {
      this.playSoundCopy(name, volume * 100);
    } else {
      super.playSound(name);
    }
  }

  @Override
  public void playSound(String name) {
    this.playSound(name, 1);
  }

  /** Plays a random sound variation, e.g. enemy_explode0 to enemy_explode5. */
  public void playRandom(String name, int count) {
    this.playSound(name + Random.randomInt(0, count - 1));
  }

  public void addEnemy(Enemy enemy) {
    this.enemies.add(enemy);
    this.add(enemy);
  }

  public void addHuman(Human human) {
    this.humans.add(human);
    this.add(human);
  }

  public void addLaser(Laser laser) {
    this.lasers.add(laser);
    this.add(laser);
  }

  public void addBullet(Bullet bullet) {
    this.bullets.add(bullet);
    this.add(bullet);
  }

  public void newWave() {
    // Add 6 lander enemies to the list for the first wave, and an additional
    // one lander for each subsequent wave. From wave 4, add a pod enemy, and
    // add an extra pod every two waves. Every 5th wave has baiters and
    // mutants at the start instead of pods/landers. Every 10th wave has
    // swarmers instead of mutants
    this.wave += 1;
    int numLanders = 4 + this.wave;
    int numPods = this.wave / 2 - 1;
    int numBaiters = 0;
    int numMutants = 0;
    int numSwarmers = 0;
    if (this.wave % 5 == 0) {
      numLanders = 0;
      numPods = 0;
      numBaiters = this.wave;
      if (this.wave % 10 == 0) {
        numSwarmers = this.wave / 2;
      } else {
        numMutants = this.wave / 2;
      }
    }
    for (int i = 0; i < numLanders; i++) {
      this.addEnemy(new Enemy(this, -i * 20, EnemyType.LANDER, null, null));
    }
    for (int i = 0; i < numPods; i++) {
      this.addEnemy(new Enemy(this, -i * 50, EnemyType.POD, null, null));
    }
    for (int i = 0; i < numBaiters; i++) {
      this.addEnemy(new Enemy(this, -i * 100, EnemyType.BAITER, null, null));
    }
    for (int i = 0; i < numMutants; i++) {
      this.addEnemy(new Enemy(this, -i * 10, EnemyType.MUTANT, null, null));
    }
    for (int i = 0; i < numSwarmers; i++) {
      this.addEnemy(new Enemy(this, -i * 10, EnemyType.SWARMER, null, null));
    }

    // Create humans
    for (Human human : this.humans) {
      human.remove();
      human.blip.remove();
    }
    this.humans.clear();
    for (int[] pos : HUMAN_START_POS) {
      this.addHuman(new Human(this, pos[0], pos[1] + TERRAIN_OFFSET_Y));
    }

    this.playSound("new_wave0");
  }

  @Override
  public void run() {
    // The ambience loops for as long as the game is running
    this.ambience.playSound("ambience");

    this.stateTimer += 1;
    this.controls.update();

    if (this.gameOver) {
      // The game carries on updating in the background of the game over
      // screen. Don't allow the player to press a button to go back to the
      // main menu until one second has passed
      this.advanceWave();
      if (this.stateTimer > 60 && this.controls.buttonPressed(0)) {
        this.ambience.stopAllSounds();
        this.stopAllSounds();
        Window.getInstance().setStage(new Menu());
        return;
      }
    } else {
      if (this.player.lives <= 0) {
        this.gameOver = true;
        this.stateTimer = 0;
      } else {
        this.advanceWave();
      }
    }

    this.updatePresentation();
  }

  private void advanceWave() {
    // The wave timer counts up each frame. At the end of a wave it's set to a
    // negative number, indicating to display the "wave complete" message for
    // that many frames before starting the next wave
    this.waveTimer += 1;
    if (this.waveTimer == 0) {
      this.newWave();
    }

    this.timer += 1;

    // Make a baiter enemy every 30 seconds, if the player is alive
    if (this.waveTimer > 0 && this.waveTimer % (30 * 60) == 0 && this.player.lives > 0) {
      this.addEnemy(new Enemy(this, 0, EnemyType.BAITER, null, null));
    }

    // Remove dead humans
    this.humans.removeIf(human -> {
      if (human.dead) {
        human.remove();
        human.blip.remove();
        return true;
      }
      return false;
    });

    // Remove dead enemies who have finished their explode animations
    int prevNumEnemies = this.enemies.size();
    this.enemies.removeIf(enemy -> {
      if (enemy.state == EnemyState.DEAD) {
        enemy.remove();
        enemy.blip.remove();
        return true;
      }
      return false;
    });

    // If there are fewer enemies this frame than there were last frame,
    // gain score
    int difference = prevNumEnemies - this.enemies.size();
    if (difference > 0) {
      this.score += 150 * difference;
    }

    // Start the next wave if there are no enemies and no falling humans, and
    // the player is not carrying a human
    boolean anyFalling = false;
    for (Human human : this.humans) {
      anyFalling |= human.falling;
    }
    if (this.waveTimer > 0 && this.enemies.isEmpty() && !anyFalling
        && !this.player.isCarryingHuman()) {
      this.waveTimer = -WAVE_COMPLETE_SCREEN_DURATION;

      // Tell the player how many shields to restore and how many humans
      // were saved; if they save all ten they get an extra life token
      this.player.levelEnded(this.getShieldRestoreAmount(), this.getHumansSaved());

      this.playSound("wave_complete0");
    }
  }

  public double getOffsetX() {
    return this.offsetX;
  }

  public double getOffsetY() {
    return this.offsetY;
  }

  /** Updates the shared camera, parallax and layers before the sprite callbacks. */
  private void updatePresentation() {
    // Shift the target camera position based on which way the player is
    // facing: facing right, the ship is a third of the screen width from the
    // left; facing left, two thirds of the screen width from the right
    double targetCameraOffsetX;
    if (this.player.facingX > 0) {
      targetCameraOffsetX = WIDTH / 3.0;
    } else {
      targetCameraOffsetX = 2 * WIDTH / 3.0;
    }

    // Also shift the camera target position based on player velocity - look
    // further ahead if moving fast
    targetCameraOffsetX -= this.player.velX * 15;

    // Transition to the new offset over a series of frames, faster when the
    // difference is bigger, but at a maximum of 8 pixels per frame
    double cameraOffsetDelta = Math.min(8,
        Math.max(-8, (targetCameraOffsetX - this.playerCameraOffsetX) / 20));
    this.playerCameraOffsetX = (int) Math.floor(this.playerCameraOffsetX + cameraOffsetDelta);

    // Calculate where to display the background, terrain and objects. Think
    // of scrolling not as the camera moving, but everything in the game
    // moving in the opposite way. The top won't go lower than -100, to
    // prevent seeing off the bottom of the terrain
    int left = -Math.floorMod((int) (this.player.x - this.playerCameraOffsetX), LEVEL_WIDTH);
    int top = Math.max(-(int) (this.player.y / 4), -100);

    // Draw the background five times. The x/y values are divided by 2 so that
    // it moves slower than the foreground terrain - parallax scrolling
    var background = this.bgSprites.get(0);
    int bgWidth = background.getWidth();
    int bgHeight = background.getHeight();
    int bgLeft = Math.floorDiv(left, 2);
    int bgTop = Math.floorDiv(top, 2);
    for (int i = 0; i < this.bgSprites.size(); i++) {
      var sprite = this.bgSprites.get(i);
      this.setScreenPosition(sprite, bgLeft + (double) bgWidth * i + bgWidth / 2.0,
          bgTop + bgHeight / 2.0);
    }

    // Draw the terrain twice, the second one for when we're near the
    // right-hand side of the level, just before it wraps back around
    var terrain = this.terrainSprites.get(0);
    int terrainWidth = terrain.getWidth();
    int terrainHeight = terrain.getHeight();
    for (int i = 0; i < this.terrainSprites.size(); i++) {
      var sprite = this.terrainSprites.get(i);
      this.setScreenPosition(sprite, left + (double) terrainWidth * i + terrainWidth / 2.0,
          top + TERRAIN_OFFSET_Y + terrainHeight / 2.0);
    }

    // The scrolling offset for the game objects
    this.offsetX = -(this.player.x - this.playerCameraOffsetX);
    this.offsetY = top;

    // Draw all objects. The order of the player and the lasers they fire
    // varies depending on the tilt of the ship, as the laser is fired from
    // the underside of the ship. The sprites are placed in the order the
    // original draws them in, behind the radar and the other interface
    // elements
    int layer = 7; // the background and terrain sprites
    for (Bullet bullet : this.bullets) {
      this.place(bullet, layer++);
    }
    for (Human human : this.humans) {
      this.place(human, layer++);
    }
    for (Enemy enemy : this.enemies) {
      this.place(enemy, layer++);
    }
    if (this.player.tiltY == 1) {
      for (Laser laser : this.lasers) {
        this.place(laser, layer++);
      }
      this.place(this.player.flashSprite, layer++);
      this.place(this.player, layer++);
      this.place(this.player.thrustSprite, layer++);
    } else {
      this.place(this.player, layer++);
      this.place(this.player.thrustSprite, layer++);
      this.place(this.player.flashSprite, layer++);
      for (Laser laser : this.lasers) {
        this.place(laser, layer++);
      }
    }

    this.drawUi(layer);
  }

  private void drawUi(int layer) {
    // The radar is behind its blips
    layer++;

    // Draw the radar blips of the enemies who are currently alive
    for (Enemy enemy : this.enemies) {
      if (enemy.state == EnemyState.ALIVE) {
        enemy.blip.show();
        this.place(enemy.blip, layer++);
      } else {
        enemy.blip.hide();
      }
    }

    // Draw the radar blips of the humans and the player
    for (Human human : this.humans) {
      human.blip.show();
      this.place(human.blip, layer++);
    }
    this.player.blip.show();
    this.place(this.player.blip, layer++);

    // Show lives
    for (int i = 0; i < this.lifeSprites.size(); i++) {
      var life = this.lifeSprites.get(i);
      if (i < this.player.lives) {
        this.setScreenPosition(life, 20 + 20.0 * i + life.getWidth() / 2.0,
            21 + life.getHeight() / 2.0);
        life.show();
      } else {
        life.hide();
      }
    }

    // Show shields
    for (int i = 0; i < this.shieldSprites.size(); i++) {
      var shield = this.shieldSprites.get(i);
      if (i < this.player.shields) {
        this.setScreenPosition(shield, 20 + 20.0 * i + shield.getWidth() / 2.0,
            52 + shield.getHeight() / 2.0);
        shield.show();
      } else {
        shield.hide();
      }
    }

    // Show extra life tokens
    for (int i = 0; i < this.tokenSprites.size(); i++) {
      var token = this.tokenSprites.get(i);
      if (i < this.player.extraLifeTokens) {
        int frame = (this.timer / 6 + i) % 8;
        token.switchCostume("token" + frame);
        this.setScreenPosition(token, 20 + 20.0 * i + token.getWidth() / 2.0,
            83 + token.getHeight() / 2.0);
        token.show();
      } else {
        token.hide();
      }
    }
  }

  /**
   * The lines of the wave end text. As the wave timer increases we display
   * more lines of text.
   */
  private List<String> getWaveEndText() {
    int humansSaved = this.getHumansSaved();
    int i = (this.waveTimer + WAVE_COMPLETE_SCREEN_DURATION)
        / (WAVE_COMPLETE_SCREEN_DURATION / 4);
    List<String> lines = new ArrayList<>();
    lines.add("WAVE " + this.wave + " COMPLETE");
    if (i >= 1) {
      lines.add(humansSaved + " HUMAN" + (humansSaved == 1 ? "" : "S") + " SAVED");
    }
    if (i >= 2) {
      int numShieldsRestored = this.getShieldRestoreAmount();
      lines.add(numShieldsRestored + " SHIELD" + (numShieldsRestored == 1 ? "" : "S")
          + " RESTORED");
    }
    if (i >= 3 && humansSaved == 10) {
      // If we saved 10 humans but have no extra life tokens, that must mean
      // we just got 3 extra life tokens, which gains an extra life and resets
      // the tokens to zero, therefore display that we got an extra life
      if (this.player.extraLifeTokens == 0) {
        lines.add("EXTRA LIFE");
      } else {
        lines.add("LIFE TOKEN GAINED");
      }
    }
    return lines;
  }

  /** The player gets 1 shield restored for every 2 humans saved. */
  public int getShieldRestoreAmount() {
    return Math.min(this.getHumansSaved() / 2, 5);
  }

  public int getHumansSaved() {
    int count = 0;
    for (Human human : this.humans) {
      if (!human.exploding) {
        count += 1;
      }
    }
    return count;
  }

  /** Sets a sprite position from screen pixel coordinates (top-left origin). */
  private void setScreenPosition(Sprite sprite, double x, double y) {
    sprite.setPosition(x - WIDTH / 2.0, HEIGHT / 2.0 - y);
  }

  /** Puts a sprite at the given layer, counting from the back of the stage. */
  private void place(Sprite sprite, int layer) {
    sprite.goToBackLayer();
    sprite.goLayersForwards(layer);
  }
}
