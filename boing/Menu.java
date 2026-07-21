package boing;

import org.openpatch.scratch.*;

public class Menu extends Stage {

  public Menu() {
    this.addBackdrop("menu0", "boing/assets/images/menu0.png");
    this.addBackdrop("menu1", "boing/assets/images/menu1.png");

    this.addSound("theme", "boing/assets/music/theme.wav");
    this.addSound("up", "boing/assets/sounds/up.wav");
    this.addSound("down", "boing/assets/sounds/down.wav");

  }

  @Override
  public void whenKeyPressed(KeyCode key) {

    if (key == KeyCode.SPACE) {
      Window.getInstance().transitionToStage(new Table(this.getCurrentBackdropName().equals("menu1")), 300);
      this.stopSound("theme");
    } else if (key == KeyCode.UP) {
      this.switchBackdrop("menu0");
      this.playSound("up");
    } else if (key == KeyCode.DOWN) {
      this.switchBackdrop("menu1");
      this.playSound("down");
    }
  }

  public void run() {
    this.playSound("theme");
  }

}
