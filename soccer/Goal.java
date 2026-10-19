package soccer;

import org.openpatch.scratch.Vector2;

public class Goal extends SoccerActor {

  public Goal(Play play, int team) {
    super(play);

    this.team = team;
    this.vpos = new Vector2(Play.HALF_LEVEL_W, team == 0 ? 0 : Play.LEVEL_H);

    this.addCostume("goal" + team, String.format("soccer/assets/images/goal%d.png", team));
    this.switchCostume("goal" + team);
    this.sync();
  }

  /** Is the ball within 500 pixels on the Y axis? */
  public boolean active() {
    return Math.abs(this.play.ball.vpos.getY() - this.vpos.getY()) < 500;
  }
}
