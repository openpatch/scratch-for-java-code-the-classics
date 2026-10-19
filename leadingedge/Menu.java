package leadingedge;

import org.openpatch.scratch.Color;
import org.openpatch.scratch.Operators;
import org.openpatch.scratch.Sprite;
import org.openpatch.scratch.Window;
import org.openpatch.scratch.extensions.drawing.Drawing;
import org.openpatch.scratch.extensions.drawing.DrawingSprite;
import org.openpatch.scratch.extensions.text.BitmapText;

/**
 * The title screen. Just like the original, a demo race runs in the background
 * (no player car), with the logo and the start prompt on top. The demo race is
 * reset after two minutes so that the CPU cars don't run out of track.
 */
public class Menu extends Play {

  private double demoResetTimer = 2 * 60;
  private double demoStartTimer = 0;

  /** Fades the demo race to black when it is reset, and back in. */
  private final Fade fade = new Fade();

  /** The logo, centred on the top third of the screen. */
  private final Sprite logo = new Sprite();

  /** The start prompt. The % sign substitutes for the controller button image. */
  private final BitmapText promptText = bitmapText("font");

  public Menu() {
    super(null);

    this.playMusic("title_theme");

    this.add(this.fade);

    this.logo.addCostume("logo", imagePath("logo"));
    this.logo.switchCostume("logo");
    this.logo.setPosition(0, HALF_HEIGHT - HEIGHT / 3.0);
    this.add(this.logo);

    this.placeText(this.promptText, WIDTH / 2.0, HEIGHT - 82, true);
    this.add(this.promptText);
  }

  @Override
  public void run() {
    super.run();

    double deltaTime = this.getDeltaTime();

    // If the demo race has been running for a while, reset it, otherwise the
    // AI cars will run out of track
    this.demoResetTimer -= deltaTime;
    this.demoStartTimer += deltaTime;
    if (this.demoResetTimer <= 0) {
      this.initWorld(null);
      this.demoResetTimer = 2 * 60;
      this.demoStartTimer = 0;
    }

    // Fade out screen prior to resetting the demo game, and fade in whenever
    // the demo (re)starts
    if (this.demoResetTimer < 1 || this.demoStartTimer < 1) {
      double value = this.demoResetTimer < 1 ? this.demoResetTimer : this.demoStartTimer;
      this.fade.setTransparency(Operators.constrain(value, 0, 1) * 100);
      this.fade.show();
    } else {
      this.fade.hide();
    }

    this.promptText.showText("PRESS % OR LEFT CONTROL");

    // Keep the title music looping, like pgzero's music.play
    this.keepMusicLooping("title_theme");

    // Check for the player starting the game
    if (this.keyboardControls.buttonPressed(0)) {
      this.stopAllGameSounds();
      Window.getInstance().setStage(new Play(new Controls(this)));
    }
  }

  /** A drawing sprite which covers the screen in black. */
  private class Fade extends DrawingSprite {
    @Override
    public void draw(Drawing drawing) {
      drawing.rectangle(0, 0, WIDTH, HEIGHT, new Color(0, 0, 0));
    }
  }
}
