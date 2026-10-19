package myriapod;

import org.openpatch.scratch.KeyCode;
import org.openpatch.scratch.Sprite;
import org.openpatch.scratch.UISprite;
import org.openpatch.scratch.Window;

/**
 * The title screen. Just like the original, the game runs in the background
 * without a player, while the title is displayed on top.
 */
public class Menu extends Play {

  private final Sprite title;
  private final Sprite pressSpace;

  public Menu() {
    super(false);

    this.title = new UISprite();
    this.title.addCostume("title", "myriapod/assets/images/title.png");
    this.title.setPosition(0, 0);
    this.add(this.title);

    this.pressSpace = new UISprite();
    for (int i = 0; i < 14; i++) {
      this.pressSpace.addCostume("space" + i, String.format("myriapod/assets/images/space%d.png", i));
    }
    // Blit position in the original: (0, 420), image size 480 x 64
    this.pressSpace.setPosition(240 - Play.WIDTH / 2.0, Play.HEIGHT / 2.0 - (420 + 32));
    this.add(this.pressSpace);
  }

  @Override
  public void run() {
    super.run();

    // Press SPACE animation, updating every 4 frames
    this.pressSpace.switchCostume("space" + (this.time / 4 % 14));
  }

  @Override
  public void whenKeyPressed(KeyCode key) {
    if (key == KeyCode.SPACE) {
      this.stopAllSounds();
      this.theme.stopAllSounds();
      Window.getInstance().setStage(new Play());
    }
  }
}
