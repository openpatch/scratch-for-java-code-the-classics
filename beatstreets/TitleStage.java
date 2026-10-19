package beatstreets;

import org.openpatch.scratch.TextAlign;
import org.openpatch.scratch.extensions.text.BitmapText;

/**
 * The title screen. Shows the flashing logo and waits for the player to press
 * the punch button.
 */
public class TitleStage extends GameStage {

  private final ImageSprite logo = new ImageSprite();
  private final BitmapText font = Util.bitmapText();
  private final Controls controls = new Controls(this);

  public TitleStage() {
    this.logo.addCostume("title0", "beatstreets/assets/images/title0.png");
    this.logo.addCostume("title1", "beatstreets/assets/images/title1.png");
    this.logo.switchCostume("title0");
    this.setBlitPosition(this.logo, 0, 0);
    this.add(this.logo);

    this.font.setAlign(TextAlign.CENTER);
    this.font.setPosition(0, Game.HEIGHT / 2.0 - (Game.HEIGHT - 50));
    this.font.showText("PRESS % OR Z");
    this.add(this.font);
  }

  @Override
  public void run() {
    super.run();

    // Draw the flashing logo
    this.logo.switchCostume(this.totalFrames / 20 % 2 == 0 ? "title0" : "title1");

    // Check for start game
    this.controls.update();
    if (this.controls.buttonPressed(0)) {
      this.switchStage(new ControlsStage());
    }
  }
}
