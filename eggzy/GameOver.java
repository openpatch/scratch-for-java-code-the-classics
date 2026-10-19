package eggzy;

import org.openpatch.scratch.Sprite;
import org.openpatch.scratch.Stage;
import org.openpatch.scratch.TextAlign;
import org.openpatch.scratch.Window;
import org.openpatch.scratch.extensions.text.BitmapText;

/**
 * The game over screen. Shows how long the player survived, and a "new
 * record" animation if they beat the high score. The in-game music keeps
 * playing until the player returns to the title screen.
 */
public class GameOver extends Stage {

  private final Controls controls = new Controls(this);
  private final int gameTimer;
  private int stateTimer = 0;
  private final Sprite gameoverAnim;
  private final Sprite newRecord;
  private final BitmapText minutesText;
  private final BitmapText secondsText;

  /**
   * The in-game music, handed over by the play stage so that it keeps playing
   * here like the original's music channel.
   */
  private final Sprite music;

  public GameOver(int gameTimer, Sprite music) {
    this.gameTimer = gameTimer;
    this.music = music;
    this.setColor(0, 54, 255);

    // Display "Game Over" images. 625 is the width of the game over images.
    this.gameoverAnim = new Sprite();
    for (int i = 0; i < 14; i++) {
      this.gameoverAnim.addCostume("gameover" + i,
          EzActor.IMAGES + "gameover" + i + ".png");
    }
    this.gameoverAnim.switchCostume("gameover0");
    Play.setPixelPosition(this.gameoverAnim, Play.WIDTH / 2.0 - 625 / 2.0 + 312.5,
        100 + 37.5);
    this.add(this.gameoverAnim);

    int seconds = this.gameTimer / 60;
    if (seconds >= 60) {
      this.addImage("survived_for_mins_seconds");
    } else {
      this.addImage("survived_for_seconds");
    }

    // Show "NEW RECORD!" if the player beat the high score. 575 is the width
    // of the new record images.
    this.newRecord = new Sprite();
    for (int i = 0; i < 8; i++) {
      this.newRecord.addCostume("newrecord" + i,
          EzActor.IMAGES + "newrecord" + i + ".png");
    }
    this.newRecord.switchCostume("newrecord0");
    Play.setPixelPosition(this.newRecord, Play.WIDTH / 2.0 - 575 / 2.0 + 287.5,
        380 + 37.5);
    if (this.gameTimer > Replays.highScore) {
      this.add(this.newRecord);
    }

    // The survival time, drawn with the large font on top of everything else
    this.minutesText = EzActor.bitmapText("fontlrg");
    this.minutesText.setAlign(TextAlign.RIGHT);
    this.minutesText.setPosition(180 - Play.WIDTH / 2.0, Play.HEIGHT / 2.0 - 270);
    this.add(this.minutesText);
    this.secondsText = EzActor.bitmapText("fontlrg");
    this.add(this.secondsText);
  }

  private void addImage(String image) {
    Sprite sprite = new Sprite();
    sprite.addCostume(image, EzActor.IMAGES + image + ".png");
    sprite.switchCostume(image);
    Play.setPixelPosition(sprite, 825 / 2.0, 500 / 2.0);
    this.add(sprite);
  }

  @Override
  public void run() {
    Game.totalFrames += 1;
    this.controls.update();

    // The in-game music keeps playing on the game over screen
    this.music.playSound("ingame_theme");
    this.music.setVolume(20);

    this.stateTimer += 1;

    this.gameoverAnim.switchCostume("gameover" + Game.totalFrames / 5 % 14);
    if (this.newRecord.getStage() != null) {
      this.newRecord.switchCostume("newrecord" + Game.totalFrames / 5 % 8);
    }

    int seconds = this.gameTimer / 60;
    if (seconds >= 60) {
      this.minutesText.showText(Integer.toString(seconds / 60));
      this.secondsText.setAlign(TextAlign.CENTER);
      this.secondsText.setPosition(470 - Play.WIDTH / 2.0, Play.HEIGHT / 2.0 - 270);
      this.secondsText.showText(Integer.toString(seconds % 60));
    } else {
      this.minutesText.showText("");
      this.secondsText.setAlign(TextAlign.RIGHT);
      this.secondsText.setPosition(300 - Play.WIDTH / 2.0, Play.HEIGHT / 2.0 - 310);
      this.secondsText.showText(Integer.toString(seconds));
    }

    // Don't allow the player to return to the title screen until one second
    // has passed, so they can't skip the game over screen by accident
    if (this.stateTimer > 60 && this.controls.buttonPressed(0)) {
      if (this.gameTimer > Replays.highScore) {
        Replays.highScore = this.gameTimer;
      }
      this.music.stopAllSounds();
      Window.getInstance().setStage(new Title());
    }
  }
}
