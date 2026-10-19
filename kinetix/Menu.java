package kinetix;

import org.openpatch.scratch.Sprite;
import org.openpatch.scratch.UISprite;
import org.openpatch.scratch.Window;

/**
 * The title screen. Just like the original, the game runs in the background
 * played by an AI player, while the title is displayed on top.
 */
public class Menu extends Play {

  /** A sprite owns the title music and its volume. */
  private final Sprite titleTheme = new Sprite();

  private final Sprite title;
  private final Sprite startGame;
  private final Sprite start;
  private final KeyboardControls menuControls;

  public Menu() {
    super(new AIControls());
    this.titleTheme.addSound("title_theme", "kinetix/assets/music/title_theme.ogg");
    this.titleTheme.setVolume(30);

    // Blit positions of the original: title (0, 0), startgame (20, 80) and
    // the press start animation (WIDTH/2 - 125, 530)
    this.title = new UISprite();
    this.title.addCostume("title", "kinetix/assets/images/title.png");
    this.setPixelPosition(this.title, WIDTH / 2.0, HEIGHT / 2.0);
    this.add(this.title);

    this.startGame = new UISprite();
    this.startGame.addCostume("startgame", "kinetix/assets/images/startgame.png");
    this.setPixelPosition(this.startGame, 20 + 300, 80 + 300);
    this.add(this.startGame);

    this.start = new UISprite();
    for (int i = 0; i < 13; i++) {
      this.start.addCostume("start" + i, String.format("kinetix/assets/images/start%d.png", i));
    }
    this.start.switchCostume("start0");
    this.setPixelPosition(this.start, WIDTH / 2.0, 530 + 25);
    this.add(this.start);

    this.menuControls = new KeyboardControls(this);
  }

  @Override
  public void run() {
    super.run();

    // The title music keeps playing while the AI player shows the game
    this.titleTheme.playSound("title_theme");

    // Press start animation
    this.start.switchCostume("start" + this.totalFrames / 4 % 13);

    this.menuControls.update();
    if (this.menuControls.firePressed()) {
      this.titleTheme.stopAllSounds();
      this.stopAllSounds();
      Window.getInstance().setStage(new Play(new KeyboardControls(this)));
    }
  }
}
