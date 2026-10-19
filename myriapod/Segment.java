package myriapod;

import java.util.HashSet;

public class Segment extends MyrActor {

  public static final int DIRECTION_UP = 0;
  public static final int DIRECTION_RIGHT = 1;
  public static final int DIRECTION_DOWN = 2;
  public static final int DIRECTION_LEFT = 3;

  private static final int[] DX = {0, 1, 0, -1};
  private static final int[] DY = {-1, 0, 1, 0};

  // How much the segment moves along the secondary axis within a 16 frame cycle
  private static final int[] SECONDARY_AXIS_SPEED = {
    0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2
  };

  // Total secondary axis movement at each phase in the cycle
  private static final int[] SECONDARY_AXIS_POSITIONS = {
    0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 10, 12, 14
  };

  // Rotation matrices indexed by the edge the segment entered the cell from
  private static final int[][] ROTATION_MATRIX = {
    {1, 0, 0, 1},
    {0, -1, 1, 0},
    {-1, 0, 0, -1},
    {0, 1, -1, 0}
  };

  public int cellX;
  public int cellY;
  public int health;

  private boolean fast;
  private boolean head;
  private int inEdge = DIRECTION_LEFT;
  private int outEdge = DIRECTION_RIGHT;
  private int disallowDirection = DIRECTION_UP;
  private int previousXDirection = DIRECTION_RIGHT;

  public Segment(Play play, int cx, int cy, int health, boolean fast, boolean head) {
    super(play);

    this.cellX = cx;
    this.cellY = cy;
    this.health = health;
    this.fast = fast;
    this.head = head;

    for (int h = 0; h < 2; h++) {
      for (int d = 0; d < 8; d++) {
        for (int l = 0; l < 4; l++) {
          this.addCostume(
              String.format("seg%d%d%d%d%d", fast ? 1 : 0, h, head ? 1 : 0, d, l),
              String.format(
                  "myriapod/assets/images/seg%d%d%d%d%d.png",
                  fast ? 1 : 0, h, head ? 1 : 0, d, l));
        }
      }
    }

    this.switchCostume(String.format("seg%d0%d00", fast ? 1 : 0, head ? 1 : 0));
    this.mx = cx * 32 + 32;
    this.my = cy * 32 + 16;
    this.sync();
  }

  private static int inverseDirection(int dir) {
    return (dir + 2) % 4;
  }

  private static boolean isHorizontal(int dir) {
    return dir == DIRECTION_LEFT || dir == DIRECTION_RIGHT;
  }

  /**
   * Ranks a proposed direction. A lower value is better. Each factor is more
   * important than the ones following it.
   */
  private int rank(int proposedOutEdge, HashSet<String> occupied) {
    int newCellX = this.cellX + DX[proposedOutEdge];
    int newCellY = this.cellY + DY[proposedOutEdge];

    boolean out =
        newCellX < 0 || newCellX > Play.NUM_GRID_COLS - 1
            || newCellY < 0 || newCellY > Play.NUM_GRID_ROWS - 1;

    boolean turningBackOnSelf = proposedOutEdge == this.inEdge;
    boolean directionDisallowed = proposedOutEdge == this.disallowDirection;

    Rock rock = null;
    if (!out && !(newCellY == 0 && newCellX < 0)) {
      rock = this.play.rockAt(newCellX, newCellY);
    }
    boolean rockPresent = rock != null;

    boolean occupiedBySegment =
        occupied.contains(newCellX + "," + newCellY)
            || occupied.contains(this.cellX + "," + this.cellY + "," + proposedOutEdge);

    boolean horizontalBlocked = rockPresent ? isHorizontal(proposedOutEdge) : !isHorizontal(proposedOutEdge);
    boolean sameAsPreviousXDirection = proposedOutEdge == this.previousXDirection;

    return (out ? 64 : 0)
        + (turningBackOnSelf ? 32 : 0)
        + (directionDisallowed ? 16 : 0)
        + (occupiedBySegment ? 8 : 0)
        + (rockPresent ? 4 : 0)
        + (horizontalBlocked ? 2 : 0)
        + (sameAsPreviousXDirection ? 1 : 0);
  }

  @Override
  public void run() {
    if (this.play.isFrozen()) {
      return;
    }

    int phase = this.play.time % 16;

    if (phase == 0) {
      // The segment is entering a new grid cell
      this.cellX += DX[this.outEdge];
      this.cellY += DY[this.outEdge];
      this.inEdge = inverseDirection(this.outEdge);

      int upperLimit = this.play.player != null ? 18 : 0;
      if (this.cellY == upperLimit) {
        this.disallowDirection = DIRECTION_UP;
      }
      if (this.cellY == Play.NUM_GRID_ROWS - 1) {
        this.disallowDirection = DIRECTION_DOWN;
      }
    } else if (phase == 4) {
      // Decide which edge we're going to leave the current cell through
      HashSet<String> occupied = this.play.occupied;
      int best = 0;
      int bestRank = this.rank(0, occupied);
      for (int d = 1; d < 4; d++) {
        int r = this.rank(d, occupied);
        if (r < bestRank) {
          best = d;
          bestRank = r;
        }
      }
      this.outEdge = best;

      if (isHorizontal(this.outEdge)) {
        this.previousXDirection = this.outEdge;
      }

      int newCellX = this.cellX + DX[this.outEdge];
      int newCellY = this.cellY + DY[this.outEdge];

      // Destroy any rock that might be in the new cell
      if (newCellX >= 0 && newCellX < Play.NUM_GRID_COLS) {
        this.play.damage(newCellX, newCellY, 5, false);
      }

      // Set the new cell as occupied
      this.play.occupied.add(newCellX + "," + newCellY);
      this.play.occupied.add(newCellX + "," + newCellY + "," + inverseDirection(this.outEdge));
    }

    int turnIdx = Math.floorMod(this.outEdge - this.inEdge, 4);

    // Offset from the centre of the cell
    int offsetX = SECONDARY_AXIS_POSITIONS[phase] * (2 - turnIdx);
    int stolenYMovement = (turnIdx % 2) * SECONDARY_AXIS_POSITIONS[phase];
    int offsetY = -16 + phase * 2 - stolenYMovement;

    int[] rotationMatrix = ROTATION_MATRIX[this.inEdge];
    int rotatedX = offsetX * rotationMatrix[0] + offsetY * rotationMatrix[1];
    int rotatedY = offsetX * rotationMatrix[2] + offsetY * rotationMatrix[3];

    this.mx = this.cellX * 32 + 32 + rotatedX;
    this.my = this.cellY * 32 + 16 + rotatedY;

    int direction =
        Math.floorMod(
            SECONDARY_AXIS_SPEED[phase] * (turnIdx - 2) + this.inEdge * 2 + 4, 8);
    int legFrame = phase / 4;

    this.switchCostume(
        String.format(
            "seg%d%d%d%d%d",
            this.fast ? 1 : 0,
            this.health == 2 ? 1 : 0,
            this.head ? 1 : 0,
            direction,
            legFrame));
    this.sync();
  }
}
