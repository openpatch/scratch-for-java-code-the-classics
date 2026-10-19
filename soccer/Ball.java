package soccer;

import org.openpatch.scratch.Sprite;
import org.openpatch.scratch.Vector2;

public class Ball extends SoccerActor {

  public static final double KICK_STRENGTH = 11.5;
  public static final double DRAG = 0.98;
  public static final double DRIBBLE_DIST_X = 18;
  public static final double DRIBBLE_DIST_Y = 16;

  public Vector2 vel = new Vector2(0, 0);
  public Player owner;
  public int timer = 0;

  public Ball(Play play) {
    super(play);

    this.vpos = new Vector2(Play.HALF_LEVEL_W, Play.HALF_LEVEL_H);
    this.team = -1;

    this.addCostume("ball", "soccer/assets/images/ball.png");

    this.shadow = new Sprite();
    this.shadow.addCostume("balls", "soccer/assets/images/balls.png");
    this.shadow.switchCostume("balls");

    this.switchCostume("ball");
    this.sync();
    this.syncShadow();
  }

  /** Check for collision with player p. */
  public boolean collide(Player p) {
    return p.timer < 0 && p.vpos.sub(this.vpos).length() <= Ball.DRIBBLE_DIST_X;
  }

  @Override
  public void run() {
    if (this.play.isFrozen()) {
      return;
    }

    this.timer -= 1;

    if (this.owner != null) {
      // The ball is being dribbled
      double newX = SoccerMath.avg(this.vpos.getX(), this.owner.vpos.getX() + Ball.DRIBBLE_DIST_X * SoccerMath.sin(this.owner.dir));
      double newY = SoccerMath.avg(this.vpos.getY(), this.owner.vpos.getY() - Ball.DRIBBLE_DIST_Y * SoccerMath.cos(this.owner.dir));

      if (this.play.onPitch(newX, newY)) {
        this.vpos = new Vector2(newX, newY);
      } else {
        // Player loses the ball
        this.owner.timer = 60;
        this.vel = SoccerMath.angleToVec(this.owner.dir).multiply(3);
        this.owner = null;
      }
    } else {
      // Run physics, one axis at a time
      double[] boundsX;
      if (Math.abs(this.vpos.getY() - Play.HALF_LEVEL_H) > Play.HALF_PITCH_H) {
        boundsX = Play.GOAL_BOUNDS_X;
      } else {
        boundsX = Play.PITCH_BOUNDS_X;
      }

      double[] boundsY;
      if (Math.abs(this.vpos.getX() - Play.HALF_LEVEL_W) < Play.HALF_GOAL_W) {
        boundsY = Play.GOAL_BOUNDS_Y;
      } else {
        boundsY = Play.PITCH_BOUNDS_Y;
      }

      double[] x = ballPhysics(this.vpos.getX(), this.vel.getX(), boundsX);
      double[] y = ballPhysics(this.vpos.getY(), this.vel.getY(), boundsY);
      this.vpos = new Vector2(x[0], y[0]);
      this.vel = new Vector2(x[1], y[1]);
    }

    this.syncShadow();

    // Search for a player that can acquire the ball
    for (Player target : this.play.players) {
      if ((this.owner == null || this.owner.team != target.team) && this.collide(target)) {
        if (this.owner != null) {
          // Taking the ball from the previous owner
          this.owner.timer = 60;
        }
        this.timer = this.play.difficulty.holdoffTimer;
        this.play.teams[target.team].activeControlPlayer = target;
        this.owner = target;
      }
    }

    // Decide whether to kick the ball
    if (this.owner != null) {
      Team team = this.play.teams[this.owner.team];

      // Find the closest targetable player or goal
      SoccerActor target = null;
      double bestDistance = Double.MAX_VALUE;
      for (Player p : this.play.players) {
        if (p.team == this.owner.team && SoccerMath.targetable(this.play, p, this.owner)) {
          double d = p.vpos.sub(this.owner.vpos).length();
          if (d < bestDistance) {
            bestDistance = d;
            target = p;
          }
        }
      }
      for (Goal g : this.play.goals) {
        if (g.team == this.owner.team && SoccerMath.targetable(this.play, g, this.owner)) {
          double d = g.vpos.sub(this.owner.vpos).length();
          if (d < bestDistance) {
            bestDistance = d;
            target = g;
          }
        }
      }

      boolean doShoot;
      if (team.human()) {
        doShoot = team.controls.shoot();
      } else {
        doShoot =
            this.timer <= 0
                && target != null
                && SoccerMath.cost(this.play, target.vpos, this.owner.team, 0)
                    < SoccerMath.cost(this.play, this.owner.vpos, this.owner.team, 0);
      }

      if (doShoot) {
        this.play.playGameSound("kick", 4);

        Vector2 vec = SoccerMath.angleToVec(this.owner.dir);
        if (target != null) {
          // Kick towards the target, leading the target if it is controlled
          double r = 0;
          int iterations = team.human() && target instanceof Player ? 8 : 1;

          for (int i = 0; i < iterations; i++) {
            Vector2 t = target.vpos.add(SoccerMath.angleToVec(this.owner.dir).multiply(r));
            double[] n = SoccerMath.safeNormalise(t.sub(this.vpos));
            vec = SoccerMath.unitVector(n);
            r = Player.HUMAN_PLAYER_WITHOUT_BALL_SPEED * steps(n[2]);
          }

          if (target instanceof Player p) {
            this.play.teams[this.owner.team].activeControlPlayer = p;
          }
        } else {
          // Kick the ball straight ahead
          vec = SoccerMath.angleToVec(this.owner.dir);

          // Guess which player the ball might end up closest to
          Player nearest = null;
          double best = Double.MAX_VALUE;
          Vector2 ahead = this.vpos.add(vec.multiply(250));
          for (Player p : this.play.players) {
            if (p.team == this.owner.team) {
              double d = p.vpos.sub(ahead).length();
              if (d < best) {
                best = d;
                nearest = p;
              }
            }
          }
          this.play.teams[this.owner.team].activeControlPlayer = nearest;
        }

        this.owner.timer = 10;
        this.vel = vec.multiply(Ball.KICK_STRENGTH);
        this.owner = null;
      }
    }

    this.sync();
  }

  /** Ball physics for one axis, bouncing at the bounds. */
  private static double[] ballPhysics(double pos, double vel, double[] bounds) {
    pos += vel;
    if (pos < bounds[0] || pos > bounds[1]) {
      pos -= vel;
      vel = -vel;
    }
    return new double[] {pos, vel * Ball.DRAG};
  }

  /** Number of physics steps for the ball to travel the given distance. */
  private static double steps(double distance) {
    int steps = 0;
    double vel = Ball.KICK_STRENGTH;

    while (distance > 0 && vel > 0.25) {
      distance -= vel;
      steps += 1;
      vel *= Ball.DRAG;
    }

    return steps;
  }
}
