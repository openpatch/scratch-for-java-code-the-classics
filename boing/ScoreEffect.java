package boing;

import org.openpatch.scratch.Sprite;

public class ScoreEffect extends Sprite {
  public ScoreEffect() {
    this.addCostume("effect0", "boing/assets/images/effect0.png");
    this.addCostume("effect1", "boing/assets/images/effect1.png");

    this.hide();
  }
}
