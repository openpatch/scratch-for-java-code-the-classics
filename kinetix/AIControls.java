package kinetix;

import org.openpatch.scratch.Operators;
import org.openpatch.scratch.Random;

/**
 * Controls used by the AI player which plays the game in the background of
 * the title screen.
 */
public class AIControls extends Controls {

  private Play game;
  private int offset = 0;

  public void setGame(Play game) {
    this.game = game;
  }

  @Override
  public double getX() {
    if (this.game.portalActive) {
      // If the portal to the next level is open, just move right so that we go through it
      return Play.BAT_SPEED;
    }

    // Randomly shift the bat AI offset over time, so the AI player doesn't
    // constantly hit the ball perfectly in the centre of the bat.
    this.offset += Random.randomInt(-1, 1);
    this.offset = Operators.constrain(this.offset, -40, 40);

    // Follow position of the first ball (in case of multiball)
    double target = this.game.balls.get(0).x - (this.game.bat.x + this.offset);
    return Operators.constrain(target, -Play.BAT_SPEED, Play.BAT_SPEED);
  }

  @Override
  public boolean fireDown() {
    // Just have the AI mash the fire button
    return Random.randomInt(0, 5) == 0;
  }
}
