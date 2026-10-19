package beatstreets;

import org.openpatch.scratch.Sprite;
import org.openpatch.scratch.Stage;
import org.openpatch.scratch.Window;

/**
 * Base class of all stages of the game. Keeps the theme music playing, which
 * the original starts once and never stops.
 */
public abstract class GameStage extends Stage {

  protected int totalFrames = 0;

  /**
   * A sprite owns the music so its volume is independent of sound effects. It is
   * handed over to the next stage on a stage switch, so that it keeps looping
   * without restarting, just like the original, which starts it once and never
   * stops it.
   */
  private Sprite theme = new Sprite();

  protected GameStage() {
    this.theme.addSound("theme", "beatstreets/assets/music/theme.ogg");
  }

  @Override
  public void run() {
    this.totalFrames += 1;

    // The original plays the theme music on loop for the whole game
    this.theme.playSound("theme");
    this.theme.setVolume(30);
  }

  /**
   * Switches to another stage, stopping all sounds of this stage and handing
   * the theme music over to the next stage, which keeps playing it.
   */
  protected void switchStage(GameStage next) {
    next.theme = this.theme;
    this.stopAllSounds();
    Window.getInstance().setStage(next);
  }

  /**
   * Sets a sprite position from a blit position in pixel coordinates (top-left
   * origin), based on the size of the sprite's current costume.
   */
  protected void setBlitPosition(Sprite sprite, double x, double y) {
    sprite.setPosition(x + sprite.getWidth() / 2.0 - Game.WIDTH / 2.0,
        Game.HEIGHT / 2.0 - (y + sprite.getHeight() / 2.0));
  }
}
