package cavern;

import org.openpatch.scratch.KeyCode;
import org.openpatch.scratch.Sprite;
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

    this.title = new Sprite();
    this.title.addCostume("title", "cavern/assets/images/title.png");
    this.title.setPosition(0, 0);
    this.add(this.title);
    this.title.goToFrontLayer();

    this.pressSpace = new Sprite();
    for (int i = 0; i < 10; i++) {
      this.pressSpace.addCostume("space" + i, String.format("cavern/assets/images/space%d.png", i));
    }
    // Blit position in the original: (130, 280), image size 540 x 90
    this.pressSpace.setPosition(130 + 540 / 2.0 - Play.WIDTH / 2.0, Play.HEIGHT / 2.0 - (280 + 45));
    this.add(this.pressSpace);
    this.pressSpace.goToFrontLayer();
  }

  @Override
  public void run() {
    super.run();

    // Press SPACE animation
    int animFrame = Math.min((this.timer + 40) % 160 / 4, 9);
    this.pressSpace.switchCostume("space" + animFrame);
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
