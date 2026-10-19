package kinetix;

/**
 * Base class for keyboard and AI controls. Call {@link #update()} each frame
 * to keep track of whether the fire button was just pressed.
 */
public abstract class Controls {

  private boolean firePreviouslyDown = false;
  private boolean firePressed = false;

  /** Call each frame to update the fire button status. */
  public void update() {
    boolean fireDown = this.fireDown();
    this.firePressed = fireDown && !this.firePreviouslyDown;
    this.firePreviouslyDown = fireDown;
  }

  /** Movement on the X axis for this frame. */
  public abstract double getX();

  /** Is the fire button currently held down? */
  public abstract boolean fireDown();

  /** Was the fire button pressed since the last frame? */
  public boolean firePressed() {
    return this.firePressed;
  }
}
