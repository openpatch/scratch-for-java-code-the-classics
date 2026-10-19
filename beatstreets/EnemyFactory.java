package beatstreets;

import org.openpatch.scratch.Vector2;

/**
 * Creates an enemy of the portal's choice at the given position. Portals hold
 * a list of these instead of the enemy classes the original passes around.
 */
@FunctionalInterface
public interface EnemyFactory {

  Enemy create(Vector2 pos);
}
