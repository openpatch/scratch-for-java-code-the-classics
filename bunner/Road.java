package bunner;

import java.util.ArrayList;
import java.util.List;
import org.openpatch.scratch.Random;

public class Road extends ActiveRow {

  public Road(Play play, Row predecessor, int index, double y) {
    super(
        play,
        Car::new,
        // Car velocities are any of the numbers from -5 to 5, except zero
        // or the velocity of the cars on the previous row
        possibleDxs(predecessor),
        "road",
        index,
        y);
  }

  private static int[] possibleDxs(Row predecessor) {
    List<Integer> dxs = new ArrayList<>();
    for (int dx = -5; dx <= 5; dx++) {
      if (dx != 0 && dx != predecessor.dx) {
        dxs.add(dx);
      }
    }
    int[] array = new int[dxs.size()];
    for (int i = 0; i < array.length; i++) {
      array[i] = dxs.get(i);
    }
    return array;
  }

  @Override
  public void run() {
    if (!this.isPlaying()) {
      return;
    }
    super.run();

    // Trigger car sound effects. The zoom effect plays when the player is on
    // the row above or below the car, the honk effect when on the same row.
    double[][] triggers = {
      {-Play.ROW_HEIGHT, Car.SOUND_ZOOM},
      {0, Car.SOUND_HONK},
      {Play.ROW_HEIGHT, Car.SOUND_ZOOM}
    };
    for (double[] trigger : triggers) {
      if (this.play.bunner != null && this.play.bunner.my == this.my + trigger[0]) {
        for (BunActor child : this.children) {
          if (child instanceof Car car) {
            double dx = car.mx - this.play.bunner.mx;
            if (Math.abs(dx) < 100
                && ((car.dx < 0) != (dx < 0))
                && (trigger[0] == 0 || Math.abs(car.dx) > 1)) {
              car.playCarSound((int) trigger[1]);
            }
          }
        }
      }
    }
  }

  @Override
  public int[] checkCollision(double x) {
    if (this.collide(x, 0) != null) {
      this.play.playSound("splat0");
      return new int[] {Bunner.STATE_SPLAT, 0};
    }
    return new int[] {Bunner.STATE_ALIVE, 0};
  }

  @Override
  public void playLandingSound() {
    this.play.playSound("road0");
  }

  @Override
  public Row next() {
    if (this.index == 0) {
      return new Road(this.play, this, 1, this.my - Play.ROW_HEIGHT);
    } else if (this.index < 5) {
      // 80% chance of another road
      double r = Random.random();
      if (r < 0.8) {
        return new Road(this.play, this, this.index + 1, this.my - Play.ROW_HEIGHT);
      } else if (r < 0.88) {
        return new Grass(this.play, this, Random.randomInt(0, 6), this.my - Play.ROW_HEIGHT);
      } else if (r < 0.94) {
        return new Rail(this.play, this, 0, this.my - Play.ROW_HEIGHT);
      } else {
        return new Pavement(this.play, this, 0, this.my - Play.ROW_HEIGHT);
      }
    } else {
      // We've reached the maximum of 5 roads in a row
      double r = Random.random();
      if (r < 0.6) {
        return new Grass(this.play, this, Random.randomInt(0, 6), this.my - Play.ROW_HEIGHT);
      } else if (r < 0.9) {
        return new Rail(this.play, this, 0, this.my - Play.ROW_HEIGHT);
      } else {
        return new Pavement(this.play, this, 0, this.my - Play.ROW_HEIGHT);
      }
    }
  }
}
