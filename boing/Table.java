package boing;

import org.openpatch.scratch.extensions.math.Random;
import org.openpatch.scratch.Stage;

public class Table extends Stage {

  public final int PLAYER_SPEED = 6;
  public final int MAX_AI_SPEED = 6;

  private Bat batRight;
  private Bat batLeft;

  private int scoreRight = 0;
  private int scoreLeft = 0;

  private Digit digitRight;
  private Digit digitLeft;
  private Ball ball;
  private ScoreEffect scoreEffect;
  private int aiOffset = 0;

  public Table(boolean twoPlayers) {
    this.addSound("theme", "boing/assets/music/theme.wav");
    this.addSound("score_goal", "boing/assets/sounds/score_goal0.wav");

    this.addBackdrop("table", "boing/assets/images/table.png");

    scoreEffect = new ScoreEffect();
    batRight = new Bat(this, false, Bat.Type.P1);
    batLeft = new Bat(this, true, twoPlayers ? Bat.Type.P2 : Bat.Type.AI);
    ball = new Ball(this, 1);
    digitRight = new Digit();
    digitRight.setPosition(50, 0);
    digitLeft = new Digit();
    digitLeft.setPosition(-50, 0);

    this.add(scoreEffect);
    this.add(ball);
    this.add(batLeft);
    this.add(batRight);
    this.add(digitLeft);
    this.add(digitRight);
  }

  public Bat getBatRight() {
    return batRight;
  }

  public Bat getBatLeft() {
    return batLeft;
  }

  public Ball getBall() {
    return ball;
  }

  public void randomizeAiOffset() {
    this.aiOffset = Random.randomInt(-10, 10);
  }

  public int getAiOffset() {
    return this.aiOffset;
  }

  @Override
  public void run() {
    this.playSound("theme");
    if (this.ball.out()) {
      var scoredLeft = this.ball.getX() > 0;
      var losingBat = scoredLeft ? this.batRight : this.batLeft;

      if (losingBat.getGlowFrames() < 0) {
        this.playSound("score_goal");
        losingBat.startGlowFor(20);
        this.scoreEffect.show();

        if (scoredLeft) {
          this.scoreEffect.switchCostume("effect0");
          this.scoreLeft++;

          if (scoreLeft > 9) {
            this.getWindow().transitionToStage(new GameOver(), 300);
            this.stopSound("theme");
          }

          this.digitLeft.switchCostume("1" + scoreLeft);
        } else {
          this.scoreEffect.switchCostume("effect1");
          this.scoreRight++;

          if (scoreRight > 9) {
            this.getWindow().transitionToStage(new GameOver(), 300);
          }

          this.digitRight.switchCostume("2" + scoreRight);
        }
      } else if (losingBat.getGlowFrames() == 0) {
        var direction = losingBat == batLeft ? -1 : 1;
        this.stopSound("score_goal");
        this.ball.remove();
        this.ball = new Ball(this, direction);
        this.add(this.ball);
        this.scoreEffect.hide();
        this.digitLeft.switchCostume("0" + scoreLeft);
        this.digitRight.switchCostume("0" + scoreRight);
      }
    }
  }
}
