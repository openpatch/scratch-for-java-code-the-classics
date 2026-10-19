package eggzy;

import org.openpatch.scratch.Sprite;
import org.openpatch.scratch.Stage;
import org.openpatch.scratch.Window;

/**
 * The title screen and the controls screen. The title shows the "press to
 * start" animation; pressing fire switches to the controls screen, and
 * pressing it again starts a new game.
 */
public class Title extends Stage {

  private final Controls controls = new Controls(this);
  private final Sprite title;
  private final Sprite pressToStart;
  private final Sprite controlsImage;
  private final Sprite start;
  private boolean showControls = false;

  /** A sprite owns the title music and its volume. */
  private final Sprite titleTheme = new Sprite();

  public Title() {
    this.titleTheme.addSound("title_theme", "eggzy/assets/music/title_theme.ogg");
    this.setColor(0, 0, 0);

    // The title music starts when the title screen is entered
    this.titleTheme.playSound("title_theme");

    // Blit positions of the original: title (0, 0), press_to_start (0, 0)
    // and the start animation (WIDTH/2 - 150, 360)
    this.title = new Sprite();
    this.title.addCostume("title", EzActor.IMAGES + "title.png");
    Play.setPixelPosition(this.title, 825 / 2.0, 575 / 2.0);
    this.add(this.title);

    this.pressToStart = new Sprite();
    this.pressToStart.addCostume("press_to_start",
        EzActor.IMAGES + "press_to_start.png");
    Play.setPixelPosition(this.pressToStart, 825 / 2.0, 500 / 2.0);
    this.add(this.pressToStart);

    this.start = new Sprite();
    for (int i = 0; i < 11; i++) {
      this.start.addCostume("start" + i, EzActor.IMAGES + "start" + i + ".png");
    }
    this.start.switchCostume("start0");
    Play.setPixelPosition(this.start, Play.WIDTH / 2.0 - 150 + 150, 360 + 37.5);
    this.add(this.start);

    // The controls screen is shown after the title screen
    this.controlsImage = new Sprite();
    this.controlsImage.addCostume("controls", EzActor.IMAGES + "controls.png");
    Play.setPixelPosition(this.controlsImage, 825 / 2.0, 500 / 2.0);
    this.controlsImage.hide();
    this.add(this.controlsImage);
  }

  @Override
  public void run() {
    Game.totalFrames += 1;
    this.controls.update();

    // The title music loops forever in the original
    this.titleTheme.playSound("title_theme");
    this.titleTheme.setVolume(30);

    if (!this.showControls) {
      // Draw the "start" animation, which has 11 frames
      this.start.switchCostume("start" + Game.totalFrames / 6 % 11);

      if (this.controls.buttonPressed(0)) {
        this.showControls = true;
        this.title.hide();
        this.pressToStart.hide();
        this.start.hide();
        this.controlsImage.show();
      }
    } else {
      if (this.controls.buttonPressed(0)) {
        this.titleTheme.stopAllSounds();
        Window.getInstance().setStage(new Play(new Controls(this)));
      }
    }
  }
}
