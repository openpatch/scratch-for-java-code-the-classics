package leadingedge;

import java.util.List;

/** The start/finish line, which also acts as a checkpoint for counting laps. */
public class TrackPieceStartLine extends TrackPiece {

  public TrackPieceStartLine(Play game) {
    super(
        List.of(new StartGantry(game)),
        0,
        0,
        null,
        new int[] {255, 255, 255},
        Play.TRACK_W);
  }
}
