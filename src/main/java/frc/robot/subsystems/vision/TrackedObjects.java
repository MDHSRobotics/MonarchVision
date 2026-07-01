// This class and related object-tracking classes were initially created by AI Claude
// but subsequently modified and fully reviewed

package frc.robot.subsystems.vision;

import edu.wpi.first.math.geometry.Pose3d;
import frc.robot.subsystems.vision.ObjectVisionIO.ObjectObservation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Maintains a list of reliably tracked objects across multiple detection cycles.
 *
 * <p>Handles the common FRC object detection problem where a given object is detected
 * intermittently. An object is kept in the tracked list as long as it was observed recently, even
 * if it was missed for a few cycles.
 */
public class TrackedObjects {

  private final List<TrackedObject> trackedObjects = new ArrayList<>();
  private int nextId = 0;

  /**
   * Process a new batch of observations, updating the tracked object list.
   *
   * <p>Call this once per robot loop with all observations from all unread results since the last
   * loop.
   *
   * @param observations All observations from this cycle (may span multiple camera results)
   * @param currentTimestamp Current FPGA timestamp in seconds
   */
  public void update(ObjectObservation[] observations, double currentTimestamp) {
    Set<TrackedObject> matchedThisCycle = new HashSet<>();

    for (var observation : observations) {

      TrackedObject bestMatch = findBestMatch(observation, matchedThisCycle);

      if (bestMatch != null) {
        // Found a match; update it with this more current observation
        bestMatch.update(observation);
        matchedThisCycle.add(bestMatch);
      } else {
        // No match found — create a new tracked object
        var newObject = new TrackedObject(nextId++, observation);
        trackedObjects.add(newObject);
      }
    }

    // Increment miss counter for unmatched tracked objects
    for (var trackedObject : trackedObjects) {
      if (!matchedThisCycle.contains(trackedObject)) {
        trackedObject.consecutiveMisses++;
      }
    }

    // Remove stale objects
    trackedObjects.removeIf(obj -> obj.isStale(currentTimestamp));
  }

  /** Returns all tracked objects including unconfirmed ones (useful for debugging). */
  public List<TrackedObject> getAllObjects() {
    return List.copyOf(trackedObjects);
  }

  /**
   * Returns only confirmed tracked objects, suitable for publishing to AdvantageScope. An object is
   * confirmed once it has been observed MIN_HITS_TO_CONFIRM times.
   */
  public List<TrackedObject> getConfirmedObjects() {
    return trackedObjects.stream().filter(obj -> obj.confirmed).toList();
  }

  /**
   * Returns confirmed field poses as an array, convenient for logging to AdvantageKit and
   * displaying in AdvantageScope as a 3D field layer.
   */
  public Pose3d[] getConfirmedFieldPoses() {
    return getConfirmedObjects().stream().map(obj -> obj.fieldPose).toArray(Pose3d[]::new);
  }

  /**
   * Returns confirmed robot relative poses as an array, convenient for logging to AdvantageKit and
   * displaying in AdvantageScope as a 3D field layer.
   */
  public Pose3d[] getConfirmedRobotRelativePoses() {
    return getConfirmedObjects().stream().map(obj -> obj.robotRelativePose).toArray(Pose3d[]::new);
  }

  /**
   * Returns only unconfirmed tracked objects, suitable for publishing to AdvantageScope. An object
   * is unconfirmed if it has not yet been observed MIN_HITS_TO_CONFIRM times.
   */
  public List<TrackedObject> getUnconfirmedObjects() {
    return trackedObjects.stream().filter(obj -> !obj.confirmed).toList();
  }

  /**
   * Returns unconfirmed field poses as an array, convenient for logging to AdvantageKit and
   * displaying in AdvantageScope as a 3D field layer.
   */
  public Pose3d[] getUnconfirmedFieldPoses() {
    return getUnconfirmedObjects().stream().map(obj -> obj.fieldPose).toArray(Pose3d[]::new);
  }

  /**
   * Returns unconfirmed robot relative poses as an array, convenient for logging to AdvantageKit
   * and displaying in AdvantageScope as a 3D field layer.
   */
  public Pose3d[] getUnconfirmedRobotRelativePoses() {
    return getUnconfirmedObjects().stream()
        .map(obj -> obj.robotRelativePose)
        .toArray(Pose3d[]::new);
  }

  /**
   * Find the best matching existing tracked object for a new observation.
   *
   * @param observation New observation to be tested against existing tracked objects
   * @param matchedThisCycle Set of objects matched during this cycle
   * @return best matched tracked object or null if no suitable match found
   */
  
  private TrackedObject findBestMatch(
      ObjectObservation observation, Set<TrackedObject> matchedThisCycle) {

    TrackedObject bestMatch = null;
    double bestDistance = ObjectVisionConstants.MATCH_DISTANCE_METERS;

    // See if this observation is close to one of the unmatched tracked objects
    for (var trackedObject : trackedObjects) {

      // Must be the same class of object
      if (trackedObject.classId != observation.classId()) continue;

      // See if trackedOjbect and observation are close
      double distance = trackedObject.distanceTo(observation.fieldPose());
      if (distance < bestDistance) {
        bestDistance = distance;
        bestMatch = trackedObject;
      }
    }

    return bestMatch;
  }
}
