package bunner;

/** The splat graphic left behind when the player is hit by a vehicle. */
public class Splat extends BunActor {

  public Splat(Play play, int direction) {
    super(play, false);

    this.addCostume("splat", String.format("bunner/assets/images/splat%d.png", direction));
    this.switchCostume("splat");
  }
}
