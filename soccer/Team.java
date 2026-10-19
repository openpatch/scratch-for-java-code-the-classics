package soccer;

/** One team. A team without controls is computer controlled. */
public class Team {

  public final Controls controls;
  public Player activeControlPlayer;
  public int score = 0;

  public Team(Controls controls) {
    this.controls = controls;
  }

  public boolean human() {
    return this.controls != null;
  }
}
