package beatstreets;

import org.openpatch.scratch.Vector2;

public class ExtraLifePowerup extends Powerup {

  private int timer = 0;

  public ExtraLifePowerup(Play game, Vector2 pos) {
    super(game, "ingame_life9", pos, 37.5, 37.5);
  }

  @Override
  public void run() {
    if (!this.isPlaying()) {
      super.run();
      return;
    }
    this.timer += 1;
    this.imageName = "ingame_life" + (this.timer / 2 % 10);

    super.run();
  }

  @Override
  public void collect(Player collector) {
    this.collected = true;

    collector.gainExtraLife();

    this.game.playSound("health", 1);
  }
}
