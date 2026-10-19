package beatstreets;

/** The game over screen. Shows a different image depending on whether the player won. */
public class GameOverStage extends GameStage {

  private final Controls controls = new Controls(this);

  public GameOverStage(boolean won) {
    ImageSprite status = new ImageSprite();
    status.showImage(won ? "status_win" : "status_lose");
    this.setBlitPosition(status, 0, 0);
    this.add(status);
  }

  @Override
  public void run() {
    super.run();

    // Wait for the punch button to go back to the title screen
    this.controls.update();
    if (this.controls.buttonPressed(0)) {
      this.switchStage(new TitleStage());
    }
  }
}
