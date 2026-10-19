package soccer;

import org.openpatch.scratch.Operators;
import org.openpatch.scratch.Sprite;
import org.openpatch.scratch.Vector2;

public class Player extends SoccerActor {

  public static final int LEAD_DISTANCE_1 = 10;
  public static final int LEAD_DISTANCE_2 = 50;

  public static final double PLAYER_DEFAULT_SPEED = 2;
  public static final double CPU_PLAYER_WITH_BALL_BASE_SPEED = 2.6;
  public static final double PLAYER_INTERCEPT_BALL_SPEED = 2.75;
  public static final double LEAD_PLAYER_BASE_SPEED = 2.9;
  public static final double HUMAN_PLAYER_WITH_BALL_SPEED = 3;
  public static final double HUMAN_PLAYER_WITHOUT_BALL_SPEED = 3.3;

  public int dir = 0;
  public double animFrame = -1;
  public int timer = 0;

  public Vector2 home;
  public SoccerActor mark;
  public Integer lead;
  public Player peer;

  public Player(Play play, double x, double y, int team) {
    super(play);

    // All players start on their own side of the pitch, in kickoff formation
    double kickoffY = y / 2 + 550 - team * 400;
    this.vpos = new Vector2(x, kickoffY);
    this.home = new Vector2(x, y);
    this.team = team;

    for (int d = 0; d < 8; d++) {
      for (int f = 0; f < 5; f++) {
        this.addCostume(
            String.format("player%d%d%d", team, d, f),
            String.format("soccer/assets/images/player%d%d%d.png", team, d, f));
      }
    }

    this.shadow = new Sprite();
    for (int d = 0; d < 8; d++) {
      for (int f = 0; f < 5; f++) {
        this.shadow.addCostume(
            String.format("players%d%d", d, f),
            String.format("soccer/assets/images/players%d%d.png", d, f));
      }
    }
    this.shadow.switchCostume("players00");

    this.switchCostume(String.format("player%d00", team));
    this.sync();
    this.syncShadow();
  }

  /** Is the ball close enough for this player to be active? */
  public boolean active() {
    return Math.abs(this.play.ball.vpos.getY() - this.home.getY()) < 400;
  }

  @Override
  public void run() {
    if (this.play.isFrozen()) {
      return;
    }

    this.timer -= 1;

    // By default, run slowly towards the home position
    Vector2 target = new Vector2(this.home);
    double speed = Player.PLAYER_DEFAULT_SPEED;

    Team myTeam = this.play.teams[this.team];
    boolean preKickoff = this.play.kickoffPlayer != null;
    boolean iAmKickoffPlayer = this == this.play.kickoffPlayer;
    Ball ball = this.play.ball;

    if (this == myTeam.activeControlPlayer && myTeam.human() && (!preKickoff || iAmKickoffPlayer)) {
      // This player is controlled by a human
      if (ball.owner == this) {
        speed = Player.HUMAN_PLAYER_WITH_BALL_SPEED;
      } else {
        speed = Player.HUMAN_PLAYER_WITHOUT_BALL_SPEED;
      }
      target = this.vpos.add(myTeam.controls.move(speed));

    } else if (ball.owner != null) {
      if (ball.owner == this) {
        // We are the computer-controlled owner of the ball
        double bestCost = Double.MAX_VALUE;
        for (int d = -2; d <= 2; d++) {
          Vector2 pos = this.vpos.add(SoccerMath.angleToVec(this.dir + d).multiply(3));
          double c = SoccerMath.cost(this.play, pos, this.team, Math.abs(d));
          if (c < bestCost) {
            bestCost = c;
            target = pos;
          }
        }
        speed = Player.CPU_PLAYER_WITH_BALL_BASE_SPEED + this.play.difficulty.speedBoost;

      } else if (ball.owner.team == this.team) {
        // The ball is owned by another player on our team
        if (this.active()) {
          int direction = this.team == 0 ? -1 : 1;
          double targetX = (ball.vpos.getX() + target.getX()) / 2;
          double targetY = (ball.vpos.getY() + 400 * direction + target.getY()) / 2;
          target = new Vector2(targetX, targetY);
        }

      } else {
        // The ball is owned by a player on the opposite team
        if (this.lead != null) {
          target = ball.owner.vpos.add(SoccerMath.angleToVec(ball.owner.dir).multiply(this.lead));
          target =
              new Vector2(
                  Operators.constrain(target.getX(), Play.AI_MIN_X, Play.AI_MAX_X),
                  Operators.constrain(target.getY(), Play.AI_MIN_Y, Play.AI_MAX_Y));

          int otherTeam = 1 - this.team;
          speed = Player.LEAD_PLAYER_BASE_SPEED;
          if (this.play.teams[otherTeam].human()) {
            speed += this.play.difficulty.speedBoost;
          }

        } else if (this.mark.active()) {
          // The player or goal we're marking is active
          if (myTeam.human()) {
            target = new Vector2(ball.vpos);
          } else {
            double[] n = SoccerMath.safeNormalise(ball.vpos.sub(this.mark.vpos));
            double length = n[2];

            if (this.mark instanceof Goal) {
              length = Math.min(150, length);
            } else {
              length /= 2;
            }

            target = this.mark.vpos.add(SoccerMath.unitVector(n).multiply(length));
          }
        }
      }
    } else {
      // Nobody has the ball
      if ((preKickoff && iAmKickoffPlayer) || (!preKickoff && this.active())) {
        // Try to intercept the ball by simulating its movement
        target = new Vector2(ball.vpos);
        Vector2 vel = new Vector2(ball.vel);
        int frame = 0;

        while (target.sub(this.vpos).length() > Player.PLAYER_INTERCEPT_BALL_SPEED * frame + Ball.DRIBBLE_DIST_X
            && vel.length() > 0.5) {
          target = target.add(vel);
          vel = vel.multiply(Ball.DRAG);
          frame += 1;
        }

        speed = Player.PLAYER_INTERCEPT_BALL_SPEED;

      } else if (preKickoff) {
        // Waiting for kickoff - just stay where we are
        target = new Vector2(this.vpos.getX(), this.vpos.getY());
      }
    }

    // Move towards the target position
    double[] n = SoccerMath.safeNormalise(target.sub(this.vpos));
    double distance = n[2];
    int targetDir;

    if (distance > 0) {
      distance = Math.min(distance, speed);
      targetDir = SoccerMath.vecToAngle(SoccerMath.unitVector(n));

      if (Play.allowMovement(this.vpos.getX() + n[0] * distance, this.vpos.getY())) {
        this.vpos = new Vector2(this.vpos.getX() + n[0] * distance, this.vpos.getY());
      }
      if (Play.allowMovement(this.vpos.getX(), this.vpos.getY() + n[1] * distance)) {
        this.vpos = new Vector2(this.vpos.getX(), this.vpos.getY() + n[1] * distance);
      }

      this.animFrame = (this.animFrame + Math.max(distance, 1.5)) % 72;
    } else {
      // Already at the target position - just turn to face the ball
      targetDir = SoccerMath.vecToAngle(ball.vpos.sub(this.vpos));
      this.animFrame = -1;
    }

    // Turn to face the target direction, one step at a time
    int dirDiff = targetDir - this.dir;
    this.dir =
        Math.floorMod(this.dir + new int[] {0, 1, 1, 1, 1, 7, 7, 7}[Math.floorMod(dirDiff, 8)], 8);

    String suffix = this.dir + "" + (Math.floorDiv((int) this.animFrame, 18) + 1);
    this.switchCostume("player" + this.team + suffix);
    this.shadow.switchCostume("players" + suffix);

    this.sync();
    this.syncShadow();
  }
}
