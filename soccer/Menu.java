package soccer;

import org.openpatch.scratch.KeyCode;
import org.openpatch.scratch.UISprite;
import org.openpatch.scratch.Window;

/**
 * The title screen. Just like the original, a computer-controlled game runs
 * in the background, while the menu is displayed on top.
 */
public class Menu extends Play {

  private enum MenuState {
    NUM_PLAYERS,
    DIFFICULTY
  }

  private final UISprite menuImage = new UISprite();

  private MenuState menuState = MenuState.NUM_PLAYERS;
  private int menuNumPlayers = 1;
  private int menuDifficulty = 0;

  public Menu() {
    super(null, null, 2);

    this.menuImage.addCostume("menu01", "soccer/assets/images/menu01.png");
    this.menuImage.addCostume("menu02", "soccer/assets/images/menu02.png");
    this.menuImage.addCostume("menu10", "soccer/assets/images/menu10.png");
    this.menuImage.addCostume("menu11", "soccer/assets/images/menu11.png");
    this.menuImage.addCostume("menu12", "soccer/assets/images/menu12.png");
    this.menuImage.setPosition(0, 0);
    this.add(this.menuImage);
  }

  @Override
  protected boolean showHud() {
    return false;
  }

  @Override
  public void run() {
    super.run();

    if (this.menuState == MenuState.NUM_PLAYERS) {
      this.menuImage.switchCostume("menu0" + this.menuNumPlayers);
    } else {
      this.menuImage.switchCostume("menu1" + this.menuDifficulty);
    }
  }

  @Override
  public void whenKeyPressed(KeyCode key) {
    if (key == KeyCode.SPACE) {
      if (this.menuState == MenuState.NUM_PLAYERS) {
        if (this.menuNumPlayers == 1) {
          this.menuState = MenuState.DIFFICULTY;
        } else {
          // Start a two player game
          this.stopAllGameSounds();
          Window.getInstance().setStage(new Play(new Controls(0), new Controls(1), 2));
        }
      } else {
        // Start a one player game
        this.stopAllGameSounds();
        Window.getInstance().setStage(new Play(new Controls(0), null, this.menuDifficulty));
      }
    } else if (key == KeyCode.DOWN || key == KeyCode.UP) {
      this.playSound("move");
      int selectionChange = key == KeyCode.DOWN ? 1 : -1;
      if (this.menuState == MenuState.NUM_PLAYERS) {
        this.menuNumPlayers = this.menuNumPlayers == 1 ? 2 : 1;
      } else {
        this.menuDifficulty = Math.floorMod(this.menuDifficulty + selectionChange, 3);
      }
    }
  }
}
