package bunner;

import org.openpatch.scratch.KeyCode;
import org.openpatch.scratch.UISprite;
import org.openpatch.scratch.Window;

/**
 * The title screen. Just like the original, the game scrolls in the
 * background without a player, while the title is displayed on top.
 */
public class Menu extends Play {

  private final UISprite title = new UISprite();
  private final UISprite start = new UISprite();

  public Menu() {
    super(false);

    this.title.addCostume("title", "bunner/assets/images/title.png");
    this.title.setPosition(0, 0);
    this.add(this.title);

    this.start.addCostume("start0", "bunner/assets/images/start0.png");
    this.start.addCostume("start1", "bunner/assets/images/start1.png");
    this.start.addCostume("start2", "bunner/assets/images/start2.png");
    // Blit position in the original: ((WIDTH - 270) / 2, HEIGHT - 240)
    this.start.setPosition(
        (Play.WIDTH - 270) / 2.0 + 135 - Play.WIDTH / 2.0,
        Play.HEIGHT / 2.0 - (Play.HEIGHT - 240 + 30));
    this.add(this.start);
  }

  @Override
  public void run() {
    super.run();

    // Press SPACE animation, updating every 6 scroll steps
    int animFrame =
        new int[] {0, 1, 2, 1}[Math.floorMod(Math.floorDiv((int) this.scrollPos, 6), 4)];
    this.start.switchCostume("start" + animFrame);
  }

  @Override
  public void whenKeyPressed(KeyCode key) {
    if (key == KeyCode.SPACE) {
      this.stopAllGameSounds();
      Window.getInstance().setStage(new Play());
    }
  }
}
