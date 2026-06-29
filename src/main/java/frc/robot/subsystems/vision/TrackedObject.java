// This class and related object-tracking classes were were initially created by AI Claude
// resulting from the following prompt:
/*
 * Related to object detection. I’d like to have a class called TrackedObjects
 * which receives the observations from the object detection pipeline and maintains
 * a list of objects that are reliably in the field of view. One issue is that often
 * a given ball will be detected one cycle, then not detected, then detected again, etc.
 * this causes its display in advantagescope to flicker.  Id like the ball to be
 * considered detected even if it is not every cycle, as long as there has been a fairly
 * recent observation.  Moving balls willbe an issue.  Any thoughts on the algorithm for
 * this class? By the way, currently i am only processing the most recent event but i
 * think in conjunction with this TracedObjects class i should process them all and rely
 * on timestamps
 */

package frc.robot.subsystems.vision;

import edu.wpi.first.math.geometry.Pose3d;
import frc.robot.subsystems.vision.ObjectVisionIO.ObjectObservation;

/** Represents a single object being tracked across multiple observation cycles. */
public class TrackedObject {

  public final int classId;
  public final int id; // Unique ID for this tracked object

  public Pose3d fieldPose; // Current best estimate of field position
  public double lastSeenTimestamp;
  public double firstSeenTimestamp;
  public int consecutiveMisses;
  public int hitCount;
  public boolean confirmed; // True once seen MIN_HITS_TO_CONFIRM times

  public TrackedObject(int id, ObjectObservation obs) {
    this.id = id;
    this.classId = obs.classId();
    this.fieldPose = obs.fieldPose();
    this.lastSeenTimestamp = obs.timestamp();
    this.firstSeenTimestamp = obs.timestamp();
    this.consecutiveMisses = 0;
    this.hitCount = 1;
    this.confirmed = false;

    if (hitCount >= ObjectVisionConstants.MIN_HITS_TO_CONFIRM) {
      confirmed = true;
    }
  }

  /** Update this tracked object with a new matching observation. */
  public void update(ObjectObservation obs) {
    // Blend old and new pose to smooth out noise
    fieldPose = blendPose(fieldPose, obs.fieldPose(), ObjectVisionConstants.BLEND_ALPHA);
    lastSeenTimestamp = obs.timestamp();
    consecutiveMisses = 0;
    hitCount++;

    if (hitCount >= ObjectVisionConstants.MIN_HITS_TO_CONFIRM) {
      confirmed = true;
    }
  }

  /** Returns true if this object has not been seen recently enough to keep. */
  public boolean isStale(double currentTimestamp) {
    return (currentTimestamp - lastSeenTimestamp) > ObjectVisionConstants.MAX_STALENESS_SECONDS;
  }

  /** Returns the distance from this tracked object's pose to an observed pose. */
  public double distanceTo(Pose3d other) {
    return fieldPose.getTranslation().getDistance(other.getTranslation());
  }

  private static Pose3d blendPose(Pose3d current, Pose3d observed, double alpha) {
    // Linear interpolation between current and observed
    // alpha=0 keeps current, alpha=1 snaps to observed
    var translation = current.getTranslation().interpolate(observed.getTranslation(), alpha);
    var rotation = current.getRotation().interpolate(observed.getRotation(), alpha);
    return new Pose3d(translation, rotation);
  }
}
