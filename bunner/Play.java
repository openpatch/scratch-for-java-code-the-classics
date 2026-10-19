package bunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.openpatch.scratch.KeyCode;
import org.openpatch.scratch.Operators;
import org.openpatch.scratch.Random;
import org.openpatch.scratch.Sprite;
import org.openpatch.scratch.Stage;
import org.openpatch.scratch.extensions.fs.SaveData;
import org.openpatch.scratch.Window;

public class Play extends Stage {

  public static final int WIDTH = 480;
  public static final int HEIGHT = 800;
  public static final int ROW_HEIGHT = 40;

  /** The saved high score, in the operating system's user data folder. */
  public static class HighScore {
    public int value = 0;
  }

  private static final SaveData<HighScore> SAVES =
      new SaveData<>("bunner", "slot1", HighScore.class);

  public final List<Row> rows = new ArrayList<>();
  public Bunner bunner;
  public Eagle eagle;

  public double scrollPos = -HEIGHT;

  /** A sprite owns the music so its volume is independent of sound effects. */
  private final Sprite theme = new Sprite();
  private final LoopSound riverSound;
  private final LoopSound trafficSound;
  private HUD hud;
  private boolean frozen = false;

  public static int highScore = loadHighScore();

  public Play() {
    this(true);
  }

  public Play(boolean withBunner) {
    this.theme.addSound("theme", "bunner/assets/music/theme.wav");
    this.addSound("bell0", "bunner/assets/sounds/bell0.wav");
    this.addSound("dirt0", "bunner/assets/sounds/dirt0.wav");
    this.addSound("eagle0", "bunner/assets/sounds/eagle0.wav");
    this.addSound("grass0", "bunner/assets/sounds/grass0.wav");
    this.addSound("jump0", "bunner/assets/sounds/jump0.wav");
    this.addSound("log0", "bunner/assets/sounds/log0.wav");
    this.addSound("road0", "bunner/assets/sounds/road0.wav");
    this.addSound("sidewalk0", "bunner/assets/sounds/sidewalk0.wav");
    this.addSound("splash0", "bunner/assets/sounds/splash0.wav");
    this.addSound("splat0", "bunner/assets/sounds/splat0.wav");
    for (int i = 0; i < 4; i++) {
      this.addSound("honk" + i, String.format("bunner/assets/sounds/honk%d.wav", i));
    }
    for (int i = 0; i < 2; i++) {
      this.addSound("train" + i, String.format("bunner/assets/sounds/train%d.wav", i));
    }
    for (int i = 0; i < 6; i++) {
      this.addSound("zoom" + i, String.format("bunner/assets/sounds/zoom%d.wav", i));
    }
    this.riverSound = new LoopSound("river", 2);
    this.trafficSound = new LoopSound("traffic", 3);

    // First (bottom) row is always grass
    this.addRow(new Grass(this, null, 0, 0));

    if (withBunner) {
      this.bunner = new Bunner(this, 240, -320);
      this.add(this.bunner);
    }

    this.hud = new HUD(this);
  }

  public boolean isFrozen() {
    return this.frozen;
  }

  /**
   * Stops all sounds of this stage, including the music and loop sounds.
   * Must be called before switching to another stage.
   */
  public void stopAllGameSounds() {
    this.riverSound.stop();
    this.trafficSound.stop();
    this.theme.stopAllSounds();
    this.stopAllSounds();
  }

  /** Plays a random sound variation. No sounds are played without a player. */
  public void playRandom(String name, int count) {
    if (this.bunner != null) {
      this.playSound(
          name + org.openpatch.scratch.Random.randomInt(0, count - 1));
    }
  }

  private void addRow(Row row) {
    this.rows.add(row);
    this.add(row);
    for (BunActor child : row.children) {
      child.row = row;
      child.my = row.my + child.relY;
      this.add(child);
      child.sync();
    }
  }

  public static int loadHighScore() {
    try {
      return SAVES.loadOrDefault(() -> new HighScore()).value;
    } catch (IOException e) {
      return 0;
    }
  }

  public static void saveHighScore(int highScore) {
    try {
      HighScore data = new HighScore();
      data.value = highScore;
      SAVES.save(data);
    } catch (IOException e) {
      // If an error occurs writing the save, just ignore it
    }
  }

  /** The current score, based on the furthest row reached. */
  public int score() {
    return (int) (-320 - this.bunner.minY) / 40;
  }

  @Override
  public void run() {
    this.theme.playSound("theme");
    this.theme.setVolume(this.bunner != null ? 40 : 100);

    if (!this.frozen) {
      // Scroll faster if the player is close to the top of the screen
      if (this.bunner != null) {
        this.scrollPos -=
            Operators.constrain((this.scrollPos + HEIGHT - this.bunner.my) / (HEIGHT / 4), 1, 3);
      } else {
        this.scrollPos -= 1;
      }

      // Remove rows which have scrolled off the bottom of the screen
      this.rows.removeIf(
          row -> {
            if (row.my >= (int) this.scrollPos + HEIGHT + ROW_HEIGHT * 2) {
              row.remove();
              row.removeChildren();
              return true;
            }
            return false;
          });

      // Add new rows above the top row
      while (this.rows.get(this.rows.size() - 1).my > (int) this.scrollPos + ROW_HEIGHT) {
        this.addRow(this.rows.get(this.rows.size() - 1).next());
      }

      // Play river and traffic sound effects, with volume based on the
      // player's proximity to rows of the appropriate types
      if (this.bunner != null) {
        double riverVolume = -0.2;
        double trafficVolume = -0.2;
        for (Row row : this.rows) {
          if (row instanceof Water) {
            riverVolume += 16.0 / Math.max(16.0, Math.abs(row.my - this.bunner.my));
          } else if (row instanceof Road) {
            trafficVolume += 16.0 / Math.max(16.0, Math.abs(row.my - this.bunner.my));
          }
        }
        this.riverSound.loop(Math.min(0.4, riverVolume));
        this.trafficSound.loop(Math.min(0.4, trafficVolume));
      } else {
        this.riverSound.stop();
        this.trafficSound.stop();
      }

      // Is it game over?
      if (this.bunner != null
          && this.bunner.state != Bunner.STATE_ALIVE
          && this.bunner.timer < 0) {
        Play.highScore = Math.max(Play.highScore, this.score());
        Play.saveHighScore(Play.highScore);
        this.frozen = true;
        var over = new org.openpatch.scratch.UISprite();
        over.addCostume("gameover", "bunner/assets/images/gameover.png");
        over.setPosition(0, 0);
        this.add(over);
      }
    }

    // Scroll the level
    this.getCamera().setPosition(0, -this.scrollPos);

    this.hud.update();
    this.relayer();
  }

  /** Sorts all rows and the player by y position, so lower rows draw in front. */
  private void relayer() {
    List<BunActor> actors = new ArrayList<>(this.rows);
    if (this.bunner != null) {
      actors.add(this.bunner);
    }
    actors.sort(Comparator.comparingInt(actor -> Math.floorDiv((int) actor.my + 39, ROW_HEIGHT)));

    for (BunActor actor : actors) {
      actor.goToFrontLayer();
      if (actor instanceof Row row) {
        for (BunActor child : row.children) {
          child.goToFrontLayer();
        }
      }
    }
    if (this.eagle != null) {
      this.eagle.goToFrontLayer();
    }
  }

  @Override
  public void whenKeyPressed(KeyCode key) {
    if (this.frozen && key == KeyCode.SPACE) {
      this.stopAllGameSounds();
      Window.getInstance().setStage(new Menu());
    }
  }
}
