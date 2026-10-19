package beatstreets;

/**
 * The separate shadow of an actor. Drawn at the actor's ground position, just
 * behind the actor itself.
 */
public class ShadowSprite extends ImageSprite {

  public final ScrollHeightActor owner;

  ShadowSprite(double anchorX, double anchorY, ScrollHeightActor owner) {
    this.owner = owner;
    this.setRotationCenter(anchorX, anchorY);
    this.hide();
  }

  public double drawSortKey() {
    // Drawn immediately before the actor that owns the shadow
    return this.owner.drawSortKey() - 0.5;
  }
}
