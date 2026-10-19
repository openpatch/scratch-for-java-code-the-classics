package boing;

import org.openpatch.scratch.*;

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
    this.addSound("score_goal0", "boing/assets/sounds/score_goal0.wav");
    this.addSound("bounce_synth0", "boing/assets/sounds/bounce_synth0.wav");
    this.addSound("hit_slow0", "boing/assets/sounds/hit_slow0.wav");
    this.addSound("hit_medium0", "boing/assets/sounds/hit_medium0.wav");
    this.addSound("hit_fast0", "boing/assets/sounds/hit_fast0.wav");
    this.addSound("hit_veryfast0", "boing/assets/sounds/hit_veryfast0.wav");
    for (int i = 0; i < 5; i++) {
      this.addSound("bounce" + i, String.format("boing/assets/sounds/bounce%d.wav", i));
      this.addSound("hit" + i, String.format("boing/assets/sounds/hit%d.wav", i));
    }

    this.addBackdrop("table", "boing/assets/images/table.png");

    scoreEffect = new ScoreEffect();
    batRight = new Bat(this, false, Bat.Type.P1);
    batLeft = new Bat(this, true, twoPlayers ? Bat.Type.P2 : Bat.Type.AI);
    ball = new Ball(this, 1);
    digitRight = new Digit();
    digitRight.setPosition(50, digitRight.getHeight() / 2.0);
    digitLeft = new Digit();
    digitLeft.setPosition(-50, digitLeft.getHeight() / 2.0);

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
        this.playSound("score_goal0");
        losingBat.startGlowFor(20);
        this.scoreEffect.show();

        if (scoredLeft) {
          this.scoreEffect.switchCostume("effect0");
          this.scoreLeft++;

          if (scoreLeft > 9) {
            Window.getInstance().transitionToStage(new GameOver(), 300);
            this.stopSound("theme");
          }

          this.digitLeft.showScore(scoreLeft, 1);
        } else {
          this.scoreEffect.switchCostume("effect1");
          this.scoreRight++;

          if (scoreRight > 9) {
            Window.getInstance().transitionToStage(new GameOver(), 300);
            this.stopSound("theme");
          }

          this.digitRight.showScore(scoreRight, 2);
        }
      } else if (losingBat.getGlowFrames() == 0) {
        var direction = losingBat == batLeft ? -1 : 1;
        this.stopSound("score_goal0");
        this.ball.remove();
        this.ball = new Ball(this, direction);
        this.add(this.ball);
        this.scoreEffect.hide();
        this.digitLeft.showScore(scoreLeft, 0);
        this.digitRight.showScore(scoreRight, 0);
      }
    }
  }
}
