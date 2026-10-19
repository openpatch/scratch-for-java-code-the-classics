package soccer;

import org.openpatch.scratch.UISprite;

/** The score bar at the top of the screen. */
public class ScoreBar {

  private final Play play;
  private final UISprite bar;
  private final UISprite[] scores = new UISprite[2];
  private final UISprite goal;

  public ScoreBar(Play play) {
    this.play = play;

    this.bar = new UISprite();
    this.bar.addCostume("bar", "soccer/assets/images/bar.png");
    this.bar.switchCostume("bar");
    // Blit position in the original: (HALF_WINDOW_W - 176, 0)
    this.bar.setPosition(
        Play.WIDTH / 2.0 - 176 + this.bar.getWidth() / 2.0 - Play.WIDTH / 2.0,
        Play.HEIGHT / 2.0 - this.bar.getHeight() / 2.0);
    play.add(this.bar);

    for (int i = 0; i < 2; i++) {
      this.scores[i] = new UISprite();
      for (int d = 0; d < 10; d++) {
        this.scores[i].addCostume("s" + d, String.format("soccer/assets/images/s%d.png", d));
      }
      play.add(this.scores[i]);
    }

    this.goal = new UISprite();
    this.goal.addCostume("goal", "soccer/assets/images/goal.png");
    this.goal.switchCostume("goal");
    // Blit position in the original: (HALF_WINDOW_W - 300, HEIGHT / 2 - 88)
    this.goal.setPosition(
        Play.WIDTH / 2.0 - 300 + this.goal.getWidth() / 2.0 - Play.WIDTH / 2.0,
        Play.HEIGHT / 2.0 - (Play.HEIGHT / 2.0 - 88 + this.goal.getHeight() / 2.0));
    this.goal.hide();
    play.add(this.goal);
  }

  public void update() {
    boolean visible = this.play.showHud();

    if (visible) {
      this.bar.show();
    } else {
      this.bar.hide();
    }
    for (int i = 0; i < 2; i++) {
      var s = this.scores[i];
      if (visible) {
        s.switchCostume("s" + this.play.teams[i].score);
        // Blit position in the original: (HALF_WINDOW_W + 7 - 39 * i, 6)
        s.setPosition(
            Play.WIDTH / 2.0 + 7 - 39 * i + s.getWidth() / 2.0 - Play.WIDTH / 2.0,
            Play.HEIGHT / 2.0 - (6 + s.getHeight() / 2.0));
      }
      if (visible) {
        s.show();
      } else {
        s.hide();
      }
    }
    if (visible && this.play.scoreTimer > 0) {
      this.goal.show();
    } else {
      this.goal.hide();
    }
  }
}
