package boingOneClass;

import org.openpatch.scratch.KeyCode;
import org.openpatch.scratch.Operators;
import org.openpatch.scratch.Sprite;
import org.openpatch.scratch.Stage;
import org.openpatch.scratch.Window;
import org.openpatch.scratch.extensions.animation.AnimatedSprite;
import org.openpatch.scratch.extensions.math.Random;
import org.openpatch.scratch.extensions.math.Vector2;

public class Game {

  static int playerRightScore = 0;
  static int playerRightGlowFrames = 0;
  static int playerLeftScore = 0;
  static int playerLeftGlowFrames = 0;
  static int playerSpeed = 6;
  static int maxAiSpeed = 6;
  static double aiOffset = 0.0;
  static double ballSpeed = 5.0;
  static Vector2 ballDirection = new Vector2(1, 0);
  static boolean playAgainstAI = true;

  public static void main(String[] args) {
    var window = new Window(800, 480, "boing/assets");

    // STAGES
    var table = new Stage();
    var gameOver = new Stage();
    var menu = new Stage();

    // Table setup
    table.addSound("theme", "boing/assets/music/theme.wav");
    table.addSound("score_goal", "boing/assets/sounds/score_goal0.wav");
    table.addBackdrop("table", "boing/assets/images/table.png");

    // Menu setup
    menu.addBackdrop("menu0", "boing/assets/images/menu0.png");
    menu.addBackdrop("menu1", "boing/assets/images/menu1.png");
    menu.addSound("theme", "boing/assets/music/theme.wav");
    menu.addSound("up", "boing/assets/sounds/up.wav");
    menu.addSound("down", "boing/assets/sounds/down.wav");

    menu.setWhenKeyPressed(key -> {
      if (key == KeyCode.VK_SPACE) {
        window.transitionToStage(table, 300);
        menu.stopSound("theme");
      } else if (key == KeyCode.VK_UP) {
        menu.switchBackdrop("menu0");
        playAgainstAI = true;
        menu.playSound("up");
      } else if (key == KeyCode.VK_DOWN) {
        menu.switchBackdrop("menu1");
        playAgainstAI = false;
        menu.playSound("down");
      }
    });

    menu.setRun(s -> {
      s.playSound("theme");
    });

    // Game Over setup
    gameOver.addBackdrop("gameover", "boing/assets/images/over.png");
    gameOver.setWhenKeyPressed(key -> {
      if (key == KeyCode.VK_SPACE) {
        window.transitionToStage(menu, 300);
      }
    });

    // SPRITES
    var batLeft = new Sprite();
    var batRight = new Sprite();
    var scoreEffect = new Sprite();
    var impact = new AnimatedSprite();
    var digit = new Sprite();
    for (int i = 0; i < 10; i++) {
      digit.addCostume(String.format("0%d", i), String.format("boing/assets/images/digit0%d.png", i));
      digit.addCostume(String.format("1%d", i), String.format("boing/assets/images/digit1%d.png", i));
      digit.addCostume(String.format("2%d", i), String.format("boing/assets/images/digit2%d.png", i));
    }
    var digitLeft = digit.clone();
    digitLeft.setPosition(-50, 0);
    var digitRight = digit.clone();
    digitRight.setPosition(50, 0);

    var ball = new Sprite();

    // batLeft setup
    batLeft.setX(-360);
    batLeft.addCostume("bat", "boing/assets/images/bat10.png");
    batLeft.addCostume("glow", "boing/assets/images/bat11.png");
    batLeft.addCostume("out", "boing/assets/images/bat12.png");

    batLeft.setRun(s -> {
      playerLeftGlowFrames--;
      if (playAgainstAI) {
        var xDistance = Math.abs(ball.getX() - s.getX());
        var targetCenter = 0;
        var targetBall = ball.getY() + aiOffset;
        var weightCenter = Math.min(1, xDistance / 400.0);
        var weightBall = 1 - weightCenter;
        var targetY = (targetCenter * weightCenter) + (targetBall * weightBall);
        var move = Operators.constrain(s.getY() - targetY, -maxAiSpeed, maxAiSpeed);
        s.setY(Operators.constrain(s.getY() - move, -160, 160));
      } else {
        if (s.isKeyPressed(KeyCode.VK_W)) {
          s.setY(Operators.constrain(s.getY() + playerSpeed, -160, 160));
        } else if (s.isKeyPressed(KeyCode.VK_S)) {
          s.setY(Operators.constrain(s.getY() - playerSpeed, -160, 160));
        }
      }

      s.switchCostume("bat");
      if (playerLeftGlowFrames > 0) {
        if (isBallOut(ball)) {
          s.switchCostume("out");
        } else {
          s.switchCostume("glow");
        }
      }
    });

    // batRight setup
    batRight.setX(360);
    batRight.addCostume("bat", "boing/assets/images/bat00.png");
    batRight.addCostume("glow", "boing/assets/images/bat01.png");
    batRight.addCostume("out", "boing/assets/images/bat02.png");

    batRight.setRun(s -> {
      playerRightGlowFrames--;
      if (s.isKeyPressed(KeyCode.VK_UP)) {
        s.setY(Operators.constrain(s.getY() + playerSpeed, -160, 160));
      } else if (s.isKeyPressed(KeyCode.VK_DOWN)) {
        s.setY(Operators.constrain(s.getY() - playerSpeed, -160, 160));
      }

      s.switchCostume("bat");
      if (playerRightGlowFrames > 0) {
        if (ball.getX() < -360 || ball.getX() > 360) {
          s.switchCostume("out");
        } else {
          s.switchCostume("glow");
        }
      }
    });

    // scoreEffect setup
    scoreEffect.addCostume("effect0", "boing/assets/images/effect0.png");
    scoreEffect.addCostume("effect1", "boing/assets/images/effect1.png");
    scoreEffect.hide();

    // impact setup
    impact.addAnimation("impact", frame -> String.format("boing/assets/images/impact%d.png", frame - 1), 5);
    impact.setRun(s -> {
      var a = (AnimatedSprite) s;
      a.playAnimation("impact", true);
      if (a.isAnimationPlayed()) {
        a.remove();
      }
    });

    // ball setup
    ball.addCostume("ball", "boing/assets/images/ball.png");

    ball.addSound("bounce0", "boing/assets/sounds/bounce0.wav");
    ball.addSound("bounce1", "boing/assets/sounds/bounce1.wav");
    ball.addSound("bounce2", "boing/assets/sounds/bounce2.wav");
    ball.addSound("bounce3", "boing/assets/sounds/bounce3.wav");
    ball.addSound("bounce4", "boing/assets/sounds/bounce4.wav");
    ball.addSound("bounce_synth0", "boing/assets/sounds/bounce_synth0.wav");

    ball.addSound("hit0", "boing/assets/sounds/hit0.wav");
    ball.addSound("hit1", "boing/assets/sounds/hit1.wav");
    ball.addSound("hit2", "boing/assets/sounds/hit2.wav");
    ball.addSound("hit3", "boing/assets/sounds/hit3.wav");
    ball.addSound("hit4", "boing/assets/sounds/hit4.wav");

    ball.addSound("hit_fast0", "boing/assets/sounds/hit_fast0.wav");
    ball.addSound("hit_medium0", "boing/assets/sounds/hit_medium0.wav");
    ball.addSound("hit_slow0", "boing/assets/sounds/hit_slow0.wav");
    ball.addSound("hit_veryfast0", "boing/assets/sounds/hit_veryfast0.wav");

    ball.setRun(s -> {
      for (int i = 0; i < ballSpeed; i++) {
        var originalX = s.getX();
        s.changePosition(ballDirection);

        if (Operators.absOf(s.getX()) >= 344 && Operators.absOf(originalX) < 344) {
          var newDirX = 1;
          var currentBat = batLeft;
          if (s.getX() > 0) {
            newDirX = -1;
            currentBat = batRight;
          }
          var differenceY = s.getY() - currentBat.getY();

          if (differenceY > -64 && differenceY < 64) {
            var dx = ballDirection.getX() * -1;
            var dy = ballDirection.getY();
            dy += differenceY / 128.0;
            dy = Operators.constrain(dy, -1, 1);

            ballDirection = new Vector2(dx, dy).unitVector();

            var newImpact = impact.clone();
            newImpact.setPosition(s.getPosition().add(new Vector2(newDirX * 10, 0)));
            newImpact.resetAnimation();
            table.add(newImpact);

            ballSpeed += 1;

            aiOffset = Random.randomInt(-10, 10);

            if (currentBat == batLeft) {
              playerLeftGlowFrames = 10;
            } else {
              playerRightGlowFrames = 10;
            }

            s.playSound("hit" + Random.randomInt(0, 4));
            if (ballSpeed < 10) {
              s.playSound("hit_slow0");
            } else if (ballSpeed < 15) {
              s.playSound("hit_medium0");
            } else if (ballSpeed < 20) {
              s.playSound("hit_fast0");
            } else {
              s.playSound("hit_veryfast0");
            }
          }
        }

        if (Operators.absOf(s.getY()) > 220) {
          ballDirection = new Vector2(ballDirection.getX(), -ballDirection.getY());
          s.changeY(ballDirection.getY());
          var newImpact = impact.clone();
          newImpact.setPosition(s.getPosition());
          table.add(newImpact);
          s.playSound("bounce" + Random.randomInt(0, 4));
          s.playSound("bounce_synth0");
        }
      }
    });

    // add sprite to stages
    table.add(scoreEffect);
    table.add(batLeft);
    table.add(batRight);
    table.add(ball);
    table.add(digitLeft);
    table.add(digitRight);

    table.setRun(s -> {
      s.playSound("theme");

      if (isBallOut(ball)) {
        var scoredLeft = ball.getX() > 0;
        var losingGlowFrames = scoredLeft ? playerRightGlowFrames : playerLeftGlowFrames;

        if (losingGlowFrames < 0) {
          s.playSound("score_goal");
          if (scoredLeft) {
            playerRightGlowFrames = 20;
          } else {
            playerLeftGlowFrames = 20;
          }
          scoreEffect.show();
          if (scoredLeft) {
            scoreEffect.switchCostume("effect0");
            playerLeftScore++;
            if (playerLeftScore > 9) {
              window.transitionToStage(gameOver, 300);
              s.stopSound("theme");
            }
            digitLeft.switchCostume("1" + playerLeftScore);
          } else {
            scoreEffect.switchCostume("effect1");
            playerRightScore++;
            if (playerRightScore > 9) {
              window.transitionToStage(gameOver, 300);
            }
            digitRight.switchCostume("2" + playerRightScore);
          }
        } else if (losingGlowFrames == 0) {
          var direction = scoredLeft ? -1 : 1;
          s.stopSound("score_goal");
          ballDirection = new Vector2(direction, 0);
          ball.setPosition(0, 0);
          scoreEffect.hide();
          digitLeft.switchCostume("0" + playerLeftScore);
          digitRight.switchCostume("0" + playerRightScore);
        }

      }
    });

    // Set the initial stage to the menu
    window.setStage(menu);
  }

  private static boolean isBallOut(Sprite ball) {
    return ball.getX() < -360 || ball.getX() > 360;
  }
}
