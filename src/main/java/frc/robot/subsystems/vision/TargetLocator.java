package frc.robot.subsystems.vision;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Translation2d;
import java.util.List;
import java.util.Optional;

/** Base class for finding a target location given a set of detected objects */
public class TargetLocator {

  protected Translation2d lockedTarget;

  public TargetLocator() {
    this.lockedTarget = null;
  }

  /**
   * Selects and locks a target. Call this when the assist command begins.
   *
   * @param robotPose current field-relative robot pose
   * @param detectedObjects field-relative positions of detected objects
   */
  public Optional<Translation2d> selectTarget(
      Pose2d robotPose, List<Translation2d> detectedObjects) {

    return Optional.empty();
  }

  /** Returns the previously selected target without changing it. */
  public Optional<Translation2d> getLockedTarget() {
    return Optional.ofNullable(lockedTarget);
  }

  public void clearTarget() {
    lockedTarget = null;
  }
}
