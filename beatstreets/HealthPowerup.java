package beatstreets;

import org.openpatch.scratch.Vector2;

public class HealthPowerup extends Powerup {

  public HealthPowerup(Play game, Vector2 pos) {
    super(game, "health_pickup", pos, 35, 37.5);
  }

  @Override
  public void collect(Player collector) {
    this.collected = true;

    // Add 20 health to the player who collected us, but don't go over their max health
    collector.health = Math.min(collector.health + 20, collector.startHealth);

    this.game.playSound("health", 1);
  }
}
