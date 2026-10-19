package kinetix;

import org.openpatch.scratch.Sprite;

/**
 * The bat. It can change its type via powerups, animating between the normal
 * bat and the new type. The sprite is anchored 15 pixels from its top edge,
 * like the original, so that the collision edge at BAT_TOP_EDGE lines up with
 * the visible bat.
 */
public class Bat extends KxActor {

  private final Play play;
  private final Controls controls;

  private BatType targetType = BatType.NORMAL;
  private int frame = 0;
  private int fireTimer = 0;

  public BatType currentType = BatType.NORMAL;
  public final Sprite shadow;

  public Bat(Play play, Controls controls) {
    this.play = play;
    this.controls = controls;

    this.addCostume("bat00", "kinetix/assets/images/bat00.png");
    for (int type = 1; type <= 4; type++) {
      for (int f = 0; f <= 3; f++) {
        this.addCostume("bat" + type + f, String.format("kinetix/assets/images/bat%d%d.png", type, f));
      }
    }
    this.addCostume("bat23f", "kinetix/assets/images/bat23f.png");
    this.switchCostume("bat00");
    this.setRotationCenter(45, 15);

    this.x = 320;
    this.y = 590;
    this.sync();

    this.shadow = new Sprite();
    this.shadow.addCostume("bats00", "kinetix/assets/images/bats00.png");
    for (int type = 1; type <= 4; type++) {
      for (int f = 0; f <= 3; f++) {
        this.shadow.addCostume(
            "bats" + type + f, String.format("kinetix/assets/images/bats%d%d.png", type, f));
      }
    }
    this.shadow.switchCostume("bats00");
    this.shadow.setRotationCenter(45, 15);
    this.syncShadow();
  }

  @Override
  public void run() {
    if (!this.isPlaying()) {
      return;
    }
    // Handle animating to a new bat type. If we're a normal bat, we animate to
    // a new type over 12 game frames. If we're switching from something other
    // than the normal bat, we first animate backwards to the normal bat.
    if (this.targetType != BatType.NORMAL && this.targetType == this.currentType && this.frame < 12) {
      this.frame += 1;
    }
    if (this.targetType != this.currentType && this.frame > 0) {
      this.frame -= 1;
    }
    if (this.frame == 0) {
      this.currentType = this.targetType;
    }

    this.switchCostume("bat" + this.currentType.ordinal() + this.frame / 4);

    this.fireTimer -= 1;

    // Fire gun?
    if (this.controls.fireDown()
        && this.currentType == BatType.GUN
        && this.frame == 12
        && this.fireTimer <= 0) {
      this.fireTimer = Play.FIRE_INTERVAL;

      this.switchCostume("bat23f");

      this.play.addBullet(new Bullet(this.play, this.x - 20, this.y, 0));
      this.play.addBullet(new Bullet(this.play, this.x + 20, this.y, 1));

      this.play.playSound("laser0");
    }

    // Move the bat, don't let it go off the edge of the screen
    double newX = this.x + this.controls.getX();

    // Enforce left boundary
    double minX = Play.BAT_MIN_X + this.getWidth() / 2.0;
    newX = Math.max(minX, newX);

    if (!this.play.portalActive) {
      // Enforce right boundary
      double maxX = Play.BAT_MAX_X - this.getWidth() / 2.0;
      newX = Math.min(maxX, newX);
    }

    this.x = newX;
    this.sync();

    this.shadow.switchCostume("bats" + this.currentType.ordinal() + this.frame / 4);
    this.syncShadow();
  }

  public void changeType(BatType type) {
    this.targetType = type;
  }

  public boolean isPortalTransitionComplete() {
    return this.x - this.getWidth() / 2.0 >= Play.WIDTH;
  }

  private void syncShadow() {
    this.shadow.setPosition(this.x + 16 - Play.WIDTH / 2.0, Play.HEIGHT / 2.0 - (this.y + 16));
  }
}
