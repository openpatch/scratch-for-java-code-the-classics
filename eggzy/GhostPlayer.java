package eggzy;

import java.util.List;

/**
 * A ghost replaying the recorded movements of a previous run. Only visible
 * while it is on the same level as the player.
 */
public class GhostPlayer extends EzActor {

  private final Play game;
  private final List<Replays.Entry> replayData;
  private int replayFrame = 0;
  private int level = 0;

  public GhostPlayer(Play game, List<Replays.Entry> replayData) {
    super(replayData.get(0).x(), replayData.get(0).y(), Play.ANCHOR_PLAYER);
    this.game = game;
    this.replayData = replayData;
  }

  @Override
  public void run() {
    if (!this.isPlaying()) {
      return;
    }
    this.replayFrame += 1;
    if (this.replayFrame < this.replayData.size()) {
      Replays.Entry entry = this.replayData.get(this.replayFrame);
      this.x = entry.x();
      this.y = entry.y();
      this.level = entry.level();
      String sprite = entry.sprite();
      this.setImage(sprite.equals("blank") ? "blank" : "ghost_" + sprite);
    }

    // Only show the ghost while it's on the same level as the player
    if (this.level == this.game.levelIndex) {
      this.show();
    } else {
      this.hide();
    }

    super.run();
  }
}
