package eggzy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.openpatch.scratch.extensions.fs.SaveData;

/**
 * The replays of previous runs, saved with {@link SaveData} in the operating
 * system's user data folder. Each replay is one line of
 * "x,y,level,sprite" entries separated by semicolons - the same format as the
 * original. Up to {@link #MAX_REPLAYS} replays are kept, the longest ones
 * first, and the length of the longest replay is the high score.
 */
public final class Replays {

  /** One recorded frame: the player's position, level and sprite. */
  public record Entry(double x, double y, int level, String sprite) {}

  /** The saved data: one line per replay. */
  public static class Data {
    public String[] replays = new String[0];
  }

  public static final int MAX_REPLAYS = 10;

  private static final SaveData<Data> SAVES = new SaveData<>("eggzy", "replays", Data.class);

  /** All replays, like the all_replays global of the original. */
  public static final List<List<Entry>> all = new ArrayList<>();

  /** The high score: the number of frames of the longest replay. */
  public static int highScore = 0;

  private Replays() {}

  /** Loads the replays and the high score. Missing save means no replays. */
  public static void load() {
    all.clear();
    Data data;
    try {
      data = SAVES.loadOrDefault(() -> new Data());
    } catch (IOException e) {
      System.out.println("Error while loading replays: '" + e
          + "'. Replay data will be reset");
      all.clear();
      highScore = 0;
      return;
    }
    for (String line : data.replays) {
      line = line.strip();
      if (line.isEmpty()) {
        continue;
      }
      List<Entry> replay = new ArrayList<>();
      for (String entryStr : line.split(";")) {
        String[] elements = entryStr.split(",");
        replay.add(new Entry(Double.parseDouble(elements[0]),
            Double.parseDouble(elements[1]), Integer.parseInt(elements[2]),
            elements[3]));
      }
      all.add(replay);
    }
    highScore = 0;
    for (List<Entry> replay : all) {
      highScore = Math.max(highScore, replay.size());
    }
  }

  /**
   * Adds the replay of the run that just ended, keeps only the
   * {@link #MAX_REPLAYS} longest replays and saves them.
   */
  public static void add(List<Entry> replay) {
    all.add(replay);
    if (all.size() > MAX_REPLAYS) {
      // Sort replays by length, longest first
      all.sort((a, b) -> b.size() - a.size());
      while (all.size() > MAX_REPLAYS) {
        all.remove(all.size() - 1);
      }
    }
    save();
  }

  public static void save() {
    Data data = new Data();
    data.replays = new String[all.size()];
    for (int i = 0; i < all.size(); i++) {
      StringBuilder line = new StringBuilder();
      for (Entry entry : all.get(i)) {
        line.append((int) entry.x()).append(',')
            .append((int) entry.y()).append(',')
            .append(entry.level()).append(',')
            .append(entry.sprite()).append(';');
      }
      data.replays[i] = line.length() > 0 ? line.substring(0, line.length() - 1) : "";
    }
    try {
      SAVES.save(data);
    } catch (IOException e) {
      System.out.println("Error while saving replays: " + e);
    }
  }
}
