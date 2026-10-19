package beatstreets;

/** The controls screen, shown between the title screen and the game. */
public class ControlsStage extends GameStage {

  private final ImageSprite controlsImage = new ImageSprite();
  private final Controls controls = new Controls(this);

  public ControlsStage() {
    this.controlsImage.showImage("menu_controls");
    this.setBlitPosition(this.controlsImage, 0, 0);
    this.add(this.controlsImage);
  }

  @Override
  public void run() {
    super.run();

    // Check for the player starting the game
    this.controls.update();
    if (this.controls.buttonPressed(0)) {
      this.switchStage(new Play(new Controls(this)));
    }
  }
}
