package leadingedge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.openpatch.scratch.Color;
import org.openpatch.scratch.KeyCode;
import org.openpatch.scratch.Operators;
import org.openpatch.scratch.Random;
import org.openpatch.scratch.Sprite;
import org.openpatch.scratch.Stage;
import org.openpatch.scratch.TextAlign;
import org.openpatch.scratch.Window;
import org.openpatch.scratch.extensions.drawing.Drawing;
import org.openpatch.scratch.extensions.drawing.DrawingSprite;
import org.openpatch.scratch.extensions.text.BitmapText;

/**
 * The race: the track, the cars, the camera and everything that is displayed
 * at the top of the screen. This class mirrors the original's Game class, with
 * the update() method running at a fixed timestep and drawScene() rasterizing
 * the pseudo-3D view.
 *
 * <p>The original draws polygons and scaled sprites, which a Scratch stage
 * cannot do with costumes. Everything is therefore drawn by one {@link Scene}
 * drawing sprite, in the same order as the original: background, track pieces
 * from far to near, scenery, cars, then the status text. The projection stays
 * in this game and draws its resulting 2D view through the drawing extension.
 */
public class Play extends Stage {

  public static final int WIDTH = 960;
  public static final int HEIGHT = 540;
  public static final String TITLE = "Leading Edge";

  public static final boolean SHOW_SCENERY = true;
  public static final boolean SHOW_TRACKSIDE = true;
  public static final boolean SHOW_RUMBLE_STRIPS = true;
  public static final boolean SHOW_YELLOW_LINES = true;
  public static final int VIEW_DISTANCE = 200;

  public static final double CLIPPING_PLANE = -0.25;
  public static final double CLIPPING_PLANE_CARS = -0.08;
  public static final double MAX_SCENERY_SCALED_WIDTH = WIDTH * 2;
  public static final double MAX_CAR_SCALED_WIDTH = WIDTH;

  // Constants for track
  public static final double SPACING = 1;
  public static final double TRACK_W = 3000;
  public static final double HALF_STRIPE_W = 25;
  public static final double HALF_RUMBLE_STRIP_W = 250;
  public static final double HALF_YELLOW_LINE_W = 80;
  public static final double YELLOW_LINE_DISTANCE_FROM_EDGE = 150;
  public static final int[] TRACK_COLOUR = {35, 96, 198};
  public static final int[] TRACKSIDE_COLOUR_1 = {0, 77, 180};
  public static final int[] TRACKSIDE_COLOUR_2 = {50, 77, 170};
  public static final int[] STRIPE_COLOUR = {70, 192, 255};
  public static final int[] YELLOW_LINE_COL = {0, 161, 88};
  public static final int[] RUMBLE_COLOUR_1 = {0, 116, 255};
  public static final int[] RUMBLE_COLOUR_2 = {0, 58, 135};
  public static final int SECTION_VERY_SHORT = 25;
  public static final int SECTION_SHORT = 50;
  public static final int SECTION_MEDIUM = 100;
  public static final int SECTION_LONG = 200;
  public static final double LAMP_X = TRACK_W / 2 + 300;
  public static final double BILLBOARD_X = TRACK_W / 2 + 600;

  public static final double CAMERA_FOLLOW_DISTANCE = 2;

  // Player car gameplay settings
  public static final double LOSE_GRIP_SPEED = 50;
  public static final double ZERO_GRIP_SPEED = 100;
  public static final double PLAYER_ACCELERATION_MAX = 20;
  public static final double PLAYER_ACCELERATION_MIN = 10;
  public static final double HIGH_ACCEL_THRESHOLD = 30;
  public static final double CORNER_OFFSET_MULTIPLIER = 5.8;
  public static final double STEERING_STRENGTH = 72;

  public static final double CPU_CAR_MIN_TARGET_SPEED = 40;
  public static final double CPU_CAR_MAX_TARGET_SPEED = 65;

  public static final int NUM_LAPS = 5;
  public static final int NUM_CARS = 20;

  public static final double GRID_CAR_SPACING = 0.55;

  public static final double HALF_WIDTH = WIDTH / 2.0;
  public static final double HALF_HEIGHT = HEIGHT / 2.0;

  public static final double SKID_SOUND_START_GRIP = 0.8;

  public static final double FIXED_TIMESTEP = 1 / 60.0;

  // Characters in the main font are italic so should overlap a little
  private static final double FONT_LETTER_SPACING = -6;

  /** Measures the size of one of the game's images without drawing it. */
  private static final Sprite IMAGE_MEASURER = new Sprite();

  /** The names of the billboard and arrow images used by the track. */
  private static final String BILLBOARD_00 = "billboard00";
  private static final String BILLBOARD_01 = "billboard01";
  private static final String BILLBOARD_02 = "billboard02";
  private static final String BILLBOARD_03 = "billboard03";
  private static final String ARROW_LEFT = "arrow_left";
  private static final String ARROW_RIGHT = "arrow_right";

  /** The width of the background image, which the background offset wraps at. */
  private final double backgroundWidth = imageWidth("background");

  /** The status texts at the top of the screen, and the race messages. */
  private final BitmapText lapText = bitmapText("status1b_");
  private final BitmapText positionText = bitmapText("status1b_");
  private final BitmapText speedText = bitmapText("status1b_");
  private final BitmapText lapTimeText = bitmapText("status2_");
  private final BitmapText timeUpText = bitmapText("font");
  private final BitmapText raceCompleteText = bitmapText("font");
  private final BitmapText racePositionLabelText = bitmapText("font");
  private final BitmapText racePositionText = bitmapText("font");
  private final BitmapText fastestLapLabelText = bitmapText("font");
  private final BitmapText fastestLapTimeText = bitmapText("font");
  private final BitmapText raceTimeLabelText = bitmapText("font");
  private final BitmapText raceTimeText = bitmapText("font");
  private final BitmapText messageText = bitmapText("font");
  private final BitmapText messageTimeText = bitmapText("font");
  private final List<BitmapText> texts = List.of(
      this.lapText, this.positionText, this.speedText, this.lapTimeText,
      this.timeUpText, this.raceCompleteText, this.racePositionLabelText,
      this.racePositionText, this.fastestLapLabelText, this.fastestLapTimeText,
      this.raceTimeLabelText, this.raceTimeText, this.messageText,
      this.messageTimeText);

  protected final Controls controls;
  protected final Controls keyboardControls = new Controls(this);

  public final List<Car> cars = new ArrayList<>();
  public PlayerCar playerCar;
  public Car cameraFollowCar;
  public List<TrackPiece> track;

  public final Vector3 camera = new Vector3(0, 400, 0);
  public double bgOffsetX;
  public double bgOffsetY;

  public boolean firstFrame = true;
  public int frameCounter = 0;
  public double timer = 0;
  public boolean raceComplete = false;
  public boolean timeUp = false;
  public double startTimer = 0;

  private double accumulatedTime = 0;

  private final Scene scene;

  /** Music has its own sprite so it can stop independently of sound effects. */
  private final Sprite music = new Sprite();

  public Play(Controls controls) {
    for (String name : new String[] {"title_theme", "engines_startline", "ambience"}) {
      this.music.addSound(name, "leadingedge/assets/music/" + name + ".ogg");
    }
    this.controls = controls;

    this.addSound("bump_behind0", "leadingedge/assets/sounds/bump_behind0.ogg");
    this.addSound("explosion0", "leadingedge/assets/sounds/explosion0.ogg");
    this.addSound("fastlap0", "leadingedge/assets/sounds/fastlap0.ogg");
    this.addSound("final_lap0", "leadingedge/assets/sounds/final_lap0.ogg");
    this.addSound("game_complete0", "leadingedge/assets/sounds/game_complete0.ogg");
    this.addSound("gobeep0", "leadingedge/assets/sounds/gobeep0.ogg");
    this.addSound("hit_grass0", "leadingedge/assets/sounds/hit_grass0.ogg");
    this.addSound("startbeep0", "leadingedge/assets/sounds/startbeep0.ogg");
    for (int i = 0; i < 6; i++) {
      this.addSound("bump" + i, String.format("leadingedge/assets/sounds/bump%d.ogg", i));
      this.addSound("overtake" + i, String.format("leadingedge/assets/sounds/overtake%d.ogg", i));
    }
    // The engine and skid sounds, played as copies with their own volume
    for (int i = 0; i < 40; i++) {
      this.addSound("engine_short" + i,
          String.format("leadingedge/assets/sounds/engine_short%d.ogg", i));
    }
    this.addSound("skid_loop0", "leadingedge/assets/sounds/skid_loop0.ogg");

    this.initWorld(controls);

    this.scene = new Scene();
    this.add(this.scene);

    // The status texts at the top of the screen, and the race messages
    double statusX = WIDTH / 2.0 - 565 / 2.0;
    this.placeText(this.lapText, statusX + 30, 37, false);
    this.placeText(this.positionText, statusX + 116, 37, false);
    this.placeText(this.speedText, statusX + 197, 37, false);
    this.placeText(this.lapTimeText, statusX + 299, 37, false);
    this.placeText(this.timeUpText, WIDTH / 2.0, HEIGHT * 0.4, true);
    this.placeText(this.raceCompleteText, WIDTH / 2.0, HEIGHT * 0.15, true);
    this.placeText(this.racePositionLabelText, WIDTH / 2.0, HEIGHT * 0.3, true);
    this.placeText(this.racePositionText, WIDTH / 2.0, HEIGHT * 0.42, true);
    this.placeText(this.fastestLapLabelText, WIDTH * 0.25, HEIGHT * 0.55, true);
    this.placeText(this.fastestLapTimeText, WIDTH * 0.25, HEIGHT * 0.68, true);
    this.placeText(this.raceTimeLabelText, WIDTH * 0.75, HEIGHT * 0.55, true);
    this.placeText(this.raceTimeText, WIDTH * 0.75, HEIGHT * 0.68, true);
    this.placeText(this.messageText, WIDTH / 2.0, HEIGHT * 0.4, true);
    this.placeText(this.messageTimeText, WIDTH / 2.0, HEIGHT * 0.4 + 60, true);
    for (BitmapText text : this.texts) {
      this.add(text);
    }

    this.stopMusic();
    if (this.playerCar != null) {
      this.playMusic("engines_startline");
    }
    this.updateStatus();
  }

  /** The path of one of the game's images, by name (without folder/extension). */
  protected static String imagePath(String name) {
    return "leadingedge/assets/images/" + name + ".png";
  }

  /** The width of one of the game's images, by name. */
  protected static double imageWidth(String name) {
    String costume = "measure_" + name;
    IMAGE_MEASURER.addCostume(costume, imagePath(name));
    IMAGE_MEASURER.switchCostume(costume);
    return IMAGE_MEASURER.getWidth();
  }

  /**
   * A text drawn with one of the game's bitmap fonts (e.g. "font" or
   * "status1b_"), on top of the scene.
   */
  protected static BitmapText bitmapText(String font) {
    BitmapText text = new BitmapText(imagePath(font + "%03d"));
    text.setSpaceWidth(30);
    // Characters in the main font are italic so should overlap a little
    text.setLetterSpacing(font.equals("font") ? FONT_LETTER_SPACING : 0);
    // The % sign substitutes for the controller button image
    text.addGlyph('%', imagePath("xb_a"));
    text.setIsUI(true);
    return text;
  }

  /** Positions a text with its top left corner at the given pixel position. */
  protected static void placeText(BitmapText text, double x, double y, boolean centre) {
    text.setAlign(centre ? TextAlign.CENTER : TextAlign.LEFT);
    text.setPosition(x - HALF_WIDTH, HALF_HEIGHT - y);
  }

  /**
   * Builds the track and the grid of cars. Called again to reset the demo race
   * on the title screen.
   */
  protected void initWorld(Controls controls) {
    this.track = this.makeTrack();

    // We only create a player car (in setupCars) when there is a controls
    // object
    this.playerCar = null;
    this.cameraFollowCar = null;

    this.setupCars(controls);

    this.camera.x = 0;
    this.camera.y = 400;
    this.camera.z = 0;

    this.bgOffsetX = -Math.floor(this.backgroundWidth / 2.0);
    this.bgOffsetY = 30;

    this.firstFrame = true;
    this.frameCounter = 0;
    this.timer = 0;
    this.raceComplete = false;
    this.timeUp = false;
    this.accumulatedTime = 0;
    this.startTimer = this.playerCar != null ? 3.999 : 0;
  }

  private void setupCars(Controls controls) {
    // Will be kept in sorted order of position
    this.cars.clear();
    for (int i = 0; i < NUM_CARS; i++) {
      double z = -3 - i * GRID_CAR_SPACING;
      double x = i % 2 == 0 ? -400 : 400;
      if (i == 0 && controls != null) {
        // Don't create a player car on the title screen
        this.playerCar = new PlayerCar(this, new Vector3(x, 0, z), controls);
        this.cars.add(this.playerCar);
      } else {
        double targetSpeed =
            Operators.map(i, 0, NUM_CARS - 1, CPU_CAR_MIN_TARGET_SPEED, CPU_CAR_MAX_TARGET_SPEED);
        double accel = Operators.map(i, 0, NUM_CARS - 1, 1.5, 2);
        this.cars.add(new CPUCar(this, new Vector3(x, 0, z), accel, targetSpeed));
      }
    }

    this.cameraFollowCar = this.playerCar != null ? this.playerCar : this.cars.get(0);
  }

  @Override
  public void run() {
    double deltaTime = this.getDeltaTime();
    if (this.controls != null) {
      this.controls.update();
    }
    this.keyboardControls.update();

    // Use the library's elapsed game time in fixed physics steps, preserving
    // the original racer's integration when frame intervals vary.
    this.accumulatedTime += deltaTime;
    while (this.accumulatedTime >= FIXED_TIMESTEP) {
      this.accumulatedTime -= FIXED_TIMESTEP;
      this.update(FIXED_TIMESTEP);
    }

    // Keep the music looping, like pgzero's music.play
    if (this.playerCar != null && !this.raceComplete) {
      this.keepMusicLooping(this.startTimer > 0 ? "engines_startline" : "ambience");
    }

    // Race complete screen - wait for the accelerate button to return to the
    // title screen
    if (this.raceComplete
        && this.playerCar != null
        && this.keyboardControls.buttonPressed(0)) {
      this.playerCar.stopEngineSound();
      this.stopAllGameSounds();
      Window.getInstance().setStage(new Menu());
    }
    this.updateStatus();
  }

  /** The game world update, called at a fixed timestep like the original. */
  public void update(double deltaTime) {
    this.timer += deltaTime;
    this.frameCounter += 1;

    // Race start sequence
    if (this.startTimer > 0) {
      // Ensure cars are added to the appropriate track piece's car list, so
      // that they're displayed during the start countdown
      for (Car car : this.cars) {
        car.updateCurrentTrackPiece();
      }
      double timerOld = this.startTimer;
      this.startTimer = Math.max(0, this.startTimer - deltaTime);
      // Every second of the countdown, make a sound effect
      if (this.startTimer == 0) {
        // Go! Ambience is stereo so is treated as music
        this.playMusic("ambience");
        this.playSound("gobeep", 1);
      } else if ((int) timerOld != (int) this.startTimer) {
        this.playSound("startbeep", 1);
      }
    }

    double oldCameraZ = this.camera.z;
    Integer prevAhead = this.getFirstTrackPieceAheadIndex(oldCameraZ);

    // If race has started, update all cars
    if (this.startTimer == 0) {
      for (Car car : this.cars) {
        car.update(deltaTime);
      }
    }

    // Is the race complete?
    if (!this.raceComplete && this.playerCar != null) {
      // End the game if lap time reaches 4 mins, or if the player presses
      // Escape. Also allow the player to finish the race normally.
      if (this.playerCar.lapTime >= 60 * 4 || this.isKeyPressed(KeyCode.ESCAPE)) {
        this.stopMusic();
        this.timeUp = true;
        this.raceComplete = true;
      } else if (this.playerCar.lap > NUM_LAPS) {
        this.stopMusic();
        this.raceComplete = true;

        this.playSound("game_complete", 1);
      }

      // Sort cars in the list based on race positions
      this.cars.sort(Comparator.comparingDouble(car -> car.pos.z));
    }

    // Update camera position to follow player car
    this.camera.x = this.cameraFollowCar.pos.x;
    this.camera.z = this.cameraFollowCar.pos.z + CAMERA_FOLLOW_DISTANCE;

    // As camera moves around corners, add to the background offset and shift
    // the player car's X position so that steering is required on corners
    Integer newAhead = this.getFirstTrackPieceAheadIndex(this.camera.z);

    double distance = oldCameraZ - this.camera.z;
    double offsetChangeX = 0;
    double offsetChangeY = 0;
    if (distance > 0 && !this.firstFrame && prevAhead != null && newAhead != null) {
      double oldZNextSpacingBoundary = Math.floor(oldCameraZ / SPACING) * SPACING;
      double newZPrevSpacingBoundary =
          Math.floor(this.camera.z / SPACING) * SPACING + SPACING;
      TrackPiece prevTrack = this.track.get(prevAhead);
      TrackPiece newTrack = this.track.get(newAhead);
      if (newAhead > prevAhead) {
        // Movement touches at least two track pieces. Figure out how much of
        // the movement was within the old and new track pieces, plus whether
        // there are any intermediate track pieces between them
        double distanceFirst = oldCameraZ - oldZNextSpacingBoundary;
        double distanceLast = newZPrevSpacingBoundary - this.camera.z;
        double fractionFirst = distanceFirst / SPACING;
        double fractionLast = distanceLast / SPACING;

        offsetChangeX =
            prevTrack.offsetX * fractionFirst + newTrack.offsetX * fractionLast;
        offsetChangeY =
            prevTrack.offsetY * fractionFirst + newTrack.offsetY * fractionLast;

        if (newAhead - prevAhead > 1) {
          // The movement spans three or more track pieces, so fully add the
          // offsets of the in-between pieces
          for (int i = prevAhead + 1; i < newAhead; i++) {
            TrackPiece piece = this.track.get(i);
            offsetChangeX += piece.offsetX;
            offsetChangeY += piece.offsetY;
          }
        }
      } else {
        // Movement was just within one track piece
        double fraction = distance / SPACING;
        offsetChangeX = prevTrack.offsetX * fraction;
        offsetChangeY = prevTrack.offsetY * fraction;
      }

      // Shift background by the calculated offset
      this.bgOffsetX += offsetChangeX;
      this.bgOffsetY += offsetChangeY;

      // Keep bgOffsetX within the range -backgroundWidth to +backgroundWidth
      while (this.bgOffsetX < -this.backgroundWidth) {
        this.bgOffsetX += this.backgroundWidth;
      }
      while (this.bgOffsetX > this.backgroundWidth) {
        this.bgOffsetX -= this.backgroundWidth;
      }
    }

    // Shift player car's X offset - this means the car will go off the track
    // if you go around a corner without steering
    if (this.playerCar != null) {
      this.playerCar.setOffsetXChange(offsetChangeX);
    }

    // This deals with moving the background when the camera is moving
    // backwards (only possible in the original's debug camera mode)
    if (newAhead != null && prevAhead != null && newAhead < prevAhead) {
      this.bgOffsetX -= this.track.get(prevAhead).offsetX;
      this.bgOffsetY -= this.track.get(prevAhead).offsetY;
    }

    this.firstFrame = false;
  }

  // Sound helpers. Some sounds have multiple varieties - if count > 1, we
  // randomly choose one of those.

  public void playSound(String name, int count) {
    this.playSound(name + Random.randomInt(0, count - 1));
  }

  /** Plays one of the looping music tracks, stopping any other. */
  public void playMusic(String name) {
    this.stopMusic();
    this.music.playSound(name);
  }

  public void stopMusic() {
    this.music.stopAllSounds();
  }

  /** playSound restarts a finished track and leaves a playing track alone. */
  protected void keepMusicLooping(String name) {
    this.music.playSound(name);
  }

  /** Stops all sounds of this stage, including the music. */
  public void stopAllGameSounds() {
    this.stopMusic();
    this.stopAllSounds();
  }

  // Track helpers

  /**
   * Returns the index of the track piece at the specified Z position, or null
   * if the position is off the end of the track.
   */
  public Integer getTrackPieceForZ(double z) {
    int idx = -(int) (z / SPACING);
    if (idx >= this.track.size()) {
      return null;
    }
    return idx;
  }

  /**
   * Returns the index of the first track piece ahead of or exactly at the
   * specified Z position, or null if the position is off the end of the track.
   */
  public Integer getFirstTrackPieceAheadIndex(double z) {
    int idx = -(int) Math.floor(z / SPACING);
    if (idx >= this.track.size()) {
      return null;
    }
    return idx;
  }

  /** The Z position at which the track piece with the given index starts. */
  public double trackPieceAheadZ(int idx) {
    return -idx * SPACING;
  }

  private static List<Scenery> generateScenery(int trackI, String image, int interval) {
    return generateScenery(trackI, image, interval, true);
  }

  private static List<Scenery> generateScenery(
      int trackI, String image, int interval, boolean lamps) {
    List<Scenery> scenery = new ArrayList<>();
    if (trackI % interval == 0) {
      // Billboards
      scenery.add(new Billboard(BILLBOARD_X, image));
      scenery.add(new Billboard(-BILLBOARD_X, image));
    } else if (lamps && trackI % 30 == 0) {
      // Lamps
      scenery.add(new LampLeft());
      scenery.add(new LampRight());
    }
    return scenery;
  }

  private List<TrackPiece> makeTrack() {
    List<TrackPiece> track = new ArrayList<>();
    for (int lap = 0; lap < NUM_LAPS + 1; lap++) {
      for (int i = 0; i < 15; i++) {
        track.add(new TrackPiece(generateScenery(i, BILLBOARD_02, 40), 0, 0, null));
      }

      // Start gantry
      track.add(new TrackPieceStartLine(this));

      for (int i = 0; i < SECTION_SHORT; i++) {
        track.add(new TrackPiece(generateScenery(i, BILLBOARD_00, 40), 0, 0, null));
      }

      // Mild right turn followed by short straight
      for (int i = 0; i < SECTION_MEDIUM; i++) {
        track.add(new TrackPiece(generateScenery(i, BILLBOARD_00, 40), -4, 0, null));
      }
      for (int i = 0; i < SECTION_SHORT; i++) {
        track.add(new TrackPiece(generateScenery(i, BILLBOARD_00, 40), 0, 0, null));
      }

      // Slight downward slope, going into moderate right hand turn
      for (int i = 0; i < SECTION_VERY_SHORT; i++) {
        track.add(new TrackPiece(generateScenery(i, BILLBOARD_00, 40), 0, -1, null));
      }
      for (int i = 0; i < SECTION_VERY_SHORT; i++) {
        track.add(new TrackPiece(generateScenery(i, BILLBOARD_00, 40), 0, -2, null));
      }
      for (int i = 0; i < SECTION_VERY_SHORT; i++) {
        track.add(new TrackPiece(generateScenery(i, BILLBOARD_00, 40), -2, -1, null));
      }
      for (int i = 0; i < SECTION_VERY_SHORT; i++) {
        track.add(new TrackPiece(generateScenery(i, BILLBOARD_03, 40), -5, 0, null));
      }
      for (int i = 0; i < SECTION_MEDIUM; i++) {
        track.add(new TrackPiece(generateScenery(i, BILLBOARD_03, 40), -10, 0, null));
      }

      // Short straight
      for (int i = 0; i < SECTION_SHORT; i++) {
        track.add(new TrackPiece(generateScenery(i, BILLBOARD_00, 40), 0, 0, null));
      }

      // Medium-sharp turn left, slight upward slope
      for (int i = 0; i < SECTION_MEDIUM; i++) {
        track.add(new TrackPiece(generateScenery(i, ARROW_LEFT, 10), 13, 1, null));
      }

      for (int i = 0; i < SECTION_MEDIUM; i++) {
        track.add(new TrackPiece(generateScenery(i, BILLBOARD_02, 40), 0, 0, null));
      }

      // Small hill
      for (int i = 0; i < SECTION_MEDIUM; i++) {
        track.add(new TrackPiece(generateScenery(i, BILLBOARD_02, 40), 0, 2, null));
      }

      // Slightly down and to the right
      for (int i = 0; i < SECTION_LONG; i++) {
        track.add(new TrackPiece(generateScenery(i, BILLBOARD_01, 40), -3, -1, null));
      }

      // Crazy downward curve
      for (int i = 0; i < SECTION_MEDIUM; i++) {
        track.add(new TrackPiece(generateScenery(i, BILLBOARD_00, 40), 0, -4, null));
      }

      // Upward slope
      for (int i = 0; i < SECTION_LONG; i++) {
        track.add(new TrackPiece(generateScenery(i, BILLBOARD_03, 40), 0, 2, null));
      }

      // Turn to left and up, gradually increasing curve
      for (int j = 1; j < 10; j++) {
        for (int i = 0; i < SECTION_VERY_SHORT; i++) {
          track.add(new TrackPiece(generateScenery(i, BILLBOARD_00, 40), j, j, null));
        }
      }

      // Downward curve, increasing then decreasing in intensity
      for (int j = 1; j < 10; j++) {
        for (int i = 0; i < SECTION_VERY_SHORT; i++) {
          track.add(new TrackPiece(generateScenery(i, BILLBOARD_00, 40), 0, -j, null));
        }
      }

      // Straight with chevron billboards at end. CPU cars will slow down in
      // this section.
      for (int i = 0; i < SECTION_MEDIUM; i++) {
        track.add(new TrackPiece(new ArrayList<>(), 0, 0, 60.0));
      }
      for (int i = 0; i < SECTION_SHORT; i++) {
        track.add(new TrackPiece(generateScenery(i, ARROW_RIGHT, 10, false), 0, 0, 58.0));
      }
      for (int i = 0; i < SECTION_SHORT; i++) {
        track.add(new TrackPiece(generateScenery(i, ARROW_RIGHT, 10, false), 0, 0, 58.0));
      }

      // Sharp turn right, easing off slightly at end
      for (int i = 0; i < SECTION_SHORT; i++) {
        track.add(new TrackPiece(generateScenery(i, ARROW_RIGHT, 10, false), -15, 0, 55.0));
      }
      for (int i = 0; i < SECTION_SHORT; i++) {
        track.add(new TrackPiece(generateScenery(i, ARROW_RIGHT, 10, false), -13, 0, 57.0));
      }
      for (int i = 0; i < SECTION_SHORT; i++) {
        track.add(new TrackPiece(generateScenery(i, BILLBOARD_00, 40), -11, 0, null));
      }
      for (int i = 0; i < SECTION_SHORT; i++) {
        track.add(new TrackPiece(generateScenery(i, BILLBOARD_00, 40), -9, 0, null));
      }

      // Straight
      for (int i = 0; i < SECTION_MEDIUM; i++) {
        track.add(new TrackPiece(generateScenery(i, BILLBOARD_00, 40), 0, 0, null));
      }

      // Cosine hills
      for (int i = 0; i < SECTION_LONG; i++) {
        track.add(
            new TrackPiece(generateScenery(i, BILLBOARD_00, 40), 0, Math.cos(i / 20.0) * 5, null));
      }

      // Mild upward slope - the purpose is to reset the Y scrolling of the
      // background so it roughly matches the position at the start of the lap
      for (int i = 0; i < SECTION_LONG; i++) {
        track.add(new TrackPiece(generateScenery(i, BILLBOARD_03, 40), 0, 0.25, null));
      }

      // Short straight
      for (int i = 0; i < SECTION_SHORT; i++) {
        track.add(new TrackPiece(generateScenery(i, BILLBOARD_03, 40), 0, 0, null));
      }
    }
    return track;
  }

  // Utility functions, ported from the original

  /**
   * Inverse lerp: returns a number which indicates where value falls between a
   * and b (0 at a, 1 at b).
   */
  public static double inverseLerp(double a, double b, double value) {
    if (a != b) {
      return Operators.constrain((value - a) / (b - a), 0, 1);
    }
    return 0;
  }

  /** Returns 1, 0 or -1 depending on whether the number is positive, zero or negative. */
  public static int sign(double x) {
    if (x == 0) {
      return 0;
    }
    return x < 0 ? -1 : 1;
  }

  public static double moveTowards(double n, double target, double speed) {
    if (n < target) {
      return Math.min(n + speed, target);
    }
    return Math.max(n - speed, target);
  }

  /** Returns a time string in the form "minutes:seconds.milliseconds". */
  public static String formatTime(double seconds) {
    return String.format(Locale.ROOT, "%d:%06.3f", (int) Math.floor(seconds / 60), seconds % 60);
  }

  // Drawing. The buffer of the stage has its origin at the centre of the
  // window with y growing downwards, so a pixel position (x, y) is drawn at
  // (x - WIDTH/2, y - HEIGHT/2).

  private record CarDraw(double z, Runnable draw) {}

  /** A sprite without a costume which draws the whole scene itself. */
  private class Scene extends DrawingSprite {
    @Override
    public void draw(Drawing drawing) {
      Play.this.drawScene(drawing);
    }
  }

  /** Draws the whole game, mirroring the original's Game.draw method. */
  protected void drawScene(Drawing d) {
    // Fill background with a single colour. We use a different background
    // colour depending on the Y offset of the background image, because the
    // top and bottom of that image are different colours
    if (this.bgOffsetY > 0) {
      d.rectangle(0, 0, WIDTH, HEIGHT, new Color(0, 20, 117));
    } else {
      d.rectangle(0, 0, WIDTH, HEIGHT, new Color(0, 77, 180));
    }

    // Draw background. Need to draw either one or two copies - the second is
    // for wrapping
    this.blit(d, "background", this.bgOffsetX, this.bgOffsetY);
    if (this.bgOffsetX > 0) {
      this.blit(d, "background", this.bgOffsetX - this.backgroundWidth, this.bgOffsetY);
    }
    if (this.bgOffsetX + this.backgroundWidth < WIDTH) {
      this.blit(d, "background", this.bgOffsetX + this.backgroundWidth, this.bgOffsetY);
    }

    // Instead of drawing track pieces etc as we come across them, we store
    // draw calls in this list. Once we've finished going through the track
    // pieces, we execute the draw calls in reverse order, so that track
    // pieces, cars and scenery in the distance are drawn before things which
    // are closer
    List<Runnable> drawList = new ArrayList<>();

    Integer firstTrackPieceIdx = this.getFirstTrackPieceAheadIndex(this.camera.z);
    if (firstTrackPieceIdx != null) {
      boolean isFirstTrackPieceAhead = true;

      // The cumulative changes in track offsets, so that each track piece is
      // drawn in the correct position
      Vector3 offset = new Vector3(0, 0, 0);
      Vector3 offsetDelta = new Vector3(0, 0, 0);

      // Screen positions of the left and right edges of the track, the central
      // stripes and the rumble strips. We remember them so they don't need to
      // be recalculated when joining up a track piece with the previous one
      double[][] prevTrackScreen = null;
      double[][] prevStripeScreen = null;
      double[] prevRumbleLeftOuterScreen = null;
      double[] prevRumbleRightOuterScreen = null;
      double[] prevYellowLeftOuterScreen = null;
      double[] prevYellowLeftInnerScreen = null;
      double[] prevYellowRightOuterScreen = null;
      double[] prevYellowRightInnerScreen = null;

      double currentPieceZ = this.trackPieceAheadZ(firstTrackPieceIdx) + SPACING;
      int trackAheadI = 0;

      // Go through each track piece ahead
      for (int i = firstTrackPieceIdx; i < this.track.size(); i++) {
        // Stop when we've displayed VIEW_DISTANCE number of track pieces
        trackAheadI += 1;
        if (trackAheadI > VIEW_DISTANCE) {
          break;
        }

        TrackPiece trackPiece = this.track.get(i);
        currentPieceZ -= SPACING;

        // Because the camera is pointing down the negative Z axis,
        // negative/positive X mean right/left from the camera's perspective
        Vector3 left = new Vector3(trackPiece.width / 2, 0, currentPieceZ);
        Vector3 right = new Vector3(-trackPiece.width / 2, 0, currentPieceZ);

        // Interpolate for X offset between the first and next track piece.
        // Without this, going around corners would look very juddery
        if (isFirstTrackPieceAhead) {
          double adjustedCameraZ = this.camera.z - SPACING;
          double fraction =
              inverseLerp(currentPieceZ - SPACING, currentPieceZ, adjustedCameraZ);
          offsetDelta = new Vector3(fraction * trackPiece.offsetX, fraction * trackPiece.offsetY, 0);
        } else {
          offsetDelta = offsetDelta.plus(new Vector3(trackPiece.offsetX, trackPiece.offsetY, 0));
        }

        isFirstTrackPieceAhead = false;

        offset.add(offsetDelta);

        left.add(offset);
        right.add(offset);

        // Calculate screen positions of track boundaries
        double[] leftScreen = this.transformPoint(left);
        double[] rightScreen = this.transformPoint(right);

        // Calculate screen pos of central stripe. Always work out stripe
        // points even for pieces which don't need them, because the next
        // track piece may make use of the calculated points to connect up to
        double[] stripeLeftScreen =
            this.transformPoint(new Vector3(HALF_STRIPE_W, 0, currentPieceZ).plus(offset));
        double[] stripeRightScreen =
            this.transformPoint(new Vector3(-HALF_STRIPE_W, 0, currentPieceZ).plus(offset));

        // Calculate screen pos of outer parts of left/right rumble strips
        double[] rumbleLeftOuterScreen =
            this.transformPoint(left.plus(new Vector3(HALF_RUMBLE_STRIP_W, 0, 0)));
        double[] rumbleRightOuterScreen =
            this.transformPoint(right.minus(new Vector3(HALF_RUMBLE_STRIP_W, 0, 0)));

        // Calculate screen pos of left and right yellow lines, which are just
        // inside the outer edges of the track
        double[] yellowLeftOuterScreen =
            this.transformPoint(left.minus(new Vector3(YELLOW_LINE_DISTANCE_FROM_EDGE, 0, 0)));
        double[] yellowLeftInnerScreen =
            this.transformPoint(
                left.minus(
                    new Vector3(YELLOW_LINE_DISTANCE_FROM_EDGE + HALF_YELLOW_LINE_W, 0, 0)));
        double[] yellowRightOuterScreen =
            this.transformPoint(right.plus(new Vector3(YELLOW_LINE_DISTANCE_FROM_EDGE, 0, 0)));
        double[] yellowRightInnerScreen =
            this.transformPoint(
                right.plus(
                    new Vector3(YELLOW_LINE_DISTANCE_FROM_EDGE + HALF_YELLOW_LINE_W, 0, 0)));

        // Only draw if both points are in front of the clipping plane
        if (leftScreen != null && rightScreen != null) {
          // To draw, there must be a previous track piece that we can
          // connect to
          if (prevTrackScreen != null) {
            // Draw stripe (3m on/off)
            if (i / 3 % 2 == 0) {
              this.drawPoints(
                  d,
                  drawList,
                  new double[][] {
                    stripeLeftScreen, stripeRightScreen, prevStripeScreen[1], prevStripeScreen[0]
                  },
                  STRIPE_COLOUR);
            }

            // Draw yellow lines. This is before the drawing of the track as we
            // want to draw on top of the track, and items in the draw list
            // are drawn in reverse order
            if (SHOW_YELLOW_LINES) {
              this.drawPoints(
                  d,
                  drawList,
                  new double[][] {
                    prevYellowLeftOuterScreen,
                    yellowLeftOuterScreen,
                    yellowLeftInnerScreen,
                    prevYellowLeftInnerScreen
                  },
                  YELLOW_LINE_COL);
              this.drawPoints(
                  d,
                  drawList,
                  new double[][] {
                    prevYellowRightOuterScreen,
                    yellowRightOuterScreen,
                    yellowRightInnerScreen,
                    prevYellowRightInnerScreen
                  },
                  YELLOW_LINE_COL);
            }

            // Draw track
            double[][] points =
                new double[][] {prevTrackScreen[0], leftScreen, rightScreen, prevTrackScreen[1]};
            this.drawPoints(d, drawList, points, trackPiece.col);

            // Draw rumble strip. This is before trackside as it draws on top
            // of trackside, and items in the draw list are drawn in reverse
            // order
            if (SHOW_RUMBLE_STRIPS) {
              int[] rumbleCol = (i / 2) % 2 == 0 ? RUMBLE_COLOUR_1 : RUMBLE_COLOUR_2;
              this.drawPoints(
                  d,
                  drawList,
                  new double[][] {
                    prevRumbleLeftOuterScreen, prevTrackScreen[0], leftScreen,
                    rumbleLeftOuterScreen
                  },
                  rumbleCol);
              this.drawPoints(
                  d,
                  drawList,
                  new double[][] {
                    prevRumbleRightOuterScreen, prevTrackScreen[1], rightScreen,
                    rumbleRightOuterScreen
                  },
                  rumbleCol);
            }

            // Draw trackside
            if (SHOW_TRACKSIDE) {
              int[] tracksideCol = (i / 5) % 2 == 0 ? TRACKSIDE_COLOUR_1 : TRACKSIDE_COLOUR_2;
              this.drawPoints(
                  d,
                  drawList,
                  new double[][] {
                    points[2], points[3], new double[] {0, points[3][1]},
                    new double[] {0, points[2][1]}
                  },
                  tracksideCol);
              this.drawPoints(
                  d,
                  drawList,
                  new double[][] {
                    points[0], points[1], new double[] {WIDTH - 1, points[1][1]},
                    new double[] {WIDTH - 1, points[0][1]}
                  },
                  tracksideCol);
            }
          }

          // Store screen positions of various parts of the track, as they
          // form half of the polygon for the next track piece
          prevTrackScreen = new double[][] {leftScreen, rightScreen};
          prevStripeScreen = new double[][] {stripeLeftScreen, stripeRightScreen};
          prevRumbleLeftOuterScreen = rumbleLeftOuterScreen;
          prevRumbleRightOuterScreen = rumbleRightOuterScreen;
          prevYellowLeftOuterScreen = yellowLeftOuterScreen;
          prevYellowLeftInnerScreen = yellowLeftInnerScreen;
          prevYellowRightOuterScreen = yellowRightOuterScreen;
          prevYellowRightInnerScreen = yellowRightInnerScreen;
        }

        // Draw scenery for the current track piece
        if (SHOW_SCENERY) {
          for (Scenery obj : trackPiece.scenery) {
            if (trackAheadI * SPACING < obj.maxDrawDistance) {
              Vector3 posV3 = new Vector3(obj.x, 0, currentPieceZ).plus(offset);
              if (this.camera.z - currentPieceZ > obj.minDrawDistance) {
                String billboard = imagePath(obj.getImageName());
                double[] t =
                    this.transformSprite(
                        posV3,
                        d.imageWidth(billboard) * obj.scale,
                        d.imageHeight(billboard) * obj.scale,
                        CLIPPING_PLANE);
                // If a piece of scenery is very close to the camera, the
                // scaled size may become enormous. Don't try to draw such
                // scenery
                if (t != null && t[2] < MAX_SCENERY_SCALED_WIDTH && t[2] >= 1 && t[3] >= 1) {
                  // Anchor point at bottom
                  double bx = t[0] - Math.floor(t[2] / 2);
                  double by = t[1] - t[3];
                  int sw = (int) t[2];
                  int sh = (int) t[3];
                  drawList.add(() -> d.image(billboard,
                      bx - HALF_WIDTH + sw / 2.0,
                      HALF_HEIGHT - (by + sh / 2.0), sw, sh));
                }
              }
            }
          }
        }

        // Draw cars
        List<CarDraw> carsToDraw = new ArrayList<>();
        for (Car car : trackPiece.cars) {
          // Each car needs to be drawn during the track piece it is on, but
          // with an additional offset interpolated towards the next track
          // piece, so that it starts turning a corner as it reaches the piece
          Vector3 carOffset = new Vector3(offset);
          if (pythonMod(car.pos.z, SPACING) != 0) {
            // Interpolate offset between this and next track piece
            double fraction = inverseLerp(currentPieceZ, currentPieceZ - SPACING, car.pos.z);
            double nextOffsetX = 0;
            double nextOffsetY = 0;
            if (i + 1 < this.track.size()) {
              TrackPiece nextTrackPiece = this.track.get(i + 1);
              nextOffsetX = nextTrackPiece.offsetX;
              nextOffsetY = nextTrackPiece.offsetY;
            }
            carOffset.add(
                fraction * nextOffsetX, fraction * nextOffsetY, -fraction * SPACING);

            // This ensures that the car's forward motion is correct on pieces
            // following a piece with an offset
            carOffset.add(
                offsetDelta.x * fraction, offsetDelta.y * fraction, offsetDelta.z * fraction);
          }

          // The rules for drawing the camera follow car are a bit different.
          // If we drew it in the same way, its position on the screen would
          // be a bit off as it would start going around corners before the
          // camera does. So don't apply any offset.
          if (car == this.cameraFollowCar) {
            carOffset.x = 0;
            carOffset.y = 0;
          }

          Vector3 posV3 = new Vector3(car.pos.x, 0, currentPieceZ).plus(carOffset);
          double scale = 2;

          // For CPU cars, choose the sprite to use based on the car's angle
          // in relation to the camera
          if (car instanceof CPUCar cpuCar) {
            // Approximate the angle we're seeing the car from. The further the
            // car is ahead, the smaller the effect
            double zDistance = Math.max(1, -(posV3.z - this.camera.z));
            double offsetForAngle = (posV3.x - this.camera.x) / zDistance;
            offsetForAngle += -cpuCar.steering * 10;
            int angleSpriteIdx = (int) Operators.constrain(Operators.map(offsetForAngle, -200, 200, -4, 4), -4, 4);

            // If this is the camera follow car (which for a CPU car will only
            // be the case during the title screen), limit to only the
            // shallowest angles
            if (car == this.cameraFollowCar) {
              angleSpriteIdx = Operators.constrain(angleSpriteIdx, -1, 1);
            }

            car.updateSprite(angleSpriteIdx, false, false);
          }

          // Calculate screen pos and scaled sprite size for car
          String carImage = imagePath(car.image);
          double[] t =
              this.transformSprite(
                  posV3, d.imageWidth(carImage) * scale, d.imageHeight(carImage) * scale,
                  CLIPPING_PLANE_CARS);

          if (t != null && t[2] < MAX_CAR_SCALED_WIDTH && t[2] >= 1 && t[3] >= 1) {
            // Anchor point at bottom, centre
            double bx = t[0] - Math.floor(t[2] / 2);
            double by = t[1] - t[3];
            int sw = (int) t[2];
            int sh = (int) t[3];
            carsToDraw.add(
                new CarDraw(
                    car.pos.z,
                    () -> d.image(carImage,
                        bx - HALF_WIDTH + sw / 2.0,
                        HALF_HEIGHT - (by + sh / 2.0), sw, sh)));
          }
        }

        // Draw the cars that are on the current track piece, starting from
        // the one furthest from the camera
        carsToDraw.sort(Comparator.comparingDouble(CarDraw::z).reversed());
        for (CarDraw entry : carsToDraw) {
          drawList.add(entry.draw());
        }
      }
    }

    // Draw everything in drawList, in reverse order - so that items furthest
    // ahead are drawn first
    for (int k = drawList.size() - 1; k >= 0; k--) {
      drawList.get(k).run();
    }

    if (this.playerCar != null && !this.timeUp && !this.raceComplete) {
      this.blit(d, "status", WIDTH / 2.0 - 565 / 2.0, 0);
    }
  }

  /** Refreshes the interface after simulation; drawing a paused scene changes no text. */
  private void updateStatus() {
    for (BitmapText text : this.texts) {
      text.showText("");
    }
    if (this.playerCar != null) {
      // Show info text
      int playerPos = this.cars.indexOf(this.playerCar) + 1;

      // Show race complete or time up screens if relevant
      if (this.timeUp) {
        this.timeUpText.showText("TIME UP!");
      } else if (this.raceComplete) {
        this.raceCompleteText.showText("RACE COMPLETE!");
        this.racePositionLabelText.showText("POSITION");
        this.racePositionText.showText(Integer.toString(playerPos));
        this.fastestLapLabelText.showText("FASTEST LAP");
        this.fastestLapTimeText.showText(formatTime(this.playerCar.fastestLap));
        this.raceTimeLabelText.showText("RACE TIME");
        this.raceTimeText.showText(formatTime(this.playerCar.raceTime));
      } else {
        // Race not complete - show status text at top of screen

        // Show lap, position, speed and lap time
        this.lapText.showText(String.format("%02d", this.playerCar.lap));
        this.positionText.showText(String.format("%02d", playerPos));
        this.speedText.showText(String.format("%03d", (int) this.playerCar.speed));
        this.lapTimeText.showText(formatTime(this.playerCar.lapTime));

        // Show fastest lap
        if (this.playerCar.lastLapWasFastest && this.playerCar.lapTime < 4) {
          this.messageText.showText("FASTEST LAP!");
          this.messageTimeText.showText(formatTime(this.playerCar.fastestLap));
        }

        // Show final lap text. If we're currently showing fastest lap text,
        // wait for that to disappear before showing the final lap text
        double beginTime = this.playerCar.lastLapWasFastest ? 4 : 0;
        double endTime = this.playerCar.lastLapWasFastest ? 8 : 4;
        if (this.playerCar.lap == NUM_LAPS
            && beginTime < this.playerCar.lapTime
            && this.playerCar.lapTime < endTime) {
          this.messageText.showText("FINAL LAP!");
        }
      }
    }
  }

  /** Adds a polygon to the draw list, if any of its points are on screen. */
  private void drawPoints(Drawing d, List<Runnable> drawList, double[][] points, int[] col) {
    // point[1] gets Y
    for (double[] point : points) {
      if (point[1] < HEIGHT) {
        drawList.add(() -> this.fillPolygon(d, points, col));
        return;
      }
    }
  }

  private void fillPolygon(Drawing d, double[][] points, int[] col) {
    double[] xs = new double[points.length];
    double[] ys = new double[points.length];
    for (int i = 0; i < points.length; i++) {
      xs[i] = points[i][0] - HALF_WIDTH;
      ys[i] = HALF_HEIGHT - points[i][1];
    }
    d.polygon(xs, ys, new Color(col[0], col[1], col[2]));
  }

  /**
   * Blits an image at the given pixel position (top-left origin), like the
   * original's screen.blit.
   */
  protected void blit(Drawing d, String name, double x, double y) {
    String path = imagePath(name);
    d.image(path,
        x - HALF_WIDTH + d.imageWidth(path) / 2.0,
        HALF_HEIGHT - y - d.imageHeight(path) / 2.0);
  }

  /**
   * Transforms a point into screen space, or returns null if the point is not
   * in front of the clipping plane.
   */
  private double[] transformPoint(Vector3 point) {
    double newZ = point.z - this.camera.z;
    if (newZ > CLIPPING_PLANE) {
      return null;
    }
    // Apply perspective and centre on the screen
    return new double[] {
      (point.x - this.camera.x) / newZ + HALF_WIDTH,
          (point.y - this.camera.y) / newZ + HALF_HEIGHT
    };
  }

  /**
   * Transforms a point into screen space and works out the scaled width and
   * height of a sprite of the given size at that distance, or returns null if
   * the point is not in front of the given clipping plane.
   */
  private double[] transformSprite(Vector3 point, double w, double h, double clippingPlane) {
    double newZ = point.z - this.camera.z;
    if (newZ > clippingPlane) {
      return null;
    }
    return new double[] {
      (point.x - this.camera.x) / newZ + HALF_WIDTH,
          (point.y - this.camera.y) / newZ + HALF_HEIGHT,
          w / -newZ,
          h / -newZ
    };
  }

  /** Python's modulo, which always returns a value with the sign of the divisor. */
  private static double pythonMod(double a, double b) {
    return a - Math.floor(a / b) * b;
  }

}
