package beatstreets;

import java.util.List;

/**
 * A stage of the level: a group of enemies plus the length the level must
 * scroll before the stage is complete. Named LevelStage so it doesn't clash
 * with the Scratch for Java Stage.
 */
public class LevelStage {

  public final List<Enemy> enemies;
  public final List<Weapon> weapons;
  public final List<Powerup> powerups;
  public final int maxScrollX;

  public LevelStage(int maxScrollX, List<Enemy> enemies, List<Weapon> weapons,
      List<Powerup> powerups) {
    this.maxScrollX = maxScrollX;
    this.enemies = enemies;
    this.weapons = weapons;
    this.powerups = powerups;
  }
}
