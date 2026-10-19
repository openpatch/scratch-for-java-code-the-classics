package avenger;

import org.openpatch.scratch.Sprite;
import org.openpatch.scratch.Stage;
import org.openpatch.scratch.Window;

/**
 * The title screen. Just like the original, the title image and the "press
 * start" animation are displayed while the menu theme plays.
 */
public class Menu extends Stage {

  /** The menu theme that loops for as long as the title screen is shown. */
  private final Sprite menuTheme = new Sprite();

  private final Sprite start;
  private final Controls controls;
  private int stateTimer = 0;

  public Menu() {
    this.menuTheme.addSound("menu_theme", "avenger/assets/music/menu_theme.ogg");
    var title = new Sprite();
    title.addCostume("title", Utils.img("title"));
    title.switchCostume("title");
    // Blit position of the original: (0, 0), the image is 960 x 540
    this.setScreenPosition(title, 960 / 2.0, 540 / 2.0);
    this.add(title);

    this.start = new Sprite();
    for (int i = 0; i < 14; i++) {
      this.start.addCostume("start" + i, String.format("avenger/assets/images/start%d.png", i));
    }
    this.start.switchCostume("start0");
    // Blit position of the original: (WIDTH // 2 - 350 // 2, 450), the image is 350 x 70
    this.setScreenPosition(this.start, 960 / 2.0, 450 + 35.0);
    this.add(this.start);

    this.controls = new Controls(this);
  }

  /** Sets a sprite position from screen pixel coordinates (top-left origin). */
  private void setScreenPosition(Sprite sprite, double x, double y) {
    sprite.setPosition(x - 960 / 2.0, 540 / 2.0 - y);
  }

  @Override
  public void run() {
    this.stateTimer += 1;

    // The menu theme loops for as long as the title screen is shown
    this.menuTheme.playSound("menu_theme");

    // Press start animation
    this.start.switchCostume("start" + this.stateTimer / 4 % 14);

    this.controls.update();
    if (this.controls.buttonPressed(0)) {
      this.menuTheme.stopAllSounds();
      this.stopAllSounds();
      Window.getInstance().setStage(new Play());
    }
  }
}
