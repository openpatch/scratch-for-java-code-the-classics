package beatstreets;

import org.openpatch.scratch.Window;

public class Game extends Window {

  public static final int WIDTH = 800;
  public static final int HEIGHT = 480;

  public static final int MIN_WALK_Y = 310;

  public static final int ENEMY_APPROACH_PLAYER_DISTANCE = 85;
  public static final int ENEMY_APPROACH_PLAYER_DISTANCE_SCOOTERBOY = 140;
  public static final int ENEMY_APPROACH_PLAYER_DISTANCE_BARREL = 180;

  public static final int BACKGROUND_TILE_SPACING = 290;

  public static final int HEALTH_STAMINA_BAR_WIDTH = 235;
  public static final int HEALTH_STAMINA_BAR_HEIGHT = 26;

  public static final double FLYING_KICK_VEL_X = 3;
  public static final double FLYING_KICK_VEL_Y = -8;

  public static final double JUMP_GRAVITY = 0.4;
  public static final double THROWN_GRAVITY = 0.025;
  public static final double WEAPON_GRAVITY = 0.5;

  public static final double BARREL_THROW_VEL_X = 4;
  public static final double BARREL_THROW_VEL_Y = 0;

  public static final double PLAYER_THROW_VEL_X = 5;
  public static final double PLAYER_THROW_VEL_Y = 0.5;

  public static final double BASE_STAMINA_DAMAGE_MULTIPLIER = 100;
  public static final double MIN_STAMINA = -100;

  public static final boolean INTRO_ENABLED = true;

  public Game() {
    super(WIDTH, HEIGHT, "beatstreets/assets");
    this.setStage(new TitleStage());
  }

  public static void main(String[] args) {
    new Game();
  }
}
