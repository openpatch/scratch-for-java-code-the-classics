package bunner;

import org.openpatch.scratch.Random;
import org.openpatch.scratch.Sprite;

/**
 * A sound that loops indefinitely while its volume is above zero, like the
 * river and traffic ambience in the original. Each sprite owns its sounds
 * and can change their volume without changing other sounds in the game.
 */
public class LoopSound extends Sprite {

  private final String name;
  private final int count;
  private String current;

  public LoopSound(String name, int count) {
    this.name = name;
    this.count = count;
    for (int i = 0; i < count; i++) {
      this.addSound(name + i, "bunner/assets/sounds/" + name + i + ".wav");
    }
  }

  public void loop(double volume) {
    if (volume > 0) {
      if (this.current == null || !this.isSoundPlaying(this.current)) {
        this.current = this.name + Random.randomInt(0, this.count - 1);
      }
      this.setVolume(volume * 100);
      this.playSound(this.current);
    } else {
      this.stop();
    }
  }

  public void stop() {
    if (this.current != null) {
      this.stopSound(this.current);
      this.current = null;
    }
  }
}
