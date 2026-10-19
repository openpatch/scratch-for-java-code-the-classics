package soccer;

/** Difficulty settings for a game. */
public class Difficulty {

  public final boolean goalieEnabled;
  public final boolean secondLeadEnabled;
  public final double speedBoost;
  public final int holdoffTimer;

  public static final Difficulty[] DIFFICULTY = {
    new Difficulty(false, false, 0, 120),
    new Difficulty(false, true, 0.1, 90),
    new Difficulty(true, true, 0.2, 60)
  };

  public Difficulty(boolean goalieEnabled, boolean secondLeadEnabled, double speedBoost, int holdoffTimer) {
    this.goalieEnabled = goalieEnabled;
    this.secondLeadEnabled = secondLeadEnabled;
    this.speedBoost = speedBoost;
    this.holdoffTimer = holdoffTimer;
  }
}
