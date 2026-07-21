package boing;

import org.openpatch.scratch.*;

public class Bat extends Sprite {

  enum Type {
    P1, P2, AI
  }

  private Table table;
  private int glowFrames = 0;
  private Type playerType;

  public Bat(Table table, boolean leftSide, Type playerType) {
    this.table = table;
    this.playerType = playerType;

    this.setY(0);

    if (leftSide) {
      this.setX(-360);
      this.addCostume("bat", "boing/assets/images/bat10.png");
      this.addCostume("glow", "boing/assets/images/bat11.png");
      this.addCostume("out", "boing/assets/images/bat12.png");
    } else {
      this.setX(360);
      this.addCostume("bat", "boing/assets/images/bat00.png");
      this.addCostume("glow", "boing/assets/images/bat01.png");
      this.addCostume("out", "boing/assets/images/bat02.png");
    }
  }

  public void startGlowFor(int frames) {
    this.glowFrames = frames;
  }

  public int getGlowFrames() {
    return this.glowFrames;
  }

  public double ai() {
    var xDistance = Math.abs(this.table.getBall().getX() - this.getX());
    var targetCenter = 0;
    var targetBall = this.table.getBall().getY() + this.table.getAiOffset();

    var weightCenter = Math.min(1, xDistance / 400.0);
    var weightBall = 1 - weightCenter;

    var targetY = (targetCenter * weightCenter) + (targetBall * weightBall);

    return Math.min(table.MAX_AI_SPEED, Math.max(-table.MAX_AI_SPEED, this.getY() - targetY));
  }

  @Override
  public void run() {
    this.glowFrames--;

    var move = 0.0;
    switch (this.playerType) {
      case AI -> {
        move = this.ai();
      }
      case P1 -> {
        if (this.isKeyPressed(KeyCode.UP)) {
          move = -table.PLAYER_SPEED;
        } else if (this.isKeyPressed(KeyCode.DOWN)) {
          move = table.PLAYER_SPEED;
        }
      }
      case P2 -> {
        if (this.isKeyPressed(KeyCode.W)) {
          move = -table.PLAYER_SPEED;
        } else if (this.isKeyPressed(KeyCode.S)) {
          move = table.PLAYER_SPEED;
        }
      }
      default -> move = 0;
    }
    this.setY(Math.min(160, Math.max(-160, this.getY() - move)));

    this.switchCostume("bat");
    if (this.glowFrames > 0) {
      if (this.table.getBall().out()) {
        this.switchCostume("out");
      } else {
        this.switchCostume("glow");
      }
    }
  }

}
