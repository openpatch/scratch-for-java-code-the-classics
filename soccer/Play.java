package soccer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.openpatch.scratch.Operators;
import org.openpatch.scratch.Random;
import org.openpatch.scratch.Sprite;
import org.openpatch.scratch.Stage;
import org.openpatch.scratch.UISprite;
import org.openpatch.scratch.Vector2;
import org.openpatch.scratch.Window;

public class Play extends Stage {

  public static final int WIDTH = 800;
  public static final int HEIGHT = 480;

  // Size of the level, including the pitch and the boundary surrounding it
  public static final int LEVEL_W = 1000;
  public static final int LEVEL_H = 1400;
  public static final int HALF_LEVEL_W = LEVEL_W / 2;
  public static final int HALF_LEVEL_H = LEVEL_H / 2;

  public static final int HALF_PITCH_W = 442;
  public static final int HALF_PITCH_H = 622;

  public static final int GOAL_WIDTH = 186;
  public static final int GOAL_DEPTH = 20;
  public static final int HALF_GOAL_W = GOAL_WIDTH / 2;

  public static final double[] PITCH_BOUNDS_X = {HALF_LEVEL_W - HALF_PITCH_W, HALF_LEVEL_W + HALF_PITCH_W};
  public static final double[] PITCH_BOUNDS_Y = {HALF_LEVEL_H - HALF_PITCH_H, HALF_LEVEL_H + HALF_PITCH_H};
  public static final double[] GOAL_BOUNDS_X = {HALF_LEVEL_W - HALF_GOAL_W, HALF_LEVEL_W + HALF_GOAL_W};
  public static final double[] GOAL_BOUNDS_Y = {
    HALF_LEVEL_H - HALF_PITCH_H - GOAL_DEPTH,
    HALF_LEVEL_H + HALF_PITCH_H + GOAL_DEPTH
  };

  public static final int AI_MIN_X = 78;
  public static final int AI_MAX_X = LEVEL_W - 78;
  public static final int AI_MIN_Y = 98;
  public static final int AI_MAX_Y = LEVEL_H - 98;

  private static final double[][] PLAYER_START_POS = {
    {350, 550}, {650, 450}, {200, 850}, {500, 750}, {800, 950}, {350, 1250}, {650, 1150}
  };

  public final Team[] teams = new Team[2];
  public final List<Player> players = new ArrayList<>();
  public final Goal[] goals = new Goal[2];
  public Ball ball;

  public Player kickoffPlayer;
  public int scoreTimer = 0;
  public int scoringTeam = 1;

  public Difficulty difficulty;

  private Vector2 cameraFocus;
  private final Sprite pitch = new Sprite();
  private final Sprite[] arrows = new Sprite[2];
  private final List<Sprite> drawOrder = new ArrayList<>();
  private ScoreBar scoreBar;
  private boolean frozen = false;

  /** The theme music played while the menu game runs in the background. */
  private final Sprite theme = new Sprite();

  public Play(Controls p1, Controls p2, int difficulty) {
    this.theme.addSound("theme", "soccer/assets/music/theme.wav");
    this.teams[0] = new Team(p1);
    this.teams[1] = new Team(p2);
    this.difficulty = Difficulty.DIFFICULTY[difficulty];

    this.addSound("crowd", "soccer/assets/sounds/crowd.wav");
    this.addSound("start", "soccer/assets/sounds/start.wav");
    this.addSound("move", "soccer/assets/sounds/move.wav");
    for (int i = 0; i < 4; i++) {
      this.addSound("kick" + i, String.format("soccer/assets/sounds/kick%d.wav", i));
    }
    for (int i = 0; i < 2; i++) {
      this.addSound("goal" + i, String.format("soccer/assets/sounds/goal%d.wav", i));
    }

    this.pitch.addCostume("pitch", "soccer/assets/images/pitch.png");
    this.add(this.pitch);
    this.pitch.goToBackLayer();

    for (int t = 0; t < 2; t++) {
      this.arrows[t] = new Sprite();
      this.arrows[t].addCostume("arrow" + t, String.format("soccer/assets/images/arrow%d.png", t));
      this.arrows[t].switchCostume("arrow" + t);
      this.arrows[t].hide();
      this.add(this.arrows[t]);
    }

    this.scoreBar = new ScoreBar(this);

    if (this.teams[0].human()) {
      this.playSound("start");
    }

    this.reset();
  }

  /** Returns true, if the given position is inside the level area. */
  public static boolean allowMovement(double x, double y) {
    if (Math.abs(x - HALF_LEVEL_W) > HALF_LEVEL_W) {
      return false;
    } else if (Math.abs(x - HALF_LEVEL_W) < HALF_GOAL_W + 20) {
      return Math.abs(y - HALF_LEVEL_H) < HALF_PITCH_H;
    } else {
      return Math.abs(y - HALF_LEVEL_H) < HALF_LEVEL_H;
    }
  }

  /** Returns true, if the given position is on the pitch or in a goal. */
  public boolean onPitch(double x, double y) {
    return (x >= PITCH_BOUNDS_X[0] && x <= PITCH_BOUNDS_X[1] && y >= PITCH_BOUNDS_Y[0] && y <= PITCH_BOUNDS_Y[1])
        || (x >= GOAL_BOUNDS_X[0]
            && x <= GOAL_BOUNDS_X[1]
            && y >= HALF_LEVEL_H - HALF_PITCH_H - GOAL_DEPTH
            && y <= HALF_LEVEL_H - HALF_PITCH_H)
        || (x >= GOAL_BOUNDS_X[0]
            && x <= GOAL_BOUNDS_X[1]
            && y >= HALF_LEVEL_H + HALF_PITCH_H
            && y <= HALF_LEVEL_H + HALF_PITCH_H + GOAL_DEPTH);
  }

  public boolean isFrozen() {
    return this.frozen;
  }

  /**
   * Stops all sounds of this stage, including the music. Must be called
   * before switching to another stage.
   */
  public void stopAllGameSounds() {
    this.theme.stopAllSounds();
    this.stopAllSounds();
  }

  /** Plays a random sound variation, but only during a game. */
  public void playGameSound(String name, int count) {
    if (this.teams[0].human()) {
      this.playSound(name + Random.randomInt(0, count - 1));
    }
  }

  /** Called at game start, and after a goal has been scored. */
  public void reset() {
    for (Player p : this.players) {
      p.remove();
      p.shadow.remove();
    }
    this.players.clear();
    if (this.ball != null) {
      this.ball.remove();
      this.ball.shadow.remove();
    }

    for (double[] pos : PLAYER_START_POS) {
      double x = pos[0] + Random.randomInt(-32, 32);
      double y = pos[1] + Random.randomInt(-32, 32);
      this.players.add(new Player(this, x, y, 0));
      this.players.add(
          new Player(this, LEVEL_W - x + Random.randomInt(-32, 32), LEVEL_H - y + Random.randomInt(-32, 32), 1));
    }

    // The peer for each player is the opposing team player at the other end of the list
    for (int i = 0; i < this.players.size(); i++) {
      this.players.get(i).peer = this.players.get(this.players.size() - 1 - i);
    }

    for (Player p : this.players) {
      this.add(p);
      this.add(p.shadow);
    }

    for (int team = 0; team < this.goals.length; team++) {
      if (this.goals[team] == null) {
        this.goals[team] = new Goal(this, team);
        this.add(this.goals[team]);
      }
    }

    this.teams[0].activeControlPlayer = this.players.get(0);
    this.teams[1].activeControlPlayer = this.players.get(1);

    // The team that didn't score last kicks off
    int otherTeam = this.scoringTeam == 0 ? 1 : 0;
    this.kickoffPlayer = this.players.get(otherTeam);
    this.kickoffPlayer.vpos = new Vector2(HALF_LEVEL_W - 30 + otherTeam * 60, HALF_LEVEL_H);

    this.ball = new Ball(this);
    this.add(this.ball);
    this.add(this.ball.shadow);

    this.cameraFocus = new Vector2(this.ball.vpos);
  }

  @Override
  public void run() {
    // Menu games are silent except for the theme music
    if (this.teams[0].human()) {
      if (!this.isSoundPlaying("crowd")) {
        this.playSound("crowd");
      }
    } else {
      this.theme.playSound("theme");
    }

    if (!this.frozen) {
      for (Team team : this.teams) {
        if (team.human()) {
          team.controls.poll(this);
        }
      }

      this.scoreTimer -= 1;

      if (this.scoreTimer == 0) {
        // Reset for a new kick-off after a goal has been scored
        this.reset();
      } else if (this.scoreTimer < 0
          && Math.abs(this.ball.vpos.getY() - HALF_LEVEL_H) > HALF_PITCH_H) {
        // A goal has been scored
        this.playGameSound("goal", 2);
        this.scoringTeam = this.ball.vpos.getY() < HALF_LEVEL_H ? 0 : 1;
        this.teams[this.scoringTeam].score += 1;
        this.scoreTimer = 60;
      }

      // Each frame, reset the mark and lead of each player
      for (Player b : this.players) {
        b.mark = b.peer;
        b.lead = null;
      }

      if (this.ball.owner != null) {
        Player o = this.ball.owner;
        Vector2 pos = o.vpos;
        int team = o.team;
        Goal ownersTargetGoal = this.goals[team];
        int otherTeam = 1 - team;

        if (this.difficulty.goalieEnabled) {
          // Make the nearest opposing player mark the goal
          Player nearest = null;
          double best = Double.MAX_VALUE;
          for (Player p : this.players) {
            if (p.team != team) {
              double d = p.vpos.sub(ownersTargetGoal.vpos).length();
              if (d < best) {
                best = d;
                nearest = p;
              }
            }
          }
          if (nearest != null) {
            o.peer.mark = nearest.mark;
            nearest.mark = ownersTargetGoal;
          }
        }

        // Choose one or two lead players to pursue the ball owner
        List<Player> l = new ArrayList<>();
        for (Player p : this.players) {
          if (p.team != team
              && p.timer <= 0
              && (!this.teams[otherTeam].human() || p != this.teams[otherTeam].activeControlPlayer)
              && !(p.mark instanceof Goal)) {
            l.add(p);
          }
        }
        l.sort(Comparator.comparingDouble(p -> p.vpos.sub(pos).length()));

        List<Player> a = new ArrayList<>();
        List<Player> b = new ArrayList<>();
        for (Player p : l) {
          if (team == 0 ? p.vpos.getY() > pos.getY() : p.vpos.getY() < pos.getY()) {
            a.add(p);
          } else {
            b.add(p);
          }
        }

        // Interleave both lists
        List<Player> zipped = new ArrayList<>();
        for (int i = 0; i < Math.max(a.size(), b.size()); i++) {
          if (i < a.size()) {
            zipped.add(a.get(i));
          }
          if (i < b.size()) {
            zipped.add(b.get(i));
          }
        }

        if (!zipped.isEmpty()) {
          zipped.get(0).lead = Player.LEAD_DISTANCE_1;
          if (this.difficulty.secondLeadEnabled && zipped.size() > 1) {
            zipped.get(1).lead = Player.LEAD_DISTANCE_2;
          }
        }

        // Kick-off has taken place
        this.kickoffPlayer = null;
      }

      // Manual player switching, one frame after the shoot key was pressed
      // and not consumed by kicking the ball
      for (int t = 0; t < 2; t++) {
        Team team = this.teams[t];
        if (team.human() && team.controls.shootDeferred()) {
          Player best = null;
          double bestDistance = Double.MAX_VALUE;
          for (Player p : this.players) {
            if (p.team == t) {
              double distToBall = p.vpos.sub(this.ball.vpos).length();
              int goalDir = 2 * t - 1;
              if (this.ball.owner != null
                  && (p.vpos.getY() - this.ball.vpos.getY()) * goalDir < 0) {
                distToBall /= 2;
              }
              if (distToBall < bestDistance) {
                bestDistance = distToBall;
                best = p;
              }
            }
          }
          team.activeControlPlayer = best;
        }
      }

      // Move the camera towards the ball
      double[] n = SoccerMath.safeNormalise(this.cameraFocus.sub(this.ball.vpos));
      double distance = n[2];
      if (distance > 0) {
        this.cameraFocus =
            this.cameraFocus.sub(SoccerMath.unitVector(n).multiply(Math.min(distance, 8)));
      }

      // First team to 9 wins
      if (Math.max(this.teams[0].score, this.teams[1].score) == 9 && this.scoreTimer == 1) {
        this.frozen = true;
        var over = new UISprite();
        int winner = this.teams[1].score > this.teams[0].score ? 1 : 0;
        over.addCostume("over", String.format("soccer/assets/images/over%d.png", winner));
        over.setPosition(0, 0);
        this.add(over);
        for (int t = 0; t < 2; t++) {
          var score = new UISprite();
          score.addCostume("l", String.format("soccer/assets/images/l%d%d.png", t, this.teams[t].score));
          score.switchCostume("l");
          // Blit position in the original: (HALF_WINDOW_W + 25 - 125 * t, 144)
          score.setPosition(
              WIDTH / 2.0 + 25 - 125 * t + score.getWidth() / 2.0 - WIDTH / 2.0,
              HEIGHT / 2.0 - (144 + score.getHeight() / 2.0));
          this.add(score);
        }
      }
    }

    // Position the camera, so the level always fills the window
    double offsetX = Operators.constrain(this.cameraFocus.getX() - WIDTH / 2.0, 0, LEVEL_W - WIDTH);
    double offsetY = Operators.constrain(this.cameraFocus.getY() - HEIGHT / 2.0, 0, LEVEL_H - HEIGHT);
    this.getCamera().setPosition(offsetX + WIDTH / 2.0 - HALF_LEVEL_W, HALF_LEVEL_H - (offsetY + HEIGHT / 2.0));

    this.scoreBar.update();
    this.relayer();
  }

  /** Sorts all objects by y position, so lower objects are drawn in front. */
  protected void relayer() {
    this.drawOrder.clear();
    this.drawOrder.add(this.goals[0]);

    List<SoccerActor> objects = new ArrayList<>(this.players);
    objects.add(this.ball);
    objects.sort(Comparator.comparingDouble(o -> o.vpos.getY()));
    this.drawOrder.addAll(objects);
    for (SoccerActor o : objects) {
      this.drawOrder.add(o.shadow);
    }
    this.drawOrder.add(this.goals[1]);

    for (Sprite s : this.drawOrder) {
      s.goToFrontLayer();
    }

    // Show an arrow above the active player of each human team
    for (int t = 0; t < 2; t++) {
      Sprite arrow = this.arrows[t];
      if (this.teams[t].human() && this.teams[t].activeControlPlayer != null) {
        Player p = this.teams[t].activeControlPlayer;
        arrow.setPosition(p.getX(), p.getY() + 39.5);
        arrow.show();
        arrow.goToFrontLayer();
      } else {
        arrow.hide();
      }
    }
  }

  /** Returns true, if the score bar should be displayed. */
  protected boolean showHud() {
    return !this.frozen;
  }

  @Override
  public void whenKeyPressed(org.openpatch.scratch.KeyCode key) {
    if (this.frozen && key == org.openpatch.scratch.KeyCode.SPACE) {
      this.stopAllGameSounds();
      Window.getInstance().setStage(new Menu());
    }
  }
}
