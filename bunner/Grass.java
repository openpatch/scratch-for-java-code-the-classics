package bunner;

import org.openpatch.scratch.Random;

/** Grass rows sometimes contain hedges which block the player's movement. */
public class Grass extends Row {

  private Integer hedgeRowIndex = null;

  public Grass(Play play, Row predecessor, int index, double y) {
    super(play, "grass", index, y);

    boolean[] hedgeMask = null;

    // Create a brand-new set of hedges, if the previous row didn't have any
    if (!(predecessor instanceof Grass) || ((Grass) predecessor).hedgeRowIndex == null) {
      if (Random.random() < 0.5 && index > 7 && index < 14) {
        hedgeMask = generateHedgeMask();
        this.hedgeRowIndex = 0;
      }
    } else if (((Grass) predecessor).hedgeRowIndex == 0) {
      hedgeMask = ((Grass) predecessor).hedgeMask;
      this.hedgeRowIndex = 1;
    }
    this.hedgeMask = hedgeMask;

    if (this.hedgeRowIndex != null) {
      // See comments in the original for the meaning of previousMidSegment
      Integer previousMidSegment = null;
      for (int i = 1; i < 13; i++) {
        boolean[] slice = {
          this.hedgeMask[i - 1], this.hedgeMask[i], this.hedgeMask[i + 1], this.hedgeMask[i + 2]
        };
        int[] result = classifyHedgeSegment(slice, previousMidSegment);
        if (result[0] != -1) {
          this.children.add(new Hedge(this.play, result[0], this.hedgeRowIndex, i * 40 - 20, 0));
        }
        previousMidSegment = result[1] == -1 ? null : result[1];
      }
    }
  }

  private final boolean[] hedgeMask;

  /**
   * Creates a mask representing the presence or absence of hedges in a grass
   * row. False means a hedge is present, True represents a gap.
   */
  private static boolean[] generateHedgeMask() {
    boolean[] mask = new boolean[12];
    for (int i = 0; i < 12; i++) {
      mask[i] = Random.random() < 0.01;
    }
    mask[Random.randomInt(0, 11)] = true;

    // Widen gaps to a minimum of 3 tiles
    boolean[] widened = new boolean[12];
    for (int i = 0; i < 12; i++) {
      int sum = 0;
      for (int j = Math.max(0, i - 1); j < Math.min(12, i + 2); j++) {
        sum += mask[j] ? 1 : 0;
      }
      widened[i] = sum > 0;
    }

    boolean[] result = new boolean[15];
    result[0] = widened[0];
    System.arraycopy(widened, 0, result, 1, 12);
    result[13] = widened[11];
    result[14] = widened[11];
    return result;
  }

  /**
   * Determines which sprite a particular hedge segment should use. Returns
   * the sprite x position (or -1 for no sprite) and the new value of
   * previousMidSegment (or -1 for none).
   */
  private static int[] classifyHedgeSegment(boolean[] mask, Integer previousMidSegment) {
    int spriteX;
    if (mask[1]) {
      spriteX = -1;
    } else {
      spriteX = 3 - 2 * (mask[0] ? 1 : 0) - (mask[2] ? 1 : 0);
    }

    if (spriteX == 3) {
      // This is a middle piece
      if (previousMidSegment != null && previousMidSegment == 4 && mask[3]) {
        return new int[] {5, -1};
      }
      if (previousMidSegment == null || previousMidSegment == 4) {
        return new int[] {3, 3};
      }
      return new int[] {4, 4};
    }
    return new int[] {spriteX, -1};
  }

  @Override
  public boolean allowMovement(double x) {
    return super.allowMovement(x) && this.collide(x, 8) == null;
  }

  @Override
  public void playLandingSound() {
    this.play.playSound("grass0");
  }

  @Override
  public Row next() {
    if (this.index <= 5) {
      return new Grass(this.play, this, this.index + 8, this.my - Play.ROW_HEIGHT);
    } else if (this.index == 6) {
      return new Grass(this.play, this, 7, this.my - Play.ROW_HEIGHT);
    } else if (this.index == 7) {
      return new Grass(this.play, this, 15, this.my - Play.ROW_HEIGHT);
    } else if (this.index >= 8 && this.index <= 14) {
      return new Grass(this.play, this, this.index + 1, this.my - Play.ROW_HEIGHT);
    } else if (Random.random() < 0.5) {
      return new Road(this.play, this, 0, this.my - Play.ROW_HEIGHT);
    } else {
      return new Water(this.play, this, 0, this.my - Play.ROW_HEIGHT);
    }
  }
}
