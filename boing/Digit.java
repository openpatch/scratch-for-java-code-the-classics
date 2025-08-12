package boing;

import org.openpatch.scratch.Sprite;

public class Digit extends Sprite {

  public Digit() {
    for (int i = 0; i < 10; i++) {
      this.addCostume(String.format("0%d", i), String.format("boing/assets/images/digit0%d.png", i));
      this.addCostume(String.format("1%d", i), String.format("boing/assets/images/digit1%d.png", i));
      this.addCostume(String.format("2%d", i), String.format("boing/assets/images/digit2%d.png", i));
    }
  }
}
